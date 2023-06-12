package de.adito.nbm.ssp.checkout;

import lombok.NonNull;

/**
 * @author m.kaspera, 05.03.2021
 */
public interface IStateChangeListener
{

  void changedValidity(@NonNull State pState);

  enum State
  {
    ISVALID,
    ISINVALID,
    CHANGED,
    FINISHED
  }

}
