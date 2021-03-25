package de.adito.nbm.ssp.checkout;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.IGitVersioningSupport;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.tunnel.*;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Performs the git checkout and writes the config files
 *
 * @author m.kaspera, 02.11.2020
 */
public class SSPCheckoutExecutor
{

  private static final Logger logger = Logger.getLogger(SSPCheckoutExecutor.class.getName());
  private static IGitVersioningSupport gitSupport;
  private static Future<?> future = null;
  private static boolean isLoading = false;

  /**
   * @param pHandle        ProgressHandle that should be used to indicate the current progress
   * @param pSystemDetails Selected system details, containg the info about the git repo and url as well as the system id
   * @param pTarget        the location that the project should be cloned to
   * @return true if the clone was performed successfully
   */
  static FileObject execute(@NotNull ProgressHandle pHandle, @NotNull ISSPSystemDetails pSystemDetails, @NotNull File pTarget)
  {
    try
    {
      pHandle.start();
      boolean cloneSuccess = performGitClone(pHandle, pSystemDetails.getGitRepoUrl(), pSystemDetails.getGitBranch(), null, "origin", pTarget);
      if (cloneSuccess)
        writeConfigs(pHandle, pSystemDetails, pTarget);
      else
        NotificationDisplayer.getDefault().notify(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TITLE.SSPCheckoutExecutor.execute.clone"),
                                                  NotificationDisplayer.Priority.HIGH.getIcon(),
                                                  SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.execute.clone.error"),
                                                  null, NotificationDisplayer.Priority.HIGH);
    }
    finally
    {
      pHandle.finish();
    }
    return FileUtil.toFileObject(pTarget);
  }

  /**
   * @param pHandle        ProgressHandle that should be used to indicate the current progress
   * @param pSystemDetails Selected system details, containg the info about the git repo and url as well as the system id
   * @param pTarget        the location that the project should be cloned to
   */
  private static void writeConfigs(ProgressHandle pHandle, ISSPSystemDetails pSystemDetails, File pTarget)
  {
    ISSPFacade sspFacade = ISSPFacade.getInstance();
    DecodedJWT currentCredentials = UserCredentialsManager.getCredentials();
    pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.serverConfig"));
    try
    {
      String serverConfigContents = sspFacade.getServerConfig(currentCredentials.getSubject(), currentCredentials, pSystemDetails);
      FileObject fileObject = FileUtil.createData(new File(pTarget, "data/config/serverconfig_default.xml"));
      pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.write.serverConfig"));
      fileObject.getOutputStream().write(serverConfigContents.getBytes(StandardCharsets.UTF_8));
    }
    catch (UnirestException | AditoSSPException | IOException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.error.serverConfig",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
    pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.fetch.tunnelConfig"));
    try
    {
      String tunnelConfigContents = sspFacade.getTunnelConfig(currentCredentials.getSubject(), currentCredentials, pSystemDetails);
      FileObject fileObject = FileUtil.createData(new File(pTarget, "data/config/tunnelConfig.xml"));
      pHandle.progress(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.write.tunnelConfig"));
      fileObject.getOutputStream().write(tunnelConfigContents.getBytes(StandardCharsets.UTF_8));
      _storeSSHPasswords(pSystemDetails, sspFacade, currentCredentials, tunnelConfigContents);
    }
    catch (UnirestException | AditoSSPException | IOException | TransformerException pE)
    {
      logger.log(Level.WARNING, pE, () -> SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutExecutor.class, "TXT.SSPCheckoutExecutor.update.error.tunnelConfig",
                                                                                      ExceptionUtils.getStackTrace(pE)));
    }
    NbPreferences.forModule(ISystemInfo.class).put(ISystemInfo.CLOUD_ID_PREF_KEY_PEFIX + "default." + pTarget.getPath().replace("\\", "/"), pSystemDetails.getSystemdId());
    NbPreferences.forModule(ISystemInfo.class).put("serverAddressDefault." + pTarget.getPath().replace("\\", "/"), pSystemDetails.getUrl());
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

}
