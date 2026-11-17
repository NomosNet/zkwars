package com.example.client;

import com.example.ZKWorkers;
import com.example.client.gui.GuiStyle;
import com.example.client.gui.ZkGuiTheme;
import com.example.registry.ModEntities;
import com.example.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class ZKWorkersClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ZkGuiTheme.bind(GuiStyle::apply);
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return ZKWorkers.id("worker_textures");
			}

			@Override
			public void onResourceManagerReload(ResourceManager resourceManager) {
				HumanWorkerRenderer.clearTextureCache();
			}
		});
		MenuScreens.register(ModMenus.WORKER, WorkerScreen::new);
		MenuScreens.register(ModMenus.WORKER_INSPECT, WorkerInspectScreen::new);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(QuarryOutline::render);
		register("miner", ModEntities.HUMAN_MINER);
		register("farmer", ModEntities.HUMAN_FARMER);
		register("lumberjack", ModEntities.HUMAN_LUMBERJACK);
		register("courier", ModEntities.HUMAN_COURIER);
		register("cow", ModEntities.HUMAN_COW_BREEDER);
		register("chicken", ModEntities.HUMAN_CHICKEN_BREEDER);
		register("sheep", ModEntities.HUMAN_SHEEP_BREEDER);
		register("pig", ModEntities.HUMAN_PIG_BREEDER);
		register("builder", ModEntities.HUMAN_BUILDER);
		register("smelter", ModEntities.HUMAN_SMELTER);
		register("fisher", ModEntities.HUMAN_FISHER);
		register("crafter", ModEntities.HUMAN_CRAFTER);
	}

	private static void register(String folder, net.minecraft.world.entity.EntityType<com.example.entity.HumanWorkerEntity> type) {
		EntityRendererRegistry.register(type, context -> new HumanWorkerRenderer(context, folder));
	}
}
