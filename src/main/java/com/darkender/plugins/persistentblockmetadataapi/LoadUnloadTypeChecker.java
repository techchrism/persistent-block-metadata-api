package com.darkender.plugins.persistentblockmetadataapi;

import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;

public interface LoadUnloadTypeChecker
{
    /**
     * Called when a chunk is loaded or unloaded to ensure saved data is as it should be
     * @param block the block with saved metadata to check
     * @param data the data container attached to the block
     * @return true if the data should be removed, false otherwise
     */
    boolean shouldRemove(Block block, PersistentDataContainer data);
}
