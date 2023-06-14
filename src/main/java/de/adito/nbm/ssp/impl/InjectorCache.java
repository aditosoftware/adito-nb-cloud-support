package de.adito.nbm.ssp.impl;

import com.google.common.cache.*;
import com.google.inject.Module;
import com.google.inject.*;
import lombok.NonNull;

/**
 * @author m.kaspera, 17.09.2021
 */
public class InjectorCache
{

  private InjectorCache()
  {
  }

  private static final LoadingCache<Class<? extends AbstractModule>, Injector> cache = CacheBuilder.newBuilder().build(new CacheLoader<>()
  {
    @Override
    public Injector load(@NonNull Class<? extends AbstractModule> pKey) throws Exception
    {
      return Guice.createInjector((Module) Class.forName(pKey.getName()).getDeclaredConstructor().newInstance());
    }
  });

  public static Injector getInjector(@NonNull Class<? extends AbstractModule> pModule)
  {
    return cache.getUnchecked(pModule);
  }

}
