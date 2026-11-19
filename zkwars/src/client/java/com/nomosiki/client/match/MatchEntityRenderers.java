package com.nomosiki.client.match;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import com.nomosiki.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class MatchEntityRenderers {
	private static final ResourceLocation WHITE =
		ResourceLocation.withDefaultNamespace("textures/misc/white.png");

	private MatchEntityRenderers() {
	}

	public static void register() {
		EntityRendererRegistry.register(ModEntities.BUILDING_HEART, HeartFlagRenderer::new);
		EntityRendererRegistry.register(ModEntities.CAPTURE_BEACON, BeaconFlagRenderer::new);
	}

	static float[] sideRgb(byte side) {
		if (side == 0) {
			return new float[] {0.23f, 0.51f, 0.96f};
		}
		if (side == 1) {
			return new float[] {0.94f, 0.27f, 0.27f};
		}
		return new float[] {0.61f, 0.64f, 0.69f};
	}

	static void drawFlag(
		PoseStack pose,
		MultiBufferSource buffers,
		byte side,
		float poleHeight,
		float clothW,
		float clothH
	) {
		VertexConsumer lines = buffers.getBuffer(RenderType.lines());
		Matrix4f mat = pose.last().pose();
		float[] rgb = sideRgb(side);
		// Pole
		line(lines, mat, 0f, 0f, 0f, 0f, poleHeight, 0f, 0.2f, 0.15f, 0.1f, 1f);
		// Cloth outline rectangle
		float y0 = poleHeight - clothH;
		float y1 = poleHeight;
		float x1 = clothW;
		line(lines, mat, 0f, y1, 0f, x1, y1, 0f, rgb[0], rgb[1], rgb[2], 1f);
		line(lines, mat, x1, y1, 0f, x1, y0, 0f, rgb[0], rgb[1], rgb[2], 1f);
		line(lines, mat, x1, y0, 0f, 0f, y0, 0f, rgb[0], rgb[1], rgb[2], 1f);
		line(lines, mat, 0f, y0, 0f, 0f, y1, 0f, rgb[0], rgb[1], rgb[2], 1f);
		// Cross fill lines for visibility
		for (int i = 1; i <= 4; i++) {
			float t = i / 5f;
			float y = y0 + clothH * t;
			line(lines, mat, 0.02f, y, 0f, x1 - 0.02f, y, 0f, rgb[0], rgb[1], rgb[2], 0.85f);
		}
	}

	private static void line(
		VertexConsumer buf,
		Matrix4f mat,
		float x0, float y0, float z0,
		float x1, float y1, float z1,
		float r, float g, float b, float a
	) {
		buf.addVertex(mat, x0, y0, z0).setColor(r, g, b, a).setNormal(0f, 1f, 0f);
		buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(0f, 1f, 0f);
	}

	private static final class HeartFlagRenderer extends EntityRenderer<BuildingHeartEntity> {
		HeartFlagRenderer(EntityRendererProvider.Context context) {
			super(context);
			this.shadowRadius = 0.4f;
		}

		@Override
		public void render(BuildingHeartEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
			pose.pushPose();
			drawFlag(pose, buffers, entity.matchSide(), 2.6f, 1.2f, 0.75f);
			pose.popPose();
			super.render(entity, yaw, partial, pose, buffers, light);
		}

		@Override
		public ResourceLocation getTextureLocation(BuildingHeartEntity entity) {
			return WHITE;
		}
	}

	private static final class BeaconFlagRenderer extends EntityRenderer<CaptureBeaconEntity> {
		BeaconFlagRenderer(EntityRendererProvider.Context context) {
			super(context);
			this.shadowRadius = 0.35f;
		}

		@Override
		public void render(CaptureBeaconEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
			pose.pushPose();
			byte side = entity.ownerSide() != CaptureBeaconEntity.SIDE_NONE
				? entity.ownerSide()
				: entity.capturingSide();
			drawFlag(pose, buffers, side, 2.1f, 0.95f, 0.6f);
			pose.popPose();
			super.render(entity, yaw, partial, pose, buffers, light);
		}

		@Override
		public ResourceLocation getTextureLocation(CaptureBeaconEntity entity) {
			return WHITE;
		}
	}
}
