package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.MalformedInputException;
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
  void testAllNulls() throws MalformedInputException
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
   * test if the SSPSystemImpl throws an exception if the array does not have the required size
   */
  @Test
  void testShortArray()
  {
    JSONArray jsonArray = new JSONArray("[]");
    assertThrows(MalformedInputException.class, () -> new SSPSystemImpl(jsonArray));
  }
}
