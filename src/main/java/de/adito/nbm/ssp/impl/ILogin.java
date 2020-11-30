package de.adito.nbm.ssp.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.HttpUtil;
import de.adito.nbm.ssp.exceptions.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author m.kaspera, 08.10.2020
 */
interface ILogin
{

  /**
   * @param pUsername name of the user
   * @param pPassword password of the user
   * @return DecodedJWT, offers methods to retrieve the different dates associated with the token, the user connected to the token, and the token itself
   * @throws UnirestException                            if an error occurs during the rest call
   * @throws com.auth0.jwt.exceptions.JWTDecodeException if the JWT Token returned by the server cannot be decoded
   * @throws AditoSSPServerException                     if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException               if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException                           if the response of the server contains an error status other than the ones listed above
   */
  @NotNull
  default DecodedJWT login(@NotNull String pUsername, char[] pPassword) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject body = new JSONObject(Map.of("username", pUsername, "password", getEncoder().encodeToString(new String(pPassword).getBytes(StandardCharsets.UTF_8))));
    HttpResponse<String> jsonHttpResponse = Unirest.post(getLoginServiceUrl())
        .headers(headers)
        .body(body)
        .asString();
    try
    {
      HttpUtil.verifyStatus(jsonHttpResponse);
    }
    catch (AditoSSPException pE)
    {
      throw new AditoSSPAuthException(pE, pE.getStatusCode());
    }
    return JWT.decode(jsonHttpResponse.getBody());
  }

  @NotNull
  Base64.Encoder getEncoder();

  @NotNull
  String getLoginServiceUrl();

}
