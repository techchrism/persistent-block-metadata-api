package com.darkender.plugins.persistentblockmetadataapi;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

public class PersistentBlockMetadataAPI implements Listener
{
    private final Plugin plugin;
    private final NamespacedKey countKey;
    private final Map<Chunk, AreaEffectCloud> loadedClouds = new HashMap<>();
    private static final MethodHandle getRawHandle = findGetRawHandle();
    private boolean attemptReconstruction;
    private final boolean preventSending;
    private boolean sendPreventionReady = false;
    private final Map<Chunk, AreaEffectCloud> reconstructedClouds = new HashMap<>();
    private final Set<CloudID> hiddenIDs = new HashSet<>();
    private LoadUnloadTypeChecker loadUnloadTypeChecker = null;
    
    /**
     * Construct the PersistentBlockMetadataAPI
     * @param plugin the plugin using this API. Registers keys, events, and timers with this plugin
     */
    public PersistentBlockMetadataAPI(@NotNull Plugin plugin)
    {
        this(plugin, true, true);
    }
    
    /**
     * Construct the PersistentBlockMetadataAPI
     * @param plugin the plugin using this API. Registers keys, events, and timers with this plugin
     * @param attemptReconstruction if cloud reconstruction (upon detected death) should be enabled
     * @param preventSending if the server should prevent sending the entity packet
     */
    public PersistentBlockMetadataAPI(@NotNull Plugin plugin, boolean attemptReconstruction, boolean preventSending)
    {
        this.plugin = plugin;
        this.attemptReconstruction = attemptReconstruction;
        this.preventSending = preventSending;
        
        countKey = new NamespacedKey(plugin, "metacount");
        for(World world : plugin.getServer().getWorlds())
        {
            for(Chunk chunk : world.getLoadedChunks())
            {
                checkChunk(chunk);
            }
        }
        
        if(preventSending)
        {
            if(Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
            {
                CloudSendPrevention.onReady(plugin, this);
                sendPreventionReady = true;
            }
        }
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () ->
        {
            loadedClouds.entrySet().removeIf(entry ->
            {
                if(!entry.getValue().isValid() || entry.getValue().isDead())
                {
                    Bukkit.getLogger().warning("AreaEffectCloud is dead or invalid!");
                    if(attemptReconstruction)
                    {
                        PersistentDataContainer old = entry.getValue().getPersistentDataContainer();
                        AreaEffectCloud newCloud = spawnCloud(entry.getValue().getLocation());
                        PersistentDataContainer newContainer = newCloud.getPersistentDataContainer();
                        getRawTags(newContainer).putAll(getRawTags(old));
                        reconstructedClouds.put(entry.getKey(), newCloud);
                    }
                    return true;
                }
                entry.getValue().setTicksLived(1);
                return false;
            });
            
            if(attemptReconstruction && reconstructedClouds.size() > 0)
            {
                loadedClouds.putAll(reconstructedClouds);
                reconstructedClouds.clear();
            }
            
        }, 1L, 20L);
    }
    
    public void setLoadUnloadTypeChecker(LoadUnloadTypeChecker loadUnloadTypeChecker)
    {
        this.loadUnloadTypeChecker = loadUnloadTypeChecker;
    }
    
    private static MethodHandle findGetRawHandle()
    {
        try
        {
            return MethodHandles.lookup().findVirtual(getCraftBukkitClass("persistence.CraftPersistentDataContainer"),
                    "getRaw", MethodType.methodType(Map.class));
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Checks if AreaEffectCloud reconstruction should be attempted
     * @return true if AreaEffectCloud reconstruction is enabled
     */
    public boolean shouldAttemptReconstruction()
    {
        return attemptReconstruction;
    }
    
    /**
     * Sets whether or not AreaEffectCloud reconstruction is enabled
     * @param attemptReconstruction whether or not AreaEffectCloud reconstruction is enabled
     */
    public void setAttemptReconstruction(boolean attemptReconstruction)
    {
        this.attemptReconstruction = attemptReconstruction;
    }
    
    private Location getCloudPos(Block block)
    {
        return new Location(block.getWorld(), block.getChunk().getX() * 16, 1, block.getChunk().getZ() * 16);
    }
    
    private AreaEffectCloud spawnCloud(Location location)
    {
        return location.getWorld().spawn(location, AreaEffectCloud.class, cloud ->
        {
            hiddenIDs.add(new CloudID(cloud.getEntityId(), cloud.getWorld().getUID()));
            cloud.clearCustomEffects();
            cloud.setDuration(60 * 20);
            cloud.setParticle(Particle.BLOCK_CRACK, Material.AIR.createBlockData());
            cloud.setSilent(true);
            cloud.setRadius(0.0F);
            cloud.setRadiusOnUse(0);
            cloud.setRadiusPerTick(0);
        });
    }
    
    /**
     * Checks if the entity id should be hidden from the client
     * @param id the entity id to check for
     * @param worldID the UUID of the world to check in
     * @return true if the UUID should be hidden
     */
    public boolean isHidden(int id, UUID worldID)
    {
        for(CloudID cloudID : hiddenIDs)
        {
            if(cloudID.getId() == id && cloudID.getWorldID().equals(worldID))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets a NamespacedKey unique to a chunk for a particular block
     * @param block the Block to get the key for
     * @return the NamespacedKey for the block's location in the chunk
     */
    public NamespacedKey keyFor(@NotNull Block block)
    {
        return new NamespacedKey(plugin,
                (block.getX() - (block.getChunk().getX() * 16))
                + "_" + block.getY() + "_" +
                (block.getZ() - (block.getChunk().getZ() * 16)));
    }
    
    /**
     * Checks if the block has metadata associated with it
     * @deprecated types should only be containers
     * @param block the block to check
     * @param type the type of data to check for
     * @param <T> the generic type of the stored primitive
     * @param <Z> the generic type of the eventually created complex object
     * @return true if the block has metadata of the type specified
     */
    @Deprecated
    public <T, Z> boolean has(@NotNull Block block, @NotNull PersistentDataType<T, Z> type)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            return false;
        }
        return loadedClouds.get(block.getChunk()).getPersistentDataContainer().has(keyFor(block), type);
    }
    
    /**
     * Checks if the block has metadata associated with it
     * @param block the block to check
     * @return true if the block has metadata of the type specified
     */
    public boolean has(@NotNull Block block)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            return false;
        }
        return loadedClouds.get(block.getChunk()).getPersistentDataContainer().has(keyFor(block), PersistentDataType.TAG_CONTAINER);
    }
    
    /**
     * Sets metadata on the block specified
     * @deprecated types should only be containers
     * @param block the block to set
     * @param type the type of data to set
     * @param value the value of the data to set
     * @param <T> the generic java type of the tag value
     * @param <Z> the generic type of the object to store
     */
    @Deprecated
    public <T, Z> void set(@NotNull Block block, @NotNull PersistentDataType<T, Z> type, @NotNull Z value)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            loadedClouds.put(block.getChunk(), spawnCloud(getCloudPos(block)));
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
     * Sets metadata on the block specified
     * @param block the block to set
     * @param value the value of the data to set
     */
    public void set(@NotNull Block block, @NotNull PersistentDataContainer value)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            loadedClouds.put(block.getChunk(), spawnCloud(getCloudPos(block)));
        }
        PersistentDataContainer data = loadedClouds.get(block.getChunk()).getPersistentDataContainer();
        NamespacedKey blockKey = keyFor(block);
        if(!data.has(blockKey, PersistentDataType.TAG_CONTAINER))
        {
            data.set(countKey, PersistentDataType.INTEGER, data.getOrDefault(countKey, PersistentDataType.INTEGER, 0) + 1);
        }
        data.set(keyFor(block), PersistentDataType.TAG_CONTAINER, value);
    }
    
    /**
     * Gets previously set metadata from the block specified
     * @deprecated types should only be containers
     * @param block the block to get metadata from
     * @param type the type of metadata
     * @param <T> the generic type of the stored primitive
     * @param <Z> the generic type of the eventually created complex object
     * @return the value or {@code null} if no value was mapped under the given value
     */
    @Deprecated
    public <T, Z> Z get(@NotNull Block block, @NotNull PersistentDataType<T, Z> type)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            return null;
        }
        return loadedClouds.get(block.getChunk()).getPersistentDataContainer().get(keyFor(block), type);
    }
    
    /**
     * Gets previously set metadata from the block specified
     * @param block the block to get metadata from
     * @return the value or {@code null} if no value was mapped under the given value
     */
    public PersistentDataContainer get(@NotNull Block block)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            loadedClouds.put(block.getChunk(), spawnCloud(getCloudPos(block)));
        }
        PersistentDataContainer data = loadedClouds.get(block.getChunk()).getPersistentDataContainer();
        NamespacedKey blockKey = keyFor(block);
        if(!data.has(blockKey, PersistentDataType.TAG_CONTAINER))
        {
            data.set(countKey, PersistentDataType.INTEGER, data.getOrDefault(countKey, PersistentDataType.INTEGER, 0) + 1);
            data.set(blockKey, PersistentDataType.TAG_CONTAINER, data.getAdapterContext().newPersistentDataContainer());
        }
        return data.get(blockKey, PersistentDataType.TAG_CONTAINER);
    }
    
    /**
     * Removes metadata from the specified block
     * @deprecated types should only be containers
     * @param block the block to remove metadata from
     * @param type the type of metadata
     * @param <T> the generic type of the stored primitive
     * @param <Z> the generic type of the eventually created complex object
     */
    @Deprecated
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
     * Removes metadata from the specified block
     * @param block the block to remove metadata from
     */
    public void remove(@NotNull Block block)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            return;
        }
        AreaEffectCloud cloud = loadedClouds.get(block.getChunk());
        PersistentDataContainer data = cloud.getPersistentDataContainer();
        NamespacedKey blockKey = keyFor(block);
        if(data.has(blockKey, PersistentDataType.TAG_CONTAINER))
        {
            int count = data.getOrDefault(countKey, PersistentDataType.INTEGER, 0);
            if(count <= 1)
            {
                cloud.remove();
                loadedClouds.remove(block.getChunk());
                if(preventSending)
                {
                    hiddenIDs.removeIf(cloudID -> cloudID.getWorldID() == cloud.getWorld().getUID() && cloudID.getId() == cloud.getEntityId());
                }
            }
            else
            {
                data.remove(blockKey);
                data.set(countKey, PersistentDataType.INTEGER, count - 1);
            }
            
        }
    }
    
    /**
     * Gets the raw tags of a container
     * @param container the container to get the raw tags for
     * @return a map of string keys to raw tags
     */
    public static Map<String, Object> getRawTags(@NotNull PersistentDataContainer container)
    {
        Map<String, Object> map;
        try
        {
            map = (Map<String, Object>) getRawHandle.invoke(container);
        }
        catch(Throwable throwable)
        {
            throwable.printStackTrace();
            return new HashMap<>();
        }
        return map;
    }
    
    /**
     * Gets a set of string keys (including namespace) stored on a PersistentDataContainer
     * @param container the container to get the keys for
     * @return the set of keys stored on the container
     */
    public static Set<String> getKeys(@NotNull PersistentDataContainer container)
    {
        return getRawTags(container).keySet();
    }
    
    /**
     * Gets a set of blocks that have metadata stored
     * @param chunk the chunk to check
     * @return a set of blocks that have metadata stored
     */
    public Set<Block> getMetadataLocations(@NotNull Chunk chunk)
    {
        if(!loadedClouds.containsKey(chunk))
        {
            return null;
        }
        Set<String> keys = getKeys(loadedClouds.get(chunk).getPersistentDataContainer());
        Set<Block> blocks = new HashSet<>();
        for(String key : keys)
        {
            if(key.contains("_"))
            {
                String after = key.split(":")[1];
                String[] parts = after.split("_");
                blocks.add(new Location(chunk.getWorld(),
                        Integer.parseInt(parts[0]) + (chunk.getX() * 16),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]) + (chunk.getZ() * 16)).getBlock());
            }
        }
        return blocks;
    }
    
    /**
     * Removes a tag container for the specified block
     * @deprecated use {@link #remove(Block)}
     * @param block the block to remove the tag container from
     */
    @Deprecated
    public void removeContainer(@NotNull Block block)
    {
        remove(block, PersistentDataType.TAG_CONTAINER);
    }
    
    /**
     * Gets a tag container for a block and creates one if it doesn't exist
     * @deprecated use {@link #get(Block)}
     * @param block the block to get the tag container for
     * @return the tag container for the block
     */
    @Deprecated
    public PersistentDataContainer getContainer(@NotNull Block block)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            loadedClouds.put(block.getChunk(), spawnCloud(getCloudPos(block)));
        }
        PersistentDataContainer data = loadedClouds.get(block.getChunk()).getPersistentDataContainer();
        NamespacedKey blockKey = keyFor(block);
        if(!data.has(blockKey, PersistentDataType.TAG_CONTAINER))
        {
            data.set(countKey, PersistentDataType.INTEGER, data.getOrDefault(countKey, PersistentDataType.INTEGER, 0) + 1);
            data.set(blockKey, PersistentDataType.TAG_CONTAINER, data.getAdapterContext().newPersistentDataContainer());
        }
        return data.get(blockKey, PersistentDataType.TAG_CONTAINER);
    }
    
    /**
     * Sets a tag container for a block
     * @deprecated use {@link #set(Block, PersistentDataContainer)}
     * @param block the block to set metadata for
     * @param container the container to attach to the block
     */
    @Deprecated
    public void setContainer(@NotNull Block block, @NotNull PersistentDataContainer container)
    {
        if(!loadedClouds.containsKey(block.getChunk()))
        {
            throw new IllegalArgumentException();
        }
        set(block, PersistentDataType.TAG_CONTAINER, container);
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
                    // Check if there's data and warn if there isn't
                    if(getKeys(container).size() == 1)
                    {
                        Bukkit.getLogger().warning("Cloud at " + chunk.getX() + " " + chunk.getZ() +
                                " has a key size of only 1! (metacount is " + container.get(countKey, PersistentDataType.INTEGER) + ")");
                    }
                    loadedClouds.put(chunk, (AreaEffectCloud) e);
                    if(preventSending)
                    {
                        hiddenIDs.add(new CloudID(e.getEntityId(), e.getWorld().getUID()));
                    }
    
                    if(loadUnloadTypeChecker != null)
                    {
                        for(Block block : getMetadataLocations(chunk))
                        {
                            if(loadUnloadTypeChecker.shouldRemove(block, get(block)))
                            {
                                remove(block);
                            }
                        }
                    }
                    
                    break;
                }
            }
        }
    }
    
    // Priority low so regular listeners can query metadata at normal priority
    // and LOWEST listeners can still cancel the event
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onChunkLoad(ChunkLoadEvent event)
    {
        checkChunk(event.getChunk());
    }
    
    // Priority monitor so regular listeners can query metadata at normal priority
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onChunkUnload(ChunkUnloadEvent event)
    {
        // "top off" the cloud timer and remove it from the map
        if(loadedClouds.containsKey(event.getChunk()))
        {
            AreaEffectCloud cloud = loadedClouds.get(event.getChunk());
            if(loadUnloadTypeChecker != null)
            {
                for(Block block : getMetadataLocations(event.getChunk()))
                {
                    if(loadUnloadTypeChecker.shouldRemove(block, get(block)))
                    {
                        remove(block);
                    }
                }
                
                // Check if *all* data was removed
                if(!loadedClouds.containsKey(event.getChunk()))
                {
                    return;
                }
            }
            if(getKeys(cloud.getPersistentDataContainer()).size() == 1)
            {
                Bukkit.getLogger().warning("Cloud at " + event.getChunk().getX() + " " + event.getChunk().getZ() +
                        " has a key size of only 1! (metacount is " +
                        cloud.getPersistentDataContainer().get(countKey, PersistentDataType.INTEGER) + ")");
            }
            cloud.setTicksLived(1);
            if(preventSending)
            {
                hiddenIDs.removeIf(cloudID -> cloudID.getWorldID() == cloud.getWorld().getUID() && cloudID.getId() == cloud.getEntityId());
            }
            loadedClouds.remove(event.getChunk());
        }
    }
    
    @EventHandler
    private void onPluginEnable(PluginEnableEvent event)
    {
        if(preventSending && !sendPreventionReady && event.getPlugin().getName().equals("ProtocolLib"))
        {
            CloudSendPrevention.onReady(plugin, this);
            sendPreventionReady = true;
        }
    }
    
    private static Class<?> getCraftBukkitClass(String name) throws ClassNotFoundException
    {
        return Class.forName("org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
    }
}
