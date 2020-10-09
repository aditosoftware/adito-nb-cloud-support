package de.adito.nbm.ssp.exceptions;

/**
 * Excption that is raised if the returned status code of a call to the SSP API indicates a server error.
 *
 * @author m.kaspera, 09.10.2020
 */
public class AditoSSPServerException extends AditoSSPException
{

  public AditoSSPServerException()
  {
    super(500);
  }

  public AditoSSPServerException(String message)
  {
    super(message, 500);
  }

  public AditoSSPServerException(String message, Throwable cause)
  {
    super(message, cause, 500);
  }

  public AditoSSPServerException(Throwable cause)
  {
    super(cause, 500);
  }

  public AditoSSPServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace, 500);
  }
}
