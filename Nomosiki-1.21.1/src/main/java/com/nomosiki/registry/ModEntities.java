package com.nomosiki.registry;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.ArcherEntity;
import com.nomosiki.entity.BritishEntity;
import com.nomosiki.entity.CaptainEntity;
import com.nomosiki.entity.KnightEntity;
import com.nomosiki.entity.AshigaruArtilleristEntity;
import com.nomosiki.entity.MonkArcherEntity;
import com.nomosiki.entity.MonkCavalryEntity;
import com.nomosiki.entity.MonkEntity;
import com.nomosiki.entity.NinjaEntity;
import com.nomosiki.entity.AshigaruMusketeerEntity;
import com.nomosiki.entity.BritishMusketeerEntity;
import com.nomosiki.entity.SpanishArtilleristEntity;
import com.nomosiki.entity.SamuraiMusketeerEntity;
import com.nomosiki.entity.NomadEntity;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SamuraiArcherCavalryEntity;
import com.nomosiki.entity.SamuraiArcherEntity;
import com.nomosiki.entity.SamuraiCavalryEntity;
import com.nomosiki.entity.SamuraiEntity;
import com.nomosiki.entity.SpanishEntity;
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
	public static final EntityType<SamuraiEntity> SAMURAI = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("samurai"),
		EntityType.Builder.of(SamuraiEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("samurai").toString()));
	public static final EntityType<SamuraiArcherEntity> SAMURAI_ARCHER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("samurai_archer"),
		EntityType.Builder.of(SamuraiArcherEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("samurai_archer").toString()));
	public static final EntityType<SamuraiCavalryEntity> SAMURAI_CAVALRY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("samurai_cavalry"),
		EntityType.Builder.of(SamuraiCavalryEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("samurai_cavalry").toString()));
	public static final EntityType<SamuraiArcherCavalryEntity> SAMURAI_ARCHER_CAVALRY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("samurai_archer_cavalry"),
		EntityType.Builder.of(SamuraiArcherCavalryEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("samurai_archer_cavalry").toString()));
	public static final EntityType<BritishEntity> BRITISH = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("british"),
		EntityType.Builder.of(BritishEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("british").toString()));
	public static final EntityType<SpanishEntity> SPANISH = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("spanish"),
		EntityType.Builder.of(SpanishEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("spanish").toString()));
	public static final EntityType<NinjaEntity> NINJA = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("ninja"),
		EntityType.Builder.of(NinjaEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("ninja").toString()));
	public static final EntityType<MonkEntity> MONK = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("monk"),
		EntityType.Builder.of(MonkEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("monk").toString()));
	public static final EntityType<MonkArcherEntity> MONK_ARCHER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("monk_archer"),
		EntityType.Builder.of(MonkArcherEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("monk_archer").toString()));
	public static final EntityType<MonkCavalryEntity> MONK_CAVALRY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("monk_cavalry"),
		EntityType.Builder.of(MonkCavalryEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("monk_cavalry").toString()));
	public static final EntityType<AshigaruArtilleristEntity> ARTILLERIST = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("artillerist"),
		EntityType.Builder.of(AshigaruArtilleristEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("artillerist").toString()));
	public static final EntityType<SpanishArtilleristEntity> SPANISH_ARTILLERIST = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("spanish_artillerist"),
		EntityType.Builder.of(SpanishArtilleristEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("spanish_artillerist").toString()));
	public static final EntityType<AshigaruMusketeerEntity> ASHIGARU_MUSKETEER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("ashigaru_musketeer"),
		EntityType.Builder.of(AshigaruMusketeerEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("ashigaru_musketeer").toString()));
	public static final EntityType<SamuraiMusketeerEntity> SAMURAI_MUSKETEER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("samurai_musketeer"),
		EntityType.Builder.of(SamuraiMusketeerEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("samurai_musketeer").toString()));
	public static final EntityType<BritishMusketeerEntity> BRITISH_MUSKETEER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("british_musketeer"),
		EntityType.Builder.of(BritishMusketeerEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("british_musketeer").toString()));
	public static final EntityType<CaptainEntity> CAPTAIN = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		NomosikiMod.id("captain"),
		EntityType.Builder.of(CaptainEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
			.clientTrackingRange(10)
			.build(NomosikiMod.id("captain").toString()));

	private ModEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(NOMOSIK, NomosikEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ARCHER, ArcherEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(KNIGHT, NomosikEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NOMAD, NomadEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SAMURAI, SamuraiEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SAMURAI_ARCHER, SamuraiArcherEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SAMURAI_CAVALRY, SamuraiCavalryEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SAMURAI_ARCHER_CAVALRY, SamuraiArcherCavalryEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BRITISH, BritishEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SPANISH, SpanishEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NINJA, NinjaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MONK, MonkEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MONK_ARCHER, MonkArcherEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MONK_CAVALRY, MonkCavalryEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ARTILLERIST, AshigaruArtilleristEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SPANISH_ARTILLERIST, SpanishArtilleristEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ASHIGARU_MUSKETEER, AshigaruMusketeerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SAMURAI_MUSKETEER, SamuraiMusketeerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BRITISH_MUSKETEER, BritishMusketeerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CAPTAIN, CaptainEntity.createAttributes());
		registerGroundPlacement(NOMOSIK);
		registerGroundPlacement(ARCHER);
		registerGroundPlacement(KNIGHT);
		registerGroundPlacement(NOMAD);
		registerGroundPlacement(SAMURAI);
		registerGroundPlacement(SAMURAI_ARCHER);
		registerGroundPlacement(SAMURAI_CAVALRY);
		registerGroundPlacement(SAMURAI_ARCHER_CAVALRY);
		registerGroundPlacement(BRITISH);
		registerGroundPlacement(SPANISH);
		registerGroundPlacement(NINJA);
		registerGroundPlacement(MONK);
		registerGroundPlacement(MONK_ARCHER);
		registerGroundPlacement(MONK_CAVALRY);
		registerGroundPlacement(ARTILLERIST);
		registerGroundPlacement(SPANISH_ARTILLERIST);
		registerGroundPlacement(ASHIGARU_MUSKETEER);
		registerGroundPlacement(SAMURAI_MUSKETEER);
		registerGroundPlacement(BRITISH_MUSKETEER);
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
