package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.*;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.kaspera, 26.01.2021
 */
public class SSPSystemImplTest
{

  /**
   * Tests if an array with only null values works (it should)
   *
   * @throws MalformedInputException if the tests fails with this exeption
   */
  @Test
  void testAllNulls() throws MalformedInputException, AditoSSPParseException
  {
    JSONArray jsonArray = new JSONArray("[null, null, null, null, null, null, null]");
    SSPSystemImpl sspSystem = new SSPSystemImpl(jsonArray);
    assertEquals("", sspSystem.getSystemdId());
    assertEquals("", sspSystem.getName());
    assertEquals("", sspSystem.getClusterId());
    assertEquals(Instant.MIN, sspSystem.getCreationDate());
    assertEquals("", sspSystem.getUrl());
  }

  /**
   * Tests if an array with only null values works for the SSPSystemDetailsImpl (it should)
   *
   * @throws MalformedInputException if the tests fails with this exeption
   */
  @Test
  void testAllNullsDetails() throws MalformedInputException, AditoSSPParseException
  {
    JSONArray jsonArray = new JSONArray("[null, null, null, null, null, null, null]");
    SSPSystemImpl sspSystem = new SSPSystemImpl(jsonArray);
    assertEquals("", sspSystem.getSystemdId());
    assertEquals("", sspSystem.getName());
    assertEquals("", sspSystem.getClusterId());
    assertEquals(Instant.MIN, sspSystem.getCreationDate());
    assertEquals("", sspSystem.getUrl());
    JSONArray detailsArray = new JSONArray("[{giturl: null, branch_tag: null, version: null}]");
    SSPSystemDetailsImpl sspSystemDetails = new SSPSystemDetailsImpl(sspSystem, detailsArray);
    assertEquals("", sspSystemDetails.getGitBranch());
    assertEquals("", sspSystemDetails.getGitRepoUrl());
    assertEquals("", sspSystemDetails.getKernelVersion());
  }

  /**
   * test if the SSPSystemImpl throws an exception if the array does not have the required size
   */
  @Test
  void testShortArray()
  {
    JSONArray jsonArray = new JSONArray("[]");
    assertThrows(MalformedInputException.class, () -> new SSPSystemImpl(jsonArray));
  }

  /**
   * test if the SSPSystemDetailsImpl throws an exception if the JSON object does not have all required keys
   *
   * @throws AditoSSPParseException if the tests fails with this exeption
   */
  @Test
  void testShortArrayDetails() throws AditoSSPParseException, MalformedInputException
  {
    JSONArray jsonArray = new JSONArray("[null, null, null, null, null, null, null]");
    SSPSystemImpl sspSystem = new SSPSystemImpl(jsonArray);
    JSONArray detailsArray = new JSONArray("[{giturl: null, branch_tag: null}]");
    assertThrows(MalformedInputException.class, () -> new SSPSystemDetailsImpl(sspSystem, detailsArray));
  }
}
