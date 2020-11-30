package de.adito.nbm.ssp.checkout;

import org.jetbrains.annotations.NotNull;
import org.netbeans.spi.project.ui.support.ProjectChooser;

import javax.swing.*;
import java.awt.*;

/**
 * Implementation des DefaultSettingCallbacks
 *
 * @author s.danner, 19.09.13
 */
public class DefaultSettingsCallback implements IDefaultSettingsCallback
{

  @Override
  public String getDefaultProjectPath()
  {
    return ProjectChooser.getProjectsFolder().getAbsolutePath();
  }

  @Override
  public int getDefaultScrollSpeed()
  {
    return 40;
  }

  @NotNull
  @Override
  public Color getDefaultBackgroundColor()
  {
    return new JLabel().getBackground();
  }

}
