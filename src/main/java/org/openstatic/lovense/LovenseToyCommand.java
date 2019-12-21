package org.openstatic.lovense;

import org.json.*;
import java.util.Map;

/**
 *  This class represents a command waiting to be issued or already issued.
 *  Each device has a thread dedicated to processing incoming commands.
 * 
 * **/

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
    
    /** increment the number of times this command has been attempted **/
    protected void addAttempt()
    {
        this.attempts++;
    }
    
    /** How many times has this command been attempted? **/
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
    
    /** get the associated toy's id **/
    public String getToyId()
    {
        return this.toy.getId();
    }
    
    /** return the toy object associated with this command **/
    public LovenseToy getToy()
    {
        return this.toy;
    }
    
    /** return the string command being passed to the LovenseConnectDevice **/
    public String getCommand()
    {
        return this.command;
    }
    
    /** retrive a Map<String, String> of the parameters being passed to the LovenseConnectDevice **/
    public Map<String, String> getParameters()
    {
        return this.params;
    }
    
    /** does this command pass any parameters **/
    public boolean hasParameters()
    {
        return this.params != null;
    }
    
    protected void setResponse(int code, JSONObject data)
    {
        if (code == 200)
        {
            String toyType = toy.getName();
            if ("Vibrate".equals(this.command) && this.params != null)
            {
                int vibrate_value = Integer.valueOf(this.params.get("v")).intValue();
                this.toy.output1_value = vibrate_value;
                if (toyType.equals("domi") || toyType.equals("hush") || toyType.equals("lush") || toyType.equals("ambi") || toyType.equals("osci") || toyType.equals("edge"))
                { 
                    this.toy.output2_value = vibrate_value;
                }
            } else if ("Vibrate1".equals(this.command) && this.params != null) {
                this.toy.output1_value = Integer.valueOf(this.params.get("v")).intValue();
            } else if ("Vibrate2".equals(this.command) && this.params != null) {
                this.toy.output2_value = Integer.valueOf(this.params.get("v")).intValue();
            } else if ("Rotate".equals(this.command) || "RotateAntiClockwise".equals(this.command) || "RotateClockwise".equals(this.command)) {
                if (this.params != null)
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
    
    //** is this command done retrying or succesfully completed? **/
    public boolean isCompleted()
    {
        return this.responseCode > 0 || (!this.shouldRetry());
    }
    
    /** did this command successfully complete? **/
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

    /** Compare if two LovenseToyCommand objects are identical **/
    public boolean equals(LovenseToyCommand ltc)
    {
        if (ltc != null)
        {
            if (this.command.equals(ltc.getCommand()) && this.getToyId().equalsIgnoreCase(ltc.getToyId()))
            {
                boolean param_match = false;
                if (this.params != null && ltc.getParameters() != null)
                {
                    if (this.params.equals(ltc.getParameters()))
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

    /** compare with another LovenseToyCommand to see if the toy and command being issued are the same **/
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
