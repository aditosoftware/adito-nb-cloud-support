package de.adito.nbm.ssp.checkout.clist;

import javax.swing.*;
import java.awt.*;

/**
 * This class represents the title picture in form of a JLabel
 * Is used by the CListObject and the SSPCheckoutProjectVisualPanel1
 *
 * @author W.Glanzer, 23.09.13
 */
public class CListObjectPicture extends JLabel
{
  public static final int MAX_HEIGHT = 80;
  public static final int MAX_WIDTH = 80;
  public static final int MIN_WIDTH = MAX_WIDTH;
  public static final int MIN_HEIGHT = MAX_HEIGHT;

  public CListObjectPicture(Image pImage)
  {
    setPreferredSize(new Dimension(MAX_WIDTH, MAX_HEIGHT));
    setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
    setMaximumSize(new Dimension(MAX_WIDTH, MAX_HEIGHT));
    setSize(MAX_WIDTH, MAX_HEIGHT);
    setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black));
    setHorizontalAlignment(JLabel.CENTER);
    setVerticalAlignment(JLabel.CENTER);
    if (pImage != null)
    {
      setIcon(new ImageIcon(pImage.getScaledInstance(MAX_WIDTH, MAX_HEIGHT, Image.SCALE_SMOOTH)));
    }
  }
}
