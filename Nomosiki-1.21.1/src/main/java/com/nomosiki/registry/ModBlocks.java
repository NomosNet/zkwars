package com.nomosiki.registry;

import com.nomosiki.NomosikiMod;
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
}
