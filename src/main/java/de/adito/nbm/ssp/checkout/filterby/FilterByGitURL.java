package de.adito.nbm.ssp.checkout.filterby;

import de.adito.nbm.ssp.facade.ISSPSystemDetails;

/**
 * Filters the clist objects by their git-URL.
 *
 * @author p.rosenthal , b.huf
 */
public class FilterByGitURL extends FilterBy
{
  public final static String FILTERNAME = "Git-URL";

  @Override
  public boolean filter(String pToCompare, ISSPSystemDetails pSelected)
  {
    String fullPattern = pattern + pToCompare + pattern;
    return  pSelected.getGitRepoUrl().matches(fullPattern);
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
