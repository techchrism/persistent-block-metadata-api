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
    
    public void setMetadataWorldTrackObserver(MetadataWorldTrackObserver metadataWorldTrackObserver)
    {
        this.metadataWorldTrackObserver = metadataWorldTrackObserver;
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBurn(BlockBreakEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event)
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
    public void onBlockFade(BlockFadeEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLeavesDecay(LeavesDecayEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPistonExtendEvent(BlockPistonExtendEvent event)
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
    public void onBlockPistonRetractEvent(BlockPistonExtendEvent event)
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
    public void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        if(persistentBlockMetadataAPI.has(event.getBlock()))
        {
            onBreak(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event)
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
