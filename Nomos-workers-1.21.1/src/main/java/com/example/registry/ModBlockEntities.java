package com.example.registry;

import com.example.ZKWorkers;
import com.example.block.entity.WorkerBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<WorkerBlockEntity> WORKER = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ZKWorkers.id("worker_block"),
			BlockEntityType.Builder.of(
					WorkerBlockEntity::new,
					ModBlocks.QUARRY,
					ModBlocks.FARMER,
					ModBlocks.LUMBERJACK,
					ModBlocks.CHICKEN_BREEDER,
					ModBlocks.COW_BREEDER,
					ModBlocks.SHEEP_BREEDER,
					ModBlocks.PIG_BREEDER,
					ModBlocks.BUILDER,
					ModBlocks.SMELTER,
					ModBlocks.FISHER,
					ModBlocks.CRAFTER
			).build()
	);

	private ModBlockEntities() {
	}

	public static void register() {
		ZKWorkers.LOGGER.debug("Registered worker block entities");
	}
}
