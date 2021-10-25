package de.adito.nbm.ssp.checkout.filterby;

import de.adito.nbm.ssp.facade.ISSPSystemDetails;

import java.time.Instant;

public abstract class FilterBy
{
  protected final String pattern = ".*";

  public abstract boolean filter(String pToCompare, ISSPSystemDetails pSelected);

  public abstract boolean filterDate(int pDay, int pMonth, int pYear, ISSPSystemDetails pSelected);

  protected static Instant createInstant(int pDay, int pMonth, int pYear){
    String day = String.format("%02d", pDay);
    String month = String.format("%02d", pMonth);
    String toParse = pYear + "-" + month + "-" + day + "T00:00:00.00Z";
    return Instant.parse(toParse);
  }

  @Override
  abstract public String toString();
}
