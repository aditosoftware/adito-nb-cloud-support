package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.actions.AbstractAsyncNodeAction;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.*;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.NonNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;

/**
 * @author m.kaspera, 21.10.2020
 */
@ActionID(category = "adito/aods", id = "de.adito.nbm.ssp.actions.StopSystemAction")
@ActionRegistration(displayName = "")
@ActionReference(path = "de/adito/aod/action/system", position = 525)
public class StopSystemAction extends AbstractAsyncNodeAction implements IContextMenuAction, IStopSystemAction, Disposable
{

  private Disposable disposable;

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
  protected boolean enable0(@NonNull Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    if (systemInfo != null)
    {
      String cloudId = systemInfo.getCloudId().blockingFirst("");
      if (cloudId.isEmpty())
        return false;
      Boolean isEnabled = ISystemStatusFacade.getInstance().triggerIsSystemRunningUpdate(cloudId);
      if (disposable == null)
        disposable = ISystemStatusFacade.getInstance().getIsSystemRunningObservable(cloudId).subscribe(this::setEnabled);
      return isEnabled;
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

  @Override
  public void dispose()
  {
    if (disposable != null)
      disposable.dispose();
  }

  @Override
  public boolean isDisposed()
  {
    return disposable == null || disposable.isDisposed();
  }
}
