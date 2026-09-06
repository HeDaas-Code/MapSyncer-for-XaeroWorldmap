package com.mapsyncer.network.payload;

import com.mapsyncer.network.NetworkHandler;
import java.util.List;

/** GTCEu-compatible ore vein records mirrored to the client/debug viewer. */
public record OreVeinSyncPayload(List<OreVeinSnapshot> veins, boolean complete, String status, int batchIndex, int totalBatches) {
    public static final String ID = NetworkHandler.ORE_VEIN_SYNC_ID;
    public record OreVeinSnapshot(String id, String dimension, int originChunkX, int originChunkZ,
                                  int centerX, int centerY, int centerZ, String definitionId, boolean depleted) {}
}
