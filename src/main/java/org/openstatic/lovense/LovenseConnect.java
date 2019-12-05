package org.openstatic.lovense;

import org.json.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;

public class LovenseConnect
{
    protected static boolean debug = false;
    private static String VERSION = "";

    private static LinkedHashMap<String, LovenseConnectDevice> devices = new LinkedHashMap<String, LovenseConnectDevice>();
    private static Vector<LovenseToy> toys = new Vector<LovenseToy>();

    private static long lastToyFetch = 0;
    private static Vector<LovenseConnectListener> listeners = new Vector<LovenseConnectListener>();
    private static Thread refreshThread;

    public static void addLovenseConnectListener(LovenseConnectListener lcl)
    {
        if (!LovenseConnect.listeners.contains(lcl))
        {
            LovenseConnect.listeners.add(lcl);
        }
    }

    public static void removeLovenseConnectListener(LovenseConnectListener lcl)
    {
        if (LovenseConnect.listeners.contains(lcl))
        {
            LovenseConnect.listeners.remove(lcl);
        }
    }

    public static Collection<LovenseConnectDevice> getDevices() throws LovenseException
    {
        refreshIfNeeded();
        return LovenseConnect.devices.values();
    }

    public static void refreshIfNeeded()
    {
        if ( ((System.currentTimeMillis() - LovenseConnect.lastToyFetch) > 20000) && LovenseConnect.refreshThread == null)
        {
            LovenseConnect.refreshThread = new Thread()
            {
                public void run()
                {
                    try
                    {
                        LovenseConnect.refresh();
                    } catch (Exception e) {

                    }
                    LovenseConnect.refreshThread = null;
                }
            };
            LovenseConnect.refreshThread.start();
        }
    }
    
    public static void addDeviceManually(String ip, int port)
    {
        String deviceId = ip + ":" + String.valueOf(port);
        if (!LovenseConnect.devices.containsKey(deviceId))
        {
            LovenseConnectDevice ld = new LovenseConnectDevice(ip, port);
            if (LovenseConnect.debug)
            {
                System.err.println("Adding Device Manually: " + ld.getDeviceId());
            }
            LovenseConnect.devices.put(deviceId, ld);
        }
    }

    /** Contact lovense servers to look for local devices **/
    private static void deviceSearch()
    {
        try
        {
            JSONObject resp = LovenseConnect.apiCall("https://api.lovense.com/api/lan/getToys");
            Iterator<String> keys = resp.keys();
            while(keys.hasNext())
            {
                String key = keys.next();
                if (LovenseConnect.devices.containsKey(key))
                {
                    
                    // commented out to focus on directly asking device
                    /*

                    LovenseConnectDevice ld = LovenseConnect.devices.get(key);
                    JSONObject dev_json = resp.getJSONObject(key);
                    if (dev_json.has("toys"))
                    {
                        JSONObject toys_json = dev_json.getJSONObject("toys");
                        ld.updateToysJSON(toys_json);
                    }*/
                } else {
                    JSONObject dev_json = resp.getJSONObject(key);
                    LovenseConnectDevice ld = new LovenseConnectDevice(dev_json);
                    LovenseConnect.devices.put(key,ld);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static void refresh() throws LovenseException
    {
        if (LovenseConnect.debug)
            System.err.println("Lovense Refresh Called");
        Vector<LovenseToy> updatedToys = new Vector<LovenseToy>();
        deviceSearch();
        
        Iterator<LovenseConnectDevice> manDevIt = LovenseConnect.devices.values().iterator();
        while(manDevIt.hasNext())
        {
            LovenseConnectDevice ld = manDevIt.next();
            ld.refresh();
            updatedToys.addAll(ld.getToys());
        }

        // Check for new toys added
        for(Iterator<LovenseToy> updatedToysIterator = updatedToys.iterator(); updatedToysIterator.hasNext();)
        {
            LovenseToy t = updatedToysIterator.next();
            if (!LovenseConnect.toys.contains(t))
            {
                LovenseConnect.toys.add(t);
                fireToyAdded(LovenseConnect.toys.indexOf(t), t);
            }
        }

        // check for toys removed
        for(Iterator<LovenseToy> oldToysIterator = LovenseConnect.toys.iterator(); oldToysIterator.hasNext();)
        {
            LovenseToy t = oldToysIterator.next();
            if (!updatedToys.contains(t))
            {
                int idx = LovenseConnect.toys.indexOf(t);
                fireToyRemoved(idx, t);
            }
        }
        LovenseConnect.toys = updatedToys;
        LovenseConnect.lastToyFetch = System.currentTimeMillis();
    }

    private static void fireToyAdded(int idx, LovenseToy toy)
    {
        if (LovenseConnect.debug)
            System.err.println("FireEVENT toyAdded - " + toy.toString());
        for (Enumeration<LovenseConnectListener> lcle = ((Vector<LovenseConnectListener>) LovenseConnect.listeners.clone()).elements(); lcle.hasMoreElements();)
        {
            try
            {
                LovenseConnectListener lcl = lcle.nextElement();
                lcl.toyAdded(idx, toy);
            } catch (Exception mlex) {
                mlex.printStackTrace(System.err);
            }
        }
    }

    private static void fireToyRemoved(int idx, LovenseToy toy)
    {
        if (LovenseConnect.debug)
            System.err.println("FireEVENT toyRemoved - " + toy.toString());
        for (Enumeration<LovenseConnectListener> lcle = ((Vector<LovenseConnectListener>) LovenseConnect.listeners.clone()).elements(); lcle.hasMoreElements();)
        {
            try
            {
                LovenseConnectListener lcl = lcle.nextElement();
                lcl.toyRemoved(idx, toy);
            } catch (Exception mlex) {
                mlex.printStackTrace(System.err);
            }
        }
    }

    public static Collection<LovenseToy> getToys() throws LovenseException
    {
        LovenseConnect.refreshIfNeeded();
        return LovenseConnect.toys;
    }

    public static LovenseToy getToyById(String toyId) throws LovenseException
    {
        for(Iterator<LovenseToy> toys = getToys().iterator(); toys.hasNext();)
        {
            LovenseToy t = toys.next();
            if (t.getId().equalsIgnoreCase(toyId) && t.isConnected())
            {
                System.err.println("Return connected toy: " + t.toString());
                return t;
            }
        }
        for(Iterator<LovenseToy> toys = getToys().iterator(); toys.hasNext();)
        {
            LovenseToy t = toys.next();
            if (t.getId().equalsIgnoreCase(toyId))
            {
                System.err.println("Return matching toy: " + t.toString());
                return t;
            }
        }
        return null;
    }

    /** Convert a Map Object into a query string **/
    protected static String mapToQuery(Map<String, String> table)
    {
        if (table != null)
        {
            try
            {
                StringBuilder sb = new StringBuilder("?");
                Set<String> keyset = table.keySet();
                Iterator<String> i = keyset.iterator();
                while(i.hasNext())
                {
                    String key = i.next();
                    String value = table.get(key);
                    sb.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
                    if (i.hasNext())
                        sb.append("&");
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return "";
    }
    
    public static int toyIndex(LovenseToy toy)
    {
        return LovenseConnect.toys.indexOf(toy);
    }

        /** Make an API call to the selected product **/
    protected static JSONObject apiCall(String path) throws LovenseException
    {
        return apiCall(path, null);
    }

    /** Read the contents of an InputStream into a String **/
    private static String readInputStreamToString(InputStream is)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int inputByte;
            while ((inputByte = is.read()) > -1)
            {
                baos.write(inputByte);
            }
            is.close();
            return new String(baos.toByteArray());
        } catch (Exception e) {
            if (LovenseConnect.debug)
            {
                System.err.println("readInputStreamToString " + e.getMessage());
                e.printStackTrace(System.err);
            }
            return null;
        }
    }

    /** Make an API call to the selected product
     * NOTE: This is the master method when talking to the BSD api
     * all other api calls should eventually lead here **/
    protected static JSONObject apiCall(String api_url, Map<String, String> params) throws LovenseException
    {

        JSONObject ro = new JSONObject();
        try
        {
            String url = api_url + mapToQuery(params);
            if (LovenseConnect.debug)
            {
                System.err.println("");
                System.err.println("Calling API: " + url);
            }
            String return_data = "";
            URL url_object = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) url_object.openConnection();
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setRequestMethod("GET");
            con.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession sslSession) {
                    return true;
                }
            });
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
            con.connect();
            int response_code = con.getResponseCode();
            if (response_code == 200)
            {
                InputStream is = con.getInputStream();
                return_data = readInputStreamToString(is);
            } else {
                String err_response = "HTTP ERROR " + String.valueOf(response_code) + ":" + url;
                throw new LovenseException(err_response, url, null);
            }
            if (return_data != null)
            {
                if (LovenseConnect.debug)
                {
                    System.err.println("  Response: " + return_data);
                    System.err.println("");
                }
                ro = new JSONObject(return_data);
                return ro;
            } else {
                throw new LovenseException("No data returned from server", url, null);
            }
        } catch (java.net.MalformedURLException ex) {
            throw new LovenseException("Bad URL: " + ex.getMessage(), api_url, null);
        } catch (java.io.IOException ex) {
            throw new LovenseException("IO Error: " + ex.getMessage(), api_url, null);
        }
    }

    /** Turns debug mode on for library, this results in System.err messages **/
    public static void setDebug(boolean value)
    {
        LovenseConnect.debug = value;
    }
}
