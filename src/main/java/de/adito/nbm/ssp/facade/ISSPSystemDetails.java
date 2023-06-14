package de.adito.nbm.ssp.facade;

import lombok.NonNull;

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
  @NonNull
  String getGitRepoUrl();

  /**
   * Returns the name of the git branch that the system is based on
   *
   * @return name of the git branch
   */
  @NonNull
  String getGitBranch();

  /**
   * Returns the ADITO Kernel verion that the system is using
   *
   * @return ADITO Kernel version
   */
  @NonNull
  String getKernelVersion();

  /**
   * @return true if the kernel version can be used with the current Designer
   */
  boolean isDesignerVersionOk();
}
