package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.HttpUtil;
import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.facade.*;
import org.jetbrains.annotations.NotNull;
import org.json.*;
import org.openide.awt.NotificationDisplayer;

import java.util.*;

/**
 * @author m.kaspera, 09.10.2020
 */
interface ISystemExplorer
{

  String USER_KEY = "user";
  String TOKEN_KEY = "jwt";
  String SYSTEM_ID_KEY = "systemId";

  /**
   * @param pUsername name of the user
   * @param pJWT      JSON Web Token for authentication purposes
   * @return List of ISSPSystems that the user has access to
   * @throws UnirestException                            if an error occurs during the rest call
   * @throws com.auth0.jwt.exceptions.JWTDecodeException if the JWT Token returned by the server cannot be decoded
   * @throws AditoSSPServerException                     if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException               if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException                           if the response of the server contains an error status other than the ones listed above
   */
  @NotNull
  default List<ISSPSystem> retrieveSystems(@NotNull String pUsername, @NotNull DecodedJWT pJWT) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject listSystemBody = new JSONObject(Map.of(USER_KEY, pUsername, TOKEN_KEY, pJWT.getToken()));
    HttpResponse<JsonNode> listSystemsResponse = Unirest.post(getListSystemsServiceUrl())
        .headers(headers)
        .body(listSystemBody)
        .asJson();
    HttpUtil.verifyStatus(listSystemsResponse);
    List<ISSPSystem> sspSystems = new ArrayList<>();
    JSONObject jsonObject = listSystemsResponse.getBody().getObject();
    for (String key : jsonObject.keySet())
    {
      try
      {
        sspSystems.add(new SSPSystemImpl((JSONArray) jsonObject.get(key)));
      }
      catch (MalformedInputException pE)
      {
        NotificationDisplayer.getDefault().notify("Malformed system details", NotificationDisplayer.Priority.NORMAL.getIcon(), pE.getMessage(), e -> {
                                                  },
                                                  NotificationDisplayer.Priority.NORMAL);
      }
    }
    return sspSystems;
  }

  default ISSPSystemDetails retrieveDetails(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException,
      MalformedInputException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject systemDetailsBody = new JSONObject(Map.of(USER_KEY, pUsername, TOKEN_KEY, pJWT.getToken(), SYSTEM_ID_KEY, pSystem.getSystemdId()));
    HttpResponse<JsonNode> systemDetailsResponse = Unirest.post(getListSystemDetailsServiceUrl())
        .headers(headers)
        .body(systemDetailsBody)
        .asJson();
    HttpUtil.verifyStatus(systemDetailsResponse);
    JSONArray jsonArray = systemDetailsResponse.getBody().getArray();
    return new SSPSystemDetailsImpl(pSystem, jsonArray);
  }

  default Map<String, String> retrieveConfigMap(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject systemDetailsBody = new JSONObject(Map.of(USER_KEY, pUsername, TOKEN_KEY, pJWT.getToken(), SYSTEM_ID_KEY, pSystem.getSystemdId()));
    HttpResponse<JsonNode> systemDetailsResponse = Unirest.post(getSystemConfigMapServiceUrl())
        .headers(headers)
        .body(systemDetailsBody)
        .asJson();
    HttpUtil.verifyStatus(systemDetailsResponse);
    Map<String, String> configMap = new HashMap<>();
    JSONObject configJsonObject = systemDetailsResponse.getBody().getObject();
    for (String key : configJsonObject.keySet())
    {
      configMap.put(key, configJsonObject.getString(key));
    }
    return configMap;
  }

  @NotNull
  String getListSystemsServiceUrl();

  String getListSystemDetailsServiceUrl();

  String getSystemConfigMapServiceUrl();

}
