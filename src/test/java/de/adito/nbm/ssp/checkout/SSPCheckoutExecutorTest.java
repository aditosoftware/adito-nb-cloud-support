package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.IGitVersioningSupport;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.facade.*;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import lombok.SneakyThrows;
import org.jetbrains.annotations.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.ImageUtilities;

import java.awt.*;
import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Test class for {@link SSPCheckoutExecutor}.
 *
 * @author r.hartinger, 22.02.2023
 */
class SSPCheckoutExecutorTest
{
  /**
   * Tests the method {@link SSPCheckoutExecutor#execute(ProgressHandle, ISSPSystemDetails, File, String, boolean)}.
   */
  @Nested
  class Execute
  {
    /**
     * Tests that a notification will be put to the facade, if the clone was not successful.
     */
    @Test
    @SneakyThrows
    void shouldNotifyWithCloneNotSuccessful()
    {
      NotificationFacadeTestUtil.verifyNotificationFacadeNotify("SSP System Checkout", "Failed to clone the project, aborting",
                                                                () -> baseExecute(false));
    }

    /**
     * Tests that not notification will be put to the facade, if the clone was successful.
     */
    @Test
    @SneakyThrows
    void shouldExecute()
    {
      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(() -> baseExecute(true));
    }

    /**
     * Base method for testing {@link SSPCheckoutExecutor#execute(ProgressHandle, ISSPSystemDetails, File, String, boolean, boolean)}.
     *
     * @param pCloneSuccess the result which should be returned by {@link SSPCheckoutExecutor#performGitClone(ProgressHandle, String, String, String, String, File)}.
     */
    private void baseExecute(boolean pCloneSuccess)
    {
      ProgressHandle progressHandle = Mockito.mock(ProgressHandle.class);

      ISSPSystemDetails systemDetails = Mockito.spy(ISSPSystemDetails.class);

      try (MockedStatic<UserCredentialsManager> userCredentialsManagerMockedStatic = Mockito.mockStatic(UserCredentialsManager.class);
           MockedStatic<SSPCheckoutExecutor> sspCheckoutExecutorMockedStatic = Mockito.mockStatic(SSPCheckoutExecutor.class);
           MockedStatic<ISSPFacade> isspFacadeMockedStatic = Mockito.mockStatic(ISSPFacade.class))
      {

        isspFacadeMockedStatic.when(ISSPFacade::getInstance).thenReturn(Mockito.spy(ISSPFacade.class));

        sspCheckoutExecutorMockedStatic.when(() -> SSPCheckoutExecutor.execute(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenCallRealMethod();

        sspCheckoutExecutorMockedStatic.when(() -> SSPCheckoutExecutor.getServerConfigContents(any(), any(), any())).thenReturn(Optional.empty());
        sspCheckoutExecutorMockedStatic.when(() -> SSPCheckoutExecutor.getTunnelConfigContents(any(), any())).thenReturn(Optional.empty());
        sspCheckoutExecutorMockedStatic.when(() -> SSPCheckoutExecutor.performGitClone(any(), any(), any(), any(), any(), any())).thenReturn(pCloneSuccess);

        DecodedJWT currentCredentials = Mockito.spy(DecodedJWT.class);
        userCredentialsManagerMockedStatic.when(UserCredentialsManager::getCredentials).thenReturn(currentCredentials);

        // method call to the method under test
        assertNotNull(SSPCheckoutExecutor.execute(progressHandle, systemDetails, new File("").getAbsoluteFile(), "main", true, true));

        sspCheckoutExecutorMockedStatic.verify(() -> SSPCheckoutExecutor.execute(any(), any(), any(), any(), anyBoolean(), anyBoolean()));
        sspCheckoutExecutorMockedStatic.verify(() -> SSPCheckoutExecutor.getServerConfigContents(any(), any(), any()));
        sspCheckoutExecutorMockedStatic.verify(() -> SSPCheckoutExecutor.getTunnelConfigContents(any(), any()));
        sspCheckoutExecutorMockedStatic.verify(() -> SSPCheckoutExecutor.getGitProject(any(), any(), any()));
        sspCheckoutExecutorMockedStatic.verify(() -> SSPCheckoutExecutor.performGitClone(any(), any(), any(), isNull(), eq("origin"), any()));
        sspCheckoutExecutorMockedStatic.verify(() -> SSPCheckoutExecutor.writeConfigs(notNull(), notNull(), notNull(), notNull()), pCloneSuccess ? Mockito.times(1) : Mockito.never());
        sspCheckoutExecutorMockedStatic.verifyNoMoreInteractions();

        userCredentialsManagerMockedStatic.verify(UserCredentialsManager::getCredentials);
        userCredentialsManagerMockedStatic.verifyNoMoreInteractions();

        isspFacadeMockedStatic.verify(ISSPFacade::getInstance);

        Mockito.verify(progressHandle).start();
        Mockito.verify(progressHandle).finish();
        Mockito.verifyNoMoreInteractions(progressHandle);

        Mockito.verifyNoMoreInteractions(systemDetails);
      }
    }
  }

  /**
   * Tests the method {@link SSPCheckoutExecutor#performGitClone(ProgressHandle, String, String, String, String, File)}.
   */
  @Nested
  class PerformGitClone
  {

    /**
     * Tests that there is a message written, if no GitSupport is there.
     */
    @Test
    @SneakyThrows
    void shouldWriteMessageWhenNoGitSupport()
    {
      NotificationFacadeTestUtil.verifyNotificationFacadeNotify("No GIT Versioning Support found", "The GIT Plugin is necessary to execute this task",
                                                                () -> basePerformGitClone(null, () -> {
                                                                  ProgressHandle progressHandle = Mockito.mock(ProgressHandle.class);

                                                                  assertFalse(SSPCheckoutExecutor.performGitClone(progressHandle, "remotePath", null, null, null, new File("")));

                                                                  Mockito.verifyNoMoreInteractions(progressHandle);
                                                                }));
    }

    /**
     * Tests that there is no message written if a GitSupport is there.
     */
    @Test
    @SneakyThrows
    void shouldPerformGitClone()
    {
      IGitVersioningSupport gitVersioningSupport = Mockito.spy(IGitVersioningSupport.class);
      Mockito.doReturn(true).when(gitVersioningSupport).performClone(any(), any(), any());

      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(
          () -> basePerformGitClone(gitVersioningSupport, () -> {
            ProgressHandle progressHandle = Mockito.mock(ProgressHandle.class);

            try
            {
              assertTrue(SSPCheckoutExecutor.performGitClone(progressHandle, "remotePath", null, null, null, new File("")));


              Mockito.verify(progressHandle).progress("performing Git clone");
              Mockito.verifyNoMoreInteractions(progressHandle);

              Mockito.verify(gitVersioningSupport).performClone(any(), any(), any());
              Mockito.verifyNoMoreInteractions(gitVersioningSupport);
            }
            catch (Exception pE)
            {
              throw new RuntimeException(pE);
            }
          }));
    }

    /**
     * Base method for testing {@link SSPCheckoutExecutor#performGitClone(ProgressHandle, String, String, String, String, File)}.
     *
     * @param pGitSupport the {@link IGitVersioningSupport} that should be set in the static attributes of {@link SSPCheckoutExecutor}
     * @param pRunnable   the Runnable which should execute the method call
     */
    private void basePerformGitClone(@Nullable IGitVersioningSupport pGitSupport, @NotNull Runnable pRunnable)
    {
      // change some fields of the class for this test
      SSPCheckoutExecutor.setLoading(false);
      SSPCheckoutExecutor.setGitSupport(pGitSupport);

      try (MockedStatic<GraphicsEnvironment> graphicsEnvironmentMockedStatic = Mockito.mockStatic(GraphicsEnvironment.class);
           MockedStatic<ImageUtilities> imageUtilitiesMockedStatic = Mockito.mockStatic(ImageUtilities.class))
      {
        imageUtilitiesMockedStatic.when(() -> ImageUtilities.loadImageIcon(any(), anyBoolean())).thenReturn(null);

        graphicsEnvironmentMockedStatic.when(GraphicsEnvironment::isHeadless).thenReturn(false);


        pRunnable.run();
      }
    }
  }

}
