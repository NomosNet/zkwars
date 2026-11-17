package com.nomosiki.work.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nomosiki.work.NomosikiWork;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BulletRenderer extends EntityRenderer<BulletEntity> {
	public static final ResourceLocation TEXTURE = NomosikiWork.id("textures/entity/bullet.png");
	public static final ResourceLocation TEXTURE_FIRE = NomosikiWork.id("textures/entity/bullet_fire.png");

	public BulletRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(BulletEntity bullet) {
		return TEXTURE;
	}

	@Override
	public void render(BulletEntity bullet, float yaw, float dt, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
		if (bullet.isFirstTick()) {
			return;
		}
		poseStack.pushPose();
		if (bullet.pelletCount() == 1) {
			poseStack.scale(0.1f, 0.1f, 0.1f);
		} else {
			poseStack.scale(bullet.isOnFire() ? 0.075f : 0.05f, 0.05f, 0.05f);
		}
		poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		PoseStack.Pose pose = poseStack.last();
		RenderType renderType = RenderType.entityCutoutNoCull(bullet.isOnFire() ? TEXTURE_FIRE : TEXTURE);
		VertexConsumer builder = bufferSource.getBuffer(renderType);
		addVertex(builder, pose, -1, -1, 0, 0, 1, 0, 0, 1, light);
		addVertex(builder, pose, 1, -1, 0, 1, 1, 0, 0, 1, light);
		addVertex(builder, pose, 1, 1, 0, 1, 0, 0, 0, 1, light);
		addVertex(builder, pose, -1, 1, 0, 0, 0, 0, 0, 1, light);
		poseStack.popPose();
		super.render(bullet, yaw, dt, poseStack, bufferSource, light);
	}

	private void addVertex(
		VertexConsumer builder,
		PoseStack.Pose pose,
		float x,
		float y,
		float z,
		float u,
		float v,
		float nx,
		float ny,
		float nz,
		int light
	) {
		builder.addVertex(pose, x, y, z)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, nx, ny, nz);
	}
}
