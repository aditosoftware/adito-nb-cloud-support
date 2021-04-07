package de.adito.nbm.ssp.actions;

import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.checkout.*;
import de.adito.nbm.ssp.checkout.clist.CListObject;
import de.adito.nbm.ssp.facade.*;
import org.netbeans.api.progress.*;
import org.openide.*;
import org.openide.awt.*;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.Optional;

/**
 * Connect an existing system with a cloud system
 *
 * @author m.kaspera, 04.03.2021
 */
@ActionID(category = "adito/aods", id = "de.adito.nbm.ssp.actions.LinkSystemAction")
@ActionRegistration(displayName = "")
@ActionReference(path = "de/adito/aod/action/system", position = 526)
public class LinkSystemAction extends NodeAction implements IContextMenuAction
{

  private static final String PROGRESS_BUNDLE_KEY = "TXT.LinkSystemAction.progress";

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
      JPanel optionsPanel = new JPanel(new BorderLayout(0, 5));
      JSeparator separator = new JSeparator();
      optionsPanel.add(separator, BorderLayout.NORTH);
      JCheckBox loadConfigsCB = new JCheckBox(NbBundle.getMessage(LinkSystemAction.class, "TXT.LinkSystemAction.downloadCB"));
      loadConfigsCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
      optionsPanel.add(loadConfigsCB, BorderLayout.SOUTH);
      optionsPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
      borderPanel.add(optionsPanel, BorderLayout.SOUTH);
      borderPanel.setPreferredSize(new Dimension(500, 375));
      DialogDescriptor dialogDescriptor = new DialogDescriptor(borderPanel, NbBundle.getMessage(LinkSystemAction.class, "LBL.LinkSystemAction.title"));
      DialogDisplayer dialogDisplayer = DialogDisplayer.getDefault();
      Dialog dialog = dialogDisplayer.createDialog(dialogDescriptor);
      dialog.setResizable(true);
      dialog.setMinimumSize(new Dimension(250, 50));
      dialog.pack();
      dialog.setVisible(true);

      CListObject selectedObject = panel.getSelected();
      if (dialogDescriptor.getValue() == DialogDescriptor.OK_OPTION && selectedObject != null)
      {
        _performLink(systemInfo, selectedObject.getSystemDetails(), loadConfigsCB.isSelected());
      }
    }
  }

  /**
   * @param pSystemInfo    SystemInfo representing the SystemDataModel
   * @param pSystemDetails SystemDetails for the Cloud system that should be loaded
   * @param pIsLoadConfigs true if the config files should be loaded and written, false otherwise
   */
  private void _performLink(ISystemInfo pSystemInfo, ISSPSystemDetails pSystemDetails, boolean pIsLoadConfigs)
  {
    // Already create the progressHandle to pass it to the SSPCheckoutExecutor during the task
    ProgressHandle progressHandle = ProgressHandle.createHandle(NbBundle.getMessage(LinkSystemAction.class, PROGRESS_BUNDLE_KEY));
    BaseProgressUtils.showProgressDialogAndRun(() -> {
      String result = pSystemDetails.getSystemdId();
      File projectDir = FileUtil.toFile(pSystemInfo.getProject().getProjectDirectory());
      // only do this if the user wants to also get the config files
      if (pIsLoadConfigs && _handleConfigFiles(pSystemDetails, progressHandle, projectDir))
      {
        return;
      }
      pSystemInfo.setCloudId(result);
      NbPreferences.forModule(ISystemInfo.class).put("serverAddressDefault." + projectDir.getPath().replace("\\", "/"), pSystemDetails.getUrl());
    }, progressHandle, true);
    NotificationDisplayer.getDefault().notify(NbBundle.getMessage(LinkSystemAction.class, PROGRESS_BUNDLE_KEY),
                                              NotificationDisplayer.Priority.NORMAL.getIcon(),
                                              NbBundle.getMessage(LinkSystemAction.class, "TXT.LinkSystemAction.success"), null);
  }

  /**
   * @param pSelectedSystem System that should be linked
   * @param pProgressHandle ProgressHandle to set the current task
   * @param pProjectDir     ProjectDirectory
   * @return true if the link should be cancelled, false if the link should be finished
   */
  private boolean _handleConfigFiles(ISSPSystemDetails pSelectedSystem, ProgressHandle pProgressHandle, File pProjectDir)
  {
    Object pressedButton = null;
    // If any of the config files already exist, ask the user if they should be overridden. If no, stop the link
    if (new File(pProjectDir, SSPCheckoutExecutor.DEFAULT_SERVER_CONFIG_PATH).exists() ||
        new File(pProjectDir, SSPCheckoutExecutor.DEFAULT_TUNNEL_CONFIG_PATH).exists())
    {
      pressedButton = DialogDisplayer.getDefault()
          .notify(new NotifyDescriptor.Confirmation(NbBundle.getMessage(LinkSystemAction.class, "TXT.LinkSystemAction.overrideQuestion")));
    }
    if (!NotifyDescriptor.YES_OPTION.equals(pressedButton))
    {
      NotificationDisplayer.getDefault().notify(NbBundle.getMessage(LinkSystemAction.class, PROGRESS_BUNDLE_KEY),
                                                NotificationDisplayer.Priority.NORMAL.getIcon(),
                                                NbBundle.getMessage(LinkSystemAction.class, "TXT.LinkSystemAction.aborted"), null);
      return true;
    }
    SSPCheckoutExecutor.storeConfigs(pProgressHandle, pSelectedSystem, pProjectDir,
                                     ISSPFacade.getInstance(), UserCredentialsManager.getCredentials());
    return false;
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
    return NbBundle.getMessage(LinkSystemAction.class, "LBL.LinkSystemAction");
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }
}
