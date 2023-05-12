package de.adito.nbm.ssp.facade;

import org.jetbrains.annotations.*;

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
  @Nullable
  String getGitRepoUrl();

  /**
   * Returns the name of the git branch that the system is based on
   *
   * @return name of the git branch
   */
  @Nullable
  String getGitBranch();

  /**
   * Returns the ADITO Kernel verion that the system is using
   *
   * @return ADITO Kernel version
   */
  @NotNull
  String getKernelVersion();

  /**
   * @return true if the kernel version can be used with the current Designer
   */
  boolean isDesignerVersionOk();
}
