package de.adito.nbm.ssp.facade;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * @author m.kaspera, 08.10.2020
 */
public interface ISSPSystem
{

  /**
   * The name of the server, intended for the end-user
   *
   * @return the name of the server
   */
  @NotNull
  String getName();


  /**
   * The URl that the system can be reached by
   *
   * @return URl
   */
  @NotNull
  String getUrl();

  /**
   * ID of the Kubernetes cluster that the system is running on
   *
   * @return ID
   */
  @NotNull
  String getClusterId();

  /**
   * Id of the sytem, used to identify the system on the technical side. Consists of the name of the system, a #, and the cluster id
   *
   * @return Id of the sytem
   */
  @NotNull
  String getSystemdId();

  /**
   * returns the RanchR id
   *
   * @return RanchR id
   */
  @NotNull
  String getRanchRId();

  /**
   * Date that the system was created
   *
   * @return Instant representing the date
   */
  Instant getCreationDate();

}
