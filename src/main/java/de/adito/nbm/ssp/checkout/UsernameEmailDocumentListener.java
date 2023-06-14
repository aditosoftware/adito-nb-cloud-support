package de.adito.nbm.ssp.checkout;

import de.adito.nbm.ssp.WarningPanel;
import lombok.NonNull;
import org.openide.util.NbBundle;

import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import java.util.logging.*;

/**
 * Document Listener that checks for usernames that might be email adresses and triggers the passed WarningPanel if that is the case
 *
 * @author m.kaspera, 11.05.2021
 */
public class UsernameEmailDocumentListener implements DocumentListener
{
  private final WarningPanel warningPanel;
  private final String warningMessage = NbBundle.getMessage(SSPCheckoutProjectVisualPanel1.class, "SSPCheckoutProjectVisualPanel1.warning.email");

  public UsernameEmailDocumentListener(@NonNull WarningPanel pWarningPanel)
  {
    warningPanel = pWarningPanel;
  }

  @Override
  public void insertUpdate(DocumentEvent e)
  {
    _checkUsername(warningPanel, e);
  }

  @Override
  public void removeUpdate(DocumentEvent e)
  {
    _checkUsername(warningPanel, e);
  }

  @Override
  public void changedUpdate(DocumentEvent e)
  {
    _checkUsername(warningPanel, e);
  }

  /**
   * @param pWarningPanel  WarningPanel used for displaying the warning
   * @param pDocumentEvent DocumentEvent with the update
   */
  private void _checkUsername(@NonNull WarningPanel pWarningPanel, @NonNull DocumentEvent pDocumentEvent)
  {
    try
    {
      if (pDocumentEvent.getDocument().getText(0, pDocumentEvent.getDocument().getLength()).contains("@"))
      {
        pWarningPanel.setMessage(warningMessage);
        pWarningPanel.setVisible(true);
      }
      else
      {
        pWarningPanel.setVisible(false);
      }
    }
    catch (BadLocationException pE)
    {
      Logger.getLogger(UsernameEmailDocumentListener.class.getName()).log(Level.INFO, pE.getMessage());
    }
  }
}
