package com.darkender.plugins.persistentblockmetadataapi;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PersistentBlockMetadataAPI implements Listener
{
    private final Plugin plugin;
    private final NamespacedKey countKey;
    private final Map<Chunk, AreaEffectCloud> loadedClouds = new HashMap<>();
    
    /**
     * Construct the PersistentBlockMetadataAPI
     * @param plugin the plugin using this API. Registers keys, events, and timers with this plugin
     */
    public PersistentBlockMetadataAPI(@NotNull Plugin plugin)
    {
        this.plugin = plugin;
        countKey = new NamespacedKey(plugin, "metacount");
        for(World world : plugin.getServer().getWorlds())
        {
            for(Chunk chunk : world.getLoadedChunks())
            {
                checkChunk(chunk);
            }
        }
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () ->
        {
            for(AreaEffectCloud cloud : loadedClouds.values())
            {
                cloud.setDuration(60 * 20);
            }
        }, 1L, 20L);
    }
    
    /**
     * Gets a NamespacedKey unique to a chunk for a particular block
     * @param block the Block to get the key for
     * @return the NamespacedKey for the block's location in the chunk
     */
    public NamespacedKey keyFor(@NotNull Block block)
    {
        return new NamespacedKey(plugin, (block.getX() % 16) + "_" + block.getY() + "_" + (block.getZ() % 16));
    }
    
    /**
     * Checks if the block has metadata associated with it
     * @param block the block to check
     * @param type the type of data to check for
     * @param <T> the generic type of the stored primitive
     * @param <Z> the generic type of the eventually created complex object
     * @return true if the block has metadata of the type specified
     */
    public <T, Z> boolean has(@NotNull Block block, @NotNull PersistentDataType<T, Z> type)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            return false;
        }
        return loadedClouds.get(block.getChunk()).getPersistentDataContainer().has(keyFor(block), type);
    }
    
    /**
     * Sets metadata on the block specified
     * @param block the block to set
     * @param type the type of data to set
     * @param value the value of the data to set
     * @param <T> the generic java type of the tag value
     * @param <Z> the generic type of the object to store
     */
    public <T, Z> void set(@NotNull Block block, @NotNull PersistentDataType<T, Z> type, @NotNull Z value)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            AreaEffectCloud cloud = block.getWorld().spawn(
                    new Location(block.getWorld(), (block.getX() % 16) * 16, 1, (block.getZ() % 16) * 16),
                    AreaEffectCloud.class);
            cloud.setDuration(60 * 20);
            cloud.setParticle(Particle.BLOCK_CRACK, Material.AIR.createBlockData());
            cloud.clearCustomEffects();
            cloud.setRadiusOnUse(0);
            cloud.setRadiusPerTick(0);
            loadedClouds.put(block.getChunk(), cloud);
        }
        PersistentDataContainer data = loadedClouds.get(block.getChunk()).getPersistentDataContainer();
        NamespacedKey blockKey = keyFor(block);
        if(!data.has(blockKey, type))
        {
            data.set(countKey, PersistentDataType.INTEGER, data.getOrDefault(countKey, PersistentDataType.INTEGER, 0) + 1);
        }
        data.set(keyFor(block), type, value);
    }
    
    /**
     * Gets previously set metadata from the block specified
     * @param block the block to get metadata from
     * @param type the type of metadata
     * @param <T> the generic type of the stored primitive
     * @param <Z> the generic type of the eventually created complex object
     * @return the value or {@code null} if no value was mapped under the given value
     */
    public <T, Z> Z get(@NotNull Block block, @NotNull PersistentDataType<T, Z> type)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            return null;
        }
        return loadedClouds.get(block.getChunk()).getPersistentDataContainer().get(keyFor(block), type);
    }
    
    /**
     * Removes metadata from the specified block
     * @param block the block to remove metadata from
     * @param type the type of metadata
     * @param <T> the generic type of the stored primitive
     * @param <Z> the generic type of the eventually created complex object
     */
    public <T, Z> void remove(@NotNull Block block, @NotNull PersistentDataType<T, Z> type)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            return;
        }
        AreaEffectCloud cloud = loadedClouds.get(block.getChunk());
        PersistentDataContainer data = cloud.getPersistentDataContainer();
        NamespacedKey blockKey = keyFor(block);
        if(data.has(blockKey, type))
        {
            int count = data.getOrDefault(countKey, PersistentDataType.INTEGER, 0);
            if(count <= 1)
            {
                cloud.remove();
                loadedClouds.remove(block.getChunk());
            }
            else
            {
                data.remove(blockKey);
                data.set(countKey, PersistentDataType.INTEGER, count - 1);
            }
            
        }
    }
    
    /**
     * Checks the chunk for area effect clouds and adds
     * @param chunk the chunk to check
     */
    private void checkChunk(Chunk chunk)
    {
        // Search for an area effect cloud for storing data
        for(Entity e : chunk.getEntities())
        {
            if(e.getType() == EntityType.AREA_EFFECT_CLOUD)
            {
                PersistentDataContainer container = e.getPersistentDataContainer();
                if(container.has(countKey, PersistentDataType.INTEGER))
                {
                    loadedClouds.put(chunk, (AreaEffectCloud) e);
                    break;
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onChunkLoad(ChunkLoadEvent event)
    {
        checkChunk(event.getChunk());
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onChunkUnload(ChunkUnloadEvent event)
    {
        // "top off" the cloud timer and remove it from the map
        if(loadedClouds.containsKey(event.getChunk()))
        {
            loadedClouds.get(event.getChunk()).setDuration(60 * 20);
            loadedClouds.remove(event.getChunk());
        }
    }
}
