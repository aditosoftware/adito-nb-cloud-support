package de.adito.nbm.ssp.auth;

import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.exceptions.AditoSSPException;
import de.adito.nbm.ssp.facade.ISSPFacade;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

import java.awt.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link UserCredentialsManager}.
 *
 * @author r.hartinger, 22.02.2023
 */
class UserCredentialsManagerTest
{

  /**
   * Tests the method {@link UserCredentialsManager#getToken(String, char[])}
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class GetToken
  {
    /**
     * Tests that the method will be called normally.
     */
    @Test
    @SneakyThrows
    void shouldGetToken()
    {
      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(() -> baseGetToken(pISSPFacade -> {
        // nothing to do here
      }));
    }

    /**
     * @return the arguments for {@link #shouldHandleError(Exception)}
     */
    @NonNull
    private Stream<Arguments> shouldHandleError()
    {
      return Stream.of(
          Arguments.of(new UnirestException("junit")),
          Arguments.of(new AditoSSPException("junit", 418))
      );
    }

    /**
     * Tests that an exception thrown in a called method will be handled correctly.
     *
     * @param pException the exception thrown
     */
    @ParameterizedTest
    @MethodSource
    @SneakyThrows
    void shouldHandleError(@NonNull Exception pException)
    {
      NotificationFacadeTestUtil.verifyNotificationFacade(pException.getClass(), "Login failed", () -> baseGetToken(pISSPFacade -> {
        try
        {
          Mockito.doThrow(pException).when(pISSPFacade).getJWT(any(), any());
        }
        catch (Exception pE)
        {
          throw new RuntimeException(pE);
        }
      }));
    }

    /**
     * Base method for testing {@link UserCredentialsManager#getToken(String, char[])}.
     *
     * @param pISSPFacadeConsumer consumer for changing the behaviour of {@link ISSPFacade}. The given value is a {@link Mockito#spy(Class)}
     */
    void baseGetToken(@NonNull Consumer<ISSPFacade> pISSPFacadeConsumer)
    {
      try (MockedStatic<ISSPFacade> sspFacadeMockedStatic = Mockito.mockStatic(ISSPFacade.class);
           MockedStatic<GraphicsEnvironment> graphicsEnvironmentMockedStatic = Mockito.mockStatic(GraphicsEnvironment.class))
      {

        graphicsEnvironmentMockedStatic.when(GraphicsEnvironment::isHeadless).thenReturn(false);

        ISSPFacade sspFacade = Mockito.spy(ISSPFacade.class);
        pISSPFacadeConsumer.accept(sspFacade);

        sspFacadeMockedStatic.when(ISSPFacade::getInstance).thenReturn(sspFacade);

        assertDoesNotThrow(() -> UserCredentialsManager.getToken("egon", new char[]{'p', 'w'}));
      }
    }
  }

}
