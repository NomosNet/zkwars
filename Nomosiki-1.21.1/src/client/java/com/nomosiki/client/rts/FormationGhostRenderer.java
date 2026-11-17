package com.nomosiki.client.rts;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FormationGhostRenderer {
	private static List<Vec3> slots = List.of();
	private static @Nullable Vec3 frontA;
	private static @Nullable Vec3 frontB;

	private FormationGhostRenderer() {
	}

	public static void setPreview(List<Vec3> previewSlots, @Nullable Vec3 a, @Nullable Vec3 b) {
		slots = previewSlots == null ? List.of() : List.copyOf(previewSlots);
		frontA = a;
		frontB = b;
	}

	public static void clear() {
		slots = List.of();
		frontA = null;
		frontB = null;
	}

	public static boolean hasPreview() {
		return !slots.isEmpty() || (frontA != null && frontB != null);
	}

	public static void render(WorldRenderContext context) {
		if (!TacticalCameraController.isActive() || !hasPreview()) {
			return;
		}
		if (context.consumers() == null || context.matrixStack() == null) {
			return;
		}
		var pose = context.matrixStack();
		Vec3 cam = context.camera().getPosition();
		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		var lines = context.consumers().getBuffer(RenderType.lines());
		for (Vec3 slot : slots) {
			AABB box = new AABB(
				slot.x - 0.22, slot.y, slot.z - 0.22,
				slot.x + 0.22, slot.y + 0.12, slot.z + 0.22
			);
			LevelRenderer.renderLineBox(pose, lines, box, 1.0f, 0.82f, 0.28f, 0.95f);
		}
		if (frontA != null && frontB != null) {
			AABB front = new AABB(
				Math.min(frontA.x, frontB.x), Math.min(frontA.y, frontB.y), Math.min(frontA.z, frontB.z),
				Math.max(frontA.x, frontB.x), Math.max(frontA.y, frontB.y) + 0.08, Math.max(frontA.z, frontB.z)
			).inflate(0.04);
			LevelRenderer.renderLineBox(pose, lines, front, 0.95f, 0.35f, 0.45f, 0.9f);
		}
		pose.popPose();
	}
}
