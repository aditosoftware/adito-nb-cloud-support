package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.HttpUtil;
import de.adito.nbm.ssp.exceptions.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author m.kaspera, 20.11.2020
 */
public interface ISSPConfigRetriever
{

  /**
   * @param pUsername name of the user
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId Id of the system whose server config should be retrieved
   * @return the contents of the server config file
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  default String doGetServerConfig(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject systemDetailsBody = new JSONObject(Map.of("user", pUsername, "jwt", pJWT.getToken(), ISystemExplorer.SYSTEM_ID_KEY, pSystemId));
    HttpResponse<String> serverConfigResponse = Unirest.post(getServerConfigServiceUrl())
        .headers(headers)
        .body(systemDetailsBody)
        .asString();
    HttpUtil.verifyStatus(serverConfigResponse);
    return serverConfigResponse.getBody();
  }

  /**
   * @param pUsername name of the user
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId Id of the system whose tunnel config should be retrieved
   * @return the contents of the tunnel config file
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  default String doGetTunnelConfig(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject systemDetailsBody = new JSONObject(Map.of("user", pUsername, "jwt", pJWT.getToken(), ISystemExplorer.SYSTEM_ID_KEY, pSystemId));
    HttpResponse<String> tunnelConfigResponse = Unirest.post(getTunnelConfigServiceUrl())
        .headers(headers)
        .body(systemDetailsBody)
        .asString();
    HttpUtil.verifyStatus(tunnelConfigResponse);
    return tunnelConfigResponse.getBody();
  }

  @NotNull
  String getServerConfigServiceUrl();

  @NotNull
  String getTunnelConfigServiceUrl();

}
