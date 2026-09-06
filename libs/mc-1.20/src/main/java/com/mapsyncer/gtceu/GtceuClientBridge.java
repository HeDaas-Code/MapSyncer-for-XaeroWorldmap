package com.mapsyncer.gtceu;

import com.mapsyncer.network.payload.OreVeinSyncPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.registries.Registries;
import java.lang.reflect.Method;

/** Applies mirrored veins without a compile-time GTCEu dependency. */
public final class GtceuClientBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(GtceuClientBridge.class);
    private GtceuClientBridge() {}
    public static int apply(OreVeinSyncPayload payload) {
        int applied=0;
        for (OreVeinSyncPayload.OreVeinSnapshot vein : payload.veins()) {
            if (applyWaypoint(vein)) applied++;
        }
        LOGGER.info("Applied GTCEu vein batch {}/{}: {}/{} records via Xaero-compatible bridge", payload.batchIndex()+1, payload.totalBatches(), applied, payload.veins().size());
        return applied;
    }
    private static boolean applyWaypoint(OreVeinSyncPayload.OreVeinSnapshot vein) {
        try {
            Class<?> manager=Class.forName("com.gregtechceu.gtceu.integration.map.WaypointManager");
            Method set=manager.getMethod("setWaypoint", String.class,String.class,int.class,Class.forName("net.minecraft.resources.ResourceKey"),int.class,int.class,int.class);
            Object dim=net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, net.minecraft.resources.ResourceLocation.tryParse(vein.dimension()));
            set.invoke(null, vein.id(), "G", 0xFFAA33, dim, vein.centerX(), vein.centerY(), vein.centerZ());
            return true;
        } catch (Throwable e) {
            LOGGER.debug("GTCEu native waypoint bridge unavailable for {}: {}", vein.id(), e.toString());
            return false;
        }
    }
}