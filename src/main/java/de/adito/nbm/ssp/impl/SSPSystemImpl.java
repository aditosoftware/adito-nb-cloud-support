package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.MalformedInputException;
import de.adito.nbm.ssp.facade.ISSPSystem;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.text.*;
import java.time.Instant;
import java.util.logging.*;

/**
 * @author m.kaspera, 08.10.2020
 */
class SSPSystemImpl implements ISSPSystem
{

  private static final Logger logger = Logger.getLogger(SSPSystemImpl.class.getName());
  private final String name;
  private final String url;
  private final String clusterId;
  private final String systemId;
  private final String ranchRId;
  private final Instant creationDate;

  SSPSystemImpl(@NotNull JSONArray pJSONArray) throws MalformedInputException
  {
    Instant creationDateVar;
    if (pJSONArray.length() < 7)
      throw new MalformedInputException(pJSONArray.toString(0));
    name = pJSONArray.optString(1);
    url = pJSONArray.optString(2);
    clusterId = pJSONArray.optString(5);
    systemId = pJSONArray.optString(0);
    ranchRId = pJSONArray.optString(3);
    String creationDateString = pJSONArray.optString(6);
    try
    {
      creationDateVar = new SimpleDateFormat("MMM dd, yyyy").parse(creationDateString).toInstant();
    }
    catch (ParseException pE)
    {
      logger.log(Level.WARNING, pE, () -> "Invalid date: " + creationDateString);
      creationDateVar = Instant.MIN;
    }
    creationDate = creationDateVar;
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
