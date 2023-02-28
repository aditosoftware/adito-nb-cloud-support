package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.*;
import de.adito.notification.INotificationFacade;
import org.jetbrains.annotations.Nullable;
import org.openide.util.NbBundle;

import java.util.List;

/**
 * @author m.kaspera, 27.10.2020
 */
public interface IStopSystemAction
{

  default void stopSystem(@Nullable ISystemInfo pSystemInfo)
  {
    if (pSystemInfo != null)
    {
      String stopSystem = NbBundle.getMessage(IStopSystemAction.class, "LBL.StopSystem.doStop");
      Object userSelection = ICloudNotificationFacade.getInstance().notifyUser(NbBundle.getMessage(IStopSystemAction.class, "LBL.StopSystem.question"),
                                                                               NbBundle.getMessage(IStopSystemAction.class, "TITLE.StopSystem"),
                                                                               List.of(NbBundle.getMessage(IStopSystemAction.class, "LBL.StopSystem.cancel"), stopSystem));
      if (userSelection.equals(stopSystem))
      {
        ISSPFacade sspFacade = ISSPFacade.getInstance();
        DecodedJWT jwt = UserCredentialsManager.getCredentials();
        if (jwt != null)
        {
          boolean stoppedSystem = sspFacade.stopSystem(jwt.getSubject(), jwt, pSystemInfo.getCloudId().blockingFirst(""));
          if (stoppedSystem)
            INotificationFacade.INSTANCE.notify(NbBundle.getMessage(IStopSystemAction.class, "LBL.IStopSystemAction"),
                                                NbBundle.getMessage(IStopSystemAction.class, "TXT.IStopSystemAction.notification.success"),
                                                true);
          else
            INotificationFacade.INSTANCE.notify(NbBundle.getMessage(IStopSystemAction.class, "LBL.IStopSystemAction"),
                                                NbBundle.getMessage(IStopSystemAction.class, "TXT.IStopSystemAction.notification.failure"),
                                                false);
        }
      }
    }
  }

}
