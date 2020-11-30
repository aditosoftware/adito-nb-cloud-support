package de.adito.nbm.ssp.checkout.clist;

import javax.swing.event.ChangeListener;
import javax.swing.text.*;
import java.awt.*;

/**
 * Caret-Adapter that suppresses the marking function of a TextArea
 * Must be manually added to each TextArea that should have this feature active
 *
 * @author W.Glanzer, 02.10.13
 */
public class CListCaretAdapter implements Caret
{
  public void addChangeListener(ChangeListener l)
  {
  }

  public void deinstall(JTextComponent c)
  {
  }

  public int getBlinkRate()
  {
    return 0;
  }

  public int getDot()
  {
    return 0;
  }

  public Point getMagicCaretPosition()
  {
    return new Point(0, 0);
  }

  public int getMark()
  {
    return 0;
  }

  public void install(JTextComponent c)
  {
  }

  public boolean isSelectionVisible()
  {
    return false;
  }

  public boolean isVisible()
  {
    return false;
  }

  public void moveDot(int dot)
  {
  }

  public void paint(Graphics g)
  {
  }

  public void removeChangeListener(ChangeListener l)
  {
  }

  public void setBlinkRate(int rate)
  {
  }

  public void setDot(int dot)
  {
  }

  public void setMagicCaretPosition(Point p)
  {
  }

  public void setSelectionVisible(boolean v)
  {
  }

  public void setVisible(boolean v)
  {
  }
}
