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
@ActionID(category = "adito/aods", id = "de.adito.nbm.ssp.actions.StopSystemAction")
@ActionRegistration(displayName = "")
@ActionReference(path = "de/adito/aod/action/system", position = 525)
public class StopSystemAction extends NodeAction implements IContextMenuAction, IStopSystemAction
{
  @Override
  protected void performAction(Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    if (systemInfo != null)
    {
      ISSPFacade sspFacade = ISSPFacade.getInstance();
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null && sspFacade.isSystemRunning(jwt.getSubject(), jwt, systemInfo.getCloudId().blockingFirst("")))
      {
        stopSystem(systemInfo);
      }
    }
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    if (systemInfo != null)
    {
      String cloudId = systemInfo.getCloudId().blockingFirst("");
      ISSPFacade sspFacade = ISSPFacade.getInstance();
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null && !cloudId.isEmpty())
      {
        return sspFacade.isSystemRunning(jwt.getSubject(), jwt, cloudId);
      }
    }
    return false;
  }

  @Override
  public String getName()
  {
    return NbBundle.getMessage(IStopSystemAction.class, "LBL.IStopSystemAction");
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }
}
