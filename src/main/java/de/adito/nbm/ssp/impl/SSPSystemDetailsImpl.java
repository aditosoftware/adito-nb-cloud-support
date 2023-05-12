package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.facade.*;
import org.jetbrains.annotations.*;
import org.json.*;

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

  SSPSystemDetailsImpl(@NotNull ISSPSystem pSystem, @NotNull JSONArray pJSONArray) throws MalformedInputException, AditoSSPParseException
  {
    super(pSystem.getName(), pSystem.getUrl(), pSystem.getClusterId(), pSystem.getSystemdId(), pSystem.getRanchRId(), pSystem.getCreationDate());
    if (pJSONArray.length() != 1 || !pJSONArray.getJSONObject(0).keySet().contains(KERNEL_VERSION_KEY))
    {
      Logger.getLogger(SSPSystemDetailsImpl.class.getName()).log(Level.WARNING, () -> String.format("Malformed JSONArray: %s", pJSONArray));
      throw new MalformedInputException(pJSONArray.toString(0));
    }
    try
    {
      gitRepoUrl = pJSONArray.getJSONObject(0).optString(GIT_REPO_KEY, null);
      gitBranch = pJSONArray.getJSONObject(0).optString(GIT_BRANCH_KEY, null);
      kernelVersion = pJSONArray.getJSONObject(0).optString(KERNEL_VERSION_KEY);
    }
    catch (JSONException pJSONException)
    {
      throw new AditoSSPParseException(pJSONException, pJSONArray);
    }
  }

  @Nullable
  @Override
  public String getGitRepoUrl()
  {
    return gitRepoUrl;
  }

  @Nullable
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
