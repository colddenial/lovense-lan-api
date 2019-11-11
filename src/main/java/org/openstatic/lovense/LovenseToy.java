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

    public LovenseConnectDevice getDevice()
    {
        return this.device;
    }
    
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

    public boolean isConnected()
    {
        return this.connected;
    }

    public int getBattery()
    {
        return this.battery;
    }

    public LovenseToyCommand vibrate(int amount)
    {
        return this.device.command(this, "Vibrate", amount);
    }

    public LovenseToyCommand vibrate1(int amount)
    {
        return this.device.command(this, "Vibrate1", amount);
    }

    public LovenseToyCommand vibrate2(int amount)
    {
        return this.device.command(this, "Vibrate2", amount);
    }

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

    public String toString()
    {
        if (this.nickname == null)
            return this.getName();
        else
            return this.nickname + " (" + this.getName() + ")";
    }
}
