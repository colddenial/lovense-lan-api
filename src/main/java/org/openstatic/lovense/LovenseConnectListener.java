package org.openstatic.lovense;

public interface LovenseConnectListener
{
    /** a new toy has been discovered on the network **/
    public void toyAdded(int idx, LovenseToy toy);
    /** a toy is no longer available on the network **/
    public void toyRemoved(int idx, LovenseToy toy);
}
