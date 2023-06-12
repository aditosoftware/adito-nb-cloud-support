package de.adito.nbm.cloud.runconfig;

import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.nbm.runconfig.api.*;
import de.adito.nbm.runconfig.category.AditoClientRunConfigCategory;
import de.adito.nbm.runconfig.spi.IActiveConfigComponentProvider;
import de.adito.observables.netbeans.*;
import de.adito.swing.icon.IconAttributes;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.*;
import org.openide.util.Lookup;

import java.awt.*;
import java.net.URI;
import java.util.Optional;

/**
 * @author m.kaspera, 22.07.2020
 */
public class WebClientRunConfig implements IRunConfig
{

  private final ISystemInfo systemInfo;
  private final IVaadinIconsProvider iconsProvider;

  public WebClientRunConfig(ISystemInfo pSystemInfo)
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
    systemInfo = pSystemInfo;
  }

  @NonNull
  @Override
  public Observable<Optional<IRunConfigCategory>> category()
  {
    return Observable.just(Optional.of(new AditoClientRunConfigCategory()));
  }

  @NonNull
  @Override
  public Observable<Optional<Image>> icon()
  {
    return Observable.just(Optional.ofNullable(iconsProvider)
                        .map(pIconProvider -> pIconProvider.findImage(IVaadinIconsProvider.VaadinIcon.BROWSER, new IconAttributes.Builder().create())));
  }

  @NonNull
  @Override
  public Observable<String> displayName()
  {
    return OpenProjectsObservable.create()
        .switchMap(pProjects -> {
          if (pProjects.size() > 1) // Nur wenn mind. 2 Projekte offen wird das Projekt angezeigt, sonst sinnlos
          {
            Project myProject = systemInfo.getProject();
            if (myProject != null)
            {
              return ProjectObservable.createInfos(myProject)
                  .map(ProjectInformation::getDisplayName)
                  .map(pName -> " (" + pName + ")");
            }
          }
          return Observable.just("");
        })
        .to(pProjectNameObs -> Observable.combineLatest(pProjectNameObs, systemInfo.getSystemName(), (pProjName, pDataObjName) ->
            "Web Client (Neon) Cloud" + IActiveConfigComponentProvider.DISPLAY_NAME_SEPARATOR + pDataObjName + pProjName));
  }

  @Override
  public void executeAsnyc(@NonNull ProgressHandle pProgressHandle) throws Exception
  {
    Desktop.getDesktop().browse(new URI(systemInfo.getParameters().get(ISystemInfo.WEBCLIENT_URL_KEY)));
  }
}
