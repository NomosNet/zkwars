package com.nomosiki.client.match;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CaptureProgressRenderer {
	private CaptureProgressRenderer() {
	}

	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || context.consumers() == null || context.matrixStack() == null) {
			return;
		}
		double radius = NomosikiServerConfig.INSTANCE.match.captureRadius;
		PoseStack pose = context.matrixStack();
		Vec3 cam = context.camera().getPosition();
		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		VertexConsumer lines = context.consumers().getBuffer(RenderType.lines());
		Matrix4f mat = pose.last().pose();

		for (CaptureBeaconEntity beacon : mc.level.getEntitiesOfClass(
			CaptureBeaconEntity.class,
			mc.player == null
				? new net.minecraft.world.phys.AABB(cam.x - 128, cam.y - 64, cam.z - 128, cam.x + 128, cam.y + 64, cam.z + 128)
				: mc.player.getBoundingBox().inflate(128.0)
		)) {
			double cx = beacon.getX();
			double cy = beacon.getY() + 0.05;
			double cz = beacon.getZ();
			byte colorSide = beacon.capturingSide() != CaptureBeaconEntity.SIDE_NONE
				? beacon.capturingSide()
				: beacon.ownerSide();
			float[] rgb = MatchEntityRenderers.sideRgb(colorSide);
			drawCircle(lines, mat, cx, cy, cz, radius, 48, rgb[0], rgb[1], rgb[2], 0.45f);

			float progress = Math.max(0f, Math.min(100f, beacon.progress())) / 100f;
			if (progress > 0.001f) {
				drawArc(lines, mat, cx, cy + 0.02, cz, radius * 0.92, progress, 64, rgb[0], rgb[1], rgb[2], 0.95f);
			}
			if (beacon.holdTicksRemaining() > 0) {
				drawCircle(lines, mat, cx, cy + 0.04, cz, radius * 0.75, 40, 1f, 0.9f, 0.2f, 0.9f);
			}
			if (beacon.ownerSide() != CaptureBeaconEntity.SIDE_NONE) {
				float[] own = MatchEntityRenderers.sideRgb(beacon.ownerSide());
				drawCircle(lines, mat, cx, cy + 0.01, cz, radius * 0.35, 24, own[0], own[1], own[2], 0.7f);
			}
		}
		pose.popPose();
	}

	private static void drawCircle(
		VertexConsumer buf,
		Matrix4f mat,
		double cx, double cy, double cz,
		double radius,
		int segments,
		float r, float g, float b, float a
	) {
		for (int i = 0; i < segments; i++) {
			double a0 = (Math.PI * 2.0 * i) / segments;
			double a1 = (Math.PI * 2.0 * (i + 1)) / segments;
			buf.addVertex(mat, (float) (cx + Math.cos(a0) * radius), (float) cy, (float) (cz + Math.sin(a0) * radius))
				.setColor(r, g, b, a).setNormal(0f, 1f, 0f);
			buf.addVertex(mat, (float) (cx + Math.cos(a1) * radius), (float) cy, (float) (cz + Math.sin(a1) * radius))
				.setColor(r, g, b, a).setNormal(0f, 1f, 0f);
		}
	}

	private static void drawArc(
		VertexConsumer buf,
		Matrix4f mat,
		double cx, double cy, double cz,
		double radius,
		float fraction,
		int segments,
		float r, float g, float b, float a
	) {
		int count = Math.max(2, Math.round(segments * fraction));
		for (int i = 0; i < count; i++) {
			double a0 = (Math.PI * 2.0 * i) / segments;
			double a1 = (Math.PI * 2.0 * (i + 1)) / segments;
			buf.addVertex(mat, (float) (cx + Math.cos(a0) * radius), (float) cy, (float) (cz + Math.sin(a0) * radius))
				.setColor(r, g, b, a).setNormal(0f, 1f, 0f);
			buf.addVertex(mat, (float) (cx + Math.cos(a1) * radius), (float) cy, (float) (cz + Math.sin(a1) * radius))
				.setColor(r, g, b, a).setNormal(0f, 1f, 0f);
		}
	}
}
