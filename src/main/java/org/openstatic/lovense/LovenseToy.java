package org.openstatic.lovense;

import org.json.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.Enumeration;

public class LovenseToy
{
    private LovenseConnectDevice device;
    private String toyId;
    protected String name;
    protected String nickname;
    protected int output1_value;
    protected int output2_value;
    protected int battery;
    protected boolean connected;
    private Vector<LovenseToyListener> listeners;


    protected LovenseToy(LovenseConnectDevice d, String toy_id)
    {
        this.listeners = new Vector<LovenseToyListener>();
        this.toyId = toy_id;
        this.device = d;
        this.connected = false;
        this.battery = 0;
    }

    /** get the associated LovenseConnectDevice this toy is connected to **/
    public LovenseConnectDevice getDevice()
    {
        return this.device;
    }
    
    /** adds a listener for toy state change events **/
    public void addLovenseToyListener(LovenseToyListener ltl)
    {
        if (!this.listeners.contains(ltl))
        {
            this.listeners.add(ltl);
        }
    }

    public void removeLovenseToyListener(LovenseToyListener ltl)
    {
        if (this.listeners.contains(ltl))
        {
            this.listeners.remove(ltl);
        }
    }
    
    protected void fireToyUpdated()
    {
        if (LovenseConnect.debug)
            System.err.println("FireEVENT toyUpdated - " + this.toString());
        for (Enumeration<LovenseToyListener> ltle = ((Vector<LovenseToyListener>) this.listeners.clone()).elements(); ltle.hasMoreElements();)
        {
            try
            {
                LovenseToyListener ltl = ltle.nextElement();
                ltl.toyUpdated(this);
            } catch (Exception mlex) {
                mlex.printStackTrace(System.err);
            }
        }
    }
    
    public int getOutputOneValue()
    {
        return this.output1_value;
    }
    
    public int getOutputTwoValue()
    {
        return this.output2_value;
    }

    public String getId()
    {
        return this.toyId;
    }

    public String getName()
    {
        return this.name;
    }

    public String getNickname()
    {
        if (this.nickname == null)
            return this.getName();
        else
            return nickname;
    }

    /** Is the toy currently connected? **/
    public boolean isConnected()
    {
        return this.connected;
    }

    /** Retrieves the last know battery level 0-100 **/
    public int getBattery()
    {
        return this.battery;
    }

    /** Issues the vibrate command to the toy **/
    public LovenseToyCommand vibrate(int amount)
    {
        return this.device.command(this, "Vibrate", amount);
    }

    /** Issues the vibrate1 command to the toy, used with edge **/
    public LovenseToyCommand vibrate1(int amount)
    {
        return this.device.command(this, "Vibrate1", amount);
    }

    /** Issues the vibrate2 command to the toy, used with edge **/
    public LovenseToyCommand vibrate2(int amount)
    {
        return this.device.command(this, "Vibrate2", amount);
    }

    /** Issues the rotate command to the toy **/
    public LovenseToyCommand rotate(int amount)
    {
        return this.device.command(this, "Rotate", amount);
    }

    public LovenseToyCommand rotateAntiClockwise(int amount)
    {
        return this.device.command(this, "RotateAntiClockwise", amount);
    }

    public LovenseToyCommand rotateClockwise(int amount)
    {
        return this.device.command(this, "RotateClockwise", amount);
    }

    public LovenseToyCommand rotateChange()
    {
        return this.device.command(this, "RotateChange");
    }
    
    public LovenseToyCommand refreshBattery()
    {
        return this.device.command(this, "Battery");
    }

    public LovenseToyCommand airAuto(int amount)
    {
        return this.device.command(this, "AirAuto", amount);
    }

    public LovenseToyCommand airIn()
    {
        return this.device.command(this, "AirIn");
    }

    public LovenseToyCommand airOut()
    {
        return this.device.command(this, "AirOut");
    }

    public LovenseToyCommand preset(int presetId)
    {
        return this.device.command(this, "Preset", presetId);
    }

    /** Return a string representation of this toy **/
    public String toString()
    {
        if (this.nickname == null)
            return this.getName();
        else
            return this.nickname + " (" + this.getName() + ")";
    }
}
