package com.nomosiki.match.schematic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class SchematicBlocks {
	private SchematicBlocks() {
	}

	static boolean placeable(BlockState state) {
		if (state == null || state.isAir()) {
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

	public static String stringify(BlockState state) {
		StringBuilder builder = new StringBuilder();
		builder.append(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
		if (state.getValues().isEmpty()) {
			return builder.toString();
		}
		builder.append('[');
		boolean first = true;
		for (Property<?> property : state.getProperties()) {
			if (!first) {
				builder.append(',');
			}
			first = false;
			builder.append(property.getName());
			builder.append('=');
			builder.append(valueName(property, state));
		}
		builder.append(']');
		return builder.toString();
	}

	private static <T extends Comparable<T>> String valueName(Property<T> property, BlockState state) {
		return property.getName(state.getValue(property));
	}
}
