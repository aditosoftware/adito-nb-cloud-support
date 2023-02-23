package de.adito.nbm.ssp.actions;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.*;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import io.reactivex.rxjava3.core.Observable;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link IStopSystemAction}.
 *
 * @author r.hartinger, 22.02.2023
 */
class IStopSystemActionTest
{

  /**
   * Tests the method {@link IStopSystemAction#stopSystem(ISystemInfo)}.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class StopSystem
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

        IStopSystemAction stopSystemAction = Mockito.spy(IStopSystemAction.class);

        NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(() -> stopSystemAction.stopSystem(null));

        sspFacadeMockedStatic.verifyNoInteractions();
        userCredentialsManagerMockedStatic.verifyNoInteractions();
      }
    }

    /**
     * @return the arguments for {@link #shouldStopSystem(String, boolean)}
     */
    @NotNull
    private Stream<Arguments> shouldStopSystem()
    {
      return Stream.of(
          Arguments.of("Stop system signal sent successfully", true),
          Arguments.of("Failed to send stop system signal", false)
      );
    }

    /**
     * Tests the method call
     *
     * @param pMessage       the expected message that should be given to {@link de.adito.notification.INotificationFacade#notify(String, String, boolean)}.
     * @param pStoppedSystem the result of {@link ISSPFacade#stopSystem(String, DecodedJWT, String)}
     */
    @ParameterizedTest
    @MethodSource
    @SneakyThrows
    void shouldStopSystem(@NotNull String pMessage, boolean pStoppedSystem)
    {
      ISystemInfo systemInfo = Mockito.spy(ISystemInfo.class);
      Mockito.doReturn(Observable.empty()).when(systemInfo).getCloudId();

      IStopSystemAction stopSystemAction = Mockito.spy(IStopSystemAction.class);

      try (MockedStatic<ICloudNotificationFacade> cloudNotificationFacadeMockedStatic = Mockito.mockStatic(ICloudNotificationFacade.class);
           MockedStatic<ISSPFacade> sspFacadeMockedStatic = Mockito.mockStatic(ISSPFacade.class);
           MockedStatic<UserCredentialsManager> userCredentialsManagerMockedStatic = Mockito.mockStatic(UserCredentialsManager.class))
      {

        ICloudNotificationFacade sspNotificationFacade = Mockito.spy(ICloudNotificationFacade.class);
        Mockito.doReturn("Stop System").when(sspNotificationFacade).notifyUser(any(), any(), any());

        cloudNotificationFacadeMockedStatic.when(ICloudNotificationFacade::getInstance).thenReturn(sspNotificationFacade);

        ISSPFacade sspFacade = Mockito.mock(ISSPFacade.class);
        Mockito.doReturn(pStoppedSystem).when(sspFacade).stopSystem(any(), any(), any());

        sspFacadeMockedStatic.when(ISSPFacade::getInstance).thenReturn(sspFacade);

        DecodedJWT decodedJWT = Mockito.spy(DecodedJWT.class);

        userCredentialsManagerMockedStatic.when(UserCredentialsManager::getCredentials).thenReturn(decodedJWT);

        NotificationFacadeTestUtil.verifyNotificationFacadeNotify("Stop System", pMessage, () -> stopSystemAction.stopSystem(systemInfo));
      }
    }
  }

}
