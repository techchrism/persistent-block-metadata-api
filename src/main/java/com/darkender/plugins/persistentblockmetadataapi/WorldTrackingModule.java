package com.darkender.plugins.persistentblockmetadataapi;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

public class WorldTrackingModule implements Listener
{
    private final PersistentBlockMetadataAPI persistentBlockMetadataAPI;
    private MetadataWorldTrackObserver metadataWorldTrackObserver = null;
    
    /**
     * Constructs the WorldTrackingModule
     * @param plugin the plugin to use as a base for event handling
     * @param persistentBlockMetadataAPI the instance of the {@link PersistentBlockMetadataAPI} storing data
     */
    public WorldTrackingModule(Plugin plugin, PersistentBlockMetadataAPI persistentBlockMetadataAPI)
    {
        this.persistentBlockMetadataAPI = persistentBlockMetadataAPI;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void onBreak(Block block)
    {
        if(metadataWorldTrackObserver != null)
        {
            metadataWorldTrackObserver.onBreak(block);
        }
        persistentBlockMetadataAPI.remove(block);
    }
    
    private void onMove(Block from, Block to)
    {
        if(metadataWorldTrackObserver != null)
        {
            metadataWorldTrackObserver.onMove(from, to);
        }
        persistentBlockMetadataAPI.set(to, persistentBlockMetadataAPI.get(from));
        persistentBlockMetadataAPI.remove(from);
    }
    
    /**
     * Sets the {@link MetadataWorldTrackObserver} interface that gets called when a block with metadata is broken or moved
     * @param metadataWorldTrackObserver interface to set
     */
    public void setMetadataWorldTrackObserver(MetadataWorldTrackObserver metadataWorldTrackObserver)
    {
        this.metadataWorldTrackObserver = metadataWorldTrackObserver;
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockBreak(BlockBreakEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockBurn(BlockBurnEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockExplode(BlockExplodeEvent event)
    {
        for(Block check : event.blockList())
        {
            if(persistentBlockMetadataAPI.has(check))
            {
                onBreak(event.getBlock());
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockFade(BlockFadeEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onLeavesDecay(LeavesDecayEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockPistonExtendEvent(BlockPistonExtendEvent event)
    {
        for(Block check : event.getBlocks())
        {
            if(persistentBlockMetadataAPI.has(check))
            {
                onMove(check, check.getRelative(event.getDirection()));
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockPistonRetractEvent(BlockPistonRetractEvent event)
    {
        for(Block check : event.getBlocks())
        {
            if(persistentBlockMetadataAPI.has(check))
            {
                onMove(check, check.getRelative(event.getDirection()));
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityExplode(EntityExplodeEvent event)
    {
        for(Block check : event.blockList())
        {
            if(persistentBlockMetadataAPI.has(check))
            {
                onBreak(check);
            }
        }
    }
}
