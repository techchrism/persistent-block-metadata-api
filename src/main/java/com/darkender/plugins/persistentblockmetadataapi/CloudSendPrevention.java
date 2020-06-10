package com.darkender.plugins.persistentblockmetadataapi;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;

public class CloudSendPrevention
{
    public static void onReady(Plugin plugin, PersistentBlockMetadataAPI persistentBlockMetadataAPI)
    {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.ENTITY_VELOCITY,
                PacketType.Play.Server.ENTITY_HEAD_ROTATION, PacketType.Play.Server.ENTITY_TELEPORT, PacketType.Play.Server.REL_ENTITY_MOVE)
        {
            @Override
            public void onPacketSending(PacketEvent event)
            {
                int entityID = event.getPacket().getIntegers().read(0);
                if(persistentBlockMetadataAPI.isHidden(entityID, event.getPlayer().getWorld().getUID()))
                {
                    event.setCancelled(true);
                }
            }
        });
    }
}
