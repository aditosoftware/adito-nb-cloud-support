package de.adito.nbm.icons;

import org.jetbrains.annotations.NotNull;
import org.openide.util.ImageUtilities;

import javax.swing.*;
import java.util.HashMap;

/**
 * @author m.kaspera, 15.07.2020
 */
public class NBIconLoader
{
  private final static HashMap<String, ImageIcon> iconCache = new HashMap<>();
  private final ImageIcon defaultIcon = new ImageIcon(ImageUtilities.icon2Image(MissingIcon.get16x16()));


  /**
   * Returns an Icon for a given IconBase.
   * Depending on the current Theme a dark or bright Icon is provided.
   *
   * @param pIconBase the IconBase
   * @return the Icon
   */
  @NotNull
  public ImageIcon getIcon(@NotNull String pIconBase)
  {
    if (iconCache.containsKey(pIconBase))
    {
      return iconCache.get(pIconBase);
    }
    else
    {
      ImageIcon icon = ImageUtilities.loadImageIcon(pIconBase, true);
      // return the default icon (signalling a missing icon) instead of null
      if (icon == null)
        return defaultIcon;
      iconCache.put(pIconBase, icon);
      return icon;
    }
  }
}
