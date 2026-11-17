package com.example.registry;

import com.example.ZKWorkers;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
	public static final CreativeModeTab MAIN = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			ZKWorkers.id("main"),
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.zkwars-workers.main"))
					.icon(() -> new ItemStack(ModItems.WORKER_SCROLL))
					.displayItems((parameters, output) -> {
						output.accept(ModItems.QUARRY);
						output.accept(ModItems.FARMER);
						output.accept(ModItems.LUMBERJACK);
						output.accept(ModItems.CHICKEN_BREEDER);
						output.accept(ModItems.COW_BREEDER);
						output.accept(ModItems.SHEEP_BREEDER);
						output.accept(ModItems.PIG_BREEDER);
						output.accept(ModItems.BUILDER);
						output.accept(ModItems.SMELTER);
						output.accept(ModItems.FISHER);
						output.accept(ModItems.CRAFTER);
						output.accept(ModItems.BUILDER_HAMMER);
						output.accept(ModItems.SMELTER_SCROLL);
						output.accept(ModItems.WORKER_SCROLL);
						output.accept(ModItems.COURIER_SCROLL);
						output.accept(ModItems.COURIER_SUPPLY_SCROLL);
						output.accept(ModItems.COURIER_SORT_SCROLL);
						output.accept(ModItems.GUILD_COINS);
						output.accept(ModItems.HUMAN_MINER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_FARMER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_LUMBERJACK_SPAWN_EGG);
						output.accept(ModItems.HUMAN_COURIER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_COW_BREEDER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_CHICKEN_BREEDER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_SHEEP_BREEDER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_PIG_BREEDER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_BUILDER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_SMELTER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_FISHER_SPAWN_EGG);
						output.accept(ModItems.HUMAN_CRAFTER_SPAWN_EGG);
					})
					.build()
	);

	private ModCreativeTabs() {
	}

	public static void register() {
		ZKWorkers.LOGGER.debug("Registered creative tab {}", MAIN);
	}
}
