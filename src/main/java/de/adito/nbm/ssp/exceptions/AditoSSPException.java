package de.adito.nbm.ssp.exceptions;

/**
 * Excption that is raised if the returned status code of a call to the SSP API indicates an error.
 * The error itself and the statusCode can be passed to this exception and analysed later on
 *
 * @author m.kaspera, 09.10.2020
 */
public class AditoSSPException extends Exception
{

  private final int statusCode;

  public AditoSSPException(int pStatusCode)
  {
    super();
    statusCode = pStatusCode;
  }

  public AditoSSPException(String message, int pStatusCode)
  {
    super(message);
    statusCode = pStatusCode;
  }

  public AditoSSPException(String message, Throwable cause, int pStatusCode)
  {
    super(message, cause);
    statusCode = pStatusCode;
  }

  public AditoSSPException(Throwable cause, int pStatusCode)
  {
    super(cause);
    statusCode = pStatusCode;
  }

  protected AditoSSPException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int pStatusCode)
  {
    super(message, cause, enableSuppression, writableStackTrace);
    statusCode = pStatusCode;
  }

  public int getStatusCode()
  {
    return statusCode;
  }
}
