package com.example.registry;

import com.example.ZKWorkers;
import com.example.item.CourierScrollItem;
import com.example.item.CourierSortScrollItem;
import com.example.item.HammerItem;
import com.example.item.SmelterScrollItem;
import com.example.item.WorkerScrollItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	public static final Item QUARRY = blockItem("quarry_block", ModBlocks.QUARRY);
	public static final Item FARMER = blockItem("farmer_block", ModBlocks.FARMER);
	public static final Item LUMBERJACK = blockItem("lumberjack_block", ModBlocks.LUMBERJACK);
	public static final Item CHICKEN_BREEDER = blockItem("chicken_breeder_block", ModBlocks.CHICKEN_BREEDER);
	public static final Item COW_BREEDER = blockItem("cow_breeder_block", ModBlocks.COW_BREEDER);
	public static final Item SHEEP_BREEDER = blockItem("sheep_breeder_block", ModBlocks.SHEEP_BREEDER);
	public static final Item PIG_BREEDER = blockItem("pig_breeder_block", ModBlocks.PIG_BREEDER);
	public static final Item BUILDER = blockItem("builder_block", ModBlocks.BUILDER);
	public static final Item SMELTER = blockItem("smelter_block", ModBlocks.SMELTER);
	public static final Item FISHER = blockItem("fisher_block", ModBlocks.FISHER);
	public static final Item CRAFTER = blockItem("crafter_block", ModBlocks.CRAFTER);
	public static final Item BUILDER_HAMMER = item("builder_hammer", new HammerItem(new Item.Properties()));
	public static final Item SMELTER_SCROLL = item("smelter_scroll", new SmelterScrollItem(new Item.Properties()));

	public static final Item WORKER_SCROLL = item("worker_scroll", new WorkerScrollItem(new Item.Properties()));
	public static final Item COURIER_SCROLL = item("courier_scroll", new CourierScrollItem(new Item.Properties(), "item.zkwars-workers.courier_scroll"));
	public static final Item COURIER_SUPPLY_SCROLL = item("courier_supply_scroll", new CourierScrollItem(new Item.Properties(), "item.zkwars-workers.courier_supply_scroll"));
	public static final Item COURIER_SORT_SCROLL = item("courier_sort_scroll", new CourierSortScrollItem(new Item.Properties(), "item.zkwars-workers.courier_sort_scroll"));
	public static final Item GUILD_COINS = item("guild_coins", new Item(new Item.Properties()));

	public static final Item HUMAN_MINER_SPAWN_EGG = egg("human_miner_spawn_egg", ModEntities.HUMAN_MINER, 0x8D6E63, 0x3E2723);
	public static final Item HUMAN_FARMER_SPAWN_EGG = egg("human_farmer_spawn_egg", ModEntities.HUMAN_FARMER, 0x7CB342, 0x33691E);
	public static final Item HUMAN_LUMBERJACK_SPAWN_EGG = egg("human_lumberjack_spawn_egg", ModEntities.HUMAN_LUMBERJACK, 0xA1887F, 0x4E342E);
	public static final Item HUMAN_COURIER_SPAWN_EGG = egg("human_courier_spawn_egg", ModEntities.HUMAN_COURIER, 0x42A5F5, 0x1565C0);
	public static final Item HUMAN_COW_BREEDER_SPAWN_EGG = egg("human_cow_breeder_spawn_egg", ModEntities.HUMAN_COW_BREEDER, 0xEFEBE9, 0x5D4037);
	public static final Item HUMAN_CHICKEN_BREEDER_SPAWN_EGG = egg("human_chicken_breeder_spawn_egg", ModEntities.HUMAN_CHICKEN_BREEDER, 0xFFF59D, 0xF9A825);
	public static final Item HUMAN_SHEEP_BREEDER_SPAWN_EGG = egg("human_sheep_breeder_spawn_egg", ModEntities.HUMAN_SHEEP_BREEDER, 0xECEFF1, 0x546E7A);
	public static final Item HUMAN_PIG_BREEDER_SPAWN_EGG = egg("human_pig_breeder_spawn_egg", ModEntities.HUMAN_PIG_BREEDER, 0xF48FB1, 0xAD1457);
	public static final Item HUMAN_BUILDER_SPAWN_EGG = egg("human_builder_spawn_egg", ModEntities.HUMAN_BUILDER, 0xBCAAA4, 0x4E342E);
	public static final Item HUMAN_SMELTER_SPAWN_EGG = egg("human_smelter_spawn_egg", ModEntities.HUMAN_SMELTER, 0xFF8F3F, 0x4E342E);
	public static final Item HUMAN_FISHER_SPAWN_EGG = egg("human_fisher_spawn_egg", ModEntities.HUMAN_FISHER, 0x4FC3F7, 0x01579B);
	public static final Item HUMAN_CRAFTER_SPAWN_EGG = egg("human_crafter_spawn_egg", ModEntities.HUMAN_CRAFTER, 0xC4A574, 0x5D4037);

	private ModItems() {
	}

	public static void register() {
		ZKWorkers.LOGGER.debug("Registered worker items");
	}

	private static Item blockItem(String name, Block block) {
		return item(name, new BlockItem(block, new Item.Properties()));
	}

	private static Item egg(String name, net.minecraft.world.entity.EntityType<com.example.entity.HumanWorkerEntity> type, int background, int highlight) {
		return item(name, new SpawnEggItem(type, background, highlight, new Item.Properties()));
	}

	private static Item item(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, ZKWorkers.id(name), item);
	}
}
