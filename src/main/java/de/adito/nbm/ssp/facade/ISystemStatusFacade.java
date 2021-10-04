package de.adito.nbm.ssp.facade;

import de.adito.nbm.ssp.impl.*;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;

/**
 * Facade for querying the system status (is a given SSP system currently running or not)
 *
 * @author m.kaspera, 21.09.2021
 */
public interface ISystemStatusFacade
{

  static ISystemStatusFacade getInstance()
  {
    return InjectorCache.getInjector(ImplModule.getInstance()).getInstance(ISystemStatusFacade.class);
  }

  /**
   * @param pCloudId CloudId of the system in question
   * @return Observable with the value of the latest value that was returned by a query to the SSP if the specified system is running
   */
  @NotNull
  Observable<Boolean> getIsSystemRunningObservable(@NotNull String pCloudId);

  /**
   * @param pCloudId CloudId of the system in question
   * @return the last stored value of the system, false if no previous value was saved
   */
  @NotNull
  Boolean triggerIsSystemRunningUpdate(@NotNull String pCloudId);

}
