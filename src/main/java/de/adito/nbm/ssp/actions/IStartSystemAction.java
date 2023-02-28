package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.ISSPFacade;
import de.adito.notification.INotificationFacade;
import org.jetbrains.annotations.Nullable;
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
            INotificationFacade.INSTANCE.notify(NbBundle.getMessage(IStartSystemAction.class, "LBL.StartSystemAction"),
                                                NbBundle.getMessage(IStartSystemAction.class, "TXT.StartSystemAction.notification.success"),
                                                true);
          else
            INotificationFacade.INSTANCE.notify(NbBundle.getMessage(IStartSystemAction.class, "LBL.StartSystemAction"),
                                                NbBundle.getMessage(IStartSystemAction.class, "TXT.StartSystemAction.notification.failure"),
                                                false);
        }
        else
        {
          INotificationFacade.INSTANCE.notify(NbBundle.getMessage(IStartSystemAction.class, "LBL.StartSystemAction"),
                                              NbBundle.getMessage(IStartSystemAction.class, "TXT.StartSystemAction.notification.running"),
                                              true);
        }
      }
    }
  }

}
