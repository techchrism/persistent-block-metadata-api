package com.darkender.plugins.persistentblockmetadataapi;

import org.bukkit.block.Block;

public interface MetadataWorldTrackObserver
{
    void onBreak(Block block);
    void onMove(Block from, Block to);
}
