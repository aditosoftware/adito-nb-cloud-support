package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Throwables;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.nbm.ssp.WarningPanel;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.checkout.clist.*;
import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.facade.*;
import de.adito.nbm.ssp.impl.SSPFacadeImpl;
import de.adito.swing.NotificationPanel;
import de.adito.swing.icon.IconAttributes;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.*;
import org.openide.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.*;
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
  private final WarningPanel warningPanel = new WarningPanel();
  private JPanel userEntrys;
  private SearchBox searchBox;
  private JButton refreshButton;
  private JButton oldDefaultButton;
  private JTextField usernameTextField;
  private JPasswordField passwordTextField;
  private JScrollPane scrollPane;
  private CList cList;
  private final List<IOptionsProvider> additionalOptionsProviders;
  private String pattern = "(.*)";

  public SSPCheckoutProjectVisualPanel1()
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
    _initSearchBox();
    _initCList();
    _initScrollPane();
    _initEntries();

    setLayout(new BorderLayout());

    UrlLabel urlLabel = new UrlLabel("Go to ADITO Self Service Portal", SSPFacadeImpl.getSSPSystemUrl());
    urlLabel.setBorder(new EmptyBorder(5, 0, 0, 0));

    add(userEntrys, BorderLayout.NORTH);
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(scrollPane, BorderLayout.CENTER);
    JPanel addtionalComponentsPanel = new JPanel();
    BoxLayout boxLayout = new BoxLayout(addtionalComponentsPanel, BoxLayout.Y_AXIS);
    addtionalComponentsPanel.setLayout(boxLayout);
    additionalOptionsProviders = getAdditionalOptionsProviders();
    additionalOptionsProviders.forEach(pOptionsProvider -> addtionalComponentsPanel.add(pOptionsProvider.getComponent()));
    centerPanel.add(addtionalComponentsPanel, BorderLayout.SOUTH);
    add(centerPanel, BorderLayout.CENTER);
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

  private void _initSearchBox()
  {
    searchBox = new SearchBox();
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
    scrollPane = new JScrollPane(cList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getViewport().setBackground(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
  }

  /**
   * Initiates the panel that contains the entry fields at the top
   * The panel is saved as userEntries
   */
  private void _initEntries()
  {
    userEntrys = new JPanel();
    userEntrys.setLayout(new BorderLayout(0, 8));
    userEntrys.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    GridBagConstraints c = new GridBagConstraints();

    /*Linkes Panel*/
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());
    JLabel lblUser = new JLabel(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel1.class, "SSPCheckoutProjectVisualPanel1.userNameLabel"));
    leftPanel.add(lblUser, BorderLayout.CENTER);

    /*Middle Panel*/
    JPanel midPanel = new JPanel();
    midPanel.setLayout(new GridBagLayout());
    midPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
    usernameTextField = new JTextField();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 0, 0, 0);
    c.gridwidth = 1;
    c.weightx = 0.5;
    c.gridx = 0;
    c.gridy = 1;
    midPanel.add(usernameTextField, c);
    JLabel lblPasswd = new JLabel(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel1.class, "SSPCheckoutProjectVisualPanel1.passwordLabel"));
    lblPasswd.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 5);
    c.weightx = 0.0;
    c.gridx = 1;
    c.gridy = 1;
    midPanel.add(lblPasswd, c);
    passwordTextField = new JPasswordField(new String(UserCredentialsManager.getLastUserPass()));
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    c.insets = new Insets(0, 0, 0, 0);
    c.weightx = 0.5;
    c.gridx = 2;
    c.gridy = 1;
    midPanel.add(passwordTextField, c);

    /*Right Panel*/
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());
    ImageIcon refreshIcon = Optional.ofNullable(iconsProvider)
        .map(pIconsProvider -> pIconsProvider.findImage(IVaadinIconsProvider.VaadinIcon.REFRESH, new IconAttributes.Builder().create()))
        .map(ImageIcon::new)
        .orElse(null);
    refreshButton = new JButton(refreshIcon);
    rightPanel.add(refreshButton, BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BorderLayout());
    bottomPanel.add(warningPanel, BorderLayout.NORTH);
    bottomPanel.add(searchBox, BorderLayout.SOUTH);

    userEntrys.add(leftPanel, BorderLayout.WEST);
    userEntrys.add(midPanel, BorderLayout.CENTER);
    userEntrys.add(rightPanel, BorderLayout.EAST);
    userEntrys.add(bottomPanel, BorderLayout.SOUTH);
    usernameTextField.addFocusListener(new FocusListenerRepositoryData());
    usernameTextField.getDocument().addDocumentListener(new UsernameEmailDocumentListener(warningPanel));
    usernameTextField.setText(UserCredentialsManager.getLastUser());
    passwordTextField.addFocusListener(new FocusListenerRepositoryData());
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
   * @throws UnirestException  if an error occurs during the rest call when loading the systems for the list
   * @throws AditoSSPException if the response of the server contains an error status when loading the systems for the list
   */
  private void loadList(@NotNull DecodedJWT pToken) throws UnirestException, AditoSSPException
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
   * @return List of IOptionsProviders whose components should be added to the panel
   */
  protected List<IOptionsProvider> getAdditionalOptionsProviders()
  {
    JCheckBox loadConfigsCB = new JCheckBox(NbBundle.getMessage(SSPCheckoutProjectVisualPanel1.class, "SSPCheckoutProjectVisualPanel1.loadDeployedSystemState"));
    loadConfigsCB.setAlignmentX(Component.RIGHT_ALIGNMENT);
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(5, 0, 5, 0));
    panel.add(loadConfigsCB);
    IOptionsProvider optionsProvider = new IOptionsProvider()
    {
      @Override
      public JComponent getComponent()
      {
        return panel;
      }

      @Override
      public void addOptions(@NotNull Map<String, Object> pOptionsMap)
      {
        pOptionsMap.put(SSPCheckoutProjectWizardIterator.CHECKOUT_DEPLOYED_STATE, loadConfigsCB.isSelected());
      }
    };
    return List.of(optionsProvider);
  }

  /**
   * @return Returns the currently selected object
   */
  public CListObject getSelected()
  {
    return cList.getSelected();
  }

  /**
   * @return Map with the selected options from the list of OptionsProviders obtained by the getAddtionalOptionsProviders
   */
  public Map<String, Object> getAdditionalOptions()
  {
    Map<String, Object> additionalOptions = new HashMap<>();
    additionalOptionsProviders.forEach(optionsProvider -> optionsProvider.addOptions(additionalOptions));
    return additionalOptions;
  }

  public List<CListObject> getObjects()
  {
    return cList.getObjectList();
  }

  public JPasswordField getPasswordTextField()
  {
    return passwordTextField;
  }

  public JTextField getUsernameTextField()
  {
    return usernameTextField;
  }

  public CList getCList()
  {
    return cList;
  }

  @Override
  public String getName()
  {
    return SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel1.class, "SSPCheckoutProjectVisualPanel1.stepName.text");
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
      errorMessage = SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel1.class, "SSPCheckoutProjectVisualPanel1.errorPanel.auth.failure");
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
      retry = new AbstractAction(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel1.class, "SSPCheckoutProjectVisualPanel1.retryButton.name"))
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
      showErrorPanel(pType, errorMessage, retry, copyClipboardAction);
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
      DecodedJWT jwt = ISSPFacade.getInstance().getJWT(getUsernameTextField().getText(), getPasswordTextField().getPassword());
      UserCredentialsManager.saveToken(jwt);
      UserCredentialsManager.saveLastUser(jwt.getSubject());
      loadList(jwt);
    }
    catch (final Exception e)
    {
      getCList().clearList();
      // On retry -> reload list again
      Runnable retry = () -> _comboBoxChangeAction(null);
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
          object.dispatchEvent(new MouseEvent(object, e.getID(), e.getWhen(), e.getModifiersEx(), 0, 0, e.getClickCount(),
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
      else if (e.getKeyCode() == KeyEvent.VK_DOWN && indexOfSelectedObject < (getCList().getObjectList().size() - 1))
        setSelected(objects.get(indexOfSelectedObject + 1), true);
    }
  }

  /**
   * Interface that wraps a component that displays actions and a functionality, that writes the selected Options to a map of Strings and Objects
   */
  protected interface IOptionsProvider
  {

    JComponent getComponent();

    void addOptions(@NotNull Map<String, Object> pOptionsMap);

  }

  private void _filter(String pSearchText)
  {
    boolean isFirst = true;
    boolean valid = false;

    if(searchBox.getComboBox().getSelectedItem() != null){
      for(int i = 0; i < cList.getObjectList().size(); i++){
        FilterBy filter = (FilterBy) searchBox.getComboBox().getSelectedItem();
        boolean matches = filter.filter(pSearchText, cList.getObjectList().get(i).getSystemDetails());
        if(!matches)
          cList.getComponent(i).setVisible(false);
        else{
          valid = true;
          cList.getComponent(i).setVisible(true);
          if(isFirst){
            cList.setSelected(cList.getObjectList().get(i));
            isFirst = false;
          }
        }
      }
      cList.revalidate();
      cList.repaint();
      if(valid)
        fireStateChanged(IStateChangeListener.State.ISVALID);
      else
        fireStateChanged(IStateChangeListener.State.ISINVALID);
    }
  }

  private class SearchBox extends JPanel
  {
    private final JTextField searchable = new JTextField(30);
    private final JComboBox<FilterBy> comboBox;

    private SearchBox() throws HeadlessException
    {
      super();
      FilterBy[] comboBoxContent = {new FilterByName(), new FilterByGitURL(), new FilterByGitBranch(), new FilterByKernelVersion(),
                                    new FilterByAfterDate(), new FilterByBeforeDate()};
      comboBox = new JComboBox<>(comboBoxContent);
      comboBox.setRenderer(createListRenderer());
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      JLabel search = new JLabel("Filter by:");
      add(search);
      add(Box.createRigidArea(new Dimension(22, 0)));
      add(comboBox);
      add(Box.createRigidArea(new Dimension(5, 0)));
      add(searchable);
      searchable.getDocument().addDocumentListener((SimpleDocumentListener) e -> _filter(searchable.getText()));
    }

    public JComboBox<FilterBy> getComboBox(){
      return comboBox;
    }
  }

  public abstract class FilterBy {

    protected final String pattern = ".*";

    boolean filter(String pToCompare, ISSPSystemDetails pSelected){
      return false;
    }

    @Override
    public String toString()
    {
      return "should not be shown";
    }
  }

  private static ListCellRenderer<? super FilterBy> createListRenderer() {
    return new DefaultListCellRenderer() {
      private final Color background = new Color(0, 100, 255, 15);
      private final Color defaultBackground = (Color) UIManager.get("List.background");

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus)
      {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        FilterBy filter = (FilterBy) value;
        this.setText(filter.toString());
        if (!isSelected) {
          this.setBackground(index % 2 == 0 ? background : defaultBackground);
        }
        return this;
      }
    };
  }

  public class FilterByName extends FilterBy{
    public final static String FILTERNAME = "Project-Name";

    @Override
    boolean filter(String pToCompare, ISSPSystemDetails pSelected)
    {
      String fullPattern = pattern + pToCompare + pattern;
      return pSelected.getName().matches(fullPattern);
    }

    @Override
    public String toString(){
      return FILTERNAME;
    }
  }

  public class FilterByGitURL extends FilterBy{
    public final static String FILTERNAME = "Git-URL";

    @Override
    boolean filter(String pToCompare, ISSPSystemDetails pSelected)
    {
      String fullPattern = pattern + pToCompare + pattern;
      return  pSelected.getGitRepoUrl().matches(fullPattern);
    }

    @Override
    public String toString(){
      return FILTERNAME;
    }
  }

  public class FilterByKernelVersion extends FilterBy{
    public final static String FILTERNAME = "ADITO-Version";

    @Override
    boolean filter(String pToCompare, ISSPSystemDetails pSelected)
    {
      String fullPattern = pattern + pToCompare + pattern;
      return pSelected.getKernelVersion().matches(fullPattern);
    }

    @Override
    public String toString(){
      return FILTERNAME;
    }
  }

  public class FilterByGitBranch extends FilterBy{
    public final static String FILTERNAME = "Git-Branch";

    @Override
    boolean filter(String pToCompare, ISSPSystemDetails pSelected)
    {
      String fullPattern = pattern + pToCompare + pattern;
      return pSelected.getGitBranch().matches(fullPattern);
    }

    @Override
    public String toString(){
      return FILTERNAME;
    }
  }

  public class FilterByAfterDate extends FilterBy{
    public final static String FILTERNAME = "After";

    @Override
    boolean filter(String pToCompare, ISSPSystemDetails pSelected)
    {
      Instant toCompare = _getInstantFromString(pToCompare);
      if(toCompare != null)
        return pSelected.getCreationDate().isAfter(toCompare);
      else
        return pSelected.getCreationDate().isAfter(Instant.MIN);
    }

    @Override
    public String toString(){
      return FILTERNAME;
    }
  }

  public class FilterByBeforeDate extends FilterBy{
    public final static String FILTERNAME = "Before";

    @Override
    boolean filter(String pToCompare, ISSPSystemDetails pSelected)
    {
      Instant toCompare = _getInstantFromString(pToCompare);
      if(toCompare != null)
        return pSelected.getCreationDate().isBefore(toCompare);
      else
        return pSelected.getCreationDate().isBefore(Instant.MAX);
    }

    @Override
    public String toString(){
      return FILTERNAME;
    }
  }

  @Nullable
  private Instant _getInstantFromString(String pToConvert){
    String patternYear = "(19|2[0-9])[0-9]{2}";
    String patternMonthYear = "(0[1-9]|1[012]).((19|2[0-9])[0-9]{2})";
    String patternYearMonth = "((19|2[0-9])[0-9]{2}).(0[1-9]|1[012])";

    String patternFullDateCorrect = "((2000|2400|2800|(19|2[0-9](0[48]|[2468][048]|[13579][26]))).02.29)"
        + "|(((19|2[0-9])[0-9]{2}).02.(0[1-9]|1[0-9]|2[0-8]))"
        + "|(((19|2[0-9])[0-9]{2}).(0[13578]|10|12).(0[1-9]|[12][0-9]|3[01]))"
        + "|(((19|2[0-9])[0-9]{2}).(0[469]|11).(0[1-9]|[12][0-9]|30))";

    String patternFullDateWrong= "29.02.(2000|2400|2800|(19|2[0-9](0[48]|[2468][048]|[13579][26])))"
        + "|((0[1-9]|1[0-9]|2[0-8]).02.((19|2[0-9])[0-9]{2}))"
        + "|((0[1-9]|[12][0-9]|3[01]).(0[13578]|10|12).((19|2[0-9])[0-9]{2}))"
        + "|((0[1-9]|[12][0-9]|30).(0[469]|11).((19|2[0-9])[0-9]{2}))";

    Instant test = null;

    if(pToConvert.matches(patternYear))
    {
      String toParse = pToConvert + "-01-01T00:00:00.00Z";
      test = Instant.parse(toParse);
    }
    if(pToConvert.matches(patternMonthYear))
    {
      String[] stringArray = pToConvert.split("\\D");
      String toParse = stringArray[1] + "-" + stringArray[0] + "-01T00:00:00.00Z";
      test = Instant.parse(toParse);
    }
    if(pToConvert.matches(patternYearMonth))
    {
      String toParse = pToConvert.replaceAll("\\D", "-") + "-00T00:00:00.00Z";
      test = Instant.parse(toParse);
    }
    if(pToConvert.matches(patternFullDateCorrect))
    {
      String toParse = pToConvert.replaceAll("\\D", "-") + "T00:00:00.00Z";
      test = Instant.parse(toParse);
    }
    if(pToConvert.matches(patternFullDateWrong))
    {
      String[] stringArray = pToConvert.split("\\D");
      String toParse = stringArray[2] + "-" + stringArray[1] + "-" + stringArray[0] + "T00:00:00.00Z";
      test = Instant.parse(toParse);
    }
    return test;
  }

  @FunctionalInterface
  public interface SimpleDocumentListener extends DocumentListener
  {
    void update(DocumentEvent e);

    @Override
    default void insertUpdate(DocumentEvent e)
    {
      update(e);
    }
    @Override
    default void removeUpdate(DocumentEvent e)
    {
      update(e);
    }
    @Override
    default void changedUpdate(DocumentEvent e)
    {
      update(e);
    }
  }
}