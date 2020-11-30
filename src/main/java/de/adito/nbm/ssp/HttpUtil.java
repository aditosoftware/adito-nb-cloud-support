package de.adito.nbm.ssp;

import com.mashape.unirest.http.HttpResponse;
import de.adito.nbm.ssp.exceptions.*;

import java.util.*;

/**
 * @author m.kaspera, 09.10.2020
 */
public class HttpUtil
{

  /**
   * @param pStatusCode status code to check
   * @return true if the status code indicates an internal server error, false otherwise
   */
  public static boolean isServerError(int pStatusCode)
  {
    // Server error codes range between 500 and 599
    return pStatusCode >= HttpStatus.INTERNAL_SERVER_ERROR && pStatusCode < 600;
  }

  /**
   * @param pStatusCode status code to check
   * @return true if the status code indicates a client error, false otherwise
   */
  public static boolean isClientError(int pStatusCode)
  {
    // Client error codes range between 400 and 499
    return pStatusCode >= HttpStatus.BAD_REQUEST && pStatusCode < HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /**
   * Checks if the response code indicates a problem or error, and throws an exception that fits the error.
   * If the response code indicates the request was handled normally with success, this method does nothing
   *
   * @param pHttpResponse The Reponse that came back from the server/service
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  public static void verifyStatus(HttpResponse<?> pHttpResponse) throws AditoSSPException
  {
    if (pHttpResponse.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR)
      throw new AditoSSPServerException(pHttpResponse.getStatusText());
    if (pHttpResponse.getStatus() == HttpStatus.FORBIDDEN)
      throw new AditoSSPUnauthorizedException(pHttpResponse.getStatusText());
    if (HttpUtil.isClientError(pHttpResponse.getStatus()) || HttpUtil.isServerError(pHttpResponse.getStatus()))
      throw new AditoSSPException(pHttpResponse.getStatusText(), pHttpResponse.getStatus());
  }

  /**
   * Get a Map that can be used as a default header for an Unirest call.
   * Has default properties the content type and the connection type
   *
   * @return Map of Strings, forms can be set as the header of an Unirest call
   */
  public static Map<String, String> getDefaultHeader()
  {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-type", "application/json");
    headers.put("Connection", "keep-alive");
    return headers;
  }
}
