package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.ISSPFacade;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link IStartSystemAction}.
 *
 * @author r.hartinger, 22.02.2023
 */
class IStartSystemActionTest
{

  /**
   * Tests the method {@link IStartSystemAction#doStartSystem(ISystemInfo)}.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class DoStartSystem
  {

    /**
     * Tests that a {@code null} argument will have no interactions with anything.
     */
    @Test
    @SneakyThrows
    void shouldDoNothingWithNoSystemInfo()
    {
      try (MockedStatic<ISSPFacade> sspFacadeMockedStatic = Mockito.mockStatic(ISSPFacade.class);
           MockedStatic<UserCredentialsManager> userCredentialsManagerMockedStatic = Mockito.mockStatic(UserCredentialsManager.class))
      {

        IStartSystemAction startSystemAction = Mockito.spy(IStartSystemAction.class);

        NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(() -> startSystemAction.doStartSystem(null));

        sspFacadeMockedStatic.verifyNoInteractions();
        userCredentialsManagerMockedStatic.verifyNoInteractions();
      }
    }

    /**
     * @return arguments for {@link #shouldDoStartSystem(String, boolean, boolean)}
     */
    @NonNull
    private Stream<Arguments> shouldDoStartSystem()
    {
      return Stream.of(
          Arguments.of("System is already running", true, false),
          Arguments.of("System is already running", true, true),

          Arguments.of("Failed to send start system signal", false, false),
          Arguments.of("Start system signal sent successfully", false, true)
      );
    }

    /**
     * Tests the method.
     *
     * @param pMessage       the expected message of {@link de.adito.notification.INotificationFacade#notify(String, String, boolean)}
     * @param pSystemRunning the result of {@link ISSPFacade#isSystemRunning(String, DecodedJWT, String)}
     * @param pStartedSystem the result of {@link ISSPFacade#startSystem(String, DecodedJWT, String)}
     */
    @ParameterizedTest
    @MethodSource
    @SneakyThrows
    void shouldDoStartSystem(@NonNull String pMessage, boolean pSystemRunning, boolean pStartedSystem)
    {

      ISystemInfo systemInfo = Mockito.spy(ISystemInfo.class);
      Mockito.doReturn(Observable.empty()).when(systemInfo).getCloudId();

      try (MockedStatic<ISSPFacade> sspFacadeMockedStatic = Mockito.mockStatic(ISSPFacade.class);
           MockedStatic<UserCredentialsManager> userCredentialsManagerMockedStatic = Mockito.mockStatic(UserCredentialsManager.class))
      {

        ISSPFacade sspFacade = Mockito.mock(ISSPFacade.class);
        Mockito.doReturn(pSystemRunning).when(sspFacade).isSystemRunning(any(), any(), any());
        Mockito.doReturn(pStartedSystem).when(sspFacade).startSystem(any(), any(), any());

        sspFacadeMockedStatic.when(ISSPFacade::getInstance).thenReturn(sspFacade);

        DecodedJWT decodedJWT = Mockito.spy(DecodedJWT.class);

        userCredentialsManagerMockedStatic.when(UserCredentialsManager::getCredentials).thenReturn(decodedJWT);


        IStartSystemAction startSystemAction = Mockito.spy(IStartSystemAction.class);

        NotificationFacadeTestUtil.verifyNotificationFacadeNotify("Start System", pMessage, () -> startSystemAction.doStartSystem(systemInfo));
      }
    }
  }

}
