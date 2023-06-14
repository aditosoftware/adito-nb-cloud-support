package de.adito.nbm.ssp;

import de.adito.nbm.icons.IconManager;
import lombok.NonNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author m.kaspera, 11.05.2021
 */
public class WarningPanel extends JPanel
{
  private final JLabel warningLabel = new JLabel();

  public WarningPanel()
  {
    super(new BorderLayout());
    add(new JLabel(IconManager.getInstance().getWarningIcon()), BorderLayout.WEST);
    warningLabel.setBorder(new EmptyBorder(3, 2, 3, 0));
    add(warningLabel, BorderLayout.CENTER);
  }

  public void setMessage(@NonNull String pMessage)
  {
    warningLabel.setText(pMessage);
  }
}
