package de.adito.nbm.ssp.exceptions;

import org.json.*;

/**
 * Exception that is thrown if an error occurs while parsing a JSON. Includes the offending JSON in the message
 *
 * @author m.kaspera, 17.02.2021
 */
public class AditoSSPParseException extends Exception
{
  public AditoSSPParseException(JSONException pJSONException, JSONArray pJSONArray)
  {
    super("Error while parsing JSON:\n" + pJSONArray.toString(), pJSONException);
  }

}
