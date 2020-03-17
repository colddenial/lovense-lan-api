package org.openstatic.lovense;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** This class just contains a bunch of library tests **/
public class LovenseConnectTests
{
    public static void main(String[] args) throws LovenseException, InterruptedException
    {
        //System.setProperty("javax.net.debug","SSL,handshake,data");
        String test_name = args[0].toLowerCase();
        // Setup the client to the correct product and turn debug on
        LovenseConnect.setDebug(true);

        if ("gettoys".equals(test_name))
            getToys();
    }


    public static void getToys() throws LovenseException, InterruptedException
    {
        LovenseConnect.addLovenseConnectListener(new LovenseConnectListener()
        {
            public void toyAdded(int idx, LovenseToy toy)
            {
                System.err.println("Toy Added(" + String.valueOf(idx) + ") - " + toy.getName());
            }
            public void toyRemoved(int idx, LovenseToy toy)
            {
                System.err.println("Toy Removed(" + String.valueOf(idx) + ") - " + toy.getName());
            }

        });
        LovenseConnect.addDeviceManually("127.0.0.1",30010);
        while (true)
        {
            Collection<LovenseToy> toys = LovenseConnect.getToys();
            Iterator<LovenseToy> toyIterator = toys.iterator();
            while(toyIterator.hasNext())
            {
                LovenseToy nextToy = toyIterator.next();
                System.err.println("  Instance: " + nextToy.toString());
                System.err.println("  Nickname: " + nextToy.getNickname());
                System.err.println("  Name: " + nextToy.getName());
                System.err.println("  id: " + nextToy.getId());
                System.err.println("  Battery:" + String.valueOf(nextToy.getBattery()));
                if (nextToy.isConnected())
                {
                    System.err.println("  Toy IS connected");
                    //nextToy.vibrate(1);
                    Thread.sleep(2000);
                    //nextToy.vibrate(0);
                    //nextToy.rotate(10);
                } else {
                    System.err.println("  Toy NOT connected");
                }
                System.err.println("");

            }
            System.err.println("");
            System.err.println("Waiting 4 Seconds...");
            Thread.sleep(4000);
            System.err.println("");
        }
    }
}
