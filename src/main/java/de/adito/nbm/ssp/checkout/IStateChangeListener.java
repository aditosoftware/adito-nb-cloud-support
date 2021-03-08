package de.adito.nbm.ssp.checkout;

import org.jetbrains.annotations.NotNull;

/**
 * @author m.kaspera, 05.03.2021
 */
public interface IStateChangeListener
{

  void changedValidity(@NotNull State pState);

  enum State
  {
    ISVALID,
    ISINVALID,
    CHANGED,
    FINISHED
  }

}
