package de.adito.nbm.cloud.runconfig.options;

import javax.swing.text.*;
import java.util.regex.Pattern;

/**
 * Allows only numbers as input
 *
 * @author m.kaspera, 04.08.2020
 */
class IntegerDocumentFilter extends DocumentFilter
{
  private final Pattern pattern = Pattern.compile("\\d*");

  @Override
  public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException
  {
    if (pattern.matcher(string).matches())
      super.insertString(fb, offset, string, attr);
  }

  @Override
  public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
  {
    if (pattern.matcher(text).matches())
      super.replace(fb, offset, length, text, attrs);
  }
}
