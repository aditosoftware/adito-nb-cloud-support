package de.adito.nbm.ssp.facade;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author m.kaspera, 08.10.2020
 */
public interface ISSPSystemDetails extends ISSPSystem
{

  /**
   * Returns the URl for the Git repository, can either be SSH or HTTPS based
   *
   * @return Git Repo URl
   */
  @NotNull
  String getGitRepoUrl();

  /**
   * Returns the name of the git branch that the system is based on
   *
   * @return name of the git branch
   */
  @NotNull
  String getGitBranch();

  /**
   * Returns the ADITO Kernel verion that the system is using
   *
   * @return ADITO Kernel version
   */
  @NotNull
  String getKernelVersion();

  /**
   * Returns the ServerConfig as a String, representing the content of the server config file
   *
   * @return ServerConfig for the system
   */
  @NotNull
  String getServerConfig();

  /**
   * Returns the contents of the TunnelConfigs as List of Strings, where each String represents the contents of one tunnel config file
   *
   * @return List of TunnelConfigs as Strings
   */
  @NotNull
  List<String> getTunnelConfigs();

}
