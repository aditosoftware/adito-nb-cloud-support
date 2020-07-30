package de.adito.nbm.cloud.runconfig;

import de.adito.aditoweb.logging.colorsupport.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.tunnel.*;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.nbm.icons.MissingIcon;
import de.adito.nbm.runconfig.api.*;
import de.adito.nbm.runconfig.spi.IActiveConfigComponentProvider;
import de.adito.observables.netbeans.*;
import de.adito.swing.icon.IconAttributes;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.*;
import org.openide.util.*;
import org.openide.windows.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 09.07.2020
 */
public class TelnetLoggerRunConfig implements IRunConfig
{

  private static final String SERVER_OUTPUT_COLOR_KEY = "nb.output.debug.foreground";
  private static final String SERVER_ERROR_COLOR_KEY = "nb.output.err.foreground";
  private static final String SERVER_OUTPUT_DEFAULT_COLOR_KEY = "nb.output.foreground";

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final ISystemInfo systemInfo;
  private final CancelAction cancelAction;
  private final StartAction startAction;
  private final ClearAction clearAction;
  private final IVaadinIconsProvider iconsProvider;
  private final IColorSupportProvider colorSupportProvider;
  private IColorSupport colorSupport = null;
  private Future<?> currentTask;
  private InputOutput inputOutput = null;
  private TelnetClient currentClient;

  public TelnetLoggerRunConfig(ISystemInfo pSystemInfo)
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
    colorSupportProvider = Lookup.getDefault().lookup(IColorSupportProvider.class);
    systemInfo = pSystemInfo;
    cancelAction = new CancelAction();
    startAction = new StartAction();
    clearAction = new ClearAction();
  }

  @NotNull
  @Override
  public Observable<Optional<IRunConfigCategory>> category()
  {
    return Observable.just(Optional.of(new TelnetLoggerRunConfigCategory()));
  }

  @NotNull
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
  public void executeAsnyc(@NotNull ProgressHandle pProgressHandle)
  {
    if (currentTask == null || currentTask.isDone())
      _startTask();
  }

  /**
   * @param pIo       InputOutput used to display the message
   * @param pMessage  message to display
   * @param pColorKey Key for the color retrieved from the UIManager. Fallback is the foreground of a disabled label
   * @throws IOException if the message cannot be printed
   */
  private static void _printlnColored(@NotNull InputOutput pIo, @Nullable IColorSupport pColorSupport, @NotNull String pMessage, @NotNull String pColorKey) throws IOException
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

  private static void _printlnColored(@NotNull InputOutput pIo, @NotNull String pMessage, @NotNull String pColorKey) throws IOException
  {
    Color color = UIManager.getColor(pColorKey);
    if (color == null)
    {
      JLabel label = new JLabel();
      label.setEnabled(false);
      color = label.getForeground();
    }
    IOColorPrint.print(pIo, pMessage + "\n", color);
  }

  /**
   * Get an icon from the iconsProvider. If the iconProvider is null or the icon cannot be found, return a default missingIcon icon
   *
   * @param pIconsProvider IconProvider
   * @param pVaadinIcon    Icon to retrieve
   * @return Icon, or an icon representing a missing icon
   */
  @NotNull
  private static Icon _getIcon(@Nullable IVaadinIconsProvider pIconsProvider, @NotNull IVaadinIconsProvider.VaadinIcon pVaadinIcon)
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
  private void _startTask()
  {
    currentTask = executorService.submit(() -> {
      _initIo();
      try
      {
        _startTunnels(systemInfo);
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
      catch (InterruptedException pE)
      {
        // return means the task is done -> No need to rethrow or re-interrupt the thread
        return;
      }
      try
      {
        _runTelnetClient();
      }
      catch (InterruptedIOException interrupt)
      {
        // If task is interrupted -> exit, so do nothing here
      }
      catch (IOException pE)
      {
        // print exception on the error stream of the io
        try
        {
          _printlnColored(inputOutput, colorSupport, ExceptionUtils.getStackTrace(pE), SERVER_ERROR_COLOR_KEY);
        }
        catch (IOException ignored)
        {
        }
      }
      cancelAction.setEnabled(false);
      startAction.setEnabled(true);
    });
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

      _printlnColored(inputOutput, "Connected to server", SERVER_OUTPUT_COLOR_KEY);
      BufferedReader reader = new BufferedReader(new InputStreamReader(currentClient.getInputStream()));
      String readLine = "";
      while (readLine != null)
      {
        readLine = reader.readLine();
        if (readLine != null)
        _printlnColored(inputOutput, colorSupport, readLine, SERVER_OUTPUT_DEFAULT_COLOR_KEY);
      }
    }
    else
    {
      _printlnColored(inputOutput, "Either Telnet logging is disabled in the Instance Configuration, or the external server address or port are not set. Shutting down",
                      SERVER_OUTPUT_COLOR_KEY);
    }
  }

  /**
   * gets an InputOutput from netbeans, if one is already in use
   */
  private void _initIo()
  {
    // re-use the io if it is already initialized
    if (inputOutput == null || inputOutput.isClosed())
    {
      inputOutput = getIO("Cloud Server: " + systemInfo.getSystemName().blockingFirst(""), startAction, cancelAction, clearAction);
    }
    else
    {
      try
      {
        inputOutput.getOut().reset();
      }
      catch (IOException pE)
      {
        Logger.getLogger(TelnetLoggerRunConfig.class.getName()).log(Level.WARNING, pE, () -> "Could not reset IO");
      }
    }
    inputOutput.setOutputVisible(true);
    if (colorSupportProvider != null)
    {
      colorSupport = colorSupportProvider.getColorSupport(inputOutput);
    }
  }

  /**
   * Start all non-running tunnels of the System
   *
   * @param pSystemInfo SystemInfo from which to get the tunnels of the system
   * @throws InterruptedException if the Thread is interrupted while waiting for the tunnels to finish connecting
   */
  private void _startTunnels(@NotNull ISystemInfo pSystemInfo) throws InterruptedException, IOException
  {
    _printlnColored(inputOutput, "Starting all tunnels that are not running", SERVER_OUTPUT_COLOR_KEY);
    ISSHTunnelProvider tunnelProvider = Lookup.getDefault().lookup(ISSHTunnelProvider.class);
    List<ISSHTunnel> tunnels = pSystemInfo.observeTunnelConfigs().blockingFirst(List.of())
        .stream()
        .filter(Objects::nonNull)
        .map(tunnelProvider::createTunnel)
        .filter(pSSHTunnel -> !pSSHTunnel.isConnected())
        .collect(Collectors.toList());
    List<ISSHTunnel> failedTunnels = new ArrayList<>();
    List<Pair<ISSHTunnel, Future<String>>> tunnelTasks = new ArrayList<>();
    for (ISSHTunnel tunnel : tunnels)
    {
      tunnelTasks.add(Pair.of(tunnel, tunnel.connect()));
    }
    _awaitFinish(tunnelTasks, failedTunnels);
    _checkTunnelStatus(tunnels, failedTunnels);
  }

  /**
   * Checks the Status of then tunnels, and prints a message that depends on the number of failed and available tunnels
   *
   * @param pTunnels       list of all tunnels that were disconnected
   * @param pFailedTunnels list of tunnels that failed to connect
   * @throws IOException if the message cannot be printed
   */
  private void _checkTunnelStatus(@NotNull List<ISSHTunnel> pTunnels, @NotNull List<ISSHTunnel> pFailedTunnels) throws IOException
  {
    if (!pFailedTunnels.isEmpty())
    {
      pFailedTunnels.forEach(pTunnel -> inputOutput.getErr().println(String.format("Could not connect to tunnel %s, see IDE Log for details", pTunnel)));
      _printlnColored(inputOutput, "Trying to connect to server nonetheless", SERVER_OUTPUT_COLOR_KEY);
    }
    else if (!pTunnels.isEmpty())
    {
      _printlnColored(inputOutput, "Tunnels are connected, connecting to server", SERVER_OUTPUT_COLOR_KEY);
    }
    else
    {
      _printlnColored(inputOutput, "No disconnected Tunnels found, connecting to server", SERVER_OUTPUT_COLOR_KEY);
    }
  }

  /**
   * Waits until all Futures in the list are finished
   *
   * @param pTunnelTaskPairs List of tasks for which to wait for termination
   * @throws InterruptedException when the Thread is interrupted while waiting
   */
  private static void _awaitFinish(@NotNull List<Pair<ISSHTunnel, Future<String>>> pTunnelTaskPairs, @NotNull List<ISSHTunnel> failedTunnels) throws InterruptedException
  {
    for (Pair<ISSHTunnel, Future<String>> tunnelTaskPair : pTunnelTaskPairs)
    {
      try
      {
        tunnelTaskPair.second().get();
      }
      catch (ExecutionException ignored)
      {
        failedTunnels.add(tunnelTaskPair.first());
      }
    }
  }

  /**
   * cancels the current task
   */
  private void _cancelTask()
  {
    currentTask.cancel(true);
    executorService.submit(() -> {
      try
      {
        currentClient.disconnect();
        _printlnColored(inputOutput, "Disconnected from server", SERVER_OUTPUT_COLOR_KEY);
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
    cancelAction.setEnabled(false);
    startAction.setEnabled(true);
  }

  /**
   * Cleans up the given adress or maps it to a resolveable address if the adress is viable but non-resolveable
   *
   * @param pAddress adress
   * @return sanitized adress
   */
  private String _sanitizeAddress(@NotNull String pAddress)
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
  @NotNull
  protected InputOutput getIO(@NotNull String pTitle, Action... pActions)
  {
    InputOutput io = IOProvider.getDefault().getIO(pTitle, pActions);
    io.setErrSeparated(false);
    io.setOutputVisible(true);
    io.setErrVisible(true);
    io.select();

    return io;
  }

  /**
   * Action for cancelling the current telnet connection. Also contains a fitting icon
   */
  private class CancelAction extends AbstractAction
  {

    public CancelAction()
    {
      super("Cancel", _getIcon(iconsProvider, IVaadinIconsProvider.VaadinIcon.STOP));
      putValue(Action.SHORT_DESCRIPTION, "Disconnect");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      _cancelTask();
    }
  }

  /**
   * Action for starting up the telnet connection, contains a fitting icon
   */
  private class StartAction extends AbstractAction
  {

    public StartAction()
    {
      super("Start", _getIcon(iconsProvider, IVaadinIconsProvider.VaadinIcon.PLAY));
      putValue(Action.SHORT_DESCRIPTION, "Start");
      setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      startAction.setEnabled(false);
      cancelAction.setEnabled(true);
      if (currentTask == null || currentTask.isDone())
        _startTask();
    }
  }

  /**
   * Action that clears the current cotent of the console
   */
  private class ClearAction extends AbstractAction
  {
    public ClearAction()
    {
      super("Clear", _getIcon(iconsProvider, IVaadinIconsProvider.VaadinIcon.DATE_INPUT));
      putValue(Action.SHORT_DESCRIPTION, "Clear console");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      try
      {
        inputOutput.getOut().reset();
      }
      catch (IOException pE)
      {
        pE.printStackTrace();
      }
    }
  }
}
