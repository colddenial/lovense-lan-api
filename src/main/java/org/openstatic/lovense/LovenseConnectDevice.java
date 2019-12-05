package org.openstatic.lovense;

import org.json.*;

import java.net.InetAddress;

import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LovenseConnectDevice implements Runnable
{
    private JSONObject toysJson;
    private Hashtable<String, LovenseToy> toys;
    private String hostname;
    private String ipAddress;
    private int httpPort;
    private Thread hostLookupThread;
    private Thread commandHandler;
    private boolean keep_running;
    private boolean manualDevice;
    private String appVersion;
    private String deviceId;
    private LinkedBlockingQueue<LovenseToyCommand> commandQueue;

    /** This constructor should be ignored for the most part, accounts should
     * always be fetched by the BSDClient class*/
    protected LovenseConnectDevice(JSONObject j)
    {
        this.keep_running = true;
        this.manualDevice = false;
        this.commandQueue = new LinkedBlockingQueue<LovenseToyCommand>();
        this.commandHandler = new Thread(this);
        this.commandHandler.start();
        this.toys = new Hashtable<String, LovenseToy>();
        this.appVersion = j.optString("appVersion", "unknown");
        this.ipAddress = j.optString("domain", "").replaceAll(".lovense.club", "").replaceAll("-",".");
        this.httpPort = j.optInt("httpsPort", 0);
        this.deviceId = j.optString("deviceId", (this.ipAddress + ":" + String.valueOf(this.httpPort)));
        hostnameLookup();
        if (j.has("toys"))
        {
            this.updateToysJSON(j.getJSONObject("toys"));
        } else {
            this.updateToysJSON(new JSONObject());
        }
    }
    
    protected LovenseConnectDevice(String ip, int port)
    {
        this.keep_running = true;
        this.manualDevice = true;
        this.commandQueue = new LinkedBlockingQueue<LovenseToyCommand>();
        this.commandHandler = new Thread(this);
        this.commandHandler.start();
        this.toys = new Hashtable<String, LovenseToy>();
        this.toysJson = new JSONObject();
        this.appVersion = "unknown";
        this.deviceId = ip + ":" + String.valueOf(port);
        this.ipAddress = ip;
        this.httpPort = port;
        hostnameLookup();
        this.refresh();
    }
    
    protected boolean isManualDevice()
    {
        return this.manualDevice;
    }
    
    private void hostnameLookup()
    {
        if (this.hostname == null)
        {
            this.hostname = this.getIPAddress();
            if (this.hostLookupThread == null)
            {
                this.hostLookupThread = new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            String ip = LovenseConnectDevice.this.getIPAddress();
                            //System.err.println("Looking up Hostname... " + ip);
                            InetAddress addr = InetAddress.getByName(ip);
                            LovenseConnectDevice.this.hostname = addr.getHostName();
                            LovenseConnectDevice.this.hostLookupThread = null;
                            //System.err.println("Finished hostname lookup " + ip);
                        } catch (Exception e) {
                        }
                    }
                };
                this.hostLookupThread.start();
            }
        }
    }
    
    protected void fireUpdateOnAllToys()
    {
        Thread t = new Thread(() -> {
            Iterator<LovenseToy> toyi = getToys().iterator();
            while(toyi.hasNext())
            {
                LovenseToy toy = toyi.next();
                toy.fireToyUpdated();
            }
        });
        t.start();
    }

    public void refresh()
    {
        try
        {
            String url = this.getHTTPPath() + "GetToys";
            JSONObject response = LovenseConnect.apiCall(url, null);
            int response_code = 0;
            if (response != null)
            {
                response_code = Integer.valueOf(response.optString("code", "0")).intValue();
                if (response_code == 200 && response.has("data"))
                {
                    JSONObject data = response.getJSONObject("data");
                    updateToysJSON(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected void updateToysJSON(JSONObject j)
    {
        this.toysJson = j;
        fireUpdateOnAllToys();
        Iterator<String> keys = this.toysJson.keys();

        // Add any new toys, found and update existing ones
        while(keys.hasNext())
        {
            String key = keys.next();
            JSONObject toyObj = this.toysJson.getJSONObject(key);
            LovenseToy lt = null;
            if (!this.toys.containsKey(key))
            {
                // toy doesn't exist and needs to be created
                lt = new LovenseToy(this, key);
                this.toys.put(key, lt);
            } else {
                // find the existing toy
                lt = this.toys.get(key);
            }
            if (toyObj.has("name"))
                lt.name = toyObj.getString("name").toLowerCase();
            if (toyObj.has("nickName"))
            {
                String nn = toyObj.getString("nickName");
                if (nn.equals(""))
                    lt.nickname = null;
                else
                    lt.nickname = nn;
            }
            lt.connected = (toyObj.optInt("status", 0) == 1);
            if (toyObj.has("battery"))
            {
                lt.battery = toyObj.optInt("battery", 0);
            } else {
                this.command(lt, "Battery");
            }
        }

        // Remove any toys that have been disconnected
        Iterator<String> keyset = this.toys.keySet().iterator();
        while(keyset.hasNext())
        {
            String key = keyset.next();
            if (!this.toysJson.has(key))
            {
                this.toys.remove(key);
            }
        }
    }

    public String getDeviceId()
    {
        return this.deviceId;
    }

    public String getAppVersion()
    {
        return this.appVersion;
    }

    public String getName()
    {
        return this.hostname;
    }

    public int getHTTPPort()
    {
        return this.httpPort;
    }

    public String getIPAddress()
    {
        return this.ipAddress;
    }

    public String getHostname()
    {
        return this.hostname;
    }

    private String getHTTPPath()
    {
        return "https://" + this.getIPAddress() + ":" + String.valueOf(this.getHTTPPort()) + "/";
    }

    protected JSONObject getToyJSON(String toyId)
    {
        return this.toysJson.getJSONObject(toyId);
    }

    protected LovenseToyCommand command(LovenseToy toy, String command)
    {
        LovenseToyCommand ltc = new LovenseToyCommand(toy, command, null);
        this.queueCommand(ltc);
        return ltc;
    }

    protected LovenseToyCommand command(LovenseToy toy, String command, int v)
    {
        Hashtable<String, String> params = new Hashtable<String, String>();
        params.put("v", String.valueOf(v));
        LovenseToyCommand ltc = new LovenseToyCommand(toy, command, params);
        this.queueCommand(ltc);
        return ltc;
    }
    
    protected synchronized void queueCommand(LovenseToyCommand ltc)
    {
        if (ltc.getToy().isConnected())
        {
            try
            {
                this.commandQueue.removeIf(n -> (ltc.commandEquals(n)));
                this.commandQueue.put(ltc);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        } else {
            if (LovenseConnect.debug)
            {
                System.err.println("Command Ignored: " + ltc.toString() + " Toy not connected!");
            }
        }
    }

    public void run()
    {
        LovenseToyCommand lastCommand = null;
        while(this.keep_running)
        {
            try
            {
                LovenseToyCommand ltc = this.commandQueue.poll(1, TimeUnit.SECONDS);
                if (ltc != null)
                {
                    if (!ltc.equals(lastCommand))
                    {
                        int code = 0;
                        try
                        {
                            code = execCommand(ltc);
                        } catch (Exception e) {
                            
                        }
                        if (code != 200 && ltc.shouldRetry())
                        {
                            this.commandQueue.put(ltc);
                            if (LovenseConnect.debug)
                            {
                                System.err.println("Retry Command: " + ltc.toString() + " Code:" + String.valueOf(code));
                            }
                        }
                        if (ltc.isSuccess())
                            lastCommand = ltc;
                    } else {
                        if (LovenseConnect.debug)
                        {
                            System.err.println("Ignore Duplicate Command: " + ltc.toString());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    protected int execCommand(LovenseToyCommand ltc) throws LovenseException
    {
        Hashtable<String, String> final_params = new Hashtable<String, String>();
        if (ltc.hasParameters())
        {
            final_params.putAll(ltc.getParameters());
        }
        final_params.put("t", ltc.getToyId());
        String url = this.getHTTPPath() + ltc.getCommand();
        ltc.addAttempt();
        JSONObject response = LovenseConnect.apiCall(url, final_params);
        int response_code = 0;
        if (response != null)
        {
            response_code = Integer.valueOf(response.optString("code", "0")).intValue();
            ltc.setResponse(response_code, response);
        }
        return response_code;
    }

    public Collection<LovenseToy> getToys()
    {
        return this.toys.values();
    }
}
