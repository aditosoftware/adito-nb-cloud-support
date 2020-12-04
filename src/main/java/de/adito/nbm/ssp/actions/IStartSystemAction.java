package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.ISSPFacade;
import org.jetbrains.annotations.Nullable;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.NbBundle;

/**
 * @author m.kaspera, 30.11.2020
 */
public interface IStartSystemAction
{

  default void doStartSystem(@Nullable ISystemInfo pSystemInfo)
  {
    if (pSystemInfo != null)
    {
      ISSPFacade sspFacade = ISSPFacade.getInstance();
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null)
      {
        boolean isSystemRunning = sspFacade.isSystemRunning(jwt.getSubject(), jwt, pSystemInfo.getCloudId().blockingFirst(""));
        if (!isSystemRunning)
        {
          boolean startedSystem = sspFacade.startSystem(jwt.getSubject(), jwt, pSystemInfo.getCloudId().blockingFirst(""));
          if (startedSystem)
            NotificationDisplayer.getDefault().notify(NbBundle.getMessage(IStartSystemAction.class, "LBL.StartSystemAction"),
                                                      NotificationDisplayer.Priority.NORMAL.getIcon(),
                                                      NbBundle.getMessage(IStartSystemAction.class, "TXT.StartSystemAction.notification.success"),
                                                      null, NotificationDisplayer.Priority.NORMAL);
          else
            NotificationDisplayer.getDefault().notify(NbBundle.getMessage(IStartSystemAction.class, "LBL.StartSystemAction"),
                                                      NotificationDisplayer.Priority.HIGH.getIcon(),
                                                      NbBundle.getMessage(IStartSystemAction.class, "TXT.StartSystemAction.notification.failure"),
                                                      null, NotificationDisplayer.Priority.HIGH);
        }
        else
        {
          NotificationDisplayer.getDefault().notify(NbBundle.getMessage(IStartSystemAction.class, "LBL.StartSystemAction"),
                                                    NotificationDisplayer.Priority.HIGH.getIcon(),
                                                    NbBundle.getMessage(IStartSystemAction.class, "TXT.StartSystemAction.notification.running"),
                                                    null, NotificationDisplayer.Priority.NORMAL);
        }
      }
    }
  }

}
