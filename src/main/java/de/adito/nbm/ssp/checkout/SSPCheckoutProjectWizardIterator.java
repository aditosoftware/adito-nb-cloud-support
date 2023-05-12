package de.adito.nbm.ssp.checkout;

import com.google.common.collect.Sets;
import de.adito.nbm.ssp.checkout.clist.CListObject;
import lombok.*;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;
import java.util.*;

/**
 * Iterator for the Wizard that allows the check out of SSP projects
 *
 * @author s.danner, 11.09.13
 */
public class SSPCheckoutProjectWizardIterator implements WizardDescriptor.ProgressInstantiatingIterator<WizardDescriptor>
{

  private static final ImageIcon PROJECT_ICON = ImageUtilities.loadImageIcon("de/adito/nbm/ssp/checkout/adito16.png", false);

  // Property names for the WizardDescriptor
  public static final String PROJECT_NAME = "de.adito.ssp.new.project.name";
  public static final String PROJECT_PATH = "de.adito.ssp.new.project.path";
  public static final String PROJECT_GIT_BRANCH = "de.adito.ssp.new.project.gitbranch";
  public static final String SELECTED = "de.adito.ssp.new.selected";
  public static final String CHECKOUT_MODE = "de.adito.ssp.new.checkout.mode";

  // Callback object for default settings
  private static final IDefaultSettingsCallback defaultSettings = new DefaultSettingsCallback();

  private int index;
  private WizardDescriptor wizard;
  private WizardDescriptor.Panel<WizardDescriptor>[] panels;

  /**
   * Creates the WizardPanels and returns them
   *
   * @return arry of panels
   */
  private WizardDescriptor.Panel<WizardDescriptor>[] getPanels()
  {
    if (panels == null)
    {
      panels = new WizardDescriptor.Panel[]{new SSPCheckoutProjectWizardPanel1(), new SSPCheckoutProjectWizardPanel2()};
      String[] steps = _createSteps();
      for (int i = 0; i < panels.length; i++)
      {
        Component c = panels[i].getComponent();
        if (steps[i] == null)
        {
          // Default step name to component name of panel. Mainly
          // useful for getting the name of the target chooser to
          // appear in the list of steps.
          steps[i] = c.getName();
        }
        if (c instanceof JComponent)
        { // assume Swing components
          JComponent jc = (JComponent) c;
          // Sets step number of a component
          jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
          // Sets steps names for a panel
          jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
          // Turn on subtitle creation on each step
          jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, Boolean.TRUE);
          // Show steps on the left side with the image on the background
          jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, Boolean.TRUE);
          // Turn on numbering of all steps
          jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, Boolean.TRUE);
        }
      }
      SSPCheckoutExecutor.preloadGitSupport();
    }
    return panels;
  }

  @Override
  public Set<FileObject> instantiate()
  {
    return Collections.emptySet();
  }

  /**
   * Executes the process that should happen with the collected data
   * The clone of the project and the download of the config files that happens here is done by the SSPCheckoutExecutor
   * In the end a set with the created fileObjects is created and returned
   *
   * @param handle ProgressHandle, to control the ProgressBar
   * @return A set, created from the Fileobject of the location of the created Project
   */
  @Override
  public Set<FileObject> instantiate(ProgressHandle handle)
  {
    wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
    FileObject instantiated = null;
    String projectPath = _getProjectPath(); // Possibly null
    if (projectPath != null)
    {
      CListObject cListObject = (CListObject) wizard.getProperty(SELECTED);
      ECheckoutMode checkoutMode = (ECheckoutMode) wizard.getProperty(CHECKOUT_MODE);

      // Find GIT-Branch to clone
      String branchToSet = null;
      if (checkoutMode.isLoadProject())
      {
        if (wizard.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_GIT_BRANCH) != null)
          branchToSet = (String) wizard.getProperty(SSPCheckoutProjectWizardIterator.PROJECT_GIT_BRANCH);
        else
          branchToSet = cListObject.getSystemDetails().getGitBranch();
      }

      // Determine, if the system state should be read
      boolean loadSystemState = checkoutMode.isLoadSystemState();

      // Download System!
      instantiated = SSPCheckoutExecutor.execute(handle, cListObject.getSystemDetails(), new File(projectPath),
                                                 branchToSet, loadSystemState);
    }
    return instantiated == null ? Collections.emptySet() : Sets.newHashSet(instantiated);
  }

  @Nullable
  private String _getProjectPath()
  {
    Object name = wizard.getProperty(PROJECT_NAME);
    if (name == null)
      return null;

    Object path = wizard.getProperty(PROJECT_PATH);
    return getProjectPath(name, path);
  }

  @Nullable
  static String getProjectPath(@NotNull Object pName, @Nullable Object pPath)
  {
    return pPath != null ? pPath.toString() + "/" + pName.toString() : null;
  }

  @Override
  public void initialize(WizardDescriptor pWizard)
  {
    wizard = pWizard;
    wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
  }

  @Override
  public void uninitialize(WizardDescriptor pWizard)
  {
    panels = null;
  }

  /**
   * returns the current panel
   */
  @Override
  public WizardDescriptor.Panel<WizardDescriptor> current()
  {
    return getPanels()[index];
  }

  @Override
  public String name()
  {
    return index + 1 + ". from " + getPanels().length;
  }

  @Override
  public boolean hasNext()
  {
    return index < getPanels().length - 1;
  }

  @Override
  public boolean hasPrevious()
  {
    return index > 0;
  }

  @Override
  public void nextPanel()
  {
    if (!hasNext())
      throw new NoSuchElementException();
    _checkForOverlayPanel();
    index++;
  }

  @Override
  public void previousPanel()
  {
    if (!hasPrevious())
      throw new NoSuchElementException();
    index--;
  }

  @Override
  public void addChangeListener(ChangeListener l)
  {

  }

  @Override
  public void removeChangeListener(ChangeListener l)
  {

  }

  /**
   * Returns the Callback object that contains the default settings
   *
   * @return the callback object
   */
  public static IDefaultSettingsCallback getCallback()
  {
    return defaultSettings;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  //used in layer.xml
  public static Image getProjectImage()
  {
    return PROJECT_ICON.getImage();
  }

  /**
   * Creates a string array with the names of the individual steps
   *
   * @return The names of the steps in a string array
   */
  private String[] _createSteps()
  {
    String[] res = new String[panels.length];
    for (int i = 0; i < res.length; i++)
      res[i] = panels[i].getComponent().getName();
    return res;
  }

  /**
   * Checks if there are any leftover panels of the first panel that have to be closed if a new call occurrs
   */
  private void _checkForOverlayPanel()
  {
    try
    {
      SSPCheckoutProjectWizardPanel1 wizardPanel1 = (SSPCheckoutProjectWizardPanel1) current();
      wizardPanel1.getComponent().setPanel(null);
    }
    catch (ClassCastException e)
    {
      // Do nothing if not first panel
    }
  }

  /**
   * Reads a text from the Bundle.properties
   *
   * @param pClass The Caller-class
   * @param pMsg   The name of the property
   * @param params Optional parameters
   * @return The text as string
   */
  public static String getMessage(Class<?> pClass, String pMsg, Object... params)
  {
    return NbBundle.getMessage(pClass, pMsg, params);
  }

  /**
   * Mode to determine, how the checkout should be done
   */
  @RequiredArgsConstructor
  public enum ECheckoutMode
  {
    /**
     * Download the project (from git) only
     */
    PROJECT_ONLY(false, true),

    /**
     * Download the project from the system database only
     */
    SYSTEMSTATE_ONLY(true, false),

    /**
     * Dowlnoad the project (from git) first and then
     * apply all changes made in the system database
     */
    PROJECT_AND_SYSTEMSTATE(true, true);

    /**
     * true, if this mode is loading the system state
     */
    @Getter
    private final boolean loadSystemState;

    /**
     * true, if this mode is loading the (git) project
     */
    @Getter
    private final boolean loadProject;
  }
}
