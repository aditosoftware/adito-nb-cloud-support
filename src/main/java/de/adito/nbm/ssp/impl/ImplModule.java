package de.adito.nbm.ssp.impl;

import com.google.inject.AbstractModule;
import de.adito.nbm.ssp.facade.ISSPFacade;

/**
 * @author m.kaspera, 30.10.2020
 */
public class ImplModule extends AbstractModule
{
  @Override
  protected void configure()
  {
    bind(ISSPFacade.class).to(SSPFacadeImpl.class);
  }
}