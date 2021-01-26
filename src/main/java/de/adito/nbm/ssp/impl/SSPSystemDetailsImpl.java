package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.MalformedInputException;
import de.adito.nbm.ssp.facade.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.util.List;
import java.util.logging.*;

/**
 * @author m.kaspera, 08.10.2020
 */
class SSPSystemDetailsImpl extends SSPSystemImpl implements ISSPSystemDetails
{

  private static final String GIT_REPO_KEY = "giturl";
  private static final String GIT_BRANCH_KEY = "branch_tag";
  private static final String KERNEL_VERSION_KEY = "version";
  private final String gitRepoUrl;
  private final String gitBranch;
  private final String kernelVersion;

  SSPSystemDetailsImpl(@NotNull ISSPSystem pSystem, @NotNull JSONArray pJSONArray) throws MalformedInputException
  {
    super(pSystem.getName(), pSystem.getUrl(), pSystem.getClusterId(), pSystem.getSystemdId(), pSystem.getRanchRId(), pSystem.getCreationDate());
    if (pJSONArray.length() != 1 || !pJSONArray.getJSONObject(0).keySet().containsAll(List.of(GIT_REPO_KEY, GIT_BRANCH_KEY, KERNEL_VERSION_KEY)))
    {
      Logger.getLogger(SSPSystemDetailsImpl.class.getName()).log(Level.WARNING, () -> String.format("Malformed JSONArray: %s", pJSONArray.toString()));
      throw new MalformedInputException(pJSONArray.toString(0));
    }
    gitRepoUrl = pJSONArray.getJSONObject(0).getString(GIT_REPO_KEY);
    gitBranch = pJSONArray.getJSONObject(0).getString(GIT_BRANCH_KEY);
    kernelVersion = pJSONArray.getJSONObject(0).getString(KERNEL_VERSION_KEY);

  }

  @NotNull
  @Override
  public String getGitRepoUrl()
  {
    return gitRepoUrl;
  }

  @NotNull
  @Override
  public String getGitBranch()
  {
    return gitBranch;
  }

  @NotNull
  @Override
  public String getKernelVersion()
  {
    return kernelVersion;
  }

  @Override
  public boolean isDesignerVersionOk()
  {
    return true;
  }
}
