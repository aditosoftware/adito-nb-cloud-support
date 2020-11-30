package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.checkout.clist.CListObject;
import de.adito.nbm.ssp.facade.ISSPFacade;
import de.adito.swing.NotificationPanel;
import org.openide.WizardDescriptor;
import org.openide.util.*;

import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Wizard-Panel for checking out a SSP project
 * Mode: Project selection
 *
 * @author s.danner, 11.09.13
 */
public class SSPCheckoutProjectWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor>
{
  private final ChangeSupport cs = new ChangeSupport(this);
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private SSPCheckoutProjectVisualPanel1 comp;
  private WizardDescriptor wd;

  private Future<?> future;

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

    if (_getSelected() == null)
    {
      _comboBoxChangeAction(null);
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
    wd.putProperty(SSPCheckoutProjectWizardIterator.SELECTED, _getSelected());
  }

  /**
   * Creates the visual component and sets the listeners
   */
  private void _createComponent()
  {
    comp = new SSPCheckoutProjectVisualPanel1();

    //Document-Listener for Name- and passwordField
    comp.getTxtUser().addFocusListener(new FocusListenerRepositoryData());
    comp.getTxtPasswd().addFocusListener(new FocusListenerRepositoryData());
    //Listener for the button
    comp.getRefreshButton().addActionListener(e -> _comboBoxChangeAction(null));
    //KeyListener of the CList
    comp.getCList().addKeyListener(new KeyAdapter()
    {
      @Override
      public void keyPressed(KeyEvent e)
      {
        List<CListObject> objects = comp.getObjects();
        int indexOfSelectedObject = objects.indexOf(_getSelected());
        if (e.getKeyCode() == KeyEvent.VK_UP && indexOfSelectedObject > 0)
          comp.setSelected(objects.get(indexOfSelectedObject - 1), true);
        else if (e.getKeyCode() == KeyEvent.VK_DOWN)
          if (indexOfSelectedObject < (comp.getCList().getObjectList().size() - 1))
            comp.setSelected(objects.get(indexOfSelectedObject + 1), true);
      }
    });
  }

  /**
   * Executes the whole reload process of the list
   *
   * @param pSelected the ID of the object that should be selected
   */
  private void _comboBoxChangeAction(final CListObject pSelected)
  {
    // Deactivate the "Next" button while the list is loading
    wd.setValid(false);
    // In case there are still details shown
    comp.setPanel(null);

    // Cancel the old loading process if still active
    if (future != null)
      future.cancel(true);
    future = executorService.submit(() -> {
      // Emtpy the Clist and show the "loading" text
      comp.setLoading();
      _updateComponent(pSelected);
      // Re-enable "Next" button
      wd.setValid(true);
    });
  }

  /**
   * Triggers the reload of the list with the data from the WizardDescriptor
   * Selects the passed Project and sets the focus on the list so the user can navigate via the arrow keys
   */
  private void _updateComponent(CListObject pSelected)
  {
    if (wd == null || comp == null)
      return;

    _reload();

    if (!comp.getObjects().isEmpty() && pSelected != null)
    {
      _setSelected(pSelected, true);
    }
    cs.fireChange();
    if (!comp.getObjects().isEmpty())
    {
      comp.setOldDefaultButton();
      comp.setDefaultButtonToRefreshButton(false);

    }
    else
      comp.setDefaultButtonToRefreshButton(true);
  }

  /**
   * Passes on the order to reload the Clist
   * The exceptions that may occur are caught and passed on to the ErrorPanel
   * This method also sets the listeners for the detailsButtons and the ClistObjects
   */
  private void _reload()
  {
    try
    {
      DecodedJWT jwt = ISSPFacade.getInstance().getJWT(comp.getTxtUser().getText(), comp.getTxtPasswd().getPassword());
      UserCredentialsManager.saveToken(jwt);
      UserCredentialsManager.saveLastUser(jwt.getSubject());
      comp.loadList(jwt);
    }
    catch (final Exception e)
    {
      comp.getCList().clearList();
      Runnable retry = () -> {
        // On retry -> reload list again
        _comboBoxChangeAction(null);
      };
      comp.showErrorPanel(NotificationPanel.NotificationType.ERROR, e, retry);
      return;
    }
    _setListeners();
    comp.focusClist();
  }

  /**
   * Changes the selected object of the CList
   *
   * @param pSelected The newly selected object
   * @param pDoScroll Should the view scroll to the object?
   */
  private void _setSelected(CListObject pSelected, boolean pDoScroll)
  {
    comp.setSelected(pSelected, pDoScroll);
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
      return _getSelected() != null;
    }
    return false;
  }

  /**
   * Sets the listener for the CListObjects and the detail buttons
   * For this purpose custom classes are used, these are implemented further down
   */
  private void _setListeners()
  {
    for (final CListObject object : comp.getObjects())
    {
      CListObjectMouseAdapter mouseAdapter = new CListObjectMouseAdapter();
      object.addMouseAdapter(mouseAdapter);
      object.addMouseAdapterOnTextField(new MouseAdapter()
      {
        @Override
        public void mousePressed(MouseEvent e)
        {
          object.dispatchEvent(new MouseEvent(object, e.getID(), e.getWhen(), e.getModifiers(), 0, 0, e.getClickCount(),
                                              false));
        }
      });
    }
  }

  /**
   * Returns the position currently selected object
   *
   * @return currently selected object
   */
  private CListObject _getSelected()
  {
    return comp.getSelected();
  }

  /**
   * A MouseAdapter, to process the clicks on a CListObject (= selection)
   */
  private class CListObjectMouseAdapter extends MouseAdapter
  {
    @Override
    public void mousePressed(MouseEvent e)
    {
      CListObject objectClicked;
      objectClicked = (CListObject) e.getComponent();
      if (objectClicked != null)
      {
        _setSelected(objectClicked, false);
        // Check for doubleclick, if yes, proceed one step further
        if (e.getClickCount() > 1)
        {
          wd.doNextClick();
        }
        else
        {
          comp.focusClist();
          comp.setDefaultButtonToRefreshButton(false);
          cs.fireChange();
        }
      }
    }
  }

  /**
   * A FocusAdapter for the ComboBox and the textfields to manage focus
   */
  private class FocusListenerRepositoryData extends FocusAdapter
  {
    @Override
    public void focusGained(FocusEvent e)
    {
      comp.setDefaultButtonToRefreshButton(true);
    }

  }
}