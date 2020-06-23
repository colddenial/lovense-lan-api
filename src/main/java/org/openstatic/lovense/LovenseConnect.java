package org.openstatic.lovense;

import org.json.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URL;
import java.net.Proxy;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;
import java.util.StringTokenizer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;

/** This is the main class for interacting with the Library
 *  Note: Most methods are static since there is no reason to instantiate this class
 *  
 * 
 * **/

public class LovenseConnect
{
    protected static boolean debug = false;
    private static String VERSION = "";

    private static LinkedHashMap<String, LovenseConnectDevice> devices = new LinkedHashMap<String, LovenseConnectDevice>();
    private static LinkedHashMap<String, LovenseToy> toys = new LinkedHashMap<String, LovenseToy>();
    private static ArrayList<String> toyOrder = new ArrayList<String>();

    private static long lastToyFetch = 0;
    private static Vector<LovenseConnectListener> listeners = new Vector<LovenseConnectListener>();
    private static Thread refreshThread;

    /* adds a LovenseConnectListener to the library for internal events */
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

    public static JSONArray getDevicesAsJSONArray()
    {
        JSONArray ra = new JSONArray();
        Iterator<LovenseConnectDevice> manDevIt = LovenseConnect.devices.values().iterator();
        while(manDevIt.hasNext())
        {
            LovenseConnectDevice ld = manDevIt.next();
            ra.put(ld.getHostPort());
        }
        return ra;
    }
    
    public static void addDevicesFromJSONArray(JSONArray ja)
    {
        for(int i = 0; i < ja.length(); i++)
        {
            String ip_port = ja.getString(i);
            try
            {
                StringTokenizer st = new StringTokenizer(ip_port,":");
                String ip = st.nextToken();
                if (st.hasMoreTokens())
                {
                    int port = Integer.valueOf(st.nextToken());
                    LovenseConnect.addDeviceManually(ip, port);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    /* return a Collection of all detected LovenseConnectDevices found on the network */
    public static Collection<LovenseConnectDevice> getDevices() throws LovenseException
    {
        refreshIfNeeded();
        return LovenseConnect.devices.values();
    }

    /* Launch a Thread to call refresh() if there hasn't been a refresh in over 20 seconds */
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
    
    /** Enter the ip address and https port of a lovenseConnect app manually **/
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
            JSONObject resp = LovenseConnect.apiCall("https://api.lovense.com/api/lan/getToys?rnd=" + String.valueOf(System.currentTimeMillis()));
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

    /** Contact lovense servers to look for local devices and refresh the status of connected devices **/
    public static synchronized void refresh() throws LovenseException
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
            String toyIdLower = t.getId().toLowerCase();
            if (!LovenseConnect.toys.containsKey(toyIdLower))
            {
                LovenseConnect.toyOrder.add(toyIdLower);
                LovenseConnect.toys.put(toyIdLower, t);
                fireToyAdded(LovenseConnect.toyOrder.indexOf(toyIdLower), t);
            }
        }

        // check for toys removed
        int idxPos = 0;
        for(Iterator<LovenseToy> oldToysIterator = LovenseConnect.toys.values().iterator(); oldToysIterator.hasNext();)
        {
            LovenseToy t = oldToysIterator.next();
            if (!updatedToys.contains(t))
            {
                LovenseConnect.toys.remove(t.getId().toLowerCase());
                fireToyRemoved(idxPos, t);
            }
            idxPos++;
        }
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

    /* Retrieves a collection of all the lovense toys detected by the library, will also call refreshIfNeeded() */
    public static Collection<LovenseToy> getToys() throws LovenseException
    {
        LovenseConnect.refreshIfNeeded();
        return LovenseConnect.toys.values();
    }

    public static LovenseToy getToyById(String toyId) throws LovenseException
    {
        if (toyId != null)
            return LovenseConnect.toys.get(toyId.toLowerCase());
        else
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
    
    /** Retrieve the Library's internal index for a particular toy **/
    public static int toyIndex(LovenseToy toy)
    {
        if (toy != null)
            return LovenseConnect.toyOrder.indexOf(toy.getId().toLowerCase());
        else
            return -1;
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

    protected static JSONObject apiCall(String path) throws LovenseException
    {
        return apiCall(path, null);
    }

    protected static void showHeaders(HttpsURLConnection con)
    {
        String headerName = null;
        for (int i = 1; (headerName = con.getHeaderFieldKey(i)) != null; i++)
        {
            if (headerName.equals("Set-Cookie"))
            {
              String cookie = con.getHeaderField(i);
              System.out.println("(Response) Cookie  ::  " + cookie);
            } else {
              System.out.println("(Response) Header: "+ con.getHeaderField(i));
            }
        }
    }

    protected static JSONObject apiCall(String api_url, Map<String, String> params) throws LovenseException
    {
        System.setProperty("java.net.preferIPv6Addresses", "true");
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
            HttpsURLConnection con = (HttpsURLConnection) url_object.openConnection(Proxy.NO_PROXY);
            if (!url.contains("api.lovense.com"))
            {
                if (LovenseConnect.debug)
                    System.err.println("Removing Trust Restrictions! - " + url);
                try
                {
                    TrustModifier.relaxHostChecking(con);
                } catch (Exception rhce) {
                    rhce.printStackTrace(System.err);
                }
            }
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36");
            con.setRequestProperty("Accept", "text/html");
            con.setRequestProperty("Cache-Control", "no-cache");
            con.setRequestProperty("Pragma", "no-cache");
            //con.setRequestProperty("Accept-Encoding", "gzip,deflate,br");
            //con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            //con.setRequestProperty("Upgrade-Insecure-Requests", "1");
            con.setInstanceFollowRedirects(true);
            con.setDefaultUseCaches(false);
            con.setUseCaches(false);
            con.setDoOutput(false);
            con.setDoInput(true);
            if (LovenseConnect.debug)
            {
                for (Map.Entry<String, List<String>> entries : con.getRequestProperties().entrySet())
                {    
                    String values = "";
                    for (String value : entries.getValue())
                    {
                        values += value + ",";
                    }
                    System.err.println("(Request) Header: " + entries.getKey() + " - " +  values );
                }
            }
            con.connect();
            if (LovenseConnect.debug)
                showHeaders(con);
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
