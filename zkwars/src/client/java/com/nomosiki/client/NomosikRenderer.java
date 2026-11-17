package com.nomosiki.client;

import com.nomosiki.client.model.NomosikHumanoidModel;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Vanilla {@link ModelLayers#PLAYER} / {@link ModelLayers#PLAYER_SLIM} models with custom nomosiki skins
 * ({@code assets/nomosiki/textures/entity/race/...} per {@link NomosikRace}).
 */
public class NomosikRenderer<T extends NomosikEntity> extends HumanoidMobRenderer<T, NomosikHumanoidModel<T>> {
	private final NomosikTextures.Profile textureProfile;
	private final NomosikHumanoidModel<T> wideModel;
	private final NomosikHumanoidModel<T> slimModel;

	public NomosikRenderer(EntityRendererProvider.Context context, NomosikTextures.Profile textureProfile) {
		super(context, new NomosikHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.textureProfile = textureProfile;
		this.wideModel = this.getModel();
		this.slimModel = new NomosikHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

		this.addLayer(new HumanoidArmorLayer<>(this,
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
			context.getModelManager()) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
				if (usesSlimArms(entity)) {
					return;
				}
				super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
			}
		});
		this.addLayer(new HumanoidArmorLayer<>(this,
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
			new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
			context.getModelManager()) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
				if (!usesSlimArms(entity)) {
					return;
				}
				super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
			}
		});
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
		this.addLayer(new NomosikFactionLayer<>(this));
	}

	static boolean usesSlimArms(NomosikEntity entity) {
		return false;
	}

	@Override
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		NomosikHumanoidModel<T> prevModel = this.getModel();
		boolean slim = usesSlimArms(entity);
		if (slim && prevModel != slimModel) {
			this.model = slimModel;
		} else if (!slim && prevModel != wideModel) {
			this.model = wideModel;
		}

		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
		this.model = prevModel;
	}

	@Override
	protected void scale(T entity, PoseStack poseStack, float partialTick) {
		float heightScale = entity.standingHeightScale();
		this.shadowRadius = 0.5F * heightScale;
		poseStack.scale(heightScale, heightScale, heightScale);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		ResourceManager resources = Minecraft.getInstance().getResourceManager();
		return NomosikTextures.resolveSkin(resources, entity, this.textureProfile);
	}
}
