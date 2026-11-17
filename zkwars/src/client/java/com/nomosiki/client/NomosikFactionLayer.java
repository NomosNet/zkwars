package com.nomosiki.client;

import com.nomosiki.entity.NomosikEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public final class NomosikFactionLayer<T extends NomosikEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
	public NomosikFactionLayer(RenderLayerParent<T, M> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.getOwnerUUID() == null) {
			return;
		}
		ResourceLocation texture = FactionClientState.getTextureForPlayer(entity.getOwnerUUID());
		if (texture == null || Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
			return;
		}
		coloredCutoutModelCopyLayerRender(
			this.getParentModel(),
			this.getParentModel(),
			texture,
			poseStack,
			buffer,
			packedLight,
			entity,
			limbSwing,
			limbSwingAmount,
			ageInTicks,
			netHeadYaw,
			headPitch,
			partialTick,
			-1
		);
	}
}
