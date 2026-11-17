package com.nomosiki.compat.ship;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Loaded only from helm calls after {@link ZkShipCompat#isLoaded()} is true.
 * A missing helm API leaves the handles unset so cushion seating still works.
 */
final class ZkShipHelmAccess {
	private static final MethodHandle IS_HELM_PILOT;
	private static final MethodHandle SHIP_NEARBY;
	private static final MethodHandle NEAREST_FREE_HELM;
	private static final MethodHandle CLAIM_NEARBY;
	private static final MethodHandle STEER;
	private static final MethodHandle RELEASE;

	static {
		MethodHandle pilot = null;
		MethodHandle nearby = null;
		MethodHandle nearest = null;
		MethodHandle claim = null;
		MethodHandle steer = null;
		MethodHandle release = null;
		try {
			Class<?> seating = Class.forName("com.zkshogun.zkship.ship.HelmSeating");
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			pilot = lookup.findStatic(seating, "isHelmPilot", MethodType.methodType(boolean.class, Entity.class));
			nearby = lookup.findStatic(
				seating, "shipNearby", MethodType.methodType(boolean.class, ServerLevel.class, Vec3.class, double.class));
			nearest = lookup.findStatic(
				seating, "nearestFreeHelm", MethodType.methodType(Vec3.class, ServerLevel.class, Vec3.class, double.class));
			claim = lookup.findStatic(
				seating, "claimNearby", MethodType.methodType(boolean.class, LivingEntity.class, double.class));
			steer = lookup.findStatic(
				seating, "steer", MethodType.methodType(void.class, LivingEntity.class, float.class));
			release = lookup.findStatic(seating, "release", MethodType.methodType(void.class, LivingEntity.class));
		} catch (ReflectiveOperationException ignored) {
			pilot = null;
			nearby = null;
			nearest = null;
			claim = null;
			steer = null;
			release = null;
		}
		IS_HELM_PILOT = pilot;
		SHIP_NEARBY = nearby;
		NEAREST_FREE_HELM = nearest;
		CLAIM_NEARBY = claim;
		STEER = steer;
		RELEASE = release;
	}

	private ZkShipHelmAccess() {
	}

	static boolean isHelmPilot(Entity entity) {
		if (IS_HELM_PILOT == null) {
			return false;
		}
		try {
			return (boolean) IS_HELM_PILOT.invoke(entity);
		} catch (Throwable e) {
			return false;
		}
	}

	static boolean shipNearby(ServerLevel level, Vec3 origin, double radius) {
		if (SHIP_NEARBY == null) {
			return false;
		}
		try {
			return (boolean) SHIP_NEARBY.invoke(level, origin, radius);
		} catch (Throwable e) {
			return false;
		}
	}

	static Vec3 nearestFreeHelm(ServerLevel level, Vec3 origin, double radius) {
		if (NEAREST_FREE_HELM == null) {
			return null;
		}
		try {
			return (Vec3) NEAREST_FREE_HELM.invoke(level, origin, radius);
		} catch (Throwable e) {
			return null;
		}
	}

	static boolean claimNearby(LivingEntity rider, double reach) {
		if (CLAIM_NEARBY == null) {
			return false;
		}
		try {
			return (boolean) CLAIM_NEARBY.invoke(rider, reach);
		} catch (Throwable e) {
			return false;
		}
	}

	static void steer(LivingEntity pilot, float forward) {
		if (STEER == null) {
			return;
		}
		try {
			STEER.invoke(pilot, forward);
		} catch (Throwable ignored) {
		}
	}

	static void release(LivingEntity pilot) {
		if (RELEASE == null) {
			return;
		}
		try {
			RELEASE.invoke(pilot);
		} catch (Throwable ignored) {
		}
	}
}
