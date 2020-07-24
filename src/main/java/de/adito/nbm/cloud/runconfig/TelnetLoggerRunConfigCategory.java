package de.adito.nbm.cloud.runconfig;

import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.nbm.runconfig.api.IRunConfigCategory;
import de.adito.swing.icon.IconAttributes;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;
import org.openide.util.Lookup;

import java.awt.*;
import java.util.Optional;

/**
 * @author m.kaspera, 22.07.2020
 */
public class TelnetLoggerRunConfigCategory implements IRunConfigCategory
{

  private final IVaadinIconsProvider iconsProvider;

  public TelnetLoggerRunConfigCategory()
  {
    iconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
  }

  @NotNull
  @Override
  public String getName()
  {
    return "Servers";
  }

  @NotNull
  @Override
  public Observable<String> title()
  {
    return Observable.just("Servers");
  }

  @NotNull
  @Override
  public Observable<Optional<Image>> icon()
  {
    return Observable.just(Optional.ofNullable(iconsProvider)
                               .map(pIconProvider -> pIconProvider.findImage(IVaadinIconsProvider.VaadinIcon.CLOUD, new IconAttributes.Builder().create())));
  }

}
