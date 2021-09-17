package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.actions.AbstractAsyncNodeAction;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.ISSPFacade;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;

/**
 * @author m.kaspera, 21.10.2020
 */
@ActionID(category = "adito/aods", id = "de.adito.nbm.ssp.actions.StartSystemAction")
@ActionRegistration(displayName = "")
@ActionReference(path = "de/adito/aod/action/system", position = 524)
public class StartSystemAction extends AbstractAsyncNodeAction implements IContextMenuAction, IStartSystemAction
{
  @Override
  protected void performAction(Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    doStartSystem(systemInfo);
  }

  @Override
  protected boolean enable0(@NotNull Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    if (systemInfo != null)
    {
      String cloudId = systemInfo.getCloudId().blockingFirst("");
      if (cloudId.isEmpty())
        return false;
      ISSPFacade sspFacade = ISSPFacade.getInstance();
      DecodedJWT jwt = UserCredentialsManager.getCredentials();
      if (jwt != null)
      {
        return !sspFacade.isSystemRunning(jwt.getSubject(), jwt, cloudId);
      }
    }
    return false;
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
