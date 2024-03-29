package de.adito.nbm.ssp.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.annotations.VisibleForTesting;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.exceptions.AditoSSPException;
import de.adito.nbm.ssp.facade.ISSPFacade;
import de.adito.notification.INotificationFacade;
import lombok.NonNull;
import org.jetbrains.annotations.*;
import org.netbeans.api.keyring.Keyring;
import org.openide.*;
import org.openide.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.*;
import java.util.prefs.Preferences;

/**
 * @author m.kaspera, 12.10.2020
 */
public class UserCredentialsManager
{

  private static final String LAST_USER_KEY = "de.adito.ssp.auth.lastUser";
  private static final String PASSWORT_STATIC_PART_KEY = "de.adito.ssp.auth.pass.";
  private static final Preferences preferences = NbPreferences.forModule(UserCredentialsManager.class);
  private static final Logger logger = Logger.getLogger(UserCredentialsManager.class.getName());
  private static DecodedJWT jwt = null;

  private UserCredentialsManager()
  {
  }

  @Nullable
  public static synchronized DecodedJWT getCredentials()
  {
    if (jwt == null)
    {
      String lastUser = getLastUser();
      char[] password = getLastUserPass();
      if (lastUser != null && password.length > 0)
      {
        getToken(lastUser, password);
      }
      if (jwt == null)
      {
        UserCredentialsDialog credentialsDialog = new UserCredentialsDialog(lastUser, password);
        JPanel borderPane = new NonScrollablePanel(new BorderLayout());
        borderPane.add(credentialsDialog, BorderLayout.CENTER);
        borderPane.setBorder(new EmptyBorder(7, 7, 0, 7));
        Object[] buttons = new Object[]{NotifyDescriptor.OK_OPTION, NotifyDescriptor.CANCEL_OPTION};
        DialogDescriptor dialogDescriptor = new DialogDescriptor(borderPane, NbBundle.getMessage(UserCredentialsManager.class, "LBL.UserCredentialsManager.title"),
                                                                 true, buttons, buttons[0], DialogDescriptor.BOTTOM_ALIGN, null, null);
        dialogDescriptor.setValid(credentialsDialog.getPassword().length > 0 && credentialsDialog.getUsername().length() > 0);
        CredentialsFieldListener isValidCredentialsListener = new CredentialsFieldListener(credentialsDialog, dialogDescriptor);
        credentialsDialog.addPasswordFieldDocumentListener(isValidCredentialsListener);
        credentialsDialog.addUsernameFieldDocumentListener(isValidCredentialsListener);

        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(250, 50));
        dialog.pack();
        dialog.setVisible(true);

        lastUser = credentialsDialog.getUsername();
        password = credentialsDialog.getPassword();

        if (dialogDescriptor.getValue() == buttons[0] && credentialsDialog.getPassword().length > 0)
        {
          saveLastUser(lastUser);
          if (credentialsDialog.isRememberPassword())
            Keyring.save(PASSWORT_STATIC_PART_KEY + lastUser, password,
                         NbBundle.getMessage(UserCredentialsManager.class, "LBL.UserCredentialsManager.credentials", lastUser));
          else
          {
            Keyring.delete(PASSWORT_STATIC_PART_KEY + lastUser);
          }
          getToken(lastUser, password);
        }
      }
    }
    return jwt;
  }

  @VisibleForTesting
  static void getToken(String lastUser, char[] password)
  {
    ISSPFacade sspFacade = ISSPFacade.getInstance();
    try
    {
      jwt = sspFacade.getJWT(lastUser, password);
    }
    catch (AditoSSPException | UnirestException pE)
    {
      if (!GraphicsEnvironment.isHeadless())
        INotificationFacade.INSTANCE.error(pE, NbBundle.getMessage(UserCredentialsManager.class, "LBL.UserCredentialsManager.login.fail"));
      else
        logger.log(Level.WARNING, pE, () -> NbBundle.getMessage(UserCredentialsManager.class, "LBL.UserCredentialsManager.login.fail.headless"));
    }
  }

  @Nullable
  public static DecodedJWT getCurrentCredentials()
  {
    // if the currently stored token is expired -> set token to null and return that
    if (jwt != null && jwt.getExpiresAt().toInstant().isBefore(Instant.now()))
    {
      jwt = null;
    }
    return jwt;
  }

  @Nullable
  public static String getLastUser()
  {
    return preferences.get(LAST_USER_KEY, null);
  }

  @NonNull
  public static char[] getLastUserPass()
  {
    String lastUser = preferences.get(LAST_USER_KEY, null);
    return Optional.ofNullable(lastUser).map(pUsername -> Keyring.read(PASSWORT_STATIC_PART_KEY + pUsername))
        .orElse(new char[0]);
  }

  public static void saveLastUser(@Nullable String pLastUser)
  {
    preferences.put(LAST_USER_KEY, pLastUser);
  }

  public static void saveToken(@NonNull DecodedJWT pJwt)
  {
    // only store the token if it is still valid
    if (pJwt.getExpiresAt().toInstant().isAfter(Instant.now()))
    {
      jwt = pJwt;
    }
  }

  /**
   * Extended JPanel that makes it so that the Panel does not provide scrolling in a ScrollPane, but instead requires wrapping or hiding of information
   * This can be of use if you want to make sure a panel can be resized to a smaller size even if it is in a ScrollPane
   */
  private static class NonScrollablePanel extends JPanel implements Scrollable
  {
    public NonScrollablePanel(LayoutManager layout)
    {
      super(layout);
    }

    /*
    Implementation of Scrollable, to disable scrolling in a scrollpane
     */
    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
      return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
      return 16;
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
      return true;
    }

  }

  private static class CredentialsFieldListener implements DocumentListener
  {
    private final UserCredentialsDialog userCredentialsDialog;
    private final DialogDescriptor dialogDescriptor;

    public CredentialsFieldListener(UserCredentialsDialog pUserCredentialsDialog, DialogDescriptor pDialogDescriptor)
    {
      userCredentialsDialog = pUserCredentialsDialog;
      dialogDescriptor = pDialogDescriptor;
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
      dialogDescriptor.setValid(userCredentialsDialog.getPassword().length > 0 && userCredentialsDialog.getUsername().length() > 0);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
      dialogDescriptor.setValid(userCredentialsDialog.getPassword().length > 0 && userCredentialsDialog.getUsername().length() > 0);
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
      dialogDescriptor.setValid(userCredentialsDialog.getPassword().length > 0 && userCredentialsDialog.getUsername().length() > 0);
    }
  }
}
