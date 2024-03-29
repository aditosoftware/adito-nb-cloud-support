package de.adito.nbm.ssp.facade;

import de.adito.nbm.ssp.impl.*;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;

/**
 * Facade for querying the system status (is a given SSP system currently running or not)
 *
 * @author m.kaspera, 21.09.2021
 */
public interface ISystemStatusFacade
{

  static ISystemStatusFacade getInstance()
  {
    return InjectorCache.getInjector(ImplModule.class).getInstance(ISystemStatusFacade.class);
  }

  /**
   * @param pCloudId CloudId of the system in question
   * @return Observable with the value of the latest value that was returned by a query to the SSP if the specified system is running
   */
  @NonNull
  Observable<Boolean> getIsSystemRunningObservable(@NonNull String pCloudId);

  /**
   * @param pCloudId CloudId of the system in question
   * @return the last stored value of the system, false if no previous value was saved
   */
  @NonNull
  Boolean triggerIsSystemRunningUpdate(@NonNull String pCloudId);

}
