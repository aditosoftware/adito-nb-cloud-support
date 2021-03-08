package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Throwables;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.checkout.clist.*;
import de.adito.nbm.ssp.exceptions.AditoSSPAuthException;
import de.adito.nbm.ssp.facade.ISSPFacade;
import de.adito.nbm.ssp.impl.SSPFacadeImpl;
import de.adito.swing.NotificationPanel;
import de.adito.swing.icon.IconAttributes;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.*;
import org.openide.util.Lookup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

/**
 * Panel for the wizard, shows the list of available projects
 *
 * @author w.glanzer, 10.09.13
 */
public class SSPCheckoutProjectVisualPanel1 extends JPanel
{
  private final List<IStateChangeListener> validListeners = new ArrayList<>();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private Future<?> future;
  private final IVaadinIconsProvider iconsProvider;
  private JPanel userEntrys;
  private JButton refreshButton;
  private JButton oldDefaultButton;
  private JTextField txtUser;
  private JPasswordField txtPasswd;
  private JScrollPane scrollPane;
  private CList cList;

  public SSPCheckoutProjectVisualPanel1()
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
    _initCList();
    _initScrollPane();
    _initEntries();

    setLayout(new BorderLayout());

    UrlLabel urlLabel = new UrlLabel("Go to ADITO Self Service Portal", SSPFacadeImpl.getSSPSystemUrl());
    urlLabel.setBorder(new EmptyBorder(5, 0, 0, 0));

    add(userEntrys, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
    add(urlLabel, BorderLayout.SOUTH);
  }

  public void addStateChangeListener(@NotNull IStateChangeListener pListener)
  {
    validListeners.add(pListener);
  }

  public void removeStateChangeListener(@NotNull IStateChangeListener pListener)
  {
    validListeners.remove(pListener);
  }

  private void fireStateChanged(@NotNull IStateChangeListener.State pState)
  {
    validListeners.forEach(pListener -> pListener.changedValidity(pState));
  }

  /**
   * Initiates the clist
   */
  private void _initCList()
  {
    cList = new CList();
    cList.addFocusListener(new FocusAdapter()
    {
      @Override
      public void focusGained(FocusEvent e)
      {
        setDefaultButtonToRefreshButton(false);
      }
    });
  }

  /**
   * Initiates the ScrollPane
   */
  private void _initScrollPane()
  {
    scrollPane = new JScrollPane(cList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getViewport().setBackground(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
  }

  /**
   * Initiates the panel that contains the entry fields at the top
   * The panel is saved as userEntries
   */
  private void _initEntries()
  {
    userEntrys = new JPanel();
    userEntrys.setLayout(new BorderLayout());
    userEntrys.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    GridBagConstraints c = new GridBagConstraints();

    /*Linkes Panel*/
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());
    JLabel lblUser = new JLabel(SSPCheckoutProjectWizardIterator.getMessage(this, "SSPCheckoutProjectVisualPanel1.userNameLabel"));
    leftPanel.add(lblUser, BorderLayout.CENTER);

    /*Middle Panel*/
    JPanel midPanel = new JPanel();
    midPanel.setLayout(new GridBagLayout());
    midPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 0, 5, 0);
    c.gridwidth = 3;
    c.weightx = 0.0;
    c.gridx = 0;
    c.gridy = 0;
    txtUser = new JTextField(UserCredentialsManager.getLastUser());
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 0, 0, 0);
    c.gridwidth = 1;
    c.weightx = 0.5;
    c.gridx = 0;
    c.gridy = 1;
    midPanel.add(txtUser, c);
    JLabel lblPasswd = new JLabel(SSPCheckoutProjectWizardIterator.getMessage(this, "SSPCheckoutProjectVisualPanel1.passwordLabel"));
    lblPasswd.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 5);
    c.weightx = 0.0;
    c.gridx = 1;
    c.gridy = 1;
    midPanel.add(lblPasswd, c);
    txtPasswd = new JPasswordField(new String(UserCredentialsManager.getLastUserPass()));
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);
    c.weightx = 0.5;
    c.gridx = 2;
    c.gridy = 1;
    midPanel.add(txtPasswd, c);

    /*Right Panel*/
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());
    ImageIcon refreshIcon = Optional.ofNullable(iconsProvider)
        .map(pIconsProvider -> pIconsProvider.findImage(IVaadinIconsProvider.VaadinIcon.REFRESH, new IconAttributes.Builder().create()))
        .map(ImageIcon::new)
        .orElse(null);
    refreshButton = new JButton(refreshIcon);
    rightPanel.add(refreshButton, BorderLayout.CENTER);

    userEntrys.add(leftPanel, BorderLayout.WEST);
    userEntrys.add(midPanel, BorderLayout.CENTER);
    userEntrys.add(rightPanel, BorderLayout.EAST);
    txtUser.addFocusListener(new FocusListenerRepositoryData());
    txtPasswd.addFocusListener(new FocusListenerRepositoryData());
    refreshButton.addActionListener(e -> _comboBoxChangeAction(null));
    cList.addKeyListener(new ClistKeyListener());
  }

  /**
   * Refreshes the panel to avoid repainting errors
   */
  public void refreshShowPanel()
  {
    SwingUtilities.invokeLater(() -> {
      revalidate();
      repaint();
    });
  }

  public void reloadList()
  {
    _comboBoxChangeAction(null);
  }

  /**
   * Creates the clist
   *
   * @param pToken JWT used to auth with the SSP system
   */
  private void loadList(@NotNull DecodedJWT pToken)
  {
    cList.fillListBasedOnURL(pToken);
    refreshShowPanel();
  }

  /**
   * Setzt anhand einer ID, welches CListObject gerade selektiert ist
   * Sets the selected CListObject
   *
   * @param pSelected The object to set as selected
   * @param pDoScroll represents if the view should scroll to the CListObject
   */
  private void setSelected(@NotNull CListObject pSelected, boolean pDoScroll)
  {
    cList.setSelected(pSelected);
    if (pDoScroll)
      scrollPane.getVerticalScrollBar().setValue(cList.getScrollValue());
    refreshShowPanel();
  }

  /**
   * If true is passed, the RefreshButton of the panel is set as the DefaultButton of the wizard
   * This means that the RefreshButton may be used by pressing "ENTER"
   *
   * @param pTakeRefreshButton TRUE, if the RefreshButton should be used as default button. FALSE sets the "oldDefaultButton" as default button
   */
  public void setDefaultButtonToRefreshButton(final boolean pTakeRefreshButton)
  {
    SwingUtilities.invokeLater(() -> {
      if (getRootPane() != null)
      {
        if (pTakeRefreshButton)
          getRootPane().setDefaultButton(refreshButton);
        else if (oldDefaultButton != null)
          getRootPane().setDefaultButton(oldDefaultButton);
      }
    });
  }

  /**
   * Sets the focus of the window on the CList
   */
  public void focusClist()
  {
    SwingUtilities.invokeLater(() -> {
      cList.requestFocusInWindow();
      scrollPane.revalidate();
      scrollPane.repaint();
      cList.repaint();
    });
  }

  /**
   * Sets "oldDefaultButton" on the new DefaultButton, that lies on the RootPane of the wizard
   */
  private void setOldDefaultButton()
  {
    SwingUtilities.invokeLater(() -> {
      if (oldDefaultButton == null && getRootPane() != null)
        oldDefaultButton = getRootPane().getDefaultButton();
    });
  }

  /**
   * Sets the viewport of the scrollPane
   *
   * @param pPanelToShow JPanel that serves as ViewPort for the JScrollPane. NULL, if only the CList should be shown
   */
  public void setPanel(@Nullable JPanel pPanelToShow)
  {
    if (pPanelToShow != null)
    {
      // This is the case if another panel should be shown instead of the cList
      scrollPane.setViewportView(pPanelToShow);
      scrollPane.revalidate();
      scrollPane.repaint();
    }
    else
    {
      scrollPane.setViewportView(cList);
    }
  }

  /**
   * @return Returns the currently selected object
   */
  public CListObject getSelected()
  {
    return cList.getSelected();
  }

  public List<CListObject> getObjects()
  {
    return cList.getObjectList();
  }

  public JPasswordField getTxtPasswd()
  {
    return txtPasswd;
  }

  public JTextField getTxtUser()
  {
    return txtUser;
  }

  public CList getCList()
  {
    return cList;
  }

  @Override
  public String getName()
  {
    return SSPCheckoutProjectWizardIterator.getMessage(this, "SSPCheckoutProjectVisualPanel1.stepName.text");
  }

  /**
   * Shows the ErrorPanel in form of the NotificationPanel, for example if the SSP server cannot be reached
   *
   * @param pType          Type of the NotificationPanel (Expects ERROR or WARNING or DB_ERROR)
   * @param pException     Exception that caused the error for which the ErrorPanel is shown
   * @param pActionOnRetry ButtonAction in form of a Runnable. This action is assigned to the "Retry..." button
   *                       If NULL no "Retry..." is shown
   */
  private void showErrorPanel(@NotNull NotificationPanel.NotificationType pType, @NotNull final Exception pException, @Nullable final Runnable pActionOnRetry)
  {
    String errorMessage = ExceptionUtils.getRootCauseMessage(pException);
    if (pException instanceof AditoSSPAuthException)
    {
      errorMessage = SSPCheckoutProjectWizardIterator.getMessage(this, "SSPCheckoutProjectVisualPanel1.errorPanel.auth.failure");
    }
    AbstractAction retry = null;

    AbstractAction copyClipboardAction = new AbstractAction("Copy to clipboard")
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(Throwables.getStackTraceAsString(pException));
        clipboard.setContents(stringSelection, stringSelection);
      }
    };

    if (pActionOnRetry != null)
    {
      retry = new AbstractAction(SSPCheckoutProjectWizardIterator.getMessage(this, "SSPCheckoutProjectVisualPanel1.retryButton.name"))
      {

        @Override
        public void actionPerformed(ActionEvent e)
        {
          pActionOnRetry.run();
        }
      };
    }


    if (pActionOnRetry == null)
      showErrorPanel(pType, errorMessage, copyClipboardAction);
    else
      showErrorPanel(pType, errorMessage, copyClipboardAction, retry);
  }

  /**
   * Shows an ErrorPanel. Options for "Retry" or "Details" may be set
   *
   * @param pType         Type of the Panels
   * @param pErrorMessage The error message
   * @param pActions      optional actions
   */
  private void showErrorPanel(NotificationPanel.NotificationType pType, String pErrorMessage, Action... pActions)
  {
    NotificationPanel panel = new NotificationPanel(pType.toIcon(), pErrorMessage, pActions);
    panel.setBackground(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
    setPanel(panel);
  }

  /**
   * Executes the whole reload process of the list
   *
   * @param pSelected the ID of the object that should be selected
   */
  private void _comboBoxChangeAction(@Nullable final CListObject pSelected)
  {
    // Deactivate the "Next" button while the list is loading
    fireStateChanged(IStateChangeListener.State.ISINVALID);
    // In case there are still details shown
    setPanel(null);

    // Cancel the old loading process if still active
    if (future != null)
      future.cancel(true);
    future = executorService.submit(() -> {
      // Emtpy the Clist and show the "loading" text
      cList.setLoading();
      _updateComponent(pSelected);
      // Re-enable "Next" button
      fireStateChanged(IStateChangeListener.State.ISVALID);
    });
  }

  /**
   * Triggers the reload of the list with the data from the WizardDescriptor
   * Selects the passed Project and sets the focus on the list so the user can navigate via the arrow keys
   */
  private void _updateComponent(@Nullable CListObject pSelected)
  {
    _reload();

    if (!getObjects().isEmpty() && pSelected != null)
    {
      setSelected(pSelected, true);
    }
    fireStateChanged(IStateChangeListener.State.CHANGED);
    if (!getObjects().isEmpty())
    {
      setOldDefaultButton();
      setDefaultButtonToRefreshButton(false);

    }
    else
      setDefaultButtonToRefreshButton(true);
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
      DecodedJWT jwt = ISSPFacade.getInstance().getJWT(getTxtUser().getText(), getTxtPasswd().getPassword());
      UserCredentialsManager.saveToken(jwt);
      UserCredentialsManager.saveLastUser(jwt.getSubject());
      loadList(jwt);
    }
    catch (final Exception e)
    {
      getCList().clearList();
      Runnable retry = () -> {
        // On retry -> reload list again
        _comboBoxChangeAction(null);
      };
      showErrorPanel(NotificationPanel.NotificationType.ERROR, e, retry);
      return;
    }
    _setListeners();
    focusClist();
  }

  /**
   * Sets the listener for the CListObjects and the detail buttons
   * For this purpose custom classes are used, these are implemented further down
   */
  private void _setListeners()
  {
    for (final CListObject object : getObjects())
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
        setSelected(objectClicked, false);
        // Check for doubleclick, if yes, proceed one step further
        if (e.getClickCount() > 1)
        {
          fireStateChanged(IStateChangeListener.State.FINISHED);
        }
        else
        {
          focusClist();
          setDefaultButtonToRefreshButton(false);
          fireStateChanged(IStateChangeListener.State.CHANGED);
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
      setDefaultButtonToRefreshButton(true);
    }

  }

  private class ClistKeyListener extends KeyAdapter
  {
    @Override
    public void keyPressed(KeyEvent e)
    {
      List<CListObject> objects = getObjects();
      int indexOfSelectedObject = objects.indexOf(getSelected());
      if (e.getKeyCode() == KeyEvent.VK_UP && indexOfSelectedObject > 0)
        setSelected(objects.get(indexOfSelectedObject - 1), true);
      else if (e.getKeyCode() == KeyEvent.VK_DOWN)
        if (indexOfSelectedObject < (getCList().getObjectList().size() - 1))
          setSelected(objects.get(indexOfSelectedObject + 1), true);
    }
  }
}