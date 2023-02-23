package de.adito.nbm.ssp.impl;

import de.adito.nbm.ssp.exceptions.*;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import lombok.SneakyThrows;
import org.json.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * Test class for {@link ISystemExplorer}.
 *
 * @author r.hartinger, 21.02.2023
 */
class ISystemExplorerTest
{

  /**
   * Tests the method {@link ISystemExplorer#extractSspSystems(JSONObject)}.
   */
  @Nested
  class ExtractSspSystems
  {

    /**
     * Tests that the normal method will work.
     */
    @Test
    @SneakyThrows
    void shouldExtractSspSystems()
    {
      SSPSystemImpl expected = new SSPSystemImpl("name", "url", "clusterId", "systemId", "ranchRID", LocalDate.of(2022, Month.JANUARY, 21).atStartOfDay(ZoneId.systemDefault()).toInstant());

      // filling some default values to the array
      JSONArray jsonArray = new JSONArray();
      jsonArray.put("systemId");
      jsonArray.put("name");
      jsonArray.put("url");
      jsonArray.put("ranchRID");
      jsonArray.put("irrelevant");
      jsonArray.put("clusterId");
      jsonArray.put("Jan. 21, 2022");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("someKey", jsonArray);

      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(() -> assertEquals(List.of(expected), ISystemExplorer.extractSspSystems(jsonObject)));
    }

    /**
     * Tests that an {@link MalformedInputException} will be handled if the json array is not long enough (in this case no elements).
     */
    @Test
    @SneakyThrows
    void shouldBeMalformedInput()
    {
      JSONArray jsonArray = new JSONArray();

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("someKey", jsonArray);

      NotificationFacadeTestUtil.verifyNotificationFacade(MalformedInputException.class, "Malformed system details",
                                                          () -> assertEquals(new ArrayList<>(), ISystemExplorer.extractSspSystems(jsonObject)));
    }

    /**
     * Tests that an {@link AditoSSPParseException} will be handled if a {@link JSONException} will be thrown manually.
     */
    @Test
    @SneakyThrows
    void shouldBeAditoSSPParse()
    {
      JSONArray jsonArray = Mockito.spy(new JSONArray());
      Mockito.doReturn(7).when(jsonArray).length();
      Mockito.doThrow(new JSONException("junit")).when(jsonArray).optString(anyInt());

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("someKey", jsonArray);

      NotificationFacadeTestUtil.verifyNotificationFacade(AditoSSPParseException.class, "Error while parsing system details",
                                                          () -> assertEquals(new ArrayList<>(), ISystemExplorer.extractSspSystems(jsonObject)));
    }
  }

}
