package com.example.client;

import com.example.ZKWorkers;
import com.example.client.compat.FemaleGenderCompat;
import com.example.entity.HumanWorkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HumanWorkerRenderer extends MobRenderer<HumanWorkerEntity, PlayerModel<HumanWorkerEntity>> {
	private static final Map<ResourceLocation, Boolean> TEXTURE_PRESENT = new ConcurrentHashMap<>();
	private final String colorFolder;
	private final PlayerModel<HumanWorkerEntity> wideModel;
	private final PlayerModel<HumanWorkerEntity> slimModel;

	public HumanWorkerRenderer(EntityRendererProvider.Context context, String colorFolder) {
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.colorFolder = colorFolder;
		this.wideModel = this.model;
		this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
		FemaleGenderCompat.attach(this);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public void addExternalLayer(RenderLayer layer) {
		this.addLayer(layer);
	}

	@Override
	public void render(HumanWorkerEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		this.model = entity.isFemale() ? this.slimModel : this.wideModel;
		FemaleGenderCompat.prepare(entity);
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(HumanWorkerEntity entity) {
		String folder = entity.isFemale() ? this.colorFolder + "/female" : this.colorFolder;
		ResourceLocation colored = colored(folder, entity.factionColorId());
		if (colored != null) {
			return colored;
		}
		return ZKWorkers.id("textures/entity/human/" + folder + "/default.png");
	}

	private static ResourceLocation colored(String folder, String color) {
		if (color == null || color.isBlank()) {
			return null;
		}
		ResourceLocation location = ZKWorkers.id("textures/entity/human/" + folder + "/" + color + ".png");
		return exists(location) ? location : null;
	}

	public static void clearTextureCache() {
		TEXTURE_PRESENT.clear();
	}

	private static boolean exists(ResourceLocation location) {
		return TEXTURE_PRESENT.computeIfAbsent(location, id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
	}
}
