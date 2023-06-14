package de.adito.nbm.cloud.runconfig;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.aditoweb.logging.colorsupport.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.tunnel.*;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.nbm.cloud.runconfig.options.CloudPluginOptionsPanel;
import de.adito.nbm.icons.MissingIcon;
import de.adito.nbm.runconfig.api.*;
import de.adito.nbm.runconfig.spi.IActiveConfigComponentProvider;
import de.adito.nbm.ssp.actions.*;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.ISSPFacade;
import de.adito.observables.netbeans.*;
import de.adito.swing.icon.IconAttributes;
import de.adito.util.reactive.backpressure.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.*;
import org.openide.*;
import org.openide.util.*;
import org.openide.windows.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 09.07.2020
 */
public class TelnetLoggerRunConfig implements IRunConfig
{

  public static final String BACKPRESSURE_INITIAL_SIZE = "de.adito.cloud.backpressure.buffersize";
  public static final int BACKPRESSURE_DEFAULT_BUFFER_SIZE = 256;
  private static final String SERVER_OUTPUT_COLOR_KEY = "nb.output.debug.foreground";
  private static final String SERVER_ERROR_COLOR_KEY = "nb.output.err.foreground";
  private static final String SERVER_OUTPUT_DEFAULT_COLOR_KEY = "nb.output.foreground";
  private static final String TUNNEL_TIMEOUT_PROPERTY = "de.adito.cloud.tunnel.timeout";
  private static final int MAX_TIME_TRIES_IN_MS = 2 * 60 * 1000;
  private static final int TIME_BETWEEN_TRIES_IN_MS = 5000;
  private static final int MAX_NUM_TRIES = MAX_TIME_TRIES_IN_MS / TIME_BETWEEN_TRIES_IN_MS;
  private static final Logger LOGGER = Logger.getLogger(TelnetLoggerRunConfig.class.getName());

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final ISystemInfo systemInfo;
  private final StartAction startAction;
  private final StopSystemAction stopSystemAction;
  private final IVaadinIconsProvider iconsProvider;
  private final IColorSupportProvider colorSupportProvider;
  private final CompositeDisposable disposable = new CompositeDisposable();
  private IColorSupport colorSupport = null;
  private Future<?> currentTask;
  private InputOutput inputOutput = null;
  private TelnetClient currentClient;

  public TelnetLoggerRunConfig(ISystemInfo pSystemInfo)
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
    colorSupportProvider = Lookup.getDefault().lookup(IColorSupportProvider.class);
    systemInfo = pSystemInfo;
    startAction = new StartAction();
    stopSystemAction = new StopSystemAction();
  }

  @NonNull
  @Override
  public Observable<Optional<IRunConfigCategory>> category()
  {
    return Observable.just(Optional.of(new TelnetLoggerRunConfigCategory()));
  }

  @NonNull
  @Override
  public Observable<String> displayName()
  {
    return OpenProjectsObservable.create()
        .switchMap(pProjects -> {
          if (pProjects.size() > 1) // Nur wenn mind. 2 Projekte offen wird das Projekt angezeigt, sonst sinnlos
          {
            Project myProject = systemInfo.getProject();
            if (myProject != null)
            {
              return ProjectObservable.createInfos(myProject)
                  .map(ProjectInformation::getDisplayName)
                  .map(pName -> " (" + pName + ")");
            }
          }
          return Observable.just("");
        })
        .to(pProjectNameObs -> Observable.combineLatest(pProjectNameObs, systemInfo.getSystemName(), (pProjName, pDataObjName) ->
            "Cloud Server" + IActiveConfigComponentProvider.DISPLAY_NAME_SEPARATOR + pDataObjName + pProjName));
  }

  @Override
  public void executeAsnyc(@NonNull ProgressHandle pProgressHandle)
  {
    if (currentTask == null || currentTask.isDone())
    {
      DecodedJWT credentials = UserCredentialsManager.getCredentials();
      String cloudId = systemInfo.getCloudId().blockingFirst("");
      if (cloudId.isEmpty() || credentials == null)
        return;
      boolean systemRunning = ISSPFacade.getInstance().isSystemRunning(credentials.getSubject(), credentials, cloudId);
      _initIo(systemRunning);
      if (systemRunning)
        _startTask(credentials, cloudId);
    }
  }

  /**
   * @param pIo       InputOutput used to display the message
   * @param pMessage  message to display
   * @param pColorKey Key for the color retrieved from the UIManager. Fallback is the foreground of a disabled label
   * @throws IOException if the message cannot be printed
   */
  private static void _printlnColored(@NonNull InputOutput pIo, @Nullable IColorSupport pColorSupport, @NonNull String pMessage, @NonNull String pColorKey) throws IOException
  {
    if (pColorSupport != null)
    {
      pColorSupport.colorPrint(pMessage + "\n");
    }
    else
    {
      _printlnColored(pIo, pMessage, pColorKey);
    }
  }

  private static void _printlnColored(@NonNull InputOutput pIo, @NonNull String pMessage, @NonNull String pColorKey) throws IOException
  {
    _printColored(pIo, pMessage + "\n", pColorKey);
  }

  private static void _printColored(@NonNull InputOutput pIo, @NonNull String pMessage, @NonNull String pColorKey) throws IOException
  {
    Color color = UIManager.getColor(pColorKey);
    if (color == null)
    {
      JLabel label = new JLabel();
      label.setEnabled(false);
      color = label.getForeground();
    }
    IOColorPrint.print(pIo, pMessage, color);
  }

  /**
   * Get an icon from the iconsProvider. If the iconProvider is null or the icon cannot be found, return a default missingIcon icon
   *
   * @param pIconsProvider IconProvider
   * @param pVaadinIcon    Icon to retrieve
   * @return Icon, or an icon representing a missing icon
   */
  @NonNull
  private static Icon _getIcon(@Nullable IVaadinIconsProvider pIconsProvider, @NonNull IVaadinIconsProvider.VaadinIcon pVaadinIcon)
  {
    if (pIconsProvider == null)
      return MissingIcon.get16x16();
    Image image = pIconsProvider.findImage(pVaadinIcon, new IconAttributes.Builder().create());
    if (image == null)
      return MissingIcon.get16x16();
    else return new ImageIcon(image);
  }

  /**
   * starts the tunnels and runs the client as a task submitted to the executorService
   */
  private void _startTask(@NonNull DecodedJWT pCredentials, @NonNull String pCloudId)
  {
    currentTask = executorService.submit(() -> {
      try
      {
        _waitForSystemStart(pCredentials, pCloudId);
        _startTunnels(systemInfo);
      }
      catch (IOException pE)
      {
        try
        {
          _printlnColored(inputOutput, colorSupport, ExceptionUtils.getStackTrace(pE), SERVER_ERROR_COLOR_KEY);
        }
        catch (IOException pIOException)
        {
          Logger.getLogger(TelnetLoggerRunConfig.class.getName()).log(Level.SEVERE, pIOException, () -> NbBundle.getMessage(TelnetLoggerRunConfig.class,
                                                                                                                            "TXT.TelnetLoggerRunConfig.error.io"));
        }
      }
      catch (InterruptedException pE)
      {
        // return means the task is done -> No need to rethrow or re-interrupt the thread
        Logger.getLogger(TelnetLoggerRunConfig.class.getName()).log(Level.SEVERE, () -> NbBundle.getMessage(TelnetLoggerRunConfig.class,
                                                                                                            "TXT.TelnetLoggerRunConfig.interrupt"));
        return;
      }
      try
      {
        _runTelnetClient();
      }
      catch (InterruptedIOException interrupt)
      {
        Logger.getLogger(TelnetLoggerRunConfig.class.getName()).log(Level.SEVERE, () -> NbBundle.getMessage(TelnetLoggerRunConfig.class,
                                                                                                            "TXT.TelnetLoggerRunConfig.interrupt"));
        // If task is interrupted -> exit, so do nothing here
      }
      catch (IOException pE)
      {
        // print exception on the error stream of the io
        try
        {
          _printlnColored(inputOutput, colorSupport, ExceptionUtils.getStackTrace(pE), SERVER_ERROR_COLOR_KEY);
        }
        catch (IOException pIOException)
        {
          Logger.getLogger(TelnetLoggerRunConfig.class.getName()).log(Level.SEVERE, pIOException, () -> NbBundle.getMessage(TelnetLoggerRunConfig.class,
                                                                                                                            "TXT.TelnetLoggerRunConfig.error.io"));
        }
      }
    });
  }

  private void _printInfo(boolean pSystemRunning)
  {
    try
    {
      Thread.sleep(50);
    }
    catch (InterruptedException pE)
    {
      // nothing, the sleep is only here so the first part
    }
    try
    {
      _printlnColored(inputOutput, colorSupport,
                      "// ---------------------------------------------------------------------------------------------------------------------------\n" +
                          "//\n" +
                          "//     _    ____ ___ _____ ___     ____ _                 _   ____            _                 \n" +
                          "//    / \\  |  _ \\_ _|_   _/ _ \\   / ___| | ___  _   _  __| | / ___| _   _ ___| |_ ___ _ __ ___  \n" +
                          "//   / _ \\ | | | | |  | || | | | | |   | |/ _ \\| | | |/ _` | \\___ \\| | | / __| __/ _ \\ '_ ` _ \\ \n" +
                          "//  / ___ \\| |_| | |  | || |_| | | |___| | (_) | |_| | (_| |  ___) | |_| \\__ \\ ||  __/ | | | | |\n" +
                          "// /_/   \\_\\____/___| |_| \\___/   \\____|_|\\___/ \\__,_|\\__,_| |____/ \\__, |___/\\__\\___|_| |_| |_|\n" +
                          "//                                                                  |___/                       \n" +
                          "//\n" +
                          "// ---------------------------------------------------------------------------------------------------------------------------\n",
                      SERVER_OUTPUT_COLOR_KEY);
      _printColored(inputOutput, "//\n//   Current system status: ", SERVER_OUTPUT_COLOR_KEY);
      IOColorPrint.print(inputOutput, pSystemRunning ? "Running" : "Stopped", pSystemRunning ? Color.GREEN : Color.RED);
      _printlnColored(inputOutput, "\n//\n// ---------------------------------------------------------------------------------------------------------------------------",
                      SERVER_OUTPUT_COLOR_KEY);
    }
    catch (IOException pE)
    {
      LOGGER.log(Level.WARNING, pE, () -> "Could not print system Info");
    }
  }

  private void _waitForSystemStart(DecodedJWT pCredentials, String pCloudId) throws InterruptedException, IOException
  {
    int tries = 0;
    while (tries < MAX_NUM_TRIES && !ISSPFacade.getInstance().isSystemRunning(pCredentials.getSubject(), pCredentials, pCloudId))
    {
      tries++;
      try
      {
        // for the first time write the waiting statement, for all the other loops just append a . so the user knows something is still happening,
        // but it isn't too intrusive
        _printColored(inputOutput, tries == 0 ? "// Waiting for the SSP system to start" : ".", SERVER_OUTPUT_COLOR_KEY);
        Thread.sleep(TIME_BETWEEN_TRIES_IN_MS);
      }
      catch (IOException pE)
      {
        // nothing, just recheck if the system is up now
      }
    }
    // add a newline so the next output starts on a fresh line and not appended to the end of the "Waiting..." line
    if (tries > 0)
      _printlnColored(inputOutput, "", SERVER_OUTPUT_COLOR_KEY);
  }

  /**
   * Initiates the telnet client connection and writes the sent information on the inputOutput, as long as the client is connected
   *
   * @throws IOException if an error occurrs while reading from the telnet client
   */
  private void _runTelnetClient() throws IOException
  {
    if (Boolean.parseBoolean(systemInfo.getParameters().get(ISystemInfo.TELNET_LOGGING_ENABLED_KEY)) &&
        systemInfo.getParameters().get(ISystemInfo.TELNET_HOST_EXTERNAL_ADRESS_KEY) != null &&
        systemInfo.getParameters().get(ISystemInfo.TELNET_PORT_KEY) != null)
    {
      currentClient = new TelnetClient();
      currentClient.connect(_sanitizeAddress(systemInfo.getParameters().get(ISystemInfo.TELNET_HOST_EXTERNAL_ADRESS_KEY)),
                            Integer.parseInt(systemInfo.getParameters().get(ISystemInfo.TELNET_PORT_KEY)));

      _printlnColored(inputOutput, "// Connected to server", SERVER_OUTPUT_COLOR_KEY);
      BufferedReader reader = new BufferedReader(new InputStreamReader(currentClient.getInputStream()));
      Flowable<String> logs = FlowableFromReader.create(reader)
          .doOnComplete(() -> _printlnColored(inputOutput, NbBundle.getMessage(TelnetLoggerRunConfig.class, "TXT.TelnetLoggerRunConfig.completed"),
                                              SERVER_OUTPUT_DEFAULT_COLOR_KEY))
          .doOnError(pE -> _printlnColored(inputOutput, NbBundle.getMessage(TelnetLoggerRunConfig.class, "TXT.TelnetLoggerRunConfig.completed.error",
                                                                            pE.getMessage(), Arrays.toString(pE.getStackTrace())), SERVER_ERROR_COLOR_KEY));
      Flowable<String> flowable = logs.onBackpressureDrop();
      BackpressureSubscriber<String> backpressureSubscriber =
          new BackpressureSubscriber.Builder<String>(pReadLine -> _printlnColored(inputOutput, colorSupport, pReadLine, SERVER_OUTPUT_DEFAULT_COLOR_KEY))
              .setInitialRequests(NbPreferences.forModule(CloudPluginOptionsPanel.class)
                                      .getInt(BACKPRESSURE_INITIAL_SIZE, BACKPRESSURE_DEFAULT_BUFFER_SIZE))
              .setRequestsBatchSize(1)
              .create();
      flowable.observeOn(Schedulers.newThread()).subscribe(backpressureSubscriber);
      disposable.add(backpressureSubscriber);
    }
    else
    {
      _printlnColored(inputOutput, "// Either Telnet logging is disabled in the Instance Configuration, or the external server address or port are not set. Shutting down",
                      SERVER_OUTPUT_COLOR_KEY);
    }
  }

  /**
   * gets an InputOutput from netbeans, if one is already in use
   */
  private void _initIo(boolean pSystemRunning)
  {
    // re-use the io if it is already initialized
    if (inputOutput == null || inputOutput.isClosed())
    {
      inputOutput = getIO("Cloud Server: " + systemInfo.getSystemName().blockingFirst(""), startAction, stopSystemAction);
    }
    else
    {
      try
      {
        inputOutput.getOut().reset();
      }
      catch (IOException pE)
      {
        LOGGER.log(Level.WARNING, pE, () -> "Could not reset IO");
      }
    }
    inputOutput.setOutputVisible(true);
    if (colorSupportProvider != null)
    {
      colorSupport = colorSupportProvider.getColorSupport(inputOutput);
    }
    _printInfo(pSystemRunning);
  }

  /**
   * Start all non-running tunnels of the System
   *
   * @param pSystemInfo SystemInfo from which to get the tunnels of the system
   * @throws InterruptedException if the Thread is interrupted while waiting for the tunnels to finish connecting
   */
  private void _startTunnels(@NonNull ISystemInfo pSystemInfo) throws InterruptedException, IOException
  {
    _printlnColored(inputOutput, "// Starting all tunnels that are not running", SERVER_OUTPUT_COLOR_KEY);
    ISSHTunnelProvider tunnelProvider = Lookup.getDefault().lookup(ISSHTunnelProvider.class);
    List<ISSHTunnel> tunnels = pSystemInfo.observeTunnelConfigs().blockingFirst(List.of())
        .stream()
        .filter(Objects::nonNull)
        .map(tunnelProvider::createTunnel)
        .filter(pSSHTunnel -> !pSSHTunnel.isConnected())
        .collect(Collectors.toList());
    List<ISSHTunnel> failedTunnels = startTunnels(tunnels);
    _checkTunnelStatus(tunnels, failedTunnels);
  }

  /**
   * @param tunnels list of tunnels to start
   * @return list of tunnels that failed to properly start
   * @throws InterruptedException if the thread is interrupted while waiting for the tunnels to start
   */
  public static List<ISSHTunnel> startTunnels(List<ISSHTunnel> tunnels) throws InterruptedException
  {
    List<ISSHTunnel> failedTunnels = new ArrayList<>();
    AtomicBoolean abortFlag = new AtomicBoolean(false);
    List<Pair<ISSHTunnel, Future<String>>> tunnelTasks = new ArrayList<>();
    for (ISSHTunnel tunnel : tunnels)
    {
      tunnelTasks.add(Pair.of(tunnel, tunnel.connect(abortFlag)));
    }
    _awaitFinish(tunnelTasks, failedTunnels);
    return failedTunnels;

  }

  /**
   * Checks the Status of then tunnels, and prints a message that depends on the number of failed and available tunnels
   *
   * @param pTunnels       list of all tunnels that were disconnected
   * @param pFailedTunnels list of tunnels that failed to connect
   * @throws IOException if the message cannot be printed
   */
  private void _checkTunnelStatus(@NonNull List<ISSHTunnel> pTunnels, @NonNull List<ISSHTunnel> pFailedTunnels) throws IOException
  {
    if (!pFailedTunnels.isEmpty())
    {
      pFailedTunnels.forEach(pTunnel -> inputOutput.getErr().println(String.format("// Could not connect to tunnel %s, see IDE Log for details", pTunnel)));
      _printlnColored(inputOutput, "// Trying to connect to server nonetheless", SERVER_OUTPUT_COLOR_KEY);
    }
    else if (!pTunnels.isEmpty())
    {
      _printlnColored(inputOutput, "// Tunnels are connected, connecting to server", SERVER_OUTPUT_COLOR_KEY);
    }
    else
    {
      _printlnColored(inputOutput, "// No disconnected Tunnels found, connecting to server", SERVER_OUTPUT_COLOR_KEY);
    }
  }

  /**
   * Waits until all Futures in the list are finished
   *
   * @param pTunnelTaskPairs List of tasks for which to wait for termination
   * @throws InterruptedException when the Thread is interrupted while waiting
   */
  private static void _awaitFinish(@NonNull List<Pair<ISSHTunnel, Future<String>>> pTunnelTaskPairs, @NonNull List<ISSHTunnel> failedTunnels) throws InterruptedException
  {
    List<CompletableFuture<?>> futureFutureList = new ArrayList<>();
    for (Pair<ISSHTunnel, Future<String>> tunnelTaskPair : pTunnelTaskPairs)
    {
      futureFutureList.add(CompletableFuture.runAsync(() -> {
        try
        {
          tunnelTaskPair.second().get(Integer.parseInt(System.getProperty(TUNNEL_TIMEOUT_PROPERTY, "30000")), TimeUnit.MILLISECONDS);
        }
        catch (Exception ignored)
        {
          if (!tunnelTaskPair.first().isConnected())
            failedTunnels.add(tunnelTaskPair.first());
        }
      }));
    }
    try
    {
      CompletableFuture.allOf(futureFutureList.toArray(new CompletableFuture[0])).get();
    }
    catch (ExecutionException pE)
    {
      LOGGER.log(Level.WARNING, "Error while waiting for the tunnels to establish their connection", pE);
    }
  }

  /**
   * cancels the current task
   */
  private void _cancelTask()
  {
    currentTask.cancel(true);
    disposable.dispose();
    executorService.submit(() -> {
      try
      {
        currentClient.disconnect();
        _printlnColored(inputOutput, "// Disconnected from server", SERVER_OUTPUT_COLOR_KEY);
      }
      catch (IOException pE)
      {
        try
        {
          _printlnColored(inputOutput, colorSupport, ExceptionUtils.getStackTrace(pE), SERVER_ERROR_COLOR_KEY);
        }
        catch (IOException ignored)
        {
        }
      }
    });
  }

  /**
   * Cleans up the given adress or maps it to a resolveable address if the adress is viable but non-resolveable
   *
   * @param pAddress adress
   * @return sanitized adress
   */
  private String _sanitizeAddress(@NonNull String pAddress)
  {
    if ("0.0.0.0".equals(pAddress))
      return "localhost";
    return pAddress;
  }

  /**
   * Erzeugt das Ausgabefenster für die App
   *
   * @param pTitle   Titel
   * @param pActions Aktionen
   * @return das Ausgabefenster
   */
  @NonNull
  protected InputOutput getIO(@NonNull String pTitle, Action... pActions)
  {
    InputOutput io = IOProvider.getDefault().getIO(pTitle, pActions);
    io.setErrSeparated(false);
    io.setOutputVisible(true);
    io.setErrVisible(true);
    io.select();

    return io;
  }

  /**
   * Action for starting up the telnet connection, contains a fitting icon
   */
  private class StartAction extends AbstractAction implements IStartSystemAction
  {
    private final ISSPFacade sspFacade;
    private final String cloudSystemId;

    public StartAction()
    {
      super("Start", _getIcon(iconsProvider, IVaadinIconsProvider.VaadinIcon.PLAY));
      putValue(Action.SHORT_DESCRIPTION, "Start the cloud system");
      sspFacade = ISSPFacade.getInstance();
      cloudSystemId = systemInfo.getCloudId().blockingFirst("");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      doStartSystem(systemInfo);
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null && (currentTask == null || currentTask.isDone()))
      {
        // to start the system the system has to be shut down, else the action does not make sense -> no reason to call the webservice
        _initIo(false);
        _startTask(jwt, cloudSystemId);
        setEnabled(false);
        stopSystemAction.setEnabled(true);
      }
    }

    @Override
    public boolean isEnabled()
    {
      if (systemInfo != null)
      {
        DecodedJWT jwt = UserCredentialsManager.getCredentials();
        if (jwt != null)
        {
          return !sspFacade.isSystemRunning(jwt.getSubject(), jwt, cloudSystemId);
        }
      }
      return false;
    }
  }

  private class StopSystemAction extends AbstractAction implements IStopSystemAction
  {

    public StopSystemAction()
    {
      super("Stop Systen", _getIcon(iconsProvider, IVaadinIconsProvider.VaadinIcon.CLOSE));
      putValue(Action.SHORT_DESCRIPTION, "Stops the cloud system");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      JButton cancelButton = new JButton("Cancel");
      JButton stopSystem = new JButton("Stop system");
      JButton[] buttons = new JButton[]{cancelButton, stopSystem};
      NotifyDescriptor.Confirmation confirmation = new NotifyDescriptor.Confirmation(NbBundle.getMessage(TelnetLoggerRunConfig.class,
                                                                                                         "TXT.TelnetLoggerRunConfig.stop.system.question"),
                                                                                     "Stop SSP system");
      confirmation.setOptions(buttons);
      Object result = DialogDisplayer.getDefault().notify(confirmation);
      if (result.equals(stopSystem))
      {
        _cancelTask();
        if (systemInfo != null)
        {
          ISSPFacade sspFacade = ISSPFacade.getInstance();
          DecodedJWT jwt = UserCredentialsManager.getCredentials();
          if (jwt != null && sspFacade.isSystemRunning(jwt.getSubject(), jwt, systemInfo.getCloudId().blockingFirst("")))
          {
            stopSystem(systemInfo);
            setEnabled(false);
            startAction.setEnabled(true);
          }
        }
      }
    }

    @Override
    public boolean isEnabled()
    {
      if (systemInfo != null)
      {
        ISSPFacade sspFacade = ISSPFacade.getInstance();
        DecodedJWT jwt = UserCredentialsManager.getCredentials();
        if (jwt != null)
        {
          return sspFacade.isSystemRunning(jwt.getSubject(), jwt, systemInfo.getCloudId().blockingFirst(""));
        }
      }
      return false;
    }
  }
}
