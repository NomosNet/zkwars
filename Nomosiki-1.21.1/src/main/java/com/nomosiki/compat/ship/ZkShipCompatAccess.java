package com.nomosiki.compat.ship;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;

/**
 * Loaded only after {@link ZkShipCompat#isLoaded()} is true, so zk-ship classes stay unloaded otherwise.
 */
final class ZkShipCompatAccess {
	private static final MethodHandle SEAT_NEARBY;
	private static final MethodHandle IS_SEAT_VEHICLE;

	static {
		try {
			Class<?> seating = Class.forName("com.zkshogun.zkship.ship.CushionSeating");
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			SEAT_NEARBY = lookup.findStatic(
				seating,
				"seatNearby",
				MethodType.methodType(int.class, Collection.class, ServerLevel.class, double.class)
			);
			IS_SEAT_VEHICLE = lookup.findStatic(
				seating,
				"isSeatVehicle",
				MethodType.methodType(boolean.class, Entity.class)
			);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("zk-ship cushion seating is missing", e);
		}
	}

	private ZkShipCompatAccess() {
	}

	static boolean isSeatVehicle(Entity vehicle) {
		try {
			return (boolean) IS_SEAT_VEHICLE.invoke(vehicle);
		} catch (Throwable e) {
			return false;
		}
	}

	static int seatNearby(Collection<? extends LivingEntity> riders, ServerLevel level, double radius) {
		try {
			return (int) SEAT_NEARBY.invoke(riders, level, radius);
		} catch (Throwable e) {
			return 0;
		}
	}
}
