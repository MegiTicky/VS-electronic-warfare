package com.lauya.vselectronicwarfare.integration.cc;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class DhCompat {
    private static final long CACHE_REFRESH_MILLIS = 10_000L;
    private static final boolean IS_DH_LOADED = ModList.get().isLoaded("distanthorizons");
    private static @Nullable Object terrainCache;
    private static @Nullable Object cachedLevelWrapper;
    private static long lastCacheLoadTime;

    private DhCompat() {
    }

    static @Nullable Boolean hasClearTerrainLine(ServerLevel level, Vec3 origin, Vec3 target) {
        if (!IS_DH_LOADED) return null;

        try {
            Class<?> delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
            Object worldProxy = staticField(delayed, "worldProxy");
            Object terrainRepo = staticField(delayed, "terrainRepo");
            if (worldProxy == null || terrainRepo == null) return null;
            Object levelWrapper = findLevelWrapper(worldProxy, level);
            if (levelWrapper == null) return null;

            long now = Util.getMillis();
            if (terrainCache == null || cachedLevelWrapper != levelWrapper || now - lastCacheLoadTime > CACHE_REFRESH_MILLIS) {
                terrainCache = terrainRepo.getClass().getMethod("createSoftCache").invoke(terrainRepo);
                cachedLevelWrapper = levelWrapper;
                lastCacheLoadTime = now;
            }
            if (terrainCache == null) return null;

            Vec3 delta = target.subtract(origin);
            double distance = delta.length();
            if (distance <= 1.0E-6) return true;
            Vec3 direction = delta.scale(1.0D / distance);
            Object result = findRaycastMethod(terrainRepo).invoke(
                terrainRepo, levelWrapper, origin.x, origin.y, origin.z,
                (float) direction.x, (float) direction.y, (float) direction.z,
                (int) Math.ceil(distance), terrainCache
            );
            if (result == null || !Boolean.TRUE.equals(instanceField(result, "success"))) return null;
            Object raycastResult = instanceField(result, "payload");
            if (raycastResult == null) return true;
            Object position = instanceField(raycastResult, "pos");
            if (position == null) return true;

            Number x = (Number) instanceField(position, "x");
            Number y = (Number) instanceField(position, "y");
            Number z = (Number) instanceField(position, "z");
            if (x == null || y == null || z == null) return null;
            Vec3 hit = new Vec3(x.doubleValue(), y.doubleValue(), z.doubleValue());
            return origin.distanceToSqr(hit) >= target.distanceToSqr(origin) - 4.0;
        } catch (Throwable throwable) {
            VSElectronicWarfare.LOGGER.debug("Distant Horizons terrain LOS failed; using exact server LOS", throwable);
            return null;
        }
    }

    private static @Nullable Object findLevelWrapper(Object worldProxy, ServerLevel level) throws ReflectiveOperationException {
        if (!Boolean.TRUE.equals(worldProxy.getClass().getMethod("worldLoaded").invoke(worldProxy))) return null;
        Object wrappers = worldProxy.getClass().getMethod("getAllLoadedLevelWrappers").invoke(worldProxy);
        if (!(wrappers instanceof Iterable<?> iterable)) return null;
        for (Object wrapper : iterable) {
            if (wrapper != null && wrapper.getClass().getMethod("getWrappedMcObject").invoke(wrapper) == level) return wrapper;
        }
        return null;
    }

    private static Method findRaycastMethod(Object terrainRepo) throws NoSuchMethodException {
        for (Method method : terrainRepo.getClass().getMethods()) {
            if (method.getName().equals("raycast") && method.getParameterCount() == 9) return method;
        }
        throw new NoSuchMethodException("Distant Horizons terrainRepo.raycast with cache");
    }

    private static @Nullable Object staticField(Class<?> type, String name) throws ReflectiveOperationException {
        return type.getField(name).get(null);
    }

    private static @Nullable Object instanceField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getField(name);
        return field.get(target);
    }
}
