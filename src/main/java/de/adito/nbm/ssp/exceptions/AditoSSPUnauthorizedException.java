package de.adito.nbm.ssp.exceptions;

/**
 * Excption that is raised if the returned status code of a call to the SSP API indicates that the user is not logged in or unauthorized for the action that was tried.
 *
 * @author m.kaspera, 09.10.2020
 */
public class AditoSSPUnauthorizedException extends AditoSSPException
{
  public AditoSSPUnauthorizedException()
  {
    super(401);
  }

  public AditoSSPUnauthorizedException(String message)
  {
    super(message, 401);
  }

  public AditoSSPUnauthorizedException(String message, Throwable cause)
  {
    super(message, cause, 401);
  }

  public AditoSSPUnauthorizedException(Throwable cause)
  {
    super(cause, 401);
  }

  protected AditoSSPUnauthorizedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace, 401);
  }
}
