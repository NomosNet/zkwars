package com.nomosiki.client.rts;

import com.nomosiki.client.SquadClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class OrderMarkerRenderer {
	private static volatile List<BlockPos> routePreview = List.of();

	private OrderMarkerRenderer() {
	}

	public static void setRoutePreview(List<BlockPos> waypoints) {
		routePreview = List.copyOf(waypoints);
	}

	public static void spawnParticles(Minecraft mc, BlockPos anchor) {
		if (mc.level == null) {
			return;
		}
		Vec3 center = Vec3.atCenterOf(anchor);
		RandomSource random = RandomSource.create();
		for (int i = 0; i < 24; i++) {
			double ox = (random.nextDouble() - 0.5) * 0.9;
			double oy = random.nextDouble() * 0.9;
			double oz = (random.nextDouble() - 0.5) * 0.9;
			mc.level.addParticle(ParticleTypes.ENCHANT, center.x + ox, center.y + oy, center.z + oz, 0.0, 0.04, 0.0);
		}
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			return;
		}
		RtsClientState.lastOrderAnchor().ifPresent(anchor -> {
			if (mc.level.getGameTime() % 8L != 0L) {
				return;
			}
			Vec3 center = Vec3.atCenterOf(anchor);
			mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.2, center.z, 0.0, 0.02, 0.0);
		});

		List<BlockPos> route = RtsClientState.waypointsForActiveSquad();
		if (route.isEmpty()) {
			route = routePreview;
		}
		if (route.isEmpty() || mc.level.getGameTime() % 6L != 0L) {
			return;
		}
		for (BlockPos pos : route) {
			Vec3 center = Vec3.atCenterOf(pos);
			mc.level.addParticle(ParticleTypes.END_ROD, center.x, center.y + 0.15, center.z, 0.0, 0.01, 0.0);
		}
		for (int i = 1; i < route.size(); i++) {
			Vec3 from = Vec3.atCenterOf(route.get(i - 1));
			Vec3 to = Vec3.atCenterOf(route.get(i));
			Vec3 mid = from.lerp(to, 0.5);
			mc.level.addParticle(ParticleTypes.ENCHANT, mid.x, mid.y + 0.1, mid.z, 0.0, 0.02, 0.0);
		}
	}
}
