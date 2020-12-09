package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Throwables;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.checkout.clist.*;
import de.adito.nbm.ssp.exceptions.AditoSSPAuthException;
import de.adito.nbm.ssp.impl.SSPFacadeImpl;
import de.adito.swing.NotificationPanel;
import de.adito.swing.icon.IconAttributes;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openide.util.Lookup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * Panel for the wizard, shows the list of available projects
 *
 * @author w.glanzer, 10.09.13
 */
public class SSPCheckoutProjectVisualPanel1 extends JPanel
{
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

  /**
   * Creates the clist
   *
   * @param pToken JWT used to auth with the SSP system
   */
  public void loadList(DecodedJWT pToken)
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
  public void setSelected(CListObject pSelected, boolean pDoScroll)
  {
    cList.setSelected(pSelected);
    if (pDoScroll)
      setScroll(cList.getScrollValue());
  }

  /**
   * Sets the value to which the ScrollPane should scroll/show
   * Only passes that information to the verticalScrollbar
   *
   * @param pScrollValue Scroll-Wert
   */
  public void setScroll(int pScrollValue)
  {
    scrollPane.getVerticalScrollBar().setValue(pScrollValue);
  }

  /**
   * Passes the setLoading request to the CList
   */
  public void setLoading()
  {
    cList.setLoading();
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
  public void setOldDefaultButton()
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
  public void setPanel(JPanel pPanelToShow)
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

  public JButton getRefreshButton()
  {
    return refreshButton;
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
  public void showErrorPanel(NotificationPanel.NotificationType pType, final Exception pException, final Runnable pActionOnRetry)
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
  public void showErrorPanel(NotificationPanel.NotificationType pType, String pErrorMessage, Action... pActions)
  {
    NotificationPanel panel = new NotificationPanel(pType.toIcon(), pErrorMessage, pActions);
    panel.setBackground(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
    setPanel(panel);
  }
}