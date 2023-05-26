package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.annotations.VisibleForTesting;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.IGitVersioningSupport;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.metainfo.IMetaInfo;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.metainfo.deploy.IDeployMetaInfoFacade;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.model.IModelFacade;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectCreationManager;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.tunnel.*;
import de.adito.nbm.cloud.runconfig.TelnetLoggerRunConfig;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.exceptions.AditoSSPException;
import de.adito.nbm.ssp.facade.*;
import de.adito.notification.INotificationFacade;
import lombok.*;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.*;
import org.netbeans.api.keyring.Keyring;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.filesystems.*;
import org.openide.util.*;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * Performs the git checkout and writes the config files
 *
 * @author m.kaspera, 02.11.2020
 */
public class SSPCheckoutExecutor
{

  public static final String CONFIG_FOLDER_PATH = "data/config/";
  public static final String DEFAULT_SERVER_CONFIG_NAME = "serverconfig_default.xml";
  public static final String DEFAULT_TUNNEL_CONFIG_NAME = "tunnelconfig.xml";
  private static final Logger logger = Logger.getLogger(SSPCheckoutExecutor.class.getName());

  @Setter(value = AccessLevel.PACKAGE, onMethod_ = {@VisibleForTesting})
  private static IGitVersioningSupport gitSupport;
  private static Future<?> future = null;
  @Setter(value = AccessLevel.PACKAGE, onMethod_ = {@VisibleForTesting})
  private static boolean isLoading = false;

  private SSPCheckoutExecutor()
  {
  }

  /**
   * @param pHandle                  ProgressHandle that should be used to indicate the current progress
   * @param pSystemDetails           Selected system details, containg the info about the git repo and url as well as the system id
   * @param pTarget                  the location that the project should be cloned to
   * @param pBranch                  Name of the branch to checkout, null may use the previously deployed branch
   * @param pIsCheckoutProject       true, if the project should be checked out. This requires pBranch to be set.
   * @param pIsCheckoutDeployedState whether the default state of the system should be checked out or the currently deployed state as it is in the database
   * @return true if the clone was performed successfully
   */
  @Nullable
  static FileObject execute(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pTarget, @Nullable String pBranch,
                            boolean pIsCheckoutProject, boolean pIsCheckoutDeployedState)
  {
    try
    {
      pHandle.start();
      DecodedJWT currentCredentials = UserCredentialsManager.getCredentials();
      if (currentCredentials == null)
      {
        logger.log(Level.WARNING, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.noCredentials"));
        return null;
      }
      Optional<String> serverConfigContentsOpt = getServerConfigContents(pHandle, pSystemDetails, currentCredentials);
      Optional<String> tunnelConfigContentsOpt = getTunnelConfigContents(pHandle, pSystemDetails);
      if (pIsCheckoutDeployedState && serverConfigContentsOpt.isPresent() && tunnelConfigContentsOpt.isPresent())
      {
        checkoutDeployedState(pHandle, pSystemDetails, currentCredentials, pTarget, serverConfigContentsOpt.get(), tunnelConfigContentsOpt.get(),
                              pIsCheckoutProject, pBranch);
      }
      else if (pIsCheckoutProject && pBranch != null)
      {
        boolean cloneSuccess = performGitClone(pHandle, getGitProject(ISSPFacade.getInstance(), pSystemDetails, currentCredentials),
                                               pBranch, null, "origin", pTarget);
        if (cloneSuccess)
          writeConfigs(pHandle, pSystemDetails, pTarget, currentCredentials);
        else
          INotificationFacade.INSTANCE.notify(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TITLE.SSPCheckoutExecutor.execute.clone"),
                                              SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.execute.clone.error"),
                                              false);
      }
    }
    finally
    {
      pHandle.finish();
    }
    return FileUtil.toFileObject(pTarget);
  }

  private static void checkoutDeployedState(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull DecodedJWT pCurrentCredentials,
                                            @NotNull File pTarget, @NotNull String pServerConfigContents, @NotNull String pTunnelConfigContents, boolean pIncludeGitProject,
                                            @Nullable String pFallbackBranch)
  {
    pHandle.setDisplayName("Starting tunnels");
    try
    {
      storeSSHPasswords(pSystemDetails, ISSPFacade.getInstance(), pCurrentCredentials, pTunnelConfigContents);
      try (StartTunnelInfo tunnelInfo = startTunnels(pTunnelConfigContents))
      {
        if (tunnelInfo.isTunnelsGo())
        {
          Path tempServerConfigFile = Files.createTempFile("", "");
          writeFileData(tempServerConfigFile.toFile(), pServerConfigContents);

          Optional<IMetaInfo> metaInfos = getMetaInfos(tempServerConfigFile.toFile());

          // Clone project, if possible and necessary
          Boolean cloneSuccess = null;
          if (pIncludeGitProject)
          {
            String branchToCheckout = metaInfos.map(SSPCheckoutExecutor::getDeployedBranch).orElse(pFallbackBranch);
            if (branchToCheckout != null)
            {
              String gitProjectUrl = getGitProject(ISSPFacade.getInstance(), pSystemDetails, pCurrentCredentials);
              cloneSuccess = performGitClone(pHandle, gitProjectUrl, branchToCheckout, null, "origin", pTarget);
            }
          }

          // Read from deployed state, if the clone did not fail
          if (cloneSuccess != Boolean.FALSE)
          {
            // if no project was cloned before, then the target directory should be created anyway
            if (!pTarget.exists())
              //noinspection ResultOfMethodCallIgnored
              pTarget.mkdirs();
            else
              cleanTargetDirectory(pTarget);

            String serverConfigPath = tempServerConfigFile.toAbsolutePath().toString();
            IProjectCreationManager projectCreationManager = Lookup.getDefault().lookup(IProjectCreationManager.class);
            String projectVersion = metaInfos.map(SSPCheckoutExecutor::getProjectVersion).orElse(null);
            projectCreationManager.createProject(pHandle, pTarget.getParentFile().getAbsolutePath(), pTarget.getName(), projectVersion, serverConfigPath);
            writeConfigs(pHandle, pSystemDetails, pTarget, pCurrentCredentials);
          }
        }
      }
    }
    catch (IOException | TransformerException | UnirestException | AditoSSPException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.execute.write.error",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
  }

  static void cleanTargetDirectory(@NotNull File pTarget) throws IOException
  {
    IModelFacade facade = Lookup.getDefault().lookup(IModelFacade.class);
    File[] filesFoldersToDelete;
    if (facade != null)
      filesFoldersToDelete = facade.getMajorModelTypes().stream()
          .map(facade::getModelGroupNameForType)
          .filter(Objects::nonNull)
          .map(pChild -> new File(pTarget, pChild))
          .toArray(File[]::new);
    else
      filesFoldersToDelete = pTarget.listFiles((dir, name) -> !name.equalsIgnoreCase(".git"));

    if (filesFoldersToDelete != null)
      for (File pFile : filesFoldersToDelete)
      {
        if (pFile.exists())
          FileUtils.deleteDirectory(pFile);
      }
  }

  /**
   * @param pServerConfigFile Serverconfig file used to know where the database is
   * @return Optional of the MetaInfos contained in the database that corresponds with the serverConfig
   */
  private static Optional<IMetaInfo> getMetaInfos(@NotNull File pServerConfigFile)
  {
    return Optional.ofNullable(Lookup.getDefault().lookup(IDeployMetaInfoFacade.class))
        .map(pFacade -> pFacade.getInfos(pServerConfigFile));
  }

  /**
   * @param pMetaInfo MetaInfo that contains the name of the branch that was used when the last deploy happened
   * @return name of the branch that was used when the last deploy happened
   */
  @Nullable
  private static String getDeployedBranch(@NotNull IMetaInfo pMetaInfo)
  {
    return pMetaInfo.getAll().get("GitDeployMetaInfoProvider.branchActualName");
  }

  /**
   * @param pMetaInfo MetaInfo that contains the project version info
   * @return version that should be used when creating the project
   */
  @Nullable
  private static String getProjectVersion(@NotNull IMetaInfo pMetaInfo)
  {
    return pMetaInfo.getAll().get("de.adito.aditoweb.nbm.deploy.metainfo.ProjectVersionDeployMetaInfoProvider.designer.project.version");
  }

  /**
   * @param pSspFacade          ISSPFacade that contains the methods for interacting with the SSP
   * @param pSystemDetails      ISSPSystemDetails that contain the information about the selected SSP system
   * @param pCurrentCredentials JWT containing the credentials for authenticating with the SSP
   * @return the git repository to use for cloning
   */
  @VisibleForTesting
  static String getGitProject(@NotNull ISSPFacade pSspFacade, @NotNull ISSPSystemDetails pSystemDetails, @NotNull DecodedJWT pCurrentCredentials)
  {
    Map<String, String> configMap = null;
    try
    {
      configMap = pSspFacade.getConfigMap(pCurrentCredentials.getSubject(), pCurrentCredentials, pSystemDetails);
    }
    catch (UnirestException | AditoSSPException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.error.configMap",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
    return Optional.ofNullable(configMap).map(pConfigMap -> pConfigMap.get("linked_git_project")).orElseGet(pSystemDetails::getGitRepoUrl);
  }

  /**
   * Starts all necessary tunnels described in the given tunnel config
   *
   * @param pTunnelConfigContents tunnel config that contains all necessary tunnels
   * @return information about the started tunnels
   */
  @NotNull
  private static StartTunnelInfo startTunnels(@NotNull String pTunnelConfigContents)
  {
    ISSHTunnelProvider tunnelProvider = Lookup.getDefault().lookup(ISSHTunnelProvider.class);
    if (tunnelProvider != null)
    {
      try
      {
        List<ITunnelConfig> tunnelConfigs = tunnelProvider.readTunnelsFromConfig(new ByteArrayInputStream(pTunnelConfigContents.getBytes(StandardCharsets.UTF_8)));
        List<ISSHTunnel> sshTunnels = tunnelConfigs.stream().map(tunnelProvider::createTunnel).collect(Collectors.toList());
        List<ISSHTunnel> failedTunnels = TelnetLoggerRunConfig.startTunnels(sshTunnels);
        if (!failedTunnels.isEmpty())
        {
          failedTunnels.forEach(pTunnel -> logger.log(Level.WARNING, SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class,
                                                                                                                 "TXT.SSPCheckoutExecutor.tunnel.start.failed"),
                                                      SSPCheckoutExecutor.tunnelToString(pTunnel)));
        }
        return new StartTunnelInfo(failedTunnels.size() != sshTunnels.size(), sshTunnels);
      }
      catch (InterruptedException | TransformerException pE)
      {
        logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.tunnel.start.error",
                                                                                        ExceptionUtils.getStackTrace(pE)));
      }
    }
    return new StartTunnelInfo(false, List.of());
  }

  /**
   * @param pHandle        ProgressHandle that should be used to indicate the current progress
   * @param pSystemDetails Selected system details, containg the info about the git repo and url as well as the system id
   * @param pTarget        the location that the project should be cloned to
   * @param pCredentials   the cre
   */
  @VisibleForTesting
  static void writeConfigs(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pTarget, @NotNull DecodedJWT pCredentials)
  {
    getServerConfigContents(pHandle, pSystemDetails, pCredentials)
        .ifPresent(pServerConfigContents -> writeServerConfig(pHandle, new File(pTarget, CONFIG_FOLDER_PATH), DEFAULT_SERVER_CONFIG_NAME, pServerConfigContents));

    getTunnelConfigContents(pHandle, pSystemDetails)
        .ifPresent(pTunnelConfigContents -> writeTunnelConfig(pHandle, pSystemDetails, new File(pTarget, CONFIG_FOLDER_PATH), DEFAULT_TUNNEL_CONFIG_NAME, pCredentials, pTunnelConfigContents));
    NbPreferences.forModule(ISystemInfo.class).put(ISystemInfo.CLOUD_ID_PREF_KEY_PEFIX + "default." + pTarget.getPath().replace("\\", "/"), pSystemDetails.getSystemdId());
    try
    {
      NbPreferences.forModule(ISystemInfo.class).put("serverAddressDefault." + pTarget.getPath().replace("\\", "/"), getUrlWithoutProtocol(pSystemDetails.getUrl()));
    }
    catch (MalformedURLException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.error.serverAddress",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
  }

  public static Optional<String> getTunnelConfigContents(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails)
  {
    ISSPFacade sspFacade = ISSPFacade.getInstance();
    DecodedJWT currentCredentials = UserCredentialsManager.getCredentials();
    if (currentCredentials == null)
    {
      logger.log(Level.WARNING, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.noCredentials"));
      return Optional.empty();
    }
    pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.tunnelConfig"));
    try
    {
      return Optional.of(sspFacade.getTunnelConfig(currentCredentials.getSubject(), currentCredentials, pSystemDetails));
    }
    catch (UnirestException | AditoSSPException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.tunnelConfig.error",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
    return Optional.empty();
  }

  public static Optional<String> getServerConfigContents(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull DecodedJWT pCurrentCredentials)
  {
    ISSPFacade sspFacade = ISSPFacade.getInstance();
    pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.serverConfig"));
    try
    {
      return Optional.of(sspFacade.getServerConfig(pCurrentCredentials.getSubject(), pCurrentCredentials, pSystemDetails));
    }
    catch (UnirestException | AditoSSPException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.serverConfig.error",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
    return Optional.empty();
  }

  /**
   * @param pHandle               ProgessHandle to progress
   * @param pConfigFolder         folder that should contain the config file
   * @param pFileName             name of the file, will be placed inside the CONFIG_FOLDER_PATH in the project folder
   * @param pServerConfigContents contents of the file to be written
   */
  public static void writeServerConfig(@NotNull ProgressHandle pHandle, @NotNull File pConfigFolder, @NotNull String pFileName, @NotNull String pServerConfigContents)
  {
    try
    {
      pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.write.serverConfig"));
      writeFileData(new File(pConfigFolder, pFileName), pServerConfigContents);
    }
    catch (IOException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.error.serverConfig",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
  }

  /**
   * @param pHandle               ProgessHandle to progress
   * @param pSystemDetails        systemDetails, used for storing the credentials necessary to establish a tunnel connection
   * @param pConfigFolder         folder that should contain the config file
   * @param pFileName             name of the file, will be placed inside the CONFIG_FOLDER_PATH in the project folder
   * @param currentCredentials    Credentials for the tunnels that should be stored so the user does not have to enter them when connecting to the tunnel
   * @param pTunnelConfigContents contents of the file to be written
   */
  public static void writeTunnelConfig(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pConfigFolder, @NotNull String pFileName,
                                       @NotNull DecodedJWT currentCredentials, @NotNull String pTunnelConfigContents)
  {
    try
    {
      pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.write.tunnelConfig"));
      writeFileData(new File(pConfigFolder, pFileName), pTunnelConfigContents);
      storeSSHPasswords(pSystemDetails, ISSPFacade.getInstance(), currentCredentials, pTunnelConfigContents);
    }
    catch (IOException | UnirestException | AditoSSPException | TransformerException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.error.tunnelConfig",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
  }

  private static void writeFileData(@NotNull File pFile, @NotNull String pFileContents) throws IOException
  {
    FileObject fileObject = FileUtil.createData(pFile);
    fileObject.getOutputStream().write(pFileContents.getBytes(StandardCharsets.UTF_8));
  }

  @NotNull
  public static String getUrlWithoutProtocol(@NotNull String pUrl) throws MalformedURLException
  {
    URL serverAddressURL = new URL(pUrl);
    String serverAddressWithoutProtocol = pUrl.replace(serverAddressURL.getProtocol() + ":", "");
    if (serverAddressWithoutProtocol.startsWith("//"))
      serverAddressWithoutProtocol = serverAddressWithoutProtocol.substring(2);
    return serverAddressWithoutProtocol;
  }

  private static void storeSSHPasswords(@NotNull ISSPSystemDetails pPSystemDetails, @NotNull ISSPFacade pSspFacade, @NotNull DecodedJWT pCurrentCredentials,
                                        @NotNull String pTunnelConfigContents) throws IOException, TransformerException, UnirestException, AditoSSPException
  {
    ISSHTunnelProvider tunnelProvider = Lookup.getDefault().lookup(ISSHTunnelProvider.class);
    try (InputStream inputStream = new ByteArrayInputStream(pTunnelConfigContents.getBytes()))
    {
      List<ITunnelConfig> tunnelConfigs = tunnelProvider.readTunnelsFromConfig(inputStream);
      String pass = pSspFacade.getConfigMap(pCurrentCredentials.getSubject(), pCurrentCredentials, pPSystemDetails).get("sshpass");
      tunnelConfigs.forEach(pTunnelConfig ->
                                Keyring.save(pTunnelConfig.getUser() + "@" + pTunnelConfig.getTunnelHostAddress() + ":" + pTunnelConfig.getTunnelHostPort(),
                                             pass.toCharArray(), ""));
    }
  }

  /**
   * @param pHandle     ProgressHandle that should be used to indicate the current progress
   * @param pRemotePath URL of the git repository
   * @param pBranch     Name of the branch to check out, may be null
   * @param pTagName    Tag to check out, may be null
   * @param pRemoteName Name of the remote to put in the config for the given repo url
   * @param pTarget     the location that the project should be cloned to
   * @return true if the clone was performed successfully
   */
  @VisibleForTesting
  static boolean performGitClone(ProgressHandle pHandle, @NotNull String pRemotePath, @Nullable String pBranch, @Nullable String pTagName,
                                 @Nullable String pRemoteName, @NotNull File pTarget)
  {
    try
    {
      if (isLoading)
      {
        pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.git.fetching"));
        future.get();
      }
      if (gitSupport == null)
      {
        if (!GraphicsEnvironment.isHeadless())
          INotificationFacade.INSTANCE.notify(
              SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TITLE.SSPCheckoutExecutor.git.missing"),
              SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.git.missing"),
              true);
        return false;
      }


      Map<String, String> options = new HashMap<>();
      if (pBranch != null)
        options.put("branch", pBranch);
      if (pTagName != null)
        options.put("tag", pTagName);
      if (pRemoteName != null)
        options.put("remote", pRemoteName);
      pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.git"));
      return checkoutProject(pRemotePath, pTarget, options);
    }
    catch (Exception pException)
    {
      logger.log(Level.WARNING, pException, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.git.error",
                                                                                              ExceptionUtils.getStackTrace(pException)));
      return false;
    }
  }

  private static boolean checkoutProject(@NotNull String pRemotePath, @NotNull File pTarget, Map<String, String> options) throws Exception
  {
    try
    {
      return gitSupport.performClone(pRemotePath, pTarget, options);
    }
    catch (Exception pE)
    {
      // If an exception was thrown because the project did not contain the requested branch or tag, try to checkout the project with the default branch
      if (pE.getClass().getSimpleName().equals("AditoGitException") && pE.getCause().getClass().getSimpleName().equals("TransportException"))
      {
        options.remove("branch");
        options.remove("tag");
        return gitSupport.performClone(pRemotePath, pTarget, options);
      }
      else
      {
        throw pE;
      }
    }
  }

  /**
   * Start an async process that loads the IGitVersioningSupport (if not already loaded)
   */
  static void preloadGitSupport()
  {
    if (gitSupport == null)
    {
      isLoading = true;
      ExecutorService executorService = Executors.newSingleThreadExecutor();
      future = executorService.submit(() -> {
        gitSupport = Lookup.getDefault().lookup(IGitVersioningSupport.class);
        isLoading = false;
        executorService.shutdown();
        return true;
      });
    }
  }

  private static String tunnelToString(ISSHTunnel pTunnel)
  {
    return pTunnel.getLocalTarget() + ":" + pTunnel.getRemoteTargetPort() + ":" + pTunnel.getRemoteTarget() + "@" + pTunnel.getTunnelHost() + ":" + pTunnel.getPort();
  }

  /**
   * Contains information about all started tunnels and provides the ability to close them afterwards.
   * This object gets created during {@link SSPCheckoutExecutor#startTunnels(String)}.
   */
  @Log
  @RequiredArgsConstructor
  private static class StartTunnelInfo implements AutoCloseable
  {
    /**
     * Determines, if the tunnels are connect and ready
     */
    @Getter
    private final boolean isTunnelsGo;

    /**
     * Contains all closables, that should be closed during {@link AutoCloseable#close()}
     */
    @NotNull
    private final List<? extends AutoCloseable> closeables;

    @Override
    public void close()
    {
      for (AutoCloseable closeable : closeables)
      {
        try
        {
          closeable.close();
        }
        catch (Exception e)
        {
          log.log(Level.WARNING, "Failed to close object: " + closeable, e);
        }
      }
    }
  }

}
