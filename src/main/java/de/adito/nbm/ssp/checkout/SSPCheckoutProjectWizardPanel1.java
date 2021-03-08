package de.adito.nbm.ssp.checkout;

import org.jetbrains.annotations.NotNull;
import org.openide.WizardDescriptor;
import org.openide.util.*;

import javax.swing.event.ChangeListener;

/**
 * Wizard-Panel for checking out a SSP project
 * Mode: Project selection
 *
 * @author s.danner, 11.09.13
 */
public class SSPCheckoutProjectWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor>, IStateChangeListener
{
  private final ChangeSupport cs = new ChangeSupport(this);
  private SSPCheckoutProjectVisualPanel1 comp;
  private WizardDescriptor wd;

  public SSPCheckoutProjectVisualPanel1 getComponent()
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
   * Determines if the "Next" button is active
   *
   * @return <tt>true</tt> if active
   */
  @Override
  public boolean isValid()
  {
    return _isValid() && wd.getProperty(WizardDescriptor.PROP_ERROR_MESSAGE) == null;
  }

  /**
   * This method is called when there is a switch from another panel to this one
   * The properties from the WizardDescriptor are read and processed. The list is loaded subsequently
   */
  @Override
  public void readSettings(WizardDescriptor pSettings)
  {
    wd = pSettings;

    if (comp.getSelected() == null)
    {
      comp.reloadList();
    }
    else
      comp.focusClist();
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
   * This method is called when there is a switch from this panel to another
   */
  @Override
  public void storeSettings(WizardDescriptor pSettings)
  {
    //Save current selection
    wd.putProperty(SSPCheckoutProjectWizardIterator.SELECTED, comp.getSelected());
  }

  /**
   * Creates the visual component and sets the listeners
   */
  private void _createComponent()
  {
    comp = new SSPCheckoutProjectVisualPanel1();
    comp.addStateChangeListener(this);
  }

  /**
   * Determines if an entry of the list was selected and if the list was loaded
   * This method is used to determine if the user may continue to the next panel
   *
   * @return <tt>true</tt>, if a project is selected
   */
  private boolean _isValid()
  {
    if (comp != null)
    {
      return comp.getSelected() != null;
    }
    return false;
  }

  @Override
  public void changedValidity(@NotNull State pState)
  {
    if (pState == State.ISVALID)
      wd.setValid(true);
    else if (pState == State.ISINVALID)
      wd.setValid(false);
    else if (pState == State.FINISHED)
      wd.doNextClick();
    else if (pState == State.CHANGED)
      cs.fireChange();
  }
}