package de.adito.nbm.ssp.checkout.filterby;

import de.adito.nbm.ssp.facade.ISSPSystemDetails;

import java.time.Instant;

/**
 * Filters the clist objects by their date.
 * Shows systems, that are created before a certain date.
 *
 * @author p.rosenthal , b.huf
 */
public class FilterByBeforeDate extends FilterBy
{
  public final static String FILTERNAME = "Created Before";

  @Override
  public boolean filter(String pToCompare, ISSPSystemDetails pSelected)
  {
    return false;
  }

  @Override
  public boolean filterDate(int pDay, int pMonth, int pYear, ISSPSystemDetails pSelected)
  {
    return pSelected.getCreationDate().isBefore(createInstant(pDay, pMonth, pYear));
  }

  @Override
  public String toString(){
    return FILTERNAME;
  }
}
