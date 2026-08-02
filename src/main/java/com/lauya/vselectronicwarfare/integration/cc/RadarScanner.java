package com.lauya.vselectronicwarfare.integration.cc;

import com.lauya.vselectronicwarfare.VSElectronicWarfare;
import com.lauya.vselectronicwarfare.block.entity.RadarBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class RadarScanner {
    public static final double MAX_RADIUS = 512.0;

    public enum ScanMode {
        ALL,
        SHIPS,
        ENTITIES,
        PLAYERS
    }

    private RadarScanner() {
    }

    public static List<Map<String, Object>> scan(RadarBlockEntity radar, Optional<Double> requestedRadius, ScanMode mode)
        throws LuaException {
        Level level = radar.getLevel();
        if (level == null) return List.of();

        double radius = clampRadius(requestedRadius);
        Vec3 origin = ValkyrienSkiesReflection.worldCoordinates(level, radar.getBlockPos());
        AABB scanBox = new AABB(
            origin.x - radius, origin.y - radius, origin.z - radius,
            origin.x + radius, origin.y + radius, origin.z + radius
        );

        List<Map<String, Object>> results = new ArrayList<>();
        if (mode == ScanMode.ALL || mode == ScanMode.SHIPS) {
            scanShips(level, origin, scanBox, radius, results);
        }
        if (mode == ScanMode.ALL || mode == ScanMode.ENTITIES || mode == ScanMode.PLAYERS) {
            scanEntities(level, origin, scanBox, radius, mode, results);
        }
        return results;
    }

    public static Map<String, Object> getConfigInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("max_radius", MAX_RADIUS);
        map.put("line_of_sight_required", true);
        map.put("line_of_sight_mode", "center_ray");
        map.put("ship_line_of_sight_uses_vs_clip_include_ships", true);
        return map;
    }

    private static double clampRadius(Optional<Double> requestedRadius) throws LuaException {
        double radius = requestedRadius.orElse(MAX_RADIUS);
        if (!Double.isFinite(radius)) throw new LuaException("radius must be finite");
        if (radius < 0.0) throw new LuaException("radius must be non-negative");
        return Math.min(radius, MAX_RADIUS);
    }

    private static void scanEntities(Level level, Vec3 origin, AABB scanBox, double radius, ScanMode mode,
        List<Map<String, Object>> results) {
        Predicate<Entity> filter = entity -> {
            if (!entity.isAlive()) return false;
            boolean isPlayer = entity instanceof Player;
            if (mode == ScanMode.PLAYERS) return isPlayer;
            if (mode == ScanMode.ENTITIES) return !isPlayer;
            return true;
        };

        for (Entity entity : level.getEntities((Entity) null, scanBox, filter)) {
            Vec3 target = entity.getBoundingBox().getCenter();
            double distance = origin.distanceTo(target);
            if (distance > radius) continue;
            if (!ValkyrienSkiesReflection.hasLineOfSight(level, origin, target, null)) continue;
            results.add(entityToMap(entity, target, distance));
        }
    }

    private static Map<String, Object> entityToMap(Entity entity, Vec3 target, double distance) {
        Map<String, Object> map = new LinkedHashMap<>();
        boolean isPlayer = entity instanceof Player;
        map.put("is_entity", true);
        map.put("is_player", isPlayer);
        map.put("id", entity.getId());
        map.put("uuid", entity.getUUID().toString());
        map.put("name", entity.getName().getString());
        map.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        map.put("pos", vectorMap(target.x, target.y, target.z));
        Vec3 velocity = entity.getDeltaMovement();
        map.put("velocity", vectorMap(velocity.x, velocity.y, velocity.z));
        map.put("distance", distance);
        map.put("line_of_sight", true);
        return map;
    }

    private static void scanShips(Level level, Vec3 origin, AABB scanBox, double radius,
        List<Map<String, Object>> results) {
        Iterable<?> ships = ValkyrienSkiesReflection.getShipsIntersecting(level, scanBox);
        if (ships == null) return;

        for (Object ship : ships) {
            Vec3 target = ValkyrienSkiesReflection.shipCenter(ship);
            if (target == null) continue;
            double distance = origin.distanceTo(target);
            if (distance > radius) continue;

            Object targetShipId = ValkyrienSkiesReflection.shipId(ship);
            if (!ValkyrienSkiesReflection.hasLineOfSight(level, origin, target, targetShipId)) continue;
            results.add(shipToMap(ship, target, distance));
        }
    }

    private static Map<String, Object> shipToMap(Object ship, Vec3 target, double distance) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("is_ship", true);
        Object id = ValkyrienSkiesReflection.shipId(ship);
        if (id != null) map.put("id", id);
        Object slug = ValkyrienSkiesReflection.invokeNoArg(ship, "getSlug");
        if (slug != null) map.put("slug", slug.toString());
        map.put("pos", vectorMap(target.x, target.y, target.z));

        Vec3 velocity = ValkyrienSkiesReflection.vectorResult(ship, "getVelocity");
        if (velocity != null) map.put("velocity", vectorMap(velocity.x, velocity.y, velocity.z));

        Map<String, Object> size = ValkyrienSkiesReflection.shipSize(ship);
        if (size != null) map.put("size", size);

        Object mass = ValkyrienSkiesReflection.shipMass(ship);
        if (mass != null) map.put("mass", mass);

        map.put("distance", distance);
        map.put("line_of_sight", true);
        return map;
    }

    private static Map<String, Object> vectorMap(double x, double y, double z) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        return map;
    }

    private static final class ValkyrienSkiesReflection {
        private static @Nullable Method getShipsIntersecting;
        private static @Nullable Method getWorldCoordinates;
        private static @Nullable Method clipIncludeShips2;
        private static @Nullable Method clipIncludeShips3;
        private static @Nullable Method clipIncludeShips4;
        private static boolean initialized;

        static @Nullable Iterable<?> getShipsIntersecting(Level level, AABB aabb) {
            init();
            if (getShipsIntersecting == null) return null;
            try {
                Object result = getShipsIntersecting.invoke(null, level, aabb);
                return result instanceof Iterable<?> iterable ? iterable : null;
            } catch (Throwable throwable) {
                VSElectronicWarfare.LOGGER.debug("VS getShipsIntersecting failed", throwable);
                return null;
            }
        }

        static Vec3 worldCoordinates(Level level, BlockPos pos) {
            init();
            Vec3 fallback = Vec3.atCenterOf(pos);
            if (getWorldCoordinates == null) return fallback;
            try {
                Object result = getWorldCoordinates.invoke(null, level, pos, new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                Vec3 vec = vectorObjectToVec3(result);
                return vec == null ? fallback : vec;
            } catch (Throwable throwable) {
                VSElectronicWarfare.LOGGER.debug("VS getWorldCoordinates failed", throwable);
                return fallback;
            }
        }

        static boolean hasLineOfSight(Level level, Vec3 origin, Vec3 target, @Nullable Object skipShipId) {
            Vec3 start = nudgeStart(origin, target);
            ClipContext ctx = new ClipContext(start, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
            HitResult hit = clip(level, ctx, skipShipId);
            if (hit == null || hit.getType() == HitResult.Type.MISS) return true;
            return hit.getLocation().distanceToSqr(target) <= 1.0E-4;
        }

        private static Vec3 nudgeStart(Vec3 origin, Vec3 target) {
            Vec3 delta = target.subtract(origin);
            double length = delta.length();
            if (length <= 1.0E-6) return origin;
            if (length <= 1.0) return origin.add(delta.scale(0.1 / length));
            return origin.add(delta.scale(0.75 / length));
        }

        private static HitResult clip(Level level, ClipContext ctx, @Nullable Object skipShipId) {
            init();
            try {
                if (skipShipId != null && clipIncludeShips4 != null) {
                    return (HitResult) clipIncludeShips4.invoke(null, level, ctx, true, skipShipId);
                }
                if (clipIncludeShips3 != null) {
                    return (HitResult) clipIncludeShips3.invoke(null, level, ctx, true);
                }
                if (clipIncludeShips2 != null) {
                    return (HitResult) clipIncludeShips2.invoke(null, level, ctx);
                }
            } catch (Throwable throwable) {
                VSElectronicWarfare.LOGGER.debug("VS clipIncludeShips failed, falling back to vanilla clip", throwable);
            }
            return level.clip(ctx);
        }

        static @Nullable Vec3 shipCenter(Object ship) {
            Object transform = invokeNoArg(ship, "getTransform");
            Vec3 pos = vectorResult(transform, "getPositionInWorld");
            if (pos != null) return pos;
            pos = vectorResult(transform, "getShipPositionInWorldCoordinates");
            if (pos != null) return pos;
            pos = vectorResult(ship, "getPositionInWorld");
            if (pos != null) return pos;
            return worldAabbCenter(ship);
        }

        static @Nullable Object shipId(Object ship) {
            Object id = invokeNoArg(ship, "getId");
            if (id != null) return id;
            return invokeNoArg(ship, "id");
        }

        static @Nullable Vec3 vectorResult(@Nullable Object target, String methodName) {
            if (target == null) return null;
            Object result = invokeNoArg(target, methodName);
            return vectorObjectToVec3(result);
        }

        static @Nullable Map<String, Object> shipSize(Object ship) {
            Object aabb = invokeNoArg(ship, "getShipAABB");
            if (aabb == null) aabb = invokeNoArg(ship, "getWorldAABB");
            if (aabb == null) return null;
            Double minX = doubleNoArg(aabb, "minX");
            Double minY = doubleNoArg(aabb, "minY");
            Double minZ = doubleNoArg(aabb, "minZ");
            Double maxX = doubleNoArg(aabb, "maxX");
            Double maxY = doubleNoArg(aabb, "maxY");
            Double maxZ = doubleNoArg(aabb, "maxZ");
            if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) return null;
            Map<String, Object> size = new LinkedHashMap<>();
            size.put("x", Math.abs(maxX - minX));
            size.put("y", Math.abs(maxY - minY));
            size.put("z", Math.abs(maxZ - minZ));
            return size;
        }

        static @Nullable Object shipMass(Object ship) {
            Object inertia = invokeNoArg(ship, "getInertiaData");
            if (inertia == null) return null;
            Object mass = invokeNoArg(inertia, "getMass");
            return mass instanceof Number ? mass : null;
        }

        private static @Nullable Vec3 worldAabbCenter(Object ship) {
            Object aabb = invokeNoArg(ship, "getWorldAABB");
            if (aabb == null) return null;
            Double minX = doubleNoArg(aabb, "minX");
            Double minY = doubleNoArg(aabb, "minY");
            Double minZ = doubleNoArg(aabb, "minZ");
            Double maxX = doubleNoArg(aabb, "maxX");
            Double maxY = doubleNoArg(aabb, "maxY");
            Double maxZ = doubleNoArg(aabb, "maxZ");
            if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) return null;
            return new Vec3((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5);
        }

        static @Nullable Object invokeNoArg(@Nullable Object target, String methodName) {
            if (target == null) return null;
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static @Nullable Double doubleNoArg(Object target, String methodName) {
            Object value = invokeNoArg(target, methodName);
            return value instanceof Number number ? number.doubleValue() : null;
        }

        private static @Nullable Vec3 vectorObjectToVec3(@Nullable Object vector) {
            if (vector == null) return null;
            Double x = doubleNoArg(vector, "x");
            Double y = doubleNoArg(vector, "y");
            Double z = doubleNoArg(vector, "z");
            if (x == null || y == null || z == null) return null;
            return new Vec3(x, y, z);
        }

        private static void init() {
            if (initialized) return;
            initialized = true;
            try {
                Class<?> utils = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
                getShipsIntersecting = utils.getMethod("getShipsIntersecting", Level.class, AABB.class);
                getWorldCoordinates = utils.getMethod("getWorldCoordinates", Level.class, BlockPos.class, Vector3d.class);
            } catch (Throwable throwable) {
                VSElectronicWarfare.LOGGER.warn("Valkyrien Skies helper methods were not found; ship radar scans may be empty", throwable);
            }

            try {
                Class<?> raycast = Class.forName("org.valkyrienskies.mod.common.world.RaycastUtilsKt");
                for (Method method : raycast.getMethods()) {
                    if (!method.getName().equals("clipIncludeShips")) continue;
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length < 2 || !Level.class.isAssignableFrom(params[0]) || params[1] != ClipContext.class) continue;
                    if (params.length == 2) clipIncludeShips2 = method;
                    if (params.length == 3) clipIncludeShips3 = method;
                    if (params.length == 4) clipIncludeShips4 = method;
                }
            } catch (Throwable throwable) {
                VSElectronicWarfare.LOGGER.warn("VS clipIncludeShips was not found; LOS will only use vanilla blocks", throwable);
            }
        }
    }
}
