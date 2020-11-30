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

  private static IconManager INSTANCE;
  private final IVaadinIconsProvider iconsProvider;
  private ImageIcon errorIcon;


  private IconManager()
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
  }

  @NotNull
  public static IconManager getInstance()
  {
    if (INSTANCE == null)
      INSTANCE = new IconManager();
    return INSTANCE;
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
}
