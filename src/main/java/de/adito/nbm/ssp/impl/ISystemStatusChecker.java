package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.HttpUtil;
import de.adito.nbm.ssp.exceptions.*;
import lombok.NonNull;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author m.kaspera, 20.10.2020
 */
interface ISystemStatusChecker
{

  /**
   * @param pUsername name of the user
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId Id of the system whose status should be checked
   * @return true if the system is running on one or more nodes, false otherwise
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  default boolean checkSystemStatus(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull String pSystemId) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject systemDetailsBody = new JSONObject(Map.of(ISystemExplorer.USER_KEY, pUsername, ISystemExplorer.TOKEN_KEY, pJWT.getToken(),
                                                         ISystemExplorer.SYSTEM_ID_KEY, pSystemId));
    HttpResponse<JsonNode> listSystemsResponse = Unirest.post(getSystemStatusUrl())
        .headers(headers)
        .body(systemDetailsBody)
        .asJson();
    HttpUtil.verifyStatus(listSystemsResponse);
    return listSystemsResponse.getBody().getObject().getBoolean("isRunning");
  }

  @NonNull
  String getSystemStatusUrl();
}
