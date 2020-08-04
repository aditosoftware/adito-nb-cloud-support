package de.adito.nbm.cloud.runconfig.options;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

import static de.adito.nbm.cloud.runconfig.TelnetLoggerRunConfig.*;

/**
 * Adds a tab in the options menu
 *
 * @author m.kaspera, 03.08.2020
 */
@OptionsPanelController.SubRegistration(displayName = "Cloud", id = "cloud", position = 400, location = "Adito")
public class CloudPluginOptionsPanelController extends OptionsPanelController
{

  private final Preferences preferences;
  private final CloudPluginOptionsPanel pluginOptionsPanel;
  private String bufferValue;

  public CloudPluginOptionsPanelController()
  {
    preferences = NbPreferences.forModule(CloudPluginOptionsPanel.class);
    pluginOptionsPanel = new CloudPluginOptionsPanel();
  }

  @Override
  public void update()
  {
    bufferValue = preferences.get(BACKPRESSURE_INITIAL_SIZE, String.valueOf(BACKPRESSURE_DEFAULT_BUFFER_SIZE));
    pluginOptionsPanel.setLoggingBufferSize(bufferValue);
  }

  @Override
  public void applyChanges()
  {
    preferences.put(BACKPRESSURE_INITIAL_SIZE, pluginOptionsPanel.getLoggingBufferSize());
    bufferValue = pluginOptionsPanel.getLoggingBufferSize();
  }

  @Override
  public void cancel()
  {
    // nothing to do here, the text field is re-set via the preferences if called again, and the current value is not stored anyways
  }

  @Override
  public boolean isValid()
  {
    try
    {
      return Integer.parseInt(pluginOptionsPanel.getLoggingBufferSize()) > 0;
    }
    catch (Exception pException)
    {
      return false;
    }
  }

  @Override
  public boolean isChanged()
  {
    return !bufferValue.equals(pluginOptionsPanel.getLoggingBufferSize());
  }

  @Override
  public JComponent getComponent(Lookup masterLookup)
  {
    return pluginOptionsPanel;
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l)
  {
    pluginOptionsPanel.getLoggingBufferSizeField().addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l)
  {
    pluginOptionsPanel.getLoggingBufferSizeField().removePropertyChangeListener(l);
  }
}
