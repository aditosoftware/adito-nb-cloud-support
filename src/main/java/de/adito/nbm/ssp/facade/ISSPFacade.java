package de.adito.nbm.ssp.facade;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.impl.*;
import lombok.NonNull;

import java.util.*;

/**
 * @author m.kaspera, 08.10.2020
 */
public interface ISSPFacade
{

  static ISSPFacade getInstance()
  {
    return InjectorCache.getInjector(ImplModule.class).getInstance(ISSPFacade.class);
  }

  /**
   * @param pUsername name of the user for which the token is requested
   * @param pPassword password of the user
   * @return Java Web Token as String
   */
  @NonNull
  DecodedJWT getJWT(@NonNull String pUsername, char @NonNull [] pPassword) throws AditoSSPException, UnirestException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @return List with ISSPSystems that the user has access to. The information in the ISSPSystem can be used to request more detailed information for each system
   * @throws UnirestException  if an error occurs during the rest call
   * @throws AditoSSPException if the response of the server contains an error status
   */
  @NonNull
  List<ISSPSystem> getSystems(@NonNull String pUsername, @NonNull DecodedJWT pJWT) throws UnirestException, AditoSSPException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystem   ISSPSystem with the general details about the system (such as the system ID used to retrieve the more specific information)
   * @return ISSPSystemDetails for the system, containing details such as the kernel version of the
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   * @throws MalformedInputException       if the JSON returned by the server cannot be transformed to the SystemDetails
   */
  @NonNull
  ISSPSystemDetails getSystemDetails(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull ISSPSystem pSystem) throws MalformedInputException, UnirestException,
      AditoSSPException, AditoSSPParseException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystem   ISSPSystem with the general details about the system (such as the system ID used to retrieve the more specific information)
   * @return ISSPSystemDetails for the system, containing details such as the kernel version of the
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  String getServerConfig(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull ISSPSystem pSystem) throws UnirestException, AditoSSPException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystem   ISSPSystem with the general details about the system (such as the system ID used to retrieve the more specific information)
   * @return ISSPSystemDetails for the system, containing details such as the kernel version of the
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  String getTunnelConfig(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull ISSPSystem pSystem) throws UnirestException, AditoSSPException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystem   ISSPSystem with the general details about the system (such as the system ID used to retrieve the more specific information)
   * @return Map of strings with the config parameters for the system
   * @throws UnirestException              if an error occurs during the rest call
   * @throws AditoSSPServerException       if the response of the server contains the 500 status, indicating an internal server error
   * @throws AditoSSPUnauthorizedException if the response of the server contains the 401 status, indicating lacking authorization
   * @throws AditoSSPException             if the response of the server contains an error status other than the ones listed above
   */
  Map<String, String> getConfigMap(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull ISSPSystem pSystem) throws UnirestException, AditoSSPException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId ID to determine the system. The ID consists of the following: the name of the system, followed by #, followed by the cluster ID
   * @return true if the replica count of the system is currently > 0, indicating that the system is running. False for replica count <= 0
   */
  boolean isSystemRunning(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull String pSystemId);

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId ID to determine the system. The ID consists of the following: the name of the system, followed by #, followed by the cluster ID
   * @return true if the status code of the reply indicates success (meaning the connection was established and the order is valid, nothing more), false otherwise
   */
  boolean startSystem(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull String pSystemId);

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId ID to determine the system. The ID consists of the following: the name of the system, followed by #, followed by the cluster ID
   * @return true if the status code of the reply indicates success (meaning the connection was established and the order is valid, nothing more), false otherwise
   */
  boolean stopSystem(@NonNull String pUsername, @NonNull DecodedJWT pJWT, @NonNull String pSystemId);

}
