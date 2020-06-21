package com.darkender.plugins.persistentblockmetadataapi;

import org.bukkit.block.Block;

public interface MetadataWorldTrackObserver
{
    /**
     * Called when a block storing metadata has been broken
     * @param block the block that was broken
     */
    void onBreak(Block block);
    
    /**
     * Called when a block storing metadata has been moved to another location
     * @param from the previous block location
     * @param to the location the block will be
     */
    void onMove(Block from, Block to);
}
