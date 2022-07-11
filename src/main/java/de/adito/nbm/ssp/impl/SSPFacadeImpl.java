package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Singleton;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.http.proxy.ProxyClientProvider;
import de.adito.nbm.ssp.checkout.SSPCheckoutProjectWizardIterator;
import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.facade.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.HttpClient;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.NotificationDisplayer;

import java.security.*;
import java.util.*;

/**
 * @author m.kaspera, 08.10.2020
 */
@Singleton
public class SSPFacadeImpl implements ISSPFacade, ILogin, ISystemExplorer, ISystemStatusChecker, ISystemStarter, ISystemStopper, ISSPConfigRetriever
{

  private static final String DEFAULT_SSP_SYSTEM_URL = "https://ssp.adito.cloud/";
  private static final String LOGIN_SERVICE_ADDRESS = "services/rest/testlogin";
  private static final String LIST_SYSTEMS_SERVICE_ADDRESS = "services/rest/listSystems";
  private static final String SYSTEM_DETAILS_SERVICE_ADDRESS = "services/rest/getSystemInformations";
  private static final String SYSTEM_STATUS_SERVICE_ADDRESS = "services/rest/systemIsRunning";
  private static final String SYSTEM_START_SERVICE_ADDRESS = "services/rest/startSystem";
  private static final String SYSTEM_STOP_SERVICE_ADDRESS = "services/rest/stopSystem";
  private static final String SYSTEM_TUNNEL_CONFIG_SERVICE_ADDRESS = "services/rest/getTunnelConfig";
  private static final String SYSTEM_SERVER_CONFIG_SERVICE_ADDRESS = "services/rest/getServerConfig";
  private static final String SYSTEM_CONFIG_MAP_SERVICE_ADDRESS = "services/rest/getConfigMap";
  private static final Base64.Encoder ENCODER = Base64.getEncoder();
  private final String sspSystemUrl;

  public SSPFacadeImpl()
  {
    sspSystemUrl = getSSPSystemUrl();
    setProxy();
    // This is a singleton, and the preferencesNode should always be there -> no need to unregister the listener
    ProxyClientProvider.addProxySettingsListener(evt -> setProxy());
  }

  /**
   * Checks if a command line parameter overriding the default ssp url is set, and returns either that url or the default ssp url if no parameter is set
   *
   * @return the url of the SSP system to be used
   */
  public static String getSSPSystemUrl()
  {
    String sspUrlProperty = System.getProperty("adito.ssp.url");
    return sspUrlProperty == null ? DEFAULT_SSP_SYSTEM_URL : sspUrlProperty;
  }

  @NotNull
  @Override
  public DecodedJWT getJWT(@NotNull String pUsername, @NotNull char[] pPassword) throws AditoSSPException, UnirestException
  {
    return login(pUsername, pPassword);
  }

  @NotNull
  @Override
  public List<ISSPSystem> getSystems(@NotNull String pUsername, @NotNull DecodedJWT pJWT) throws AditoSSPException, UnirestException
  {
    return retrieveSystems(pUsername, pJWT);
  }

  @NotNull
  @Override
  public ISSPSystemDetails getSystemDetails(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws MalformedInputException,
      UnirestException, AditoSSPException, AditoSSPParseException
  {
    return retrieveDetails(pUsername, pJWT, pSystem);
  }

  @Override
  public String getServerConfig(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException
  {
    return doGetServerConfig(pUsername, pJWT, pSystem.getSystemdId());
  }

  @Override
  public String getTunnelConfig(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException
  {
    return doGetTunnelConfig(pUsername, pJWT, pSystem.getSystemdId());
  }

  @Override
  public Map<String, String> getConfigMap(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException
  {
    return retrieveConfigMap(pUsername, pJWT, pSystem);
  }

  @Override
  public boolean isSystemRunning(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId)
  {
    try
    {
      return checkSystemStatus(pUsername, pJWT, pSystemId);
    }
    catch (UnirestException | AditoSSPException pE)
    {
      NotificationDisplayer.getDefault().notify(SSPCheckoutProjectWizardIterator.getMessage(SSPFacadeImpl.class, "LBL.SSPFacadeImpl.checkSystem.error"),
                                                NotificationDisplayer.Priority.HIGH.getIcon(),
                                                SSPCheckoutProjectWizardIterator.getMessage(SSPFacadeImpl.class, "TXT.SSPFacadeImpl.checkSystem.error",
                                                                                            pSystemId, ExceptionUtils.getStackTrace(pE)),
                                                null, NotificationDisplayer.Priority.HIGH);
      return false;
    }
  }

  @Override
  public boolean startSystem(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId)
  {
    try
    {
      return doStartSystem(pUsername, pJWT, pSystemId);
    }
    catch (UnirestException | AditoSSPException pE)
    {
      NotificationDisplayer.getDefault().notify(SSPCheckoutProjectWizardIterator.getMessage(SSPFacadeImpl.class, "LBL.SSPFacadeImpl.startSystem.error"),
                                                NotificationDisplayer.Priority.HIGH.getIcon(),
                                                SSPCheckoutProjectWizardIterator.getMessage(SSPFacadeImpl.class, "TXT.SSPFacadeImpl.startSystem.error",
                                                                                            pSystemId, ExceptionUtils.getStackTrace(pE)),
                                                null, NotificationDisplayer.Priority.HIGH);
      return false;
    }
  }

  @Override
  public boolean stopSystem(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId)
  {
    try
    {
      return doStopSystem(pUsername, pJWT, pSystemId);
    }
    catch (UnirestException | AditoSSPException pE)
    {
      NotificationDisplayer.getDefault().notify(SSPCheckoutProjectWizardIterator.getMessage(SSPFacadeImpl.class, "LBL.SSPFacadeImpl.stopSystem.error"),
                                                NotificationDisplayer.Priority.HIGH.getIcon(),
                                                SSPCheckoutProjectWizardIterator.getMessage(SSPFacadeImpl.class, "TXT.SSPFacadeImpl.stopSystem.error",
                                                                                            pSystemId, ExceptionUtils.getStackTrace(pE)),
                                                null, NotificationDisplayer.Priority.HIGH);
      return false;
    }
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
    return sspSystemUrl + LOGIN_SERVICE_ADDRESS;
  }

  @NotNull
  @Override
  public String getListSystemsServiceUrl()
  {
    return sspSystemUrl + LIST_SYSTEMS_SERVICE_ADDRESS;
  }

  @Override
  public String getListSystemDetailsServiceUrl()
  {
    return sspSystemUrl + SYSTEM_DETAILS_SERVICE_ADDRESS;
  }


  @NotNull
  @Override
  public String getSystemStatusUrl()
  {
    return sspSystemUrl + SYSTEM_STATUS_SERVICE_ADDRESS;
  }

  @NotNull
  @Override
  public String getSystemStartUrl()
  {
    return sspSystemUrl + SYSTEM_START_SERVICE_ADDRESS;
  }

  @NotNull
  @Override
  public String getSystemStopUrl()
  {
    return sspSystemUrl + SYSTEM_STOP_SERVICE_ADDRESS;
  }

  @NotNull
  @Override
  public String getServerConfigServiceUrl()
  {
    return sspSystemUrl + SYSTEM_SERVER_CONFIG_SERVICE_ADDRESS;
  }

  @NotNull
  @Override
  public String getTunnelConfigServiceUrl()
  {
    return sspSystemUrl + SYSTEM_TUNNEL_CONFIG_SERVICE_ADDRESS;
  }

  @NotNull
  @Override
  public String getSystemConfigMapServiceUrl()
  {
    return sspSystemUrl + SYSTEM_CONFIG_MAP_SERVICE_ADDRESS;
  }

  void setProxy()
  {
    try
    {
      HttpClient client = ProxyClientProvider.getClient(false);
      Unirest.setHttpClient(client);
    }
    catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException pIgnored)
    {
      pIgnored.printStackTrace();
      // ignore exception, because none of these is thrown if certificates are not ignored
    }
  }
}
