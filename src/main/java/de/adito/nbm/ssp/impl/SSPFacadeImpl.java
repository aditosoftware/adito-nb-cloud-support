package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.exceptions.AditoSSPException;
import de.adito.nbm.ssp.facade.*;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author m.kaspera, 08.10.2020
 */
public class SSPFacadeImpl implements ISSPFacade, ILogin, ISystemExplorer
{

  private static final String LOGIN_SERVICE_URL = "https://test.ssp.adito.cloud/services/rest/testlogin";
  private static final String LIST_SYSTEMS_SERVICE_URL = "https://test.ssp.adito.cloud/services/rest/listSystems";
  private static final Base64.Encoder ENCODER = Base64.getEncoder();

  @NotNull
  @Override
  public DecodedJWT getJWT(@NotNull String pUsername, @NotNull char[] pPassword)
  {
    try
    {
      return login(pUsername, pPassword);
    }
    catch (Exception pE)
    {
      pE.printStackTrace();
    }
    throw new NotImplementedException("");
  }

  @NotNull
  @Override
  public List<ISSPSystem> getSystems(@NotNull String pUsername, @NotNull DecodedJWT pJWT)
  {
    try
    {
      return retrieveSystems(pUsername, pJWT);
    }
    catch (UnirestException | AditoSSPException pE)
    {
      pE.printStackTrace();
    }
    return List.of();
  }

  @NotNull
  @Override
  public ISSPSystemDetails getSystemDetails(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId)
  {
    throw new NotImplementedException("");
  }

  @Override
  public boolean isSystemRunning(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId)
  {
    throw new NotImplementedException("");
  }

  @Override
  public boolean startSystem(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId)
  {
    throw new NotImplementedException("");
  }

  @Override
  public boolean stopSystem(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId)
  {
    throw new NotImplementedException("");
  }

  @NotNull
  @Override
  public Base64.Encoder getEncoder()
  {
    return ENCODER;
  }

  @NotNull
  @Override
  public String getLoginServiceUrl()
  {
    return LOGIN_SERVICE_URL;
  }

  @NotNull
  @Override
  public String getListSystemsServiceUrl()
  {
    return LIST_SYSTEMS_SERVICE_URL;
  }
}
