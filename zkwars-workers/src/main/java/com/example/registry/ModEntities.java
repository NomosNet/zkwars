package com.example.registry;

import com.example.ZKWorkers;
import com.example.entity.HumanWorkerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<HumanWorkerEntity> HUMAN_MINER = register("human_miner");
	public static final EntityType<HumanWorkerEntity> HUMAN_FARMER = register("human_farmer");
	public static final EntityType<HumanWorkerEntity> HUMAN_LUMBERJACK = register("human_lumberjack");
	public static final EntityType<HumanWorkerEntity> HUMAN_COURIER = register("human_courier");
	public static final EntityType<HumanWorkerEntity> HUMAN_COW_BREEDER = register("human_cow_breeder");
	public static final EntityType<HumanWorkerEntity> HUMAN_CHICKEN_BREEDER = register("human_chicken_breeder");
	public static final EntityType<HumanWorkerEntity> HUMAN_SHEEP_BREEDER = register("human_sheep_breeder");
	public static final EntityType<HumanWorkerEntity> HUMAN_PIG_BREEDER = register("human_pig_breeder");
	public static final EntityType<HumanWorkerEntity> HUMAN_BUILDER = register("human_builder");
	public static final EntityType<HumanWorkerEntity> HUMAN_SMELTER = register("human_smelter");
	public static final EntityType<HumanWorkerEntity> HUMAN_FISHER = register("human_fisher");
	public static final EntityType<HumanWorkerEntity> HUMAN_CRAFTER = register("human_crafter");

	private ModEntities() {
	}

	public static void register() {
		ZKWorkers.LOGGER.debug("Registered human workers");
	}

	public static void registerAttributes() {
		registerAttributes(HUMAN_MINER);
		registerAttributes(HUMAN_FARMER);
		registerAttributes(HUMAN_LUMBERJACK);
		registerAttributes(HUMAN_COURIER);
		registerAttributes(HUMAN_COW_BREEDER);
		registerAttributes(HUMAN_CHICKEN_BREEDER);
		registerAttributes(HUMAN_SHEEP_BREEDER);
		registerAttributes(HUMAN_PIG_BREEDER);
		registerAttributes(HUMAN_BUILDER);
		registerAttributes(HUMAN_SMELTER);
		registerAttributes(HUMAN_FISHER);
		registerAttributes(HUMAN_CRAFTER);
	}

	private static void registerAttributes(EntityType<HumanWorkerEntity> type) {
		FabricDefaultAttributeRegistry.register(type, HumanWorkerEntity.createAttributes());
	}

	private static EntityType<HumanWorkerEntity> register(String name) {
		return Registry.register(
				BuiltInRegistries.ENTITY_TYPE,
				ZKWorkers.id(name),
				EntityType.Builder.of(HumanWorkerEntity::new, MobCategory.CREATURE)
						.sized(0.6F, 1.8F)
						.clientTrackingRange(10)
						.build(ZKWorkers.id(name).toString())
		);
	}
}
