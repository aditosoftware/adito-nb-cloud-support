package de.adito.nbm.icons;

import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.swing.icon.IconAttributes;
import org.jetbrains.annotations.*;
import org.openide.util.Lookup;

import javax.swing.*;
import java.awt.*;

/**
 * @author m.kaspera, 02.11.2020
 */
public class IconManager
{

  private static IconManager instance;
  private final IVaadinIconsProvider iconsProvider;
  private ImageIcon errorIcon;


  private IconManager()
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
  }

  @NotNull
  public static IconManager getInstance()
  {
    if (instance == null)
      instance = new IconManager();
    return instance;
  }

  @Nullable
  public ImageIcon getErrorIcon()
  {
    if (errorIcon == null && iconsProvider != null)
    {
      Image image = iconsProvider.findImage(IVaadinIconsProvider.VaadinIcon.STOP, new IconAttributes.Builder().create());
      if (image != null)
        errorIcon = new ImageIcon(image);
    }
    return errorIcon;
  }

  /**
   * @return Icon for marking a warning
   */
  @Nullable
  public ImageIcon getWarningIcon()
  {
    if (errorIcon == null && iconsProvider != null)
    {
      Image image = iconsProvider.findImage(IVaadinIconsProvider.VaadinIcon.WARNING, new IconAttributes.Builder()
          .setColor(Color.YELLOW)
          .setSize(12f)
          .create());
      if (image != null)
        errorIcon = new ImageIcon(image);
    }
    return errorIcon;
  }
}
