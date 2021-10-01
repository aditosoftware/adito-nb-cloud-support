package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.*;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.*;
import de.adito.util.reactive.cache.ObservableCache;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.*;

/**
 * @author m.kaspera, 21.09.2021
 */
public class SystemStatusFacadeImpl implements ISystemStatusFacade
{

  private static final ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
  private static final Cache<String, Subject<String>> SUBJECT_CACHE = CacheBuilder.newBuilder().build();
  private static final Cache<String, Observable<Boolean>> OBS_CACHE = CacheBuilder.newBuilder().build();
  private static final ObservableCache OBSERVABLE_CACHE = new ObservableCache();

  @NotNull
  @Override
  public Observable<Boolean> getIsSystemRunningObservable(@NotNull String pCloudId)
  {
    try
    {
      OBSERVABLE_CACHE.calculate(pCloudId, () -> _createSubjectAndObs(pCloudId).right);
      return OBS_CACHE.get(pCloudId, () -> _createSubjectAndObs(pCloudId).right);
    }
    catch (ExecutionException pE)
    {
      pE.printStackTrace();
      return Observable.just(Boolean.FALSE);
    }
  }

  @NotNull
  @Override
  public Boolean triggerIsSystemRunningUpdate(@NotNull String pCloudId)
  {
    Subject<String> triggerUpdateSubject = SUBJECT_CACHE.getIfPresent(pCloudId);
    if (triggerUpdateSubject == null)
    {
      triggerUpdateSubject = _createSubjectAndObs(pCloudId).left;
    }
    triggerUpdateSubject.onNext(pCloudId);
    return Optional.ofNullable(OBS_CACHE.getIfPresent(pCloudId))
        .map(pObs -> pObs.blockingFirst(Boolean.FALSE))
        .orElse(Boolean.FALSE);
  }

  private ImmutablePair<Subject<String>, Observable<Boolean>> _createSubjectAndObs(@NotNull String pCloudId)
  {
    Subject<String> triggerUpdateSubject;
    triggerUpdateSubject = BehaviorSubject.createDefault(pCloudId);
    SUBJECT_CACHE.put(pCloudId, triggerUpdateSubject);
    Observable<Boolean> isRunningObs = triggerUpdateSubject.observeOn(Schedulers.from(backgroundThread))
        .throttleFirst(5, TimeUnit.SECONDS)
        .map(pId -> {
          ISSPFacade sspFacade = ISSPFacade.getInstance();
          DecodedJWT jwt = UserCredentialsManager.getCredentials();
          if (jwt != null)
          {
            return sspFacade.isSystemRunning(jwt.getSubject(), jwt, pCloudId);
          }
          return false;
        }).publish().autoConnect();
    OBS_CACHE.put(pCloudId, isRunningObs);
    return new ImmutablePair<>(triggerUpdateSubject, isRunningObs);
  }
}
