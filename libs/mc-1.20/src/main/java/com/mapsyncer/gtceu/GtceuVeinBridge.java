package com.mapsyncer.gtceu;

import com.mapsyncer.network.payload.OreVeinSyncPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Optional reflection bridge for the GTCEu-compatible runtime embedded by GTOCore. */
public final class GtceuVeinBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(GtceuVeinBridge.class);
    private static final String SERVER_CACHE = "com.gregtechceu.gtceu.integration.map.cache.server.ServerCache";
    private GtceuVeinBridge() {}

    public static boolean isAvailable() {
        try { Class.forName(SERVER_CACHE, false, GtceuVeinBridge.class.getClassLoader()); return true; }
        catch (Throwable e) { return false; }
    }

    public static List<OreVeinSyncPayload.OreVeinSnapshot> all(MinecraftServer server) {
        List<OreVeinSyncPayload.OreVeinSnapshot> out = new ArrayList<>();
        if (server == null) return out;
        if (!isAvailable()) return out;
        try {
            Class<?> cacheType = Class.forName(SERVER_CACHE);
            Field instanceField = cacheType.getField("instance");
            Object cache = instanceField.get(null);
            Method query = cacheType.getMethod("getVeinsInArea", ResourceKey.class, int[].class);
            for (ServerLevelRef level : levels(server)) {
                Object result = query.invoke(cache, level.key, new int[]{-30000000, -30000000, 60000000, 60000000});
                if (!(result instanceof Iterable<?> iterable)) continue;
                for (Object vein : iterable) {
                    OreVeinSyncPayload.OreVeinSnapshot snapshot = snapshot(level.key.location().toString(), vein);
                    if (snapshot != null) out.add(snapshot);
                }
            }
            LOGGER.info("GTCEu bridge captured {} vein records across {} dimensions", out.size(), server.levelKeys().size());
        } catch (Throwable e) {
            LOGGER.warn("GTCEu vein bridge unavailable at runtime: {}", e.toString());
        }
        return out;
    }

    private static OreVeinSyncPayload.OreVeinSnapshot snapshot(String dimension, Object vein) {
        try {
            Object id = call(vein, "id"); Object origin = call(vein, "originChunk"); Object center = call(vein, "center");
            Object definition = call(vein, "definition"); Object depleted = call(vein, "depleted");
            int ox = number(call(origin, "x")); int oz = number(call(origin, "z"));
            int cx = number(call(center, "getX")); int cy = number(call(center, "getY")); int cz = number(call(center, "getZ"));
            String def = definition == null ? "" : definition.getClass().getName();
            return new OreVeinSyncPayload.OreVeinSnapshot(String.valueOf(id), dimension, ox, oz, cx, cy, cz, def, Boolean.TRUE.equals(depleted));
        } catch (Throwable ignored) { return null; }
    }

    private static Object call(Object target, String method) throws Exception {
        if (target == null) return null;
        return target.getClass().getMethod(method).invoke(target);
    }
    private static int number(Object value) { return value instanceof Number n ? n.intValue() : 0; }
    private record ServerLevelRef(ResourceKey<Level> key) {}
    private static List<ServerLevelRef> levels(MinecraftServer server) {
        List<ServerLevelRef> out = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) out.add(new ServerLevelRef(level.dimension()));
        return out;
    }
}