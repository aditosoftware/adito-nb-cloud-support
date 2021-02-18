package de.adito.nbm.ssp.impl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.kaspera, 18.02.2021
 */
public class ILoginTest
{

  /**
   * Test if the Base64 encoding of special characters works as inteded
   */
  @Test
  void testCharEncodeWithSpecialChars()
  {
    String specialCharacters = "# '+*?\\})/&%$§\"!!üäöôé";
    ILogin login = new SSPFacadeImpl();
    String encodedCharacters = login.encodeBase64(specialCharacters.toCharArray());
    assertEquals("IyAnKyo/XH0pLyYlJMKnIiEhw7zDpMO2w7TDqQ==", encodedCharacters);
    assertEquals(specialCharacters, new String(Base64.getDecoder().decode(encodedCharacters), StandardCharsets.UTF_8));
    assertArrayEquals(specialCharacters.getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(encodedCharacters));
  }

  /**
   * Test if the Base64 encoding of some normal characters works as inteded
   */
  @Test
  void testNormalCharacters()
  {
    String specialCharacters = "asdf";
    ILogin login = new SSPFacadeImpl();
    String encodedCharacters = login.encodeBase64(specialCharacters.toCharArray());
    assertEquals(specialCharacters, new String(Base64.getDecoder().decode(encodedCharacters)));
  }
}
