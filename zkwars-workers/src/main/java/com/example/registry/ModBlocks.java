package com.example.registry;

import com.example.ZKWorkers;
import com.example.block.WorkerBlock;
import com.example.block.WorkerKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	public static final TagKey<Block> WORKER_BLOCKS = TagKey.create(Registries.BLOCK, ZKWorkers.id("worker_blocks"));

	public static final Block QUARRY = register("quarry_block", WorkerKind.QUARRY);
	public static final Block FARMER = register("farmer_block", WorkerKind.FARMER);
	public static final Block LUMBERJACK = register("lumberjack_block", WorkerKind.LUMBERJACK);
	public static final Block CHICKEN_BREEDER = register("chicken_breeder_block", WorkerKind.CHICKEN);
	public static final Block COW_BREEDER = register("cow_breeder_block", WorkerKind.COW);
	public static final Block SHEEP_BREEDER = register("sheep_breeder_block", WorkerKind.SHEEP);
	public static final Block PIG_BREEDER = register("pig_breeder_block", WorkerKind.PIG);
	public static final Block BUILDER = register("builder_block", WorkerKind.BUILDER);
	public static final Block SMELTER = register("smelter_block", WorkerKind.SMELTER);
	public static final Block FISHER = register("fisher_block", WorkerKind.FISHER);
	public static final Block CRAFTER = register("crafter_block", WorkerKind.CRAFTER);

	private ModBlocks() {
	}

	public static void register() {
		ZKWorkers.LOGGER.debug("Registered worker blocks");
	}

	public static boolean isWorkerBlock(BlockState state) {
		return state.is(WORKER_BLOCKS);
	}

	public static boolean isBed(BlockState state) {
		return state.getBlock() instanceof BedBlock;
	}

	private static BlockBehaviour.Properties table() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD)
				.strength(2.0F)
				.sound(SoundType.WOOD)
				.pushReaction(PushReaction.BLOCK);
	}

	private static Block register(String name, WorkerKind kind) {
		return net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, ZKWorkers.id(name), new WorkerBlock(kind, table()));
	}
}
