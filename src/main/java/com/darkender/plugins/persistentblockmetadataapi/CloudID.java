package com.darkender.plugins.persistentblockmetadataapi;

import java.util.Objects;
import java.util.UUID;

public class CloudID
{
    private final int id;
    private final UUID worldID;
    
    public CloudID(int id, UUID worldID)
    {
        this.id = id;
        this.worldID = worldID;
    }
    
    public int getId()
    {
        return id;
    }
    
    public UUID getWorldID()
    {
        return worldID;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        CloudID cloudID = (CloudID) o;
        return id == cloudID.id && worldID == cloudID.worldID;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(id, worldID);
    }
}
