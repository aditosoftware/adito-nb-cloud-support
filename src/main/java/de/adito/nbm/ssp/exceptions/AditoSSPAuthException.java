package de.adito.nbm.ssp.exceptions;

/**
 * @author m.kaspera, 22.11.2020
 */
public class AditoSSPAuthException extends AditoSSPException
{
  public AditoSSPAuthException(int pStatusCode)
  {
    super(pStatusCode);
  }

  public AditoSSPAuthException(String message, int pStatusCode)
  {
    super(message, pStatusCode);
  }

  public AditoSSPAuthException(String message, Throwable cause, int pStatusCode)
  {
    super(message, cause, pStatusCode);
  }

  public AditoSSPAuthException(Throwable cause, int pStatusCode)
  {
    super(cause, pStatusCode);
  }

  protected AditoSSPAuthException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int pStatusCode)
  {
    super(message, cause, enableSuppression, writableStackTrace, pStatusCode);
  }
}
