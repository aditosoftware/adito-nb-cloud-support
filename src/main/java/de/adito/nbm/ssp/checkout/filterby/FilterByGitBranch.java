package de.adito.nbm.ssp.checkout.filterby;

import de.adito.nbm.ssp.facade.ISSPSystemDetails;

public class FilterByGitBranch extends FilterBy
{
  public final static String FILTERNAME = "Git-Branch";

  @Override
  public boolean filter(String pToCompare, ISSPSystemDetails pSelected)
  {
    String fullPattern = pattern + pToCompare + pattern;
    return pSelected.getGitBranch().matches(fullPattern);
  }

  @Override
  public boolean filterDate(int pDay, int pMonth, int pYear, ISSPSystemDetails pSelected)
  {
    return false;
  }

  @Override
  public String toString(){
    return FILTERNAME;
  }
}
