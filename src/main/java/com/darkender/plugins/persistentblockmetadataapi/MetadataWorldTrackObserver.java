package com.darkender.plugins.persistentblockmetadataapi;

import org.bukkit.block.Block;
import org.bukkit.event.Event;

public interface MetadataWorldTrackObserver
{
    /**
     * Called when a block storing metadata has been broken
     * @param block the block that was broken
     * @param event the event that was triggered
     */
    void onBreak(Block block, Event event);
    
    /**
     * Called when a block storing metadata has been moved to another location
     * @param from the previous block location
     * @param to the location the block will be
     * @param event the event that was triggered
     */
    void onMove(Block from, Block to, Event event);
}
