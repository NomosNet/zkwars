package com.nomosiki.registry;

import com.nomosiki.NomosikiMod;
import com.nomosiki.block.MatchShellBlock;
import com.nomosiki.block.TesterSquadShopBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	public static final Block TESTER_SQUAD_SHOP = register(
		"tester_squad_shop",
		new TesterSquadShopBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
			.mapColor(MapColor.COLOR_PURPLE)));

	/** Match building shell. No item: only the match spawner places it. */
	public static final Block MATCH_SHELL = registerBlock(
		"match_shell",
		new MatchShellBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
			.noCollission()
			.noOcclusion()
			.strength(-1.0F, 3600000.0F)
			.noLootTable()));

	private ModBlocks() {
	}

	public static void register() {
		// Static fields register blocks and block items on class load.
	}

	private static Block register(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, NomosikiMod.id(path), block);
		Registry.register(BuiltInRegistries.ITEM, NomosikiMod.id(path), new BlockItem(block, new Item.Properties()));
		return block;
	}

	private static Block registerBlock(String path, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, NomosikiMod.id(path), block);
	}
}
