package com.example;

import com.example.advancement.WorkerAdvancements;
import com.example.compat.WartimeGate;
import com.example.entity.HumanWorkerEntity;
import com.example.network.WorkerNetworking;
import com.example.registry.ModBlockEntities;
import com.example.registry.ModBlocks;
import com.example.registry.ModCreativeTabs;
import com.example.registry.ModEntities;
import com.example.registry.ModItems;
import com.example.registry.ModMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZKWorkers implements ModInitializer {
	public static final String MOD_ID = "zkwars-workers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		WorkerAdvancements.register();
		ModBlocks.register();
		ModBlockEntities.register();
		ModMenus.register();
		ModEntities.register();
		ModItems.register();
		ModCreativeTabs.register();
		ModEntities.registerAttributes();
		WorkerNetworking.register();
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!ModBlocks.isBed(world.getBlockState(hitResult.getBlockPos()))) {
				return InteractionResult.PASS;
			}
			HumanWorkerEntity worker = HumanWorkerEntity.sleepingAt(world, hitResult.getBlockPos());
			if (worker == null) {
				return InteractionResult.PASS;
			}
			return worker.mobInteract(player, hand);
		});
		ServerTickEvents.END_SERVER_TICK.register(WartimeGate::tick);
		LOGGER.info("ZK Workers loaded");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
