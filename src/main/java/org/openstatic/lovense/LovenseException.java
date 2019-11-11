package org.openstatic.lovense;

import org.json.JSONObject;

/** Exception Class for API Errors **/
public class LovenseException extends Exception
{
    String apiPath;
    JSONObject data;

    /** Should only be constructed by classes in net.bsdtelecom **/
    protected LovenseException(String error, String apiPath, JSONObject data)
    {
      super(error);
      this.apiPath = apiPath;
      this.data = data;
    }

    protected LovenseException(String error)
    {
      super(error);
      this.apiPath = "?";
      this.data = new JSONObject();
    }

    /** The api path that was attempted **/
    public String getApiPath()
    {
        return this.apiPath;
    }

    /** Get the error object returned by the server **/
    public JSONObject getReturnObject()
    {
        return this.data;
    }
}
