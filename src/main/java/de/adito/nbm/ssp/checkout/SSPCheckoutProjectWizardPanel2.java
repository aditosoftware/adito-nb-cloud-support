package de.adito.nbm.ssp.checkout;


import de.adito.nbm.ssp.checkout.clist.CListObject;
import de.adito.nbm.ssp.facade.ISSPSystemDetails;
import de.adito.swing.IFileChooserProvider;
import org.jetbrains.annotations.Nullable;
import org.openide.WizardDescriptor;
import org.openide.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.io.File;

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

  public SSPCheckoutProjectWizardPanel2()
  {
  }

  public SSPCheckoutProjectVisualPanel2 getComponent()
  {
    if (comp == null)
      _createComponent();
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
    return _updateMessage(projName, projPath);
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
    _updateComponent();
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
  private void _createComponent()
  {
    _loadFileChooserAsync();
    comp = new SSPCheckoutProjectVisualPanel2();
    //Listener for the ProjectPath button
    comp.getProjectLocationBrowseButton().addActionListener(e -> {
      _applyFileChooser(comp.getProjectLocationTextField());
      _updateComponent();
    });

    //DocumentListener for the two textFields
    comp.getProjectNameTextField().getDocument().addDocumentListener(new DocumentUpdateListener()
    {
      @Override
      public void update(DocumentEvent e)
      {
        if (wd != null)
          wd.putProperty(SSPCheckoutProjectWizardIterator.PROJECT_NAME, _getProjectName());
        cs.fireChange();
      }
    });
    comp.getProjectLocationTextField().getDocument().addDocumentListener(new DocumentUpdateListener()
    {
      @Override
      public void update(DocumentEvent e)
      {
        if (wd != null)
          wd.putProperty(SSPCheckoutProjectWizardIterator.PROJECT_PATH, _getProjectPath());
        cs.fireChange();
      }
    });
  }

  private void _loadFileChooserAsync()
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
  private void _updateComponent()
  {
    if (wd == null || comp == null)
      return;


    ISSPSystemDetails projectDescription = _getProjectDescription();

    Object projectName = wd.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_NAME);
    if (projectName != null)
      comp.getProjectNameTextField().setText(projectName.toString());
    else if (projectDescription != null)
      comp.getProjectNameTextField().setText(projectDescription.getName());

    Object projectPath = wd.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_PATH);
    if (projectPath != null)
      comp.getProjectLocationTextField().setText(projectPath.toString());
  }

  /**
   * Sets the FileChooser for a JTextField
   *
   * @param pTarget a JTextField
   */
  private void _applyFileChooser(JTextField pTarget)
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
  private boolean _updateMessage(@Nullable Object pProjName, @Nullable Object pProjPath)
  {
    if (wd == null)
      return false;
    String errorMessge = "";
    if (pProjName == null || pProjPath == null)
    {
      errorMessge = SSPCheckoutProjectWizardIterator.getMessage(this, "TXT.SSPCheckoutProjectWizardPanel2.noPath.error");
    }
    else if (new File(SSPCheckoutProjectWizardIterator.getProjectPath(pProjName, pProjPath)).exists())
    {
      errorMessge = SSPCheckoutProjectWizardIterator.getMessage(this, "TXT.SSPCheckoutProjectWizardPanel2.alreadyExists.error");
    }
    wd.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, errorMessge.isEmpty() ? null : errorMessge);
    return errorMessge.isEmpty();
  }

  /**
   * @return content of the name field
   */
  @Nullable
  private String _getProjectName()
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
  private String _getProjectPath()
  {
    String text = comp.getProjectLocationTextField().getText();
    if (text.trim().isEmpty())
      return null;
    return text;
  }

  @Nullable
  private ISSPSystemDetails _getProjectDescription()
  {
    Object selectedObj = wd.getProperty(SSPCheckoutProjectWizardIterator.SELECTED);
    if (selectedObj != null)
      return ((CListObject) selectedObj).getSystemDetails();

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