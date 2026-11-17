package com.nomosiki.rts;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * When a player enters RTS command view (tactical camera), their body stays anchored on the ground
 * so other players can still reach and damage them.
 */
public final class RtsCommandModeService {
	private static final Map<UUID, CommandAnchor> ANCHORS = new HashMap<>();

	private RtsCommandModeService() {
	}

	public record CommandAnchor(
		ResourceKey<Level> dimension,
		double x,
		double y,
		double z,
		float yRot,
		float xRot,
		float playerYRot,
		@Nullable UUID vehicleId
	) {
	}

	public static void setActive(ServerPlayer player, boolean active) {
		if (!active) {
			ANCHORS.remove(player.getUUID());
			return;
		}
		Entity vehicle = player.getVehicle();
		Vec3 pos = vehicle != null ? vehicle.position() : player.position();
		ANCHORS.put(player.getUUID(), new CommandAnchor(
			player.level().dimension(),
			pos.x,
			pos.y,
			pos.z,
			vehicle != null ? vehicle.getYRot() : player.getYRot(),
			player.getXRot(),
			player.getYRot(),
			vehicle != null ? vehicle.getUUID() : null
		));
	}

	public static boolean isActive(UUID playerId) {
		return ANCHORS.containsKey(playerId);
	}

	public static void onDisconnect(ServerPlayer player) {
		ANCHORS.remove(player.getUUID());
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			CommandAnchor anchor = ANCHORS.get(player.getUUID());
			if (anchor == null) {
				continue;
			}
			if (!player.isAlive()) {
				ANCHORS.remove(player.getUUID());
				continue;
			}
			if (!player.level().dimension().equals(anchor.dimension())) {
				ANCHORS.remove(player.getUUID());
				continue;
			}
			Entity vehicle = trackedVehicle(player, anchor);
			if (vehicle != null) {
				freezeAtAnchor(vehicle, anchor, true);
				lockLook(player, anchor);
				player.setDeltaMovement(Vec3.ZERO);
				player.fallDistance = 0.0f;
			} else {
				freezeAtAnchor(player, anchor, false);
				lockLook(player, anchor);
			}
		}
	}

	public static void clearAll() {
		ANCHORS.clear();
	}

	@Nullable
	private static Entity trackedVehicle(ServerPlayer player, CommandAnchor anchor) {
		if (anchor.vehicleId() == null) {
			return null;
		}
		Entity vehicle = player.getVehicle();
		if (vehicle != null && vehicle.getUUID().equals(anchor.vehicleId())) {
			return vehicle;
		}
		return null;
	}

	private static void freezeAtAnchor(Entity entity, CommandAnchor anchor, boolean lockYaw) {
		entity.setDeltaMovement(Vec3.ZERO);
		entity.fallDistance = 0.0f;
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
			mob.setSpeed(0f);
		}
		if (entity instanceof LivingEntity living) {
			living.xxa = 0f;
			living.zza = 0f;
		}
		if (entity.distanceToSqr(anchor.x, anchor.y, anchor.z) > 1.0E-4) {
			entity.teleportTo(anchor.x, anchor.y, anchor.z);
		}
		if (lockYaw) {
			entity.setYRot(anchor.yRot());
			entity.yRotO = anchor.yRot();
		}
	}

	private static void lockLook(ServerPlayer player, CommandAnchor anchor) {
		if (Math.abs(player.getYRot() - anchor.playerYRot()) > 0.01f || Math.abs(player.getXRot() - anchor.xRot()) > 0.01f) {
			player.setYRot(anchor.playerYRot());
			player.setXRot(anchor.xRot());
			player.yRotO = anchor.playerYRot();
			player.xRotO = anchor.xRot();
		}
	}
}
