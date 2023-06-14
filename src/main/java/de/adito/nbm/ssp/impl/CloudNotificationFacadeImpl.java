package de.adito.nbm.ssp.impl;

import com.google.inject.Singleton;
import de.adito.nbm.ssp.facade.ICloudNotificationFacade;
import lombok.NonNull;
import org.openide.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * @author m.kaspera, 16.09.2021
 */
@Singleton
public class CloudNotificationFacadeImpl implements ICloudNotificationFacade
{
  @NonNull
  @Override
  public Object notifyUser(@NonNull String pMessage, @NonNull String pTitle, @NonNull List<Object> pOptions)
  {
    if (pOptions.isEmpty())
      return new Object();
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(10, 5, 10, 5));
    contentPanel.add(new JLabel(pMessage));
    DialogDescriptor dialogDescriptor = new DialogDescriptor(contentPanel, pTitle, true, pOptions.toArray(), pOptions.get(0),
                                                             DialogDescriptor.BOTTOM_ALIGN, null, null);
    Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
    dialog.setResizable(true);
    dialog.setMinimumSize(new Dimension(250, 50));
    dialog.pack();
    dialog.setVisible(true);
    return dialogDescriptor.getValue();
  }
}
