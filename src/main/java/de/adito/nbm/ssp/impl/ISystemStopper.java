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
 * @author m.kaspera, 20.10.2020
 */
interface ISystemStopper
{

  /**
   * @param pUsername name of the user
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId Id of the system whose status should be checked
   * @return true if the server returns that it is stopping the system, false otherwise
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  default boolean doStopSystem(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject systemDetailsBody = new JSONObject(Map.of(ISystemExplorer.USER_KEY, pUsername, ISystemExplorer.TOKEN_KEY, pJWT.getToken(),
                                                         ISystemExplorer.SYSTEM_ID_KEY, pSystemId));
    HttpResponse<JsonNode> listSystemsResponse = Unirest.post(getSystemStopUrl())
        .headers(headers)
        .body(systemDetailsBody)
        .asJson();
    HttpUtil.verifyStatus(listSystemsResponse);
    Object replicaCount = listSystemsResponse.getBody().getObject().get("replicaCount");
    return replicaCount != null && (int) replicaCount == 0;
  }

  @NotNull
  String getSystemStopUrl();

}
