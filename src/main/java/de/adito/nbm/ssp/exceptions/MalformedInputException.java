package de.adito.nbm.ssp.exceptions;

/**
 * This exception is raised if the input or the return value from e.g. the SSP API does not match the expected format and the data can thus not be read correctly
 *
 * @author m.kaspera, 09.10.2020
 */
public class MalformedInputException extends Exception
{

  public MalformedInputException()
  {
  }

  public MalformedInputException(String message)
  {
    super(message);
  }

  public MalformedInputException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public MalformedInputException(Throwable cause)
  {
    super(cause);
  }

  public MalformedInputException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
