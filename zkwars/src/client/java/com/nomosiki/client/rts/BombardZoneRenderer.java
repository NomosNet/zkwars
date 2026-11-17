package com.nomosiki.client.rts;

import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Ground frame for a bombardment zone. The aim preview stays while the gesture is held.
 * A placed frame fades out over 15 seconds; the squad keeps firing after it is gone.
 */
public final class BombardZoneRenderer {
	private static final long FADE_TICKS = 15L * 20L;

	private static final Map<ZoneKey, Long> seenAt = new HashMap<>();

	private BombardZoneRenderer() {
	}

	/** Start the fade clock when the player places a zone, before the army sync arrives. */
	public static void notePlaced(BlockPos anchor, int radius) {
		if (anchor == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		long now = mc.level == null ? 0L : mc.level.getGameTime();
		seenAt.put(new ZoneKey(anchor.immutable(), radius), now);
	}

	public static void render(WorldRenderContext context) {
		if (context.consumers() == null || context.matrixStack() == null) {
			return;
		}
		var pose = context.matrixStack();
		Vec3 cam = context.camera().getPosition();
		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		var lines = context.consumers().getBuffer(RenderType.lines());

		if (RtsClientState.bombardDesignate()) {
			BlockPos aim = previewAnchor();
			if (aim != null) {
				drawZone(pose, lines, aim, RtsClientState.bombardRadius(), 1.0F, 0.62F, 0.22F, 0.7F, false);
			}
		}

		Minecraft mc = Minecraft.getInstance();
		long now = mc.level == null ? 0L : mc.level.getGameTime();
		Set<ZoneKey> live = new HashSet<>();
		for (SquadArmyStatePayload.SquadRow row : RtsClientState.armySquads()) {
			if (!row.hasFireZone()
				|| NomosikRangedFireMode.fromId(row.rangedFireModeId()) != NomosikRangedFireMode.STRATEGIC_FIRE) {
				continue;
			}
			BlockPos anchor = row.fireAnchor();
			if (anchor == null) {
				continue;
			}
			ZoneKey key = new ZoneKey(anchor.immutable(), row.fireRadius());
			if (!live.add(key)) {
				continue;
			}
			long start = seenAt.computeIfAbsent(key, ignored -> now);
			long age = now - start;
			if (age >= FADE_TICKS) {
				continue;
			}
			float alpha = 0.95F * (1.0F - age / (float) FADE_TICKS);
			drawZone(pose, lines, anchor, row.fireRadius(), 0.92F, 0.32F, 0.14F, alpha, true);
		}
		seenAt.entrySet().removeIf(entry -> !live.contains(entry.getKey()) && now - entry.getValue() >= FADE_TICKS);
		pose.popPose();
	}

	private static BlockPos previewAnchor() {
		if (RtsClientState.bombardDragActive()) {
			return RtsClientState.bombardDragCenter();
		}
		BlockPos hover = RtsClientState.bombardHover();
		if (hover != null) {
			return hover;
		}
		return RtsOrderInputHandler.raycastAnchor();
	}

	private static void drawZone(
		com.mojang.blaze3d.vertex.PoseStack pose,
		com.mojang.blaze3d.vertex.VertexConsumer lines,
		BlockPos anchor,
		int radius,
		float red,
		float green,
		float blue,
		float alpha,
		boolean pillar
	) {
		int span = Math.max(1, radius);
		double minX = anchor.getX() - span;
		double maxX = anchor.getX() + 1.0 + span;
		double minZ = anchor.getZ() - span;
		double maxZ = anchor.getZ() + 1.0 + span;
		double y = anchor.getY() + 1.02;
		AABB footprint = new AABB(minX, y, minZ, maxX, y + 0.05, maxZ);
		LevelRenderer.renderLineBox(pose, lines, footprint, red, green, blue, alpha);
		if (!pillar) {
			return;
		}
		double cx = (minX + maxX) * 0.5;
		double cz = (minZ + maxZ) * 0.5;
		AABB post = new AABB(cx - 0.08, y, cz - 0.08, cx + 0.08, y + 1.35, cz + 0.08);
		LevelRenderer.renderLineBox(pose, lines, post, red, green, blue, alpha);
	}

	private record ZoneKey(BlockPos anchor, int radius) {
	}
}
