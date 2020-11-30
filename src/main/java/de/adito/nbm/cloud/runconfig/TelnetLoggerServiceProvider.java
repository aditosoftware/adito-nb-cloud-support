package de.adito.nbm.cloud.runconfig;

import de.adito.nbm.runconfig.api.*;
import de.adito.util.reactive.ObservableCollectors;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.util.Pair;
import org.openide.util.lookup.ServiceProvider;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 13.07.2020
 */
@ServiceProvider(service = ISystemRunConfigProvider.class)
public class TelnetLoggerServiceProvider implements ISystemRunConfigProvider
{
  @NotNull
  @Override
  public Observable<List<IRunConfig>> runConfigurations(List<ISystemInfo> pSystemInfos)
  {
    return pSystemInfos.stream()
        .map(pInfo -> pInfo.getCloudId()
            // combine info about whether the system is a cloud system with the system itself
            .map(pIsCloud -> Pair.of(pInfo, !pIsCloud.isEmpty())))
        .collect(ObservableCollectors.combineToList())
        .map(pList -> pList.stream()
            // throw out all non-cloud systems
            .filter(Pair::second)
            .map(Pair::first)
            // create a new TelnetLoggerRunConfig for each cloud system
            .map(pSystemInfo -> List.of(new TelnetLoggerRunConfig(pSystemInfo), new WebClientRunConfig(pSystemInfo)))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
  }

  @Override
  public ISystemRunConfigProvider getInstance(Project pProject)
  {
    return this;
  }
}
