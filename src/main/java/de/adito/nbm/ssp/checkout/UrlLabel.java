package de.adito.nbm.ssp.checkout;

import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.util.logging.*;

/**
 * Label that acts and looks like a normal html link - including mouseOver and color
 *
 * @author m.kaspera, 09.12.2020
 */
public class UrlLabel extends JPanel
{

  private final JLabel label;
  private final String labelText;
  private final String labelHoverText;
  private final String url;

  public UrlLabel(@NonNull String pLabelText, @NonNull String pUrl)
  {
    super(new BorderLayout());
    labelText = pLabelText;
    url = pUrl;
    labelHoverText = "<html><a href=\"" + pUrl + "\">" + pLabelText + "</a></html>";
    label = new JLabel(labelText);
    Color color = UIManager.getColor("nb.html.link.foreground");
    if (color == null)
      color = Color.BLUE.darker();
    label.setForeground(color);
    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    label.addMouseListener(new HyperlinkMouseAdapter());
    add(label, BorderLayout.WEST);
  }

  /**
   * Changes the text of the label if the mouse enters the label to a full html link and resets the text back to the labelText part after the mouse exits.
   * This makes it look like a normal html link, in that it is only underscored if the mouse is over the label.
   * Also calls the given url if the label is clicked
   */
  private class HyperlinkMouseAdapter extends MouseAdapter
  {
    @Override
    public void mouseEntered(MouseEvent e)
    {
      label.setText(labelHoverText);
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
      label.setText(labelText);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
      try
      {
        Desktop.getDesktop().browse(new URI(url));
      }
      catch (IOException | URISyntaxException pE)
      {
        Logger.getLogger(UrlLabel.class.getName()).log(Level.WARNING, pE, () -> "Error while trying to open the underlying address of the UrlLabel. Adress: " + url);
      }
    }
  }
}
