package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.IGitVersioningSupport;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.metainfo.IMetaInfo;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.metainfo.deploy.IDeployMetaInfoFacade;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectCreationManager;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.tunnel.*;
import de.adito.nbm.cloud.runconfig.TelnetLoggerRunConfig;
import de.adito.nbm.icons.IconManager;
import de.adito.nbm.runconfig.api.ISystemInfo;
import de.adito.nbm.ssp.auth.UserCredentialsManager;
import de.adito.nbm.ssp.exceptions.AditoSSPException;
import de.adito.nbm.ssp.facade.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.*;
import org.netbeans.api.keyring.Keyring;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.awt.NotificationDisplayer;
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
import java.util.stream.*;

/**
 * Performs the git checkout and writes the config files
 *
 * @author m.kaspera, 02.11.2020
 */
public class SSPCheckoutExecutor
{

  public static final String DEFAULT_SERVER_CONFIG_PATH = "data/config/serverconfig_default.xml";
  public static final String DEFAULT_TUNNEL_CONFIG_PATH = "data/config/tunnelConfig.xml";
  private static final Logger logger = Logger.getLogger(SSPCheckoutExecutor.class.getName());
  private static IGitVersioningSupport gitSupport;
  private static Future<?> future = null;
  private static boolean isLoading = false;

  private SSPCheckoutExecutor()
  {
  }

  /**
   * @param pHandle                  ProgressHandle that should be used to indicate the current progress
   * @param pSystemDetails           Selected system details, containg the info about the git repo and url as well as the system id
   * @param pTarget                  the location that the project should be cloned to
   * @param pIsCheckoutDeployedState whether the default state of the system should be checked out or the currently deployed state as it is in the database
   * @return true if the clone was performed successfully
   */
  static FileObject execute(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pTarget, boolean pIsCheckoutDeployedState)
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
        _checkoutDeployedState(pHandle, pSystemDetails, pTarget, serverConfigContentsOpt.get(), tunnelConfigContentsOpt.get());
      }
      else
      {
        boolean cloneSuccess = performGitClone(pHandle, pSystemDetails.getGitRepoUrl(), pSystemDetails.getGitBranch(), null, "origin", pTarget);
        if (cloneSuccess)
          writeConfigs(pHandle, pSystemDetails, pTarget, currentCredentials);
        else
          NotificationDisplayer.getDefault().notify(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TITLE.SSPCheckoutExecutor.execute.clone"),
                                                    NotificationDisplayer.Priority.HIGH.getIcon(),
                                                    SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.execute.clone.error"),
                                                    null, NotificationDisplayer.Priority.HIGH);
      }
    }
    finally
    {
      pHandle.finish();
    }
    return FileUtil.toFileObject(pTarget);
  }

  private static void _checkoutDeployedState(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pTarget,
                                             @NotNull String pServerConfigContents, @NotNull String pTunnelConfigContents)
  {
    pHandle.setDisplayName("Starting tunnels");
    boolean isTunnelsGo = _startTunnels(pTunnelConfigContents);
    if (isTunnelsGo)
    {
      try
      {
        Path tempServerConfigFile = Files.createTempFile("", "");
        writeFileData(tempServerConfigFile.toFile(), pServerConfigContents);
        Optional<String> deployedBranchName = _getDeployedBranch(tempServerConfigFile.toFile());
        boolean cloneSuccess = performGitClone(pHandle, pSystemDetails.getGitRepoUrl(), deployedBranchName.orElse(pSystemDetails.getGitBranch()), null,
                                               "origin", pTarget);
        if (cloneSuccess)
        {
          _cleanTargetDirectory(pTarget);
          IProjectCreationManager projectCreationManager = Lookup.getDefault().lookup(IProjectCreationManager.class);
          projectCreationManager.createProject(pHandle, pTarget.getParentFile().getAbsolutePath(),
                                               pTarget.getName(), tempServerConfigFile.toAbsolutePath().toString());
        }
      }
      catch (IOException pE)
      {
        logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.execute.write.error",
                                                                                        ExceptionUtils.getStackTrace(pE)));
      }
    }
  }

  static void _cleanTargetDirectory(@NotNull File pTarget) throws IOException
  {
    File[] filesFoldersToDelete = pTarget.listFiles((dir, name) -> !name.equalsIgnoreCase(".git"));
    if (filesFoldersToDelete != null)
    {
      for (File toDelete : filesFoldersToDelete)
      {
        try (Stream<Path> fileStream = Files.walk(toDelete.toPath()))
        {
          fileStream
              .sorted(Comparator.reverseOrder())
              .map(Path::toFile)
              .forEach(File::delete);
        }
      }
    }
  }

  @NotNull
  private static Optional<String> _getDeployedBranch(@NotNull File pServerConfigFile)
  {
    IDeployMetaInfoFacade deployMetaInfoFacade = Lookup.getDefault().lookup(IDeployMetaInfoFacade.class);
    if (deployMetaInfoFacade != null)
    {
      IMetaInfo metaInfo = deployMetaInfoFacade.getInfos(pServerConfigFile);
      if (metaInfo != null)
      {
        return Optional.ofNullable(metaInfo.getAll().get("branchActualName"));
      }
    }
    return Optional.empty();
  }

  private static boolean _startTunnels(@NotNull String pTunnelConfigContents)
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
                                                      SSPCheckoutExecutor._tunnelToString(pTunnel)));
        }
        return failedTunnels.size() != sshTunnels.size();
      }
      catch (InterruptedException | TransformerException pE)
      {
        logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.tunnel.start.error",
                                                                                        ExceptionUtils.getStackTrace(pE)));
      }
    }
    return false;
  }

  /**
   * @param pHandle        ProgressHandle that should be used to indicate the current progress
   * @param pSystemDetails Selected system details, containg the info about the git repo and url as well as the system id
   * @param pTarget        the location that the project should be cloned to
   * @param pCredentials   the cre
   */
  private static void writeConfigs(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pTarget, @NotNull DecodedJWT pCredentials)
  {
    getServerConfigContents(pHandle, pSystemDetails, pCredentials)
        .ifPresent(pServerConfigContents -> writeServerConfig(pHandle, pTarget, pServerConfigContents));

    getTunnelConfigContents(pHandle, pSystemDetails)
        .ifPresent(pTunnelConfigContents -> writeTunnelConfig(pHandle, pSystemDetails, pTarget, pCredentials, pTunnelConfigContents));
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

  public static void writeServerConfig(@NotNull ProgressHandle pHandle, @NotNull File pTargetDir, @NotNull String pServerConfigContents)
  {
    try
    {
      pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.write.serverConfig"));
      writeFileData(new File(pTargetDir, DEFAULT_SERVER_CONFIG_PATH), pServerConfigContents);
    }
    catch (IOException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.error.serverConfig",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
  }

  public static void writeTunnelConfig(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pTargetDir,
                                       @NotNull DecodedJWT currentCredentials, @NotNull String pTunnelConfigContents)
  {
    try
    {
      pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.write.tunnelConfig"));
      writeFileData(new File(pTargetDir, DEFAULT_TUNNEL_CONFIG_PATH), pTunnelConfigContents);
      _storeSSHPasswords(pSystemDetails, ISSPFacade.getInstance(), currentCredentials, pTunnelConfigContents);
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

  private static void _storeSSHPasswords(@NotNull ISSPSystemDetails pPSystemDetails, @NotNull ISSPFacade pSspFacade, @NotNull DecodedJWT pCurrentCredentials,
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
  private static boolean performGitClone(ProgressHandle pHandle, @NotNull String pRemotePath, @Nullable String pBranch, @Nullable String pTagName,
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
          NotificationDisplayer.getDefault().notify(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TITLE.SSPCheckoutExecutor.git.missing"),
                                                    IconManager.getInstance().getErrorIcon(),
                                                    SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.git.missing"), null);
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
      return gitSupport.performClone(pRemotePath, pTarget, options);
    }
    catch (Exception pException)
    {
      logger.log(Level.WARNING, pException, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.git.error",
                                                                                              ExceptionUtils.getStackTrace(pException)));
      return false;
    }
  }

  /**
   * Start an asnyc process that loads the IGitVersioningSupport (if not already loaded)
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

  private static String _tunnelToString(ISSHTunnel pTunnel)
  {
    return pTunnel.getLocalTarget() + ":" + pTunnel.getRemoteTargetPort() + ":" + pTunnel.getRemoteTarget() + "@" + pTunnel.getTunnelHost() + ":" + pTunnel.getPort();
  }

}
