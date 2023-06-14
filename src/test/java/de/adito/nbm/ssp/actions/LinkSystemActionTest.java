package de.adito.nbm.ssp.actions;

import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.actions.LinkSystemAction.CONFIG_RESULTS;
import de.adito.nbm.ssp.facade.ISSPSystemDetails;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

import static de.adito.nbm.ssp.actions.LinkSystemAction.CONFIG_RESULTS.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link LinkSystemAction}.
 *
 * @author r.hartinger, 22.02.2023
 */
class LinkSystemActionTest
{

  /**
   * Tests if this inner enum contains all the values. If this test fails, you need to adjust other tests in this class that also use the enum.
   */
  @Test
  void shouldValidateEnumContainsExpectedValues()
  {
    assertEquals(Set.of(DO_NOT_WRITE, WRITTEN, OVERRIDDEN, NOT_OVERRIDDEN, CANCELLED), Set.of(CONFIG_RESULTS.values()));
  }

  /**
   * Tests the method {@link LinkSystemAction#performLink(ISystemInfo, ISSPSystemDetails, boolean)}.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class PerformLink
  {

    /**
     * @return the arguments for {@link LinkSystemAction#performLink(ISystemInfo, ISSPSystemDetails, boolean)}
     */
    @NonNull
    private Stream<Arguments> shouldPerformLink()
    {
      String cancelledMessage = "Link was aborted";
      String notOverriddenMessage = "Link was successful.\nThe config files were not overridden and probably do not match";
      String successWithConfigsMessage = "Link was successful\nMake sure the serverConfigPath and tunnelConfigPath properties point to the downloaded configuration files";
      String successMessage = "Link was successful";

      return Stream.of(
          Arguments.of(successMessage, 1, DO_NOT_WRITE, false),
          Arguments.of(successMessage, 1, WRITTEN, false),
          Arguments.of(successMessage, 1, OVERRIDDEN, false),
          Arguments.of(successMessage, 1, NOT_OVERRIDDEN, false),
          Arguments.of(successMessage, 1, CANCELLED, false),

          Arguments.of(successWithConfigsMessage, 1, DO_NOT_WRITE, true),
          Arguments.of(successWithConfigsMessage, 1, WRITTEN, true),
          Arguments.of(successWithConfigsMessage, 1, OVERRIDDEN, true),
          Arguments.of(notOverriddenMessage, 1, NOT_OVERRIDDEN, true),
          Arguments.of(cancelledMessage, 0, CANCELLED, true)
      );
    }

    /**
     * Tests the method {@link LinkSystemAction#performLink(ISystemInfo, ISSPSystemDetails, boolean)}.
     *
     * @param pMessage                the expected message that should be given to {@link de.adito.notification.INotificationFacade#notify(String, String, boolean)}
     * @param pSetCloudAndGetUrlTimes the number of times {@link ISystemInfo#setCloudId(String)} and {@link ISSPSystemDetails#getUrl()} should be called
     * @param pConfigResults          the {@link CONFIG_RESULTS} that should be returned by
     *                                {@link LinkSystemAction#handleConfigFiles(ISystemInfo, ISSPSystemDetails, ProgressHandle, File)}
     * @param pIsLoadConfigs          parameter that should be passed to the method. Also indicated as a verification if
     *                                {@link LinkSystemAction#handleConfigFiles(ISystemInfo, ISSPSystemDetails, ProgressHandle, File)} was called.
     *                                If this method would not be called, the given {@code pConfigResults} will be ignored
     */
    @ParameterizedTest
    @MethodSource
    @SneakyThrows
    void shouldPerformLink(@NonNull String pMessage, int pSetCloudAndGetUrlTimes, @NonNull CONFIG_RESULTS pConfigResults, boolean pIsLoadConfigs)
    {
      Project project = Mockito.spy(Project.class);
      Mockito.doReturn(FileUtil.toFileObject(new File("").getAbsoluteFile())).when(project).getProjectDirectory();

      ISystemInfo systemInfo = Mockito.spy(ISystemInfo.class);
      Mockito.doReturn(project).when(systemInfo).getProject();

      ISSPSystemDetails systemDetails = Mockito.spy(ISSPSystemDetails.class);
      Mockito.doReturn("http://localhost").when(systemDetails).getUrl();

      LinkSystemAction linkSystemAction = Mockito.spy(new LinkSystemAction());

      Mockito.doReturn(pConfigResults).when(linkSystemAction).handleConfigFiles(any(), any(), any(), any());

      NotificationFacadeTestUtil.verifyNotificationFacadeNotify("Linking Cloud System", pMessage,
                                                                () -> linkSystemAction.performLink(systemInfo, systemDetails, pIsLoadConfigs));

      Mockito.verify(linkSystemAction, Mockito.times(pIsLoadConfigs ? 1 : 0)).handleConfigFiles(any(), any(), any(), any());

      Mockito.verify(systemInfo).getProject();
      Mockito.verify(systemInfo, Mockito.times(pSetCloudAndGetUrlTimes)).setCloudId(any());
      Mockito.verifyNoMoreInteractions(systemInfo);

      Mockito.verify(systemDetails).getSystemdId();
      Mockito.verify(systemDetails, Mockito.times(pSetCloudAndGetUrlTimes)).getUrl();
      Mockito.verifyNoMoreInteractions(systemDetails);
    }
  }

}
