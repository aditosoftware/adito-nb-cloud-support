package de.adito.nbm.ssp.checkout.filterby;

import de.adito.nbm.ssp.facade.ISSPSystemDetails;

import java.time.Instant;
/**
 * Filter for the clistobjects.
 *
 * @author p.rosenthal, b.huf, 25.10.2021
 */

public abstract class FilterBy
{
  protected final String pattern = ".*";

  /**
   * Filters the selected SystemDetails by the pToCompare String.
   * @param pToCompare
   * @param pSelected
   * @return <<true>> if systemdetail matches the given string
   */
  public abstract boolean filter(String pToCompare, ISSPSystemDetails pSelected);

  /**
   * Filters the Systemdetail by a given date (Created after or created before).
   * @param pDay
   * @param pMonth
   * @param pYear
   * @param pSelected
   * @return <<true>> if the Systemdetail matches the filter
   */
  public abstract boolean filterDate(int pDay, int pMonth, int pYear, ISSPSystemDetails pSelected);

  /**
   * Creates an Instant with the given day, month and year
   * @param pDay given day.
   * @param pMonth given month.
   * @param pYear given year.
   * @return Instant.
   */
  protected static Instant createInstant(int pDay, int pMonth, int pYear){
    String day = String.format("%02d", pDay);
    String month = String.format("%02d", pMonth);
    String toParse = pYear + "-" + month + "-" + day + "T00:00:00.00Z";
    return Instant.parse(toParse);
  }

  @Override
  abstract public String toString();
}
