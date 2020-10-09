package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.MalformedInputException;
import de.adito.nbm.ssp.facade.ISSPSystemDetails;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.util.List;

/**
 * @author m.kaspera, 08.10.2020
 */
public class SSPSystemDetailsImpl extends SSPSystemImpl implements ISSPSystemDetails
{
  public SSPSystemDetailsImpl(@NotNull JSONArray pJSONArray) throws MalformedInputException
  {
    super(pJSONArray);
  }

  @NotNull
  @Override
  public String getGitRepoUrl()
  {
    throw new NotImplementedException("");
  }

  @NotNull
  @Override
  public String getGitBranch()
  {
    throw new NotImplementedException("");
  }

  @NotNull
  @Override
  public String getKernelVersion()
  {
    throw new NotImplementedException("");
  }

  @NotNull
  @Override
  public String getServerConfig()
  {
    throw new NotImplementedException("");
  }

  @Override
  @NotNull
  public List<String> getTunnelConfigs()
  {
    throw new NotImplementedException("");
  }
}
