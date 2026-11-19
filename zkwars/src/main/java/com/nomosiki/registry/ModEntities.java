package com.nomosiki.registry;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.ArcherEntity;
import com.nomosiki.entity.CaptainEntity;
import com.nomosiki.entity.KnightEntity;
import com.nomosiki.entity.AshigaruArtilleristEntity;
import com.nomosiki.entity.NomadEntity;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final EntityType<NomosikEntity> NOMOSIK = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("nomosik"),
		EntityType.Builder.of(NomosikEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("nomosik").toString()));
	public static final EntityType<ArcherEntity> ARCHER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("archer"),
		EntityType.Builder.of(ArcherEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("archer").toString()));
	public static final EntityType<KnightEntity> KNIGHT = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("knight"),
		EntityType.Builder.of(KnightEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("knight").toString()));
	public static final EntityType<NomadEntity> NOMAD = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("nomad"),
		EntityType.Builder.of(NomadEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("nomad").toString()));
	public static final EntityType<AshigaruArtilleristEntity> ARTILLERIST = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("artillerist"),
		EntityType.Builder.of(AshigaruArtilleristEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("artillerist").toString()));
	public static final EntityType<CaptainEntity> CAPTAIN = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("captain"),
		EntityType.Builder.of(CaptainEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("captain").toString()));
	public static final EntityType<BuildingHeartEntity> BUILDING_HEART = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("building_heart"),
		EntityType.Builder.of(BuildingHeartEntity::new, MobCategory.MISC)
			.sized(1.0F, 1.5F)
			.clientTrackingRange(10)
			.fireImmune()
			.build(NomosikiMod.id("building_heart").toString()));
	public static final EntityType<CaptureBeaconEntity> CAPTURE_BEACON = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("capture_beacon"),
		EntityType.Builder.of(CaptureBeaconEntity::new, MobCategory.MISC)
			.sized(0.9F, 1.8F)
			.clientTrackingRange(10)
			.fireImmune()
			.build(NomosikiMod.id("capture_beacon").toString()));

	private ModEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(NOMOSIK, NomosikEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ARCHER, ArcherEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(KNIGHT, NomosikEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NOMAD, NomadEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ARTILLERIST, AshigaruArtilleristEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CAPTAIN, CaptainEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BUILDING_HEART, BuildingHeartEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CAPTURE_BEACON, CaptureBeaconEntity.createAttributes());
		registerGroundPlacement(NOMOSIK);
		registerGroundPlacement(ARCHER);
		registerGroundPlacement(KNIGHT);
		registerGroundPlacement(NOMAD);
		registerGroundPlacement(ARTILLERIST);
		registerGroundPlacement(CAPTAIN);
	}

	private static <T extends net.minecraft.world.entity.Mob> void registerGroundPlacement(EntityType<T> type) {
		SpawnPlacements.register(
			type,
			SpawnPlacementTypes.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(entityType, level, spawnReason, pos, random) -> true);
	}
}
