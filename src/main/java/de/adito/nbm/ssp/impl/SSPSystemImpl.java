package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.MalformedInputException;
import de.adito.nbm.ssp.facade.ISSPSystem;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.text.*;
import java.time.Instant;

/**
 * @author m.kaspera, 08.10.2020
 */
class SSPSystemImpl implements ISSPSystem
{

  private final String name;
  private final String url;
  private final String clusterId;
  private final String systemId;
  private final String ranchRId;
  private final Instant creationDate;

  SSPSystemImpl(@NotNull JSONArray pJSONArray) throws MalformedInputException
  {
    if (pJSONArray.length() < 7)
      throw new MalformedInputException(pJSONArray.toString(0));
    name = pJSONArray.getString(1);
    url = pJSONArray.getString(2);
    clusterId = pJSONArray.getString(5);
    systemId = pJSONArray.getString(0);
    ranchRId = pJSONArray.getString(3);
    try
    {
      creationDate = new SimpleDateFormat("MMM dd, yyyy").parse(pJSONArray.getString(6)).toInstant();
    }
    catch (ParseException pE)
    {
      throw new MalformedInputException(pE);
    }
  }

  SSPSystemImpl(String pName, String pUrl, String pClusterId, String pSystemId, String pRanchRId, Instant pCreationDate)
  {
    name = pName;
    url = pUrl;
    clusterId = pClusterId;
    systemId = pSystemId;
    ranchRId = pRanchRId;
    creationDate = pCreationDate;
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
