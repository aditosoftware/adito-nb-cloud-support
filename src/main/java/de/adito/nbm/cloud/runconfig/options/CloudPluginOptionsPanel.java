package de.adito.nbm.cloud.runconfig.options;

import de.adito.swing.TableLayoutUtil;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.text.*;

/**
 * @author m.kaspera, 03.08.2020
 */
public class CloudPluginOptionsPanel extends JPanel
{

  private final JTextField loggingBufferSizeField;

  public CloudPluginOptionsPanel()
  {
    loggingBufferSizeField = new JTextField();
    Document document = loggingBufferSizeField.getDocument();
    if (document instanceof AbstractDocument)
      ((AbstractDocument) document).setDocumentFilter(new IntegerDocumentFilter());
    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    final double gap = 15;
    double[] cols = {gap, pref, gap, fill, gap};
    double[] rows = {gap,
                     pref,
                     gap,
                     pref,
                     gap};
    setLayout(new TableLayout(cols, rows));
    TableLayoutUtil tlu = new TableLayoutUtil(this);
    tlu.add(1, 1, new JLabel("Buffer size used for logging with RXJava Backpressure"));
    tlu.add(3, 1, loggingBufferSizeField);
  }

  /**
   * sets the bufferSize field
   *
   * @param pBufferSize size of the buffer that should be set in the field
   */
  void setLoggingBufferSize(String pBufferSize)
  {
    loggingBufferSizeField.setText(pBufferSize);
  }

  /**
   * @return the bufferSize currently set in the field
   */
  String getLoggingBufferSize()
  {
    return loggingBufferSizeField.getText();
  }

  /**
   * @return the JTextField used for displaying the bufferSize
   */
  JTextField getLoggingBufferSizeField()
  {
    return loggingBufferSizeField;
  }

}
