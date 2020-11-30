package de.adito.nbm.ssp.actions;

import de.adito.nbm.runconfig.api.*;
import org.jetbrains.annotations.Nullable;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 * @author m.kaspera, 26.10.2020
 */
public interface IContextMenuAction
{

  @Nullable
  default ISystemInfo getSystemInfoFromNodes(Node[] pActivatedNodes)
  {
    return Lookup.getDefault().lookup(INodeSystemInfoProvider.class).getSystemInfoFromNodes(pActivatedNodes);
  }

}
