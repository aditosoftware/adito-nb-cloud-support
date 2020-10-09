package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.MalformedInputException;
import de.adito.nbm.ssp.facade.ISSPSystem;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.time.Instant;

/**
 * @author m.kaspera, 08.10.2020
 */
public class SSPSystemImpl implements ISSPSystem
{

  private final String name;
  private final String url;
  private final String clusterId;
  private final String systemId;
  private final String ranchRId;
  private final Instant creationDate;

  public SSPSystemImpl(@NotNull JSONArray pJSONArray) throws MalformedInputException
  {
    if (pJSONArray.length() < 7)
      throw new MalformedInputException(pJSONArray.toString(0));
    name = pJSONArray.getString(1);
    url = pJSONArray.getString(2);
    clusterId = pJSONArray.getString(5);
    systemId = pJSONArray.getString(0);
    ranchRId = pJSONArray.getString(3);
    creationDate = Instant.parse((String) pJSONArray.get(6));
  }

  @NotNull
  @Override
  public String getName()
  {
    return name;
  }

  @NotNull
  @Override
  public String getUrl()
  {
    return url;
  }

  @NotNull
  @Override
  public String getClusterId()
  {
    return clusterId;
  }

  @NotNull
  @Override
  public String getSystemdId()
  {
    return systemId;
  }

  @NotNull
  @Override
  public String getRanchRId()
  {
    return ranchRId;
  }

  @Override
  public Instant getCreationDate()
  {
    return creationDate;
  }
}
