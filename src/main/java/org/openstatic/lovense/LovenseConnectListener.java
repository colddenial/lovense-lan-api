package org.openstatic.lovense;

public interface LovenseConnectListener
{
    public void toyAdded(int idx, LovenseToy toy);
    public void toyRemoved(int idx, LovenseToy toy);
}
