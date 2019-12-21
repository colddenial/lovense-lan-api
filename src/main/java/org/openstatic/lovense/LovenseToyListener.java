package org.openstatic.lovense;

public interface LovenseToyListener
{
    /** Fired when the toys status updates, or the battery level is updated, useful for interface updates **/
    public void toyUpdated(LovenseToy toy);
}
