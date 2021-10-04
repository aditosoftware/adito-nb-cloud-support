package de.adito.nbm.ssp.facade;

import de.adito.nbm.ssp.impl.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Offers methods for notifying or warning the user with messages in a dialog
 *
 * @author m.kaspera, 16.09.2021
 */
public interface INotificationFacade
{

  static INotificationFacade getInstance()
  {
    return InjectorCache.getInjector(ImplModule.getInstance()).getInstance(INotificationFacade.class);
  }

  /**
   * Displays a dialog that shows the user the message given as pMessage and offers the given Options to respond
   *
   * @param pMessage Message displayed for the user
   * @param pOptions Selectable buttons for the user
   * @return the selected option the user chose
   */
  @NotNull
  Object notifyUser(@NotNull String pMessage, @NotNull String pTitle, @NotNull List<Object> pOptions);

}
