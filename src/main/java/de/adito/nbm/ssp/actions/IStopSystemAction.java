package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.ISSPFacade;
import org.jetbrains.annotations.Nullable;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.NbBundle;

/**
 * @author m.kaspera, 27.10.2020
 */
public interface IStopSystemAction
{

  default void stopSystem(@Nullable ISystemInfo pSystemInfo)
  {
    if (pSystemInfo != null)
    {
      ISSPFacade sspFacade = ISSPFacade.getInstance();
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null)
      {
        boolean stoppedSystem = sspFacade.stopSystem(jwt.getSubject(), jwt, pSystemInfo.getCloudId().blockingFirst(""));
        if (stoppedSystem)
          NotificationDisplayer.getDefault().notify(NbBundle.getMessage(IStopSystemAction.class, "LBL.IStopSystemAction"),
                                                    NotificationDisplayer.Priority.NORMAL.getIcon(),
                                                    NbBundle.getMessage(IStopSystemAction.class, "TXT.IStopSystemAction.notification.success"),
                                                    null, NotificationDisplayer.Priority.NORMAL);
        else
          NotificationDisplayer.getDefault().notify(NbBundle.getMessage(IStopSystemAction.class, "LBL.IStopSystemAction"),
                                                    NotificationDisplayer.Priority.HIGH.getIcon(),
                                                    NbBundle.getMessage(IStopSystemAction.class, "TXT.IStopSystemAction.notification.failure"),
                                                    null, NotificationDisplayer.Priority.HIGH);
      }
    }
  }

}
