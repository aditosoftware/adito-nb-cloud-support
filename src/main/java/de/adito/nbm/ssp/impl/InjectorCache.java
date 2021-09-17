package de.adito.nbm.ssp.impl;

import com.google.common.cache.*;
import com.google.inject.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author m.kaspera, 17.09.2021
 */
public class InjectorCache
{

  private InjectorCache()
  {
  }

  private static final LoadingCache<AbstractModule, Injector> cache = CacheBuilder.newBuilder().build(new CacheLoader<>()
  {
    @Override
    public Injector load(@NotNull AbstractModule pKey)
    {
      return Guice.createInjector(pKey);
    }
  });

  public static Injector getInjector(@NotNull AbstractModule pModule)
  {
    return cache.getUnchecked(pModule);
  }

}
