package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.HttpUtil;
import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.facade.ISSPSystem;
import org.jetbrains.annotations.NotNull;
import org.json.*;
import org.openide.awt.NotificationDisplayer;

import java.util.*;

/**
 * @author m.kaspera, 09.10.2020
 */
interface ISystemExplorer
{

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
    JSONObject listSystemBody = new JSONObject(Map.of("user", pUsername, "jwt", pJWT.getToken()));
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

  @NotNull
  String getListSystemsServiceUrl();

}
