package de.adito.nbm.ssp.actions;

import de.adito.actions.AbstractAsyncNodeAction;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.facade.ISystemStatusFacade;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.NonNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;

/**
 * @author m.kaspera, 21.10.2020
 */
@ActionID(category = "adito/aods", id = "de.adito.nbm.ssp.actions.StartSystemAction")
@ActionRegistration(displayName = "")
@ActionReference(path = "de/adito/aod/action/system", position = 524)
public class StartSystemAction extends AbstractAsyncNodeAction implements IContextMenuAction, IStartSystemAction, Disposable
{

  private Disposable disposable;

  @Override
  protected void performAction(Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    doStartSystem(systemInfo);
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
      boolean isEnabled = !ISystemStatusFacade.getInstance().triggerIsSystemRunningUpdate(cloudId);
      if (disposable == null)
        disposable = ISystemStatusFacade.getInstance().getIsSystemRunningObservable(cloudId).subscribe(pIsEnabled -> setEnabled(!pIsEnabled));
      return isEnabled;
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
