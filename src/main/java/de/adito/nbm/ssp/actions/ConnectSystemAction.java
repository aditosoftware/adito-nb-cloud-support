package de.adito.nbm.ssp.actions;

import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.checkout.SSPCheckoutProjectVisualPanel1;
import de.adito.nbm.ssp.checkout.clist.CListObject;
import org.openide.*;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

/**
 * Connect an existing system with a cloud system
 *
 * @author m.kaspera, 04.03.2021
 */
@ActionID(category = "adito/aods", id = "de.adito.nbm.ssp.actions.ConnectSystemAction")
@ActionRegistration(displayName = "")
@ActionReference(path = "de/adito/aod/action/system", position = 526)
public class ConnectSystemAction extends NodeAction implements IContextMenuAction
{
  @Override
  protected void performAction(Node[] activatedNodes)
  {
    ISystemInfo systemInfo = getSystemInfoFromNodes(activatedNodes);
    if (systemInfo != null)
    {
      SSPCheckoutProjectVisualPanel1 panel = new SSPCheckoutProjectVisualPanel1();
      new Thread(panel::reloadList).start();
      JPanel borderPanel = new JPanel(new BorderLayout());
      borderPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
      borderPanel.add(panel, BorderLayout.CENTER);
      borderPanel.setPreferredSize(new Dimension(500, 375));
      DialogDescriptor dialogDescriptor = new DialogDescriptor(borderPanel, "Select System");
      DialogDisplayer dialogDisplayer = DialogDisplayer.getDefault();
      Dialog dialog = dialogDisplayer.createDialog(dialogDescriptor);
      dialog.setResizable(true);
      dialog.setMinimumSize(new Dimension(250, 50));
      dialog.pack();
      dialog.setVisible(true);

      Object pressedButtonObject = dialogDescriptor.getValue();
      if (pressedButtonObject == DialogDescriptor.OK_OPTION)
      {
        CListObject selectedObject = panel.getSelected();
        if (selectedObject != null)
        {
          String result = selectedObject.getSystemDetails().getSystemdId();
          systemInfo.setCloudId(result);
        }
      }
    }
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    return Optional.ofNullable(getSystemInfoFromNodes(activatedNodes))
        .map(pISystemInfo -> pISystemInfo.getCloudId().blockingFirst(""))
        .orElse("").isEmpty();
  }

  @Override
  public String getName()
  {
    return "Connect to SSP System";
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }
}
