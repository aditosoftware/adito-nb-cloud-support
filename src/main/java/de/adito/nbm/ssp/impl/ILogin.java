package de.adito.nbm.ssp.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.HttpUtil;
import de.adito.nbm.ssp.exceptions.*;
import lombok.NonNull;
import org.json.JSONObject;

import java.nio.*;
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
  @NonNull
  default DecodedJWT login(@NonNull String pUsername, char[] pPassword) throws UnirestException, AditoSSPException
  {
    Map<String, String> headers = HttpUtil.getDefaultHeader();
    JSONObject body = new JSONObject(Map.of("username", pUsername, "password", encodeBase64(pPassword)));
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
    try
    {
      return JWT.decode(jsonHttpResponse.getBody());
    }
    catch (NoClassDefFoundError pE)
    {
      throw new AditoSSPException("Deocde Error due to missing Class:\n" + pE.getMessage(), 200);
    }
  }

  /**
   * encode a char array to a Base64-encoded String
   *
   * @param pChars char array to encode
   * @return Base64-encoded String
   */
  default String encodeBase64(char[] pChars)
  {
    // To use the encodeToString method we have to transform the char array to a byte array
    // Transform char array in ByteBuffer -> chars are written to the ByteBuffer
    ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(pChars));
    // Create byte array with size "Number of elements in byteBuffer" -> the get call only reads that many bytes and not more
    byte[] bytes = new byte[buffer.remaining()];
    // Write bytes from the byteBuffer into the byteArray
    buffer.get(bytes);
    Arrays.fill(pChars, '0');
    return getEncoder().encodeToString(bytes);
  }

  @NonNull
  Base64.Encoder getEncoder();

  @NonNull
  String getLoginServiceUrl();

}
