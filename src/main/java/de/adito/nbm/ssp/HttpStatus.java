package de.adito.nbm.ssp;

/**
 * @author m.kaspera, 14.10.2020
 */
public interface HttpStatus
{

  /*
   * Information responses
   */

  /**
   * Indicates that the server has received the request headers and the client should proceed to send the request body
   */
  int CONTINUE = 100;
  /**
   * The requester asked to switch protocols and the server agrees to that switch
   */
  int SWITCHING_PROTOCOLS = 101;
  /**
   * Indicated that the server received the request and is processing it, but no response is available yet. Used to avoid a timeout
   */
  int PROCESSING = 102;
  /**
   * Used to return some response headers before final HTTP message
   */
  int EARLY_HINTS = 103;

  /*
   * Success Codes
   */

  /**
   * Standard response for successful requests
   */
  int OK = 200;
  /**
   * Request has been fulfilled and the resource has been created
   */
  int CREATED = 201;
  /**
   * Request was accepted for processing, but the processing is still ongoing
   */
  int ACCEPTED = 202;
  /**
   * The server successfully processed the request, but is not returning any content
   */
  int NO_CONTENT = 204;
  /**
   * The server successfully processed the request and asks the user to reset its document view
   */
  int RESET_CONTENT = 205;

  /*
   * Redirects
   */

  /**
   * Used if the client may choose from e.g. multiple different video formats
   */
  int MUTLIPLE_CHOICES = 300;
  /**
   * This and all future requests should be redirected to the given url
   */
  int MOVED_PERMANENTLY = 301;
  /**
   * Also known as "Moved temporarily"
   */
  int FOUND = 302;
  /**
   * The response to the request can be found under another url via GET
   */
  int SEE_OTHER = 303;
  /**
   * The requested resource is available only through a proxy, whose address is provided in the response
   */
  int USER_PROXY = 305;
  /**
   * Indicates that the request should be repeated with another url, but future requests should still use the current url
   */
  int TEMPORARY_REDIRECT = 307;
  /**
   * Like 301, but does not allow the HTTP method to change
   */
  int PERMANENT_REDIRECT = 308;

  /*
   * Client error codes
   */

  /**
   * The server cannot or will not process the request because the request contains an apparent client error
   */
  int BAD_REQUEST = 400;
  /**
   * Similar to 403, but specifically for when authentication is required but failed or has not yet been provided
   */
  int UNAUTHORIZED = 401;
  /**
   * The request contained valid data and was understood by the server, but the server will not process the request because the user does not have the
   * necessary permissions for the action or lacks permissions for a required resource
   */
  int FORBIDDEN = 403;
  /**
   * The requested resource could not be found, but may be available in the future
   */
  int NOT_FOUND = 404;
  /**
   * The server timed out waiting for the request
   */
  int REQUEST_TIMEOUT = 408;

  /*
   * Server error codes
   */

  /**
   * Generic error message, used when an unexpected condition occurred and no more specific message can be given
   */
  int INTERNAL_SERVER_ERROR = 500;
  /**
   * The server does not recognize the request method or lacks the ability to fulfill said request. Usually implies future availability of the request
   */
  int NOT_IMPLEMENTED = 501;
  /**
   * The server was acting as a gateway or proxy and received an invalid response from the upstream server
   */
  int BAD_GATEWAY = 502;
  /**
   * The server cannot presently service the request (overloaded, down for maintenance, ...)
   */
  int SERVICE_UNAVAILABLE = 503;
  /**
   * The server was acting as a gateway or proxy and did not receive a timely response from the upstream server
   */
  int GATEWAY_TIMEOUT = 504;
}
