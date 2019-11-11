package org.openstatic.lovense.swing;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import java.util.Enumeration;
import java.util.Vector;
import org.openstatic.lovense.*;

public class LovenseToyListModel implements ListModel<LovenseToy>, LovenseConnectListener
{
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();

    public LovenseToyListModel()
    {
        this.listeners = new Vector<ListDataListener>();
        LovenseConnect.addLovenseConnectListener(this);
    }

    public int getSize()
    {
        try
        {
            return LovenseConnect.getToys().size();
        } catch (Exception e) {
            return 0;
        }
    }

    public LovenseToy getElementAt(int index)
    {
        try
        {
            LovenseToy[] toys = LovenseConnect.getToys().toArray(new LovenseToy[0]);
            return toys[index];
        } catch (Exception e) {
            return null;
        }
    }

    public void addListDataListener(ListDataListener l)
    {
        if (!this.listeners.contains(l))
            this.listeners.add(l);
    }

    public void removeListDataListener(ListDataListener l)
    {
        try
        {
            this.listeners.remove(l);
        } catch (Exception e) {}
    }

    public void toyAdded(int idx, LovenseToy toy)
    {
        System.err.println("Toy added fired on List model");
        ListDataEvent lde = new ListDataEvent(toy, ListDataEvent.INTERVAL_ADDED, idx, idx);
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ldl.intervalAdded(lde);
            } catch (Exception mlex) {
                mlex.printStackTrace(System.err);
            }
        }
    }

    public void toyRemoved(int idx, LovenseToy toy)
    {
        ListDataEvent lde = new ListDataEvent(toy, ListDataEvent.INTERVAL_REMOVED, idx, idx);
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ldl.intervalRemoved(lde);
            } catch (Exception mlex) {
                mlex.printStackTrace(System.err);
            }
        }
    }

}
