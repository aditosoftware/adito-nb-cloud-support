package de.adito.nbm.ssp.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.exceptions.AditoSSPException;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link SSPFacadeImpl}.
 *
 * @author r.hartinger, 21.02.2023
 */
class SSPFacadeImplTest
{
  @NonNull
  private static final String SYSTEM_ID = "1234";
  @NonNull
  private static final String USERNAME = "Alice";
  @NonNull
  private static final String SYSTEM_ID_MESSAGE = "SystemId: " + SYSTEM_ID;

  /**
   * @return the arguments for all handle error methods in this test class
   */
  @NonNull
  private static Stream<Arguments> createArgumentsForHandleError()
  {
    return Stream.of(
        Arguments.of(new UnirestException("junit")),
        Arguments.of(new AditoSSPException("junit", 418))
    );
  }

  /**
   * Tests the method {@link SSPFacadeImpl#generateSystemIdMessage(String)}.
   */
  @Nested
  class GenerateSystemIdMessage
  {
    /**
     * Tests that the correct message will be generated.
     */
    @Test
    void shouldGenerateSystemIdMessage()
    {
      SSPFacadeImpl sspFacade = new SSPFacadeImpl();

      assertEquals(SYSTEM_ID_MESSAGE, sspFacade.generateSystemIdMessage(SYSTEM_ID));
    }
  }


  /**
   * Tests the method {@link SSPFacadeImpl#isSystemRunning(String, DecodedJWT, String)}.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class IsSystemRunning
  {
    /**
     * Tests that the normal call without an exception thrown works.
     */
    @Test
    @SneakyThrows
    void shouldIsSystemRunning()
    {
      SSPFacadeImpl sspFacade = Mockito.spy(new SSPFacadeImpl());
      Mockito.doReturn(true).when(sspFacade).checkSystemStatus(any(), any(), any());

      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(
          () -> assertTrue(sspFacade.isSystemRunning(USERNAME, Mockito.spy(DecodedJWT.class), SYSTEM_ID)));
    }

    /**
     * Tests that an exception thrown in a called method will be handled correctly
     *
     * @param pException the exception thrown
     */
    @SneakyThrows
    @ParameterizedTest
    @MethodSource("de.adito.nbm.ssp.impl.SSPFacadeImplTest#createArgumentsForHandleError")
    void shouldHandleError(@NonNull Exception pException)
    {
      SSPFacadeImpl sspFacade = Mockito.spy(new SSPFacadeImpl());
      Mockito.doThrow(pException).when(sspFacade).checkSystemStatus(any(), any(), any());

      NotificationFacadeTestUtil.verifyNotificationFacade(pException.getClass(), "Checking the status of a system failed", SYSTEM_ID_MESSAGE,
                                                          () -> assertFalse(sspFacade.isSystemRunning(USERNAME, Mockito.spy(DecodedJWT.class), SYSTEM_ID)));
    }
  }


  /**
   * Tests the method {@link SSPFacadeImpl#startSystem(String, DecodedJWT, String)}.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class StartSystem
  {
    /**
     * Tests that the normal call without an exception thrown works.
     */
    @Test
    @SneakyThrows
    void shouldStartSystem()
    {
      SSPFacadeImpl sspFacade = Mockito.spy(new SSPFacadeImpl());
      Mockito.doReturn(true).when(sspFacade).doStartSystem(any(), any(), any());

      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(
          () -> assertTrue(sspFacade.startSystem(USERNAME, Mockito.spy(DecodedJWT.class), SYSTEM_ID)));
    }

    /**
     * Tests that an exception thrown in a called method will be handled correctly
     *
     * @param pException the exception thrown
     */
    @ParameterizedTest
    @MethodSource("de.adito.nbm.ssp.impl.SSPFacadeImplTest#createArgumentsForHandleError")
    @SneakyThrows
    void shouldHandleError(@NonNull Exception pException)
    {
      SSPFacadeImpl sspFacade = Mockito.spy(new SSPFacadeImpl());
      Mockito.doThrow(pException).when(sspFacade).doStartSystem(any(), any(), any());

      NotificationFacadeTestUtil.verifyNotificationFacade(pException.getClass(), "Sending the start signal to the system failed", SYSTEM_ID_MESSAGE,
                                                          () -> assertFalse(sspFacade.startSystem(USERNAME, Mockito.spy(DecodedJWT.class), SYSTEM_ID)));
    }
  }

  /**
   * Tests the method {@link SSPFacadeImpl#stopSystem(String, DecodedJWT, String)}.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class StopSystem
  {
    /**
     * Tests that the normal call without an exception thrown works.
     */
    @Test
    @SneakyThrows
    void shouldStopSystem()
    {
      SSPFacadeImpl sspFacade = Mockito.spy(new SSPFacadeImpl());
      Mockito.doReturn(true).when(sspFacade).doStopSystem(any(), any(), any());

      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(
          () -> assertTrue(sspFacade.stopSystem(USERNAME, Mockito.spy(DecodedJWT.class), SYSTEM_ID)));
    }

    /**
     * Tests that an exception thrown in a called method will be handled correctly
     *
     * @param pException the exception thrown
     */
    @ParameterizedTest
    @MethodSource("de.adito.nbm.ssp.impl.SSPFacadeImplTest#createArgumentsForHandleError")
    @SneakyThrows
    void shouldHandleError(@NonNull Exception pException)
    {
      SSPFacadeImpl sspFacade = Mockito.spy(new SSPFacadeImpl());
      Mockito.doThrow(pException).when(sspFacade).doStopSystem(any(), any(), any());

      NotificationFacadeTestUtil.verifyNotificationFacade(pException.getClass(), "Sending the stop signal to the system failed", SYSTEM_ID_MESSAGE,
                                                          () -> assertFalse(sspFacade.stopSystem(USERNAME, Mockito.spy(DecodedJWT.class), SYSTEM_ID)));
    }
  }

}
