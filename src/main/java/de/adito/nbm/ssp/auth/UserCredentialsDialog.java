package de.adito.nbm.ssp.auth;

import de.adito.swing.TableLayoutUtil;
import info.clearthought.layout.TableLayout;
import org.jetbrains.annotations.*;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.event.DocumentListener;

/**
 * @author m.kaspera, 12.10.2020
 */
public class UserCredentialsDialog extends JPanel
{

  private static final int PW_FIELD_NUM_CHARS = 30;
  private static final String USERNAME_FIELD_LABEL = NbBundle.getMessage(UserCredentialsDialog.class, "LBL.UserCredentialsDialog.username");
  private static final String PASSWORD_FIELD_LABEL = NbBundle.getMessage(UserCredentialsDialog.class, "LBL.UserCredentialsDialog.password");
  private final JTextField usernameField = new JTextField();
  private final JPasswordField passwordField = new JPasswordField(PW_FIELD_NUM_CHARS);
  private final JCheckBox rememberPasswordCheckbox = new JCheckBox(NbBundle.getMessage(UserCredentialsDialog.class, "LBL.UserCredentialsDialog.password.store"));

  UserCredentialsDialog(@Nullable String pUsername, @NotNull char[] pPassword)
  {
    _initGui();
    if (pUsername != null)
      usernameField.setText(pUsername);
    if (pPassword.length > 0)
    {
      passwordField.setText(new String(pPassword));
      rememberPasswordCheckbox.setSelected(true);
    }
  }

  private void _initGui()
  {
    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    final double gap = 15;
    double[] cols = {gap, pref, gap, fill, gap};
    double[] rows = {gap,
                     pref,
                     gap,
                     pref,
                     gap,
                     pref,
                     gap,
                     pref,
                     gap};
    setLayout(new TableLayout(cols, rows));
    TableLayoutUtil tlu = new TableLayoutUtil(this);
    tlu.add(1, 1, 3, 1, new JLabel(NbBundle.getMessage(UserCredentialsDialog.class, "LBL.UserCredentialsDialog.origin")));
    tlu.add(1, 3, new JLabel(USERNAME_FIELD_LABEL));
    tlu.add(3, 3, usernameField);
    tlu.add(1, 5, new JLabel(PASSWORD_FIELD_LABEL));
    tlu.add(3, 5, passwordField);
    tlu.add(3, 7, rememberPasswordCheckbox);
  }

  @NotNull
  public String getUsername()
  {
    return usernameField.getText();
  }

  @NotNull
  public char[] getPassword()
  {
    return passwordField.getPassword();
  }

  public boolean isRememberPassword()
  {
    return rememberPasswordCheckbox.isSelected();
  }

  public void discard()
  {
    passwordField.setText("");
  }

  public void addPasswordFieldDocumentListener(@NotNull DocumentListener pDocumentListener)
  {
    passwordField.getDocument().addDocumentListener(pDocumentListener);
  }

  public void addUsernameFieldDocumentListener(@NotNull DocumentListener pDocumentListener)
  {
    passwordField.getDocument().addDocumentListener(pDocumentListener);
  }

}
