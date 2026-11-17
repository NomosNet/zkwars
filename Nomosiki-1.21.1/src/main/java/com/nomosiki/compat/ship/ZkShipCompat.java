package com.nomosiki.compat.ship;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Soft compat with zk-ship cushions. The access class is loaded only when that mod is present.
 */
public final class ZkShipCompat {
	public static final String MOD_ID = "zk-ship";

	private ZkShipCompat() {
	}

	public static boolean isLoaded() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static boolean isSeated(Entity entity) {
		if (!isLoaded() || entity == null || !entity.isPassenger()) {
			return false;
		}
		return ZkShipCompatAccess.isSeatVehicle(entity.getVehicle());
	}

	public static int trySeatNearby(Collection<? extends LivingEntity> riders, ServerLevel level, double radius) {
		if (!isLoaded() || riders == null || riders.isEmpty()) {
			return 0;
		}
		return ZkShipCompatAccess.seatNearby(riders, level, radius);
	}

	public static boolean isHelmPilot(Entity entity) {
		if (!isLoaded() || entity == null) {
			return false;
		}
		return ZkShipHelmAccess.isHelmPilot(entity);
	}

	public static boolean shipNearby(ServerLevel level, Vec3 origin, double radius) {
		if (!isLoaded() || level == null || origin == null) {
			return false;
		}
		return ZkShipHelmAccess.shipNearby(level, origin, radius);
	}

	public static Vec3 nearestFreeHelm(ServerLevel level, Vec3 origin, double radius) {
		if (!isLoaded() || level == null || origin == null) {
			return null;
		}
		return ZkShipHelmAccess.nearestFreeHelm(level, origin, radius);
	}

	public static boolean claimNearbyHelm(LivingEntity rider, double reach) {
		if (!isLoaded() || rider == null) {
			return false;
		}
		return ZkShipHelmAccess.claimNearby(rider, reach);
	}

	public static void steerHelm(LivingEntity pilot, float forward) {
		if (!isLoaded() || pilot == null) {
			return;
		}
		ZkShipHelmAccess.steer(pilot, forward);
	}

	public static void releaseHelm(LivingEntity pilot) {
		if (!isLoaded() || pilot == null) {
			return;
		}
		ZkShipHelmAccess.release(pilot);
	}
}
