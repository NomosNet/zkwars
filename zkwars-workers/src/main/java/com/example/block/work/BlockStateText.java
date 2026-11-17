package com.example.block.work;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockStateText {
	private BlockStateText() {
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
