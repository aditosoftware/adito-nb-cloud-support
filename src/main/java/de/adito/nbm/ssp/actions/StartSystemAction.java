package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.ISSPFacade;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;

/**
 * @author m.kaspera, 21.10.2020
 */
@ActionID(category = "adito/aods", id = "de.adito.nbm.ssp.actions.StartSystemAction")
@ActionRegistration(displayName = "")
@ActionReference(path = "de/adito/aod/action/system", position = 524)
public class StartSystemAction extends NodeAction implements IContextMenuAction
{
  @Override
  protected void performAction(Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    if (systemInfo != null)
    {
      ISSPFacade sspFacade = ISSPFacade.getInstance();
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null)
      {
        boolean isSystemRunning = sspFacade.isSystemRunning(jwt.getSubject(), jwt, systemInfo.getCloudId().blockingFirst(""));
        if (!isSystemRunning)
        {
          boolean startedSystem = sspFacade.startSystem(jwt.getSubject(), jwt, systemInfo.getCloudId().blockingFirst(""));
          if (startedSystem)
            NotificationDisplayer.getDefault().notify(NbBundle.getMessage(StartSystemAction.class, "LBL.StartSystemAction"),
                                                      NotificationDisplayer.Priority.NORMAL.getIcon(),
                                                      NbBundle.getMessage(StartSystemAction.class, "TXT.StartSystemAction.notification.success"),
                                                      null, NotificationDisplayer.Priority.NORMAL);
          else
            NotificationDisplayer.getDefault().notify(NbBundle.getMessage(StartSystemAction.class, "LBL.StartSystemAction"),
                                                      NotificationDisplayer.Priority.HIGH.getIcon(),
                                                      NbBundle.getMessage(StartSystemAction.class, "TXT.StartSystemAction.notification.failure"),
                                                      null, NotificationDisplayer.Priority.HIGH);
        }
        else
        {
          NotificationDisplayer.getDefault().notify(NbBundle.getMessage(StartSystemAction.class, "LBL.StartSystemAction"),
                                                    NotificationDisplayer.Priority.HIGH.getIcon(),
                                                    NbBundle.getMessage(StartSystemAction.class, "TXT.StartSystemAction.notification.running"),
                                                    null, NotificationDisplayer.Priority.NORMAL);
        }
      }
    }
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    if (systemInfo != null)
    {
      ISSPFacade sspFacade = ISSPFacade.getInstance();
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null)
      {
        return !sspFacade.isSystemRunning(jwt.getSubject(), jwt, systemInfo.getCloudId().blockingFirst(""));
      }
    }
    return true;
  }

  @Override
  public String getName()
  {
    return NbBundle.getMessage(StartSystemAction.class, "LBL.StartSystemAction");
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }
}
