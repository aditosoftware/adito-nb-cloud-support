package de.adito.nbm.ssp.facade;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author m.kaspera, 08.10.2020
 */
public interface ISSPFacade
{

  /**
   * @param pUsername name of the user for which the token is requested
   * @param pPassword password of the user
   * @return Java Web Token as String
   */
  @NotNull
  DecodedJWT getJWT(@NotNull String pUsername, @NotNull char[] pPassword);

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
   * @param pSystemId ID to determine the system. The ID consists of the following: the name of the system, followed by #, followed by the cluster ID
   * @return ISSPSystemDetails for the system, containing details such as the kernel version of the
   */
  @NotNull
  ISSPSystemDetails getSystemDetails(@NotNull String pUsername, @NotNull DecodedJWT pJWT, @NotNull String pSystemId);

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
