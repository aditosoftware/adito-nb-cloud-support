package de.adito.nbm.ssp.checkout;


import com.google.common.annotations.VisibleForTesting;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.exceptions.AditoVersioningException;
import de.adito.nbm.ssp.checkout.clist.CListObject;
import de.adito.nbm.ssp.facade.ISSPSystemDetails;
import de.adito.notification.INotificationFacade;
import de.adito.swing.IFileChooserProvider;
import org.jetbrains.annotations.*;
import org.openide.WizardDescriptor;
import org.openide.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.*;
import java.util.logging.*;

/**
 * Wizard-Panel for checking out a SSP project
 * Mode: Path selection
 *
 * @author s.danner, 13.09.13
 */
public class SSPCheckoutProjectWizardPanel2 implements WizardDescriptor.Panel<WizardDescriptor>
{
  private final ChangeSupport cs = new ChangeSupport(this);
  private JFileChooser fileChooser;
  private SSPCheckoutProjectVisualPanel2 comp;
  private WizardDescriptor wd;
  private final DefaultComboBoxModel<IRef> model = new DefaultComboBoxModel<>();

  public SSPCheckoutProjectVisualPanel2 getComponent()
  {
    if (comp == null)
    {
      createComponent();
    }
    return comp;
  }

  @Override
  public HelpCtx getHelp()
  {
    return HelpCtx.DEFAULT_HELP;
  }

  /**
   * This method determines if the "Next" button is active
   */
  @Override
  public boolean isValid()
  {
    //Validate paths and adjust message
    Object projName = wd.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_NAME);
    Object projPath = wd.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_PATH);
    return updateMessage(projName, projPath);
  }

  @Override
  public final void addChangeListener(ChangeListener pL)
  {
    cs.addChangeListener(pL);
  }

  @Override
  public final void removeChangeListener(ChangeListener pL)
  {
    cs.removeChangeListener(pL);
  }

  /**
   * This method is called when there is a switch from another panel to this one
   * The properties from the WizardDescriptor are read and treated accordingly
   */
  @Override
  public void readSettings(WizardDescriptor pSettings)
  {
    wd = pSettings;
    String projectPath = SSPCheckoutProjectWizardIterator.getCallback().getDefaultProjectPath();
    if (projectPath != null && !projectPath.isEmpty())
      wd.putProperty(SSPCheckoutProjectWizardIterator.PROJECT_PATH, projectPath);
    updateComponent();
  }

  /**
   * This method is called when there is a switch from this panel to another
   */
  @Override
  public void storeSettings(WizardDescriptor pSettings)
  {
    if (comp == null || pSettings == null)
      return;
    if (wd.getProperty(WizardDescriptor.PROP_ERROR_MESSAGE) != null)
      wd.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
  }

  /**
   * Sets the listener of all visual components
   */
  private void createComponent()
  {
    loadFileChooserAsync();

    comp = new SSPCheckoutProjectVisualPanel2();
    //Listener for the ProjectPath button
    comp.getProjectLocationBrowseButton().addActionListener(e -> {
      applyFileChooser(comp.getProjectLocationTextField());
      updateComponent();
    });

    //DocumentListener for the two textFields
    comp.getProjectNameTextField().getDocument().addDocumentListener(new DocumentUpdateListener()
    {
      @Override
      public void update(DocumentEvent e)
      {
        if (wd != null)
          wd.putProperty(SSPCheckoutProjectWizardIterator.PROJECT_NAME, getProjectName());
        cs.fireChange();
      }
    });
    comp.getProjectLocationTextField().getDocument().addDocumentListener(new DocumentUpdateListener()
    {
      @Override
      public void update(DocumentEvent e)
      {
        if (wd != null)
          wd.putProperty(SSPCheckoutProjectWizardIterator.PROJECT_PATH, getProjectPath());
        cs.fireChange();
      }
    });
    comp.getCheckoutModeSelectionButtons().forEach(pButton -> pButton.addActionListener(e -> {
      if (wd != null)
        wd.putProperty(SSPCheckoutProjectWizardIterator.CHECKOUT_MODE, getCheckoutMode());
      cs.fireChange();
    }));
    comp.getGitBranchComboBox().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED)
      {
        if (wd != null)
          wd.putProperty(SSPCheckoutProjectWizardIterator.PROJECT_GIT_BRANCH, ((IRef) e.getItem()).getName());
        cs.fireChange();
      }
    });
  }

  private void loadFileChooserAsync()
  {
    new Thread("SSPCheckoutProjectWizardPanel2.FileChooserLoader")
    {
      @Override
      public void run()
      {
        IFileChooserProvider fileChooserProvider = Lookup.getDefault().lookup(IFileChooserProvider.class);
        if (fileChooserProvider != null)
          fileChooser = fileChooserProvider.getFileChooser();
        else
          fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      }
    }.start();
  }

  /**
   * Writes the name and paths as they are in the WizardDescriptor to the fields
   */
  private void updateComponent()
  {
    if (wd == null || comp == null)
      return;


    ISSPSystemDetails projectDescription = getProjectDescription();

    Object projectName = wd.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_NAME);
    if (projectName != null)
      comp.getProjectNameTextField().setText(projectName.toString());
    else if (projectDescription != null)
      comp.getProjectNameTextField().setText(projectDescription.getName());

    Object projectPath = wd.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_PATH);
    if (projectPath != null)
      comp.getProjectLocationTextField().setText(projectPath.toString());

    CListObject cListObject = (CListObject) wd.getProperty(SSPCheckoutProjectWizardIterator.SELECTED);
    String repoUrl = cListObject == null ? null : cListObject.getSystemDetails().getGitRepoUrl();
    if (cListObject != null)
    {
      model.removeAllElements();
      if (repoUrl != null)
      {
        IRef[] getAvailableRefs = getAvailableRef(repoUrl);
        for (IRef getAvailableRef : getAvailableRefs)
          model.addElement(getAvailableRef);

        comp.getGitBranchComboBox().setModel(model);
        IRef toSet = getCurrentRef(getAvailableRefs);
        if (toSet != null)
          comp.getGitBranchComboBox().setSelectedItem(toSet);
      }
    }

    Object checkoutMode = wd.getProperty(SSPCheckoutProjectWizardIterator.CHECKOUT_MODE);
    SSPCheckoutProjectVisualPanel2.CheckoutModeRadioButton firstPossibleItem = null;
    boolean isAnythingSelected = false;
    for (SSPCheckoutProjectVisualPanel2.CheckoutModeRadioButton pItem : comp.getCheckoutModeSelectionButtons())
    {
      // check enabled state, based on git repo
      pItem.updateEnabledBasedOnProjectAvailability(repoUrl != null);
      if (pItem.isEnabled())
      {
        // check selected state, based on current checkout mode
        boolean isSelected = pItem.getCheckoutMode() == checkoutMode;
        pItem.setSelected(isSelected);

        // store information, so we can select the first possible item if necessary
        firstPossibleItem = firstPossibleItem == null ? pItem : firstPossibleItem;
        isAnythingSelected = isAnythingSelected || isSelected;
      }
      else
        // disabled items can not be selected
        pItem.setSelected(false);
    }
    if (!isAnythingSelected && firstPossibleItem != null)
      firstPossibleItem.setSelected(true);
  }


  /**
   * Sets the FileChooser for a JTextField
   *
   * @param pTarget a JTextField
   */
  private void applyFileChooser(JTextField pTarget)
  {
    String fieldText = pTarget.getText();
    if (fieldText != null && !fieldText.isEmpty())
    {
      File f = new File(fieldText);
      if (f.exists())
        fileChooser.setCurrentDirectory(f);
    }
    int result = fileChooser.showOpenDialog(pTarget);
    if (result == JFileChooser.APPROVE_OPTION)
      pTarget.setText(fileChooser.getSelectedFile().getAbsolutePath());
  }

  /**
   * Validates the projectName and projectPath and adjusts the ERROR_MESSAGE accordingly
   *
   * @param pProjName name of the project/project folder
   * @param pProjPath path to the project folder
   */
  private boolean updateMessage(@Nullable Object pProjName, @Nullable Object pProjPath)
  {
    if (wd == null)
      return false;
    String errorMessge = "";
    if (pProjName == null || pProjPath == null)
    {
      errorMessge = SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectWizardPanel2.class, "TXT.SSPCheckoutProjectWizardPanel2.noPath.error");
    }
    else if (new File(SSPCheckoutProjectWizardIterator.getProjectPath(pProjName, pProjPath)).exists())
    {
      errorMessge = SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectWizardPanel2.class, "TXT.SSPCheckoutProjectWizardPanel2.alreadyExists.error");
    }
    wd.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, errorMessge.isEmpty() ? null : errorMessge);
    return errorMessge.isEmpty();
  }

  /**
   * @return content of the name field
   */
  @Nullable
  private String getProjectName()
  {
    String projectName = comp.getProjectNameTextField().getText();
    if (projectName.trim().isEmpty())
      return null;
    return projectName;
  }

  /**
   * @return content of the projectPath field
   */
  @Nullable
  private String getProjectPath()
  {
    String text = comp.getProjectLocationTextField().getText();
    if (text.trim().isEmpty())
      return null;
    return text;
  }

  /**
   * @return the current checkout mode
   */
  @NotNull
  private SSPCheckoutProjectWizardIterator.ECheckoutMode getCheckoutMode()
  {
    return comp.getCheckoutModeSelectionButtons().stream()
        .filter(AbstractButton::isSelected)
        .map(SSPCheckoutProjectVisualPanel2.CheckoutModeRadioButton::getCheckoutMode)
        .findFirst()
        .orElseThrow();
  }

  @Nullable
  private ISSPSystemDetails getProjectDescription()
  {
    Object selectedObj = wd.getProperty(SSPCheckoutProjectWizardIterator.SELECTED);
    if (selectedObj != null)
      return ((CListObject) selectedObj).getSystemDetails();

    return null;
  }

  @NotNull
  @VisibleForTesting
  IRef[] getAvailableRef(@NotNull String pGitUrl)
  {
    List<IRef> gitRefs = new ArrayList<>();
    List<ITag> tags = new ArrayList<>();
    List<IRemoteBranch> branches = new ArrayList<>();
    try
    {
      tags = Lookup.getDefault().lookup(IGitVersioningSupport.class).getTagsInRepository(pGitUrl);
      branches = Lookup.getDefault().lookup(IGitVersioningSupport.class).getBranchesInRepository(pGitUrl);
    }
    catch (AditoVersioningException pE)
    {
      Logger.getLogger(SSPCheckoutProjectWizardPanel2.class.getName()).log(Level.WARNING, pE.getMessage(), pE);
      INotificationFacade.INSTANCE.error(pE, NbBundle.getMessage(SSPCheckoutProjectWizardPanel2.class, "SSPCheckoutProjectVisualPanel2.error.branchesList"));
    }
    catch (AbstractMethodError error)
    {
      comp.getGitBranchComboBox().setVisible(false);
      comp.getGitBranchNameLabel().setVisible(false);
      comp.validate();
      comp.repaint();
    }
    gitRefs.addAll(branches);
    gitRefs.addAll(tags);

    return gitRefs.toArray(new IRef[0]);
  }


  @Nullable
  private IRef getCurrentRef(@NotNull IRef[] pRefs)
  {
    CListObject cListObject = (CListObject) wd.getProperty(SSPCheckoutProjectWizardIterator.SELECTED);
    String gitBranch = cListObject.getSystemDetails().getGitBranch();
    if (gitBranch != null)
    {
      for (IRef ref : pRefs)
      {
        if (ref.getName().matches(gitBranch))
          return ref;
      }
    }

    return null;
  }

  private abstract static class DocumentUpdateListener implements DocumentListener
  {
    public void changedUpdate(DocumentEvent e)
    {
      update(e);
    }

    public void insertUpdate(DocumentEvent e)
    {
      update(e);
    }

    public void removeUpdate(DocumentEvent e)
    {
      update(e);
    }

    public abstract void update(DocumentEvent e);
  }
}