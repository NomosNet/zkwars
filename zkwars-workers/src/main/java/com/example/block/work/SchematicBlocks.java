package com.example.block.work;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SchematicBlocks {
	private SchematicBlocks() {
	}

	public static boolean placeable(BlockState state) {
		if (state.isAir()) {
			return false;
		}
		Block block = state.getBlock();
		if (block instanceof LiquidBlock || block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock) {
			return false;
		}
		if (block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK || block == Blocks.REPEATING_COMMAND_BLOCK
				|| block == Blocks.STRUCTURE_BLOCK || block == Blocks.JIGSAW
				|| block == Blocks.STRUCTURE_VOID || block == Blocks.BARRIER || block == Blocks.BEDROCK
				|| block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL || block == Blocks.END_GATEWAY) {
			return false;
		}
		return block.asItem() != Items.AIR;
	}
}
