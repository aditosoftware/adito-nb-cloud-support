package de.adito.nbm.ssp.facade;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.impl.ImplModule;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author m.kaspera, 08.10.2020
 */
public interface ISSPFacade
{

  static ISSPFacade getInstance()
  {
    Injector INJECTOR = Guice.createInjector(new ImplModule());
    return INJECTOR.getInstance(ISSPFacade.class);
  }

  /**
   * @param pUsername name of the user for which the token is requested
   * @param pPassword password of the user
   * @return Java Web Token as String
   */
  @NotNull
  DecodedJWT getJWT(@NotNull String pUsername, @NotNull char[] pPassword) throws AditoSSPException, UnirestException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @return List with ISSPSystems that the user has access to. The information in the ISSPSystem can be used to request more detailed information for each system
   */
  @NotNull
  List<ISSPSystem> getSystems(@NotNull String pUsername, @NotNull DecodedJWT pJWT);

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
  @NotNull
  ISSPSystemDetails getSystemDetails(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws MalformedInputException, UnirestException,
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
  String getServerConfig(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException;

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
  String getTunnelConfig(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException;

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
  Map<String, String> getConfigMap(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull ISSPSystem pSystem) throws UnirestException, AditoSSPException;

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId ID to determine the system. The ID consists of the following: the name of the system, followed by #, followed by the cluster ID
   * @return true if the replica count of the system is currently > 0, indicating that the system is running. False for replica count <= 0
   */
  boolean isSystemRunning(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId);

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId ID to determine the system. The ID consists of the following: the name of the system, followed by #, followed by the cluster ID
   * @return true if the status code of the reply indicates success (meaning the connection was established and the order is valid, nothing more), false otherwise
   */
  boolean startSystem(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId);

  /**
   * @param pUsername name of the user for which the JWT was issued
   * @param pJWT      JSON Web Token for authentication purposes
   * @param pSystemId ID to determine the system. The ID consists of the following: the name of the system, followed by #, followed by the cluster ID
   * @return true if the status code of the reply indicates success (meaning the connection was established and the order is valid, nothing more), false otherwise
   */
  boolean stopSystem(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId);

}
