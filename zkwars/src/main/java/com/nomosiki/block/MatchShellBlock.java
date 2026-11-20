package com.nomosiki.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Visual shell of a match building. No collision, so units walk through it.
 */
public class MatchShellBlock extends Block {
	public static final EnumProperty<Look> LOOK = EnumProperty.create("look", Look.class);

	public MatchShellBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(LOOK, Look.BRICKS));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LOOK);
	}

	public enum Look implements StringRepresentable {
		IRON("iron"),
		COBBLE("cobble"),
		PLANKS("planks"),
		BRICKS("bricks"),
		GOLD("gold"),
		STONE("stone"),
		CRACKED("cracked");

		private final String name;

		Look(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}
