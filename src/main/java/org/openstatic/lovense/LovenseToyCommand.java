package org.openstatic.lovense;

import org.json.*;
import java.util.Map;

public class LovenseToyCommand
{
    private LovenseToy toy;
    private String command;
    private Map<String, String> params;
    private int attempts;
    private int responseCode;
    
    public LovenseToyCommand(LovenseToy toy, String command, Map<String, String> params)
    {
        this.toy = toy;
        this.command = command;
        this.params = params;
        this.attempts = 0;
        this.responseCode = 0;
    }
    
    public void addAttempt()
    {
        this.attempts++;
    }
    
    public int getAttempts()
    {
        return this.attempts;
    }
    
    public boolean shouldRetry()
    {
        if ( !(this.responseCode >= 400 && this.responseCode <= 404) && this.attempts < 3 )
        {
            return true;
        } else {
            return false;
        }
    }
    
    public String getToyId()
    {
        return this.toy.getId();
    }
    
    public String getCommand()
    {
        return this.command;
    }
    
    public Map<String, String> getParameters()
    {
        return this.params;
    }
    
    public boolean hasParameters()
    {
        return this.params != null;
    }
    
    protected void setResponse(int code, JSONObject data)
    {
        if (code == 200 && this.params != null)
        {
            String toyType = toy.getName();
            if ("Vibrate".equals(this.command))
            {
                int vibrate_value = Integer.valueOf(this.params.get("v")).intValue();
                this.toy.output1_value = vibrate_value;
                if (toyType.equals("domi") || toyType.equals("hush") || toyType.equals("lush") || toyType.equals("ambi") || toyType.equals("osci") || toyType.equals("edge"))
                { 
                    this.toy.output2_value = vibrate_value;
                }
            } else if ("Vibrate1".equals(this.command)) {
                this.toy.output1_value = Integer.valueOf(this.params.get("v")).intValue();
            } else if ("Vibrate2".equals(this.command)) {
                this.toy.output2_value = Integer.valueOf(this.params.get("v")).intValue();
            } else if ("Rotate".equals(this.command) || "RotateAntiClockwise".equals(this.command) || "RotateClockwise".equals(this.command)) {
                this.toy.output2_value = Integer.valueOf(this.params.get("v")).intValue();
            } else if ("Battery".equals(this.command)) {
                this.toy.battery = data.optInt("data",0);
            }
            Thread t = new Thread(() -> {
                this.toy.fireToyUpdated();
            });
            t.start();
        }
        this.responseCode = code;
    }
    
    public boolean isCompleted()
    {
        return this.responseCode > 0 || (!this.shouldRetry());
    }
    
    public boolean isSuccess()
    {
        return this.responseCode == 200;
    }
    
    public int getResponseCode()
    {
        return this.responseCode;
        /*
        if ("400".equals(code))
            throw new LovenseException("Invalid Command", url, response);
        if ("401".equals(code))
            throw new LovenseException("Toy Not Found", url, response);
        if ("402".equals(code))
            throw new LovenseException("Toy Not Connected", url, response);
        if ("403".equals(code))
            throw new LovenseException("Toy Doesn't support this command", url, response);
        if ("404".equals(code))
            throw new LovenseException("Invalid Parameter on toy command", url, response);
        return response;
        */
    }

    public boolean equals(LovenseToyCommand ltc)
    {
        if (ltc != null)
        {
            if (this.command.equals(ltc.getCommand()) && this.getToyId().equalsIgnoreCase(ltc.getToyId()))
            {
                boolean param_match = false;
                if (this.params != null)
                {
                    if (this.params.equals(ltc.getParameters()))
                        param_match = true;
                } else if (ltc.getParameters() == null) {
                    param_match = true;
                }
                return param_match;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean commandEquals(LovenseToyCommand ltc)
    {
        if (ltc != null)
        {
            if (this.command.equals(ltc.getCommand()) && this.getToyId().equalsIgnoreCase(ltc.getToyId()))
            {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public String toString()
    {
        return this.toy.toString() + " - " + this.command + LovenseConnect.mapToQuery(this.params);
    }
}
