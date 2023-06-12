package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.facade.*;
import lombok.NonNull;
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

  SSPSystemDetailsImpl(@NonNull ISSPSystem pSystem, @NonNull JSONArray pJSONArray) throws MalformedInputException, AditoSSPParseException
  {
    super(pSystem.getName(), pSystem.getUrl(), pSystem.getClusterId(), pSystem.getSystemdId(), pSystem.getRanchRId(), pSystem.getCreationDate());
    if (pJSONArray.length() != 1 || !pJSONArray.getJSONObject(0).keySet().containsAll(List.of(GIT_REPO_KEY, GIT_BRANCH_KEY, KERNEL_VERSION_KEY)))
    {
      Logger.getLogger(SSPSystemDetailsImpl.class.getName()).log(Level.WARNING, () -> String.format("Malformed JSONArray: %s", pJSONArray.toString()));
      throw new MalformedInputException(pJSONArray.toString(0));
    }
    try
    {
      gitRepoUrl = pJSONArray.getJSONObject(0).optString(GIT_REPO_KEY);
      gitBranch = pJSONArray.getJSONObject(0).optString(GIT_BRANCH_KEY);
      kernelVersion = pJSONArray.getJSONObject(0).optString(KERNEL_VERSION_KEY);
    }
    catch (JSONException pJSONException)
    {
      throw new AditoSSPParseException(pJSONException, pJSONArray);
    }
  }

  @NonNull
  @Override
  public String getGitRepoUrl()
  {
    return gitRepoUrl;
  }

  @NonNull
  @Override
  public String getGitBranch()
  {
    return gitBranch;
  }

  @NonNull
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
