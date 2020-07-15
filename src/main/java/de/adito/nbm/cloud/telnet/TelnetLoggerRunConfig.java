package de.adito.nbm.cloud.telnet;

import de.adito.nbm.icons.NBIconLoader;
import de.adito.nbm.runconfig.api.*;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.net.telnet.TelnetClient;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.windows.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * @author m.kaspera, 09.07.2020
 */
public class TelnetLoggerRunConfig implements IRunConfig
{

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final ISystemInfo systemInfo;
  private final NBIconLoader nbIconLoader = new NBIconLoader();
  private final CancelAction cancelAction;
  private final StartAction startAction;
  private Future<?> task = null;
  private InputOutput io = null;

  public TelnetLoggerRunConfig(ISystemInfo pSystemInfo)
  {
    systemInfo = pSystemInfo;
    cancelAction = new CancelAction();
    startAction = new StartAction();
  }

  @NotNull
  @Override
  public Observable<Optional<IRunConfigCategory>> category()
  {
    return Observable.just(Optional.of(new IRunConfigCategory()
    {
      @NotNull
      @Override
      public String getName()
      {
        return "Servers";
      }

      @NotNull
      @Override
      public Observable<String> title()
      {
        return Observable.just("Servers");
      }
    }));
  }

  @NotNull
  @Override
  public Observable<String> displayName()
  {
    return Observable.just("Cloud Server: " + systemInfo.getSystemName());
  }

  @Override
  public void executeAsnyc(@NotNull ProgressHandle pProgressHandle)
  {
    _startTask();
  }

  private void _startTask()
  {
    task = executorService.submit(() -> {
      // re-use the io if it is already initialized
      if (io == null)
        io = getIO("Cloud Server: " + systemInfo.getSystemName(), startAction, cancelAction);
      if (Boolean.parseBoolean(systemInfo.getParameters().get("loggingTelnetEnabled")) &&
          systemInfo.getParameters().get("loggingTelnetAddress") != null &&
          systemInfo.getParameters().get("loggingTelnetPort") != null)
      {
        TelnetClient telnetClient = new TelnetClient();
        try
        {
          telnetClient.connect(_sanitizeAddress(systemInfo.getParameters().get("loggingTelnetAddress")),
                               Integer.parseInt(systemInfo.getParameters().get("loggingTelnetPort")));
          BufferedReader reader = new BufferedReader(new InputStreamReader(telnetClient.getInputStream()));
          String readLine = "";
          while (readLine != null)
          {
            readLine = reader.readLine();
            io.getOut().println(readLine);
          }
        }
        catch (InterruptedIOException interrupt)
        {
          // If task is interrupted -> exit, so do nothing here
        }
        catch (IOException pE)
        {
          // print exception on the error stream of the io
          pE.printStackTrace(io.getErr());
        }
      }
      cancelAction.setEnabled(false);
      startAction.setEnabled(true);
    });
  }

  /**
   * cancels the current task
   */
  private void cancelTask()
  {
    if (task != null)
      task.cancel(true);
  }

  /**
   * Cleans up the given adress or maps it to a resolveable address if the adress is viable but non-resolveable
   *
   * @param pAddress adress
   * @return sanitized adress
   */
  private String _sanitizeAddress(String pAddress)
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
  protected InputOutput getIO(String pTitle, Action... pActions)
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
      super("Cancel", nbIconLoader.getIcon("/de/adito/nbm/icons/stop.png"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      cancelTask();
    }
  }

  /**
   * Action for starting up the telnet connection, contains a fitting icon
   */
  private class StartAction extends AbstractAction
  {

    public StartAction()
    {
      super("Start", nbIconLoader.getIcon("/de/adito/nbm/icons/start.png"));
      setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      startAction.setEnabled(false);
      _startTask();
      cancelAction.setEnabled(true);
    }
  }
}
