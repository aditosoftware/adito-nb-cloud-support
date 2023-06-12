package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.*;
import com.google.inject.Singleton;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.*;
import de.adito.util.reactive.cache.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.concurrent.*;

/**
 * @author m.kaspera, 21.09.2021
 */
@Singleton
public class SystemStatusFacadeImpl implements ISystemStatusFacade, Disposable
{

  private static final ExecutorService backgroundThread = Executors.newSingleThreadExecutor();
  private final Cache<String, Subject<String>> subjectCache = CacheBuilder.newBuilder().build();
  private final ObservableCache observableCache = new ObservableCache();
  private final ObservableCacheDisposable observableCacheDisposable = new ObservableCacheDisposable(observableCache);

  @NonNull
  @Override
  public Observable<Boolean> getIsSystemRunningObservable(@NonNull String pCloudId)
  {
    return observableCache.calculate(pCloudId, () -> _createSubjectAndObs(pCloudId).right);
  }

  @NonNull
  @Override
  public Boolean triggerIsSystemRunningUpdate(@NonNull String pCloudId)
  {
    Subject<String> triggerUpdateSubject = subjectCache.getIfPresent(pCloudId);
    if (triggerUpdateSubject == null)
    {
      triggerUpdateSubject = _createSubjectAndObs(pCloudId).left;
    }
    triggerUpdateSubject.onNext(pCloudId);
    return observableCache.calculate(pCloudId, () -> _createSubjectAndObs(pCloudId).right)
        .blockingFirst(Boolean.FALSE);
  }

  private ImmutablePair<Subject<String>, Observable<Boolean>> _createSubjectAndObs(@NonNull String pCloudId)
  {
    Subject<String> triggerUpdateSubject;
    try
    {
      triggerUpdateSubject = subjectCache.get(pCloudId, () -> BehaviorSubject.createDefault(pCloudId));
    }
    catch (ExecutionException pE)
    {
      triggerUpdateSubject = BehaviorSubject.createDefault(pCloudId);
      subjectCache.put(pCloudId, triggerUpdateSubject);
    }
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
    observableCache.calculate(pCloudId, () -> isRunningObs);
    return new ImmutablePair<>(triggerUpdateSubject, isRunningObs);
  }

  @Override
  public void dispose()
  {
    if (!observableCacheDisposable.isDisposed())
      observableCacheDisposable.dispose();
    subjectCache.invalidateAll();
  }

  @Override
  public boolean isDisposed()
  {
    return observableCacheDisposable.isDisposed();
  }
}
