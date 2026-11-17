package com.nomosiki.registry;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public final class ModItems {
	public static final Item NOMOSIK_SPAWN_EGG = register(
		"nomosik_spawn_egg",
		new SpawnEggItem(ModEntities.NOMOSIK, NomosikRace.HUMAN.spawnEggShellRgb(), NomosikRace.HUMAN.spawnEggSpotRgb(), new Item.Properties()));
	public static final Item ARCHER_SPAWN_EGG = register(
		"archer_spawn_egg",
		new SpawnEggItem(ModEntities.ARCHER, 0x8B6D4A, 0x2B2B2B, new Item.Properties()));
	public static final Item KNIGHT_SPAWN_EGG = register(
		"knight_spawn_egg",
		new SpawnEggItem(ModEntities.KNIGHT, 0x5A5A6E, 0xC4A35A, new Item.Properties()));
	public static final Item SAMURAI_SPAWN_EGG = register(
		"samurai_spawn_egg",
		new SpawnEggItem(ModEntities.SAMURAI, 0x7A1F1F, 0xC4A35A, new Item.Properties()));
	public static final Item SAMURAI_ARCHER_SPAWN_EGG = register(
		"samurai_archer_spawn_egg",
		new SpawnEggItem(ModEntities.SAMURAI_ARCHER, 0x6B1A1A, 0x2B2B2B, new Item.Properties()));
	public static final Item SAMURAI_CAVALRY_SPAWN_EGG = register(
		"samurai_cavalry_spawn_egg",
		new SpawnEggItem(ModEntities.SAMURAI_CAVALRY, 0x7A1F1F, 0x5A5A6E, new Item.Properties()));
	public static final Item SAMURAI_ARCHER_CAVALRY_SPAWN_EGG = register(
		"samurai_archer_cavalry_spawn_egg",
		new SpawnEggItem(ModEntities.SAMURAI_ARCHER_CAVALRY, 0x6B1A1A, 0x5A5A6E, new Item.Properties()));
	public static final Item NOMAD_SPAWN_EGG = register(
		"nomad_spawn_egg",
		new SpawnEggItem(ModEntities.NOMAD, 0x8B7355, 0x3D5A7A, new Item.Properties()));
	public static final Item BRITISH_SPAWN_EGG = register(
		"british_spawn_egg",
		new SpawnEggItem(ModEntities.BRITISH, 0x8B1E2D, 0xC4A35A, new Item.Properties()));
	public static final Item SPANISH_SPAWN_EGG = register(
		"spanish_spawn_egg",
		new SpawnEggItem(ModEntities.SPANISH, 0xE8E4D8, 0xC9A227, new Item.Properties()));
	public static final Item NINJA_SPAWN_EGG = register(
		"ninja_spawn_egg",
		new SpawnEggItem(ModEntities.NINJA, 0x1A1A1A, 0x8B1E2D, new Item.Properties()));
	public static final Item MONK_SPAWN_EGG = register(
		"monk_spawn_egg",
		new SpawnEggItem(ModEntities.MONK, 0xF4E8C8, 0x6B3A1F, new Item.Properties()));
	public static final Item MONK_ARCHER_SPAWN_EGG = register(
		"monk_archer_spawn_egg",
		new SpawnEggItem(ModEntities.MONK_ARCHER, 0xF4E8C8, 0x6B3A1F, new Item.Properties()));
	public static final Item MONK_CAVALRY_SPAWN_EGG = register(
		"monk_cavalry_spawn_egg",
		new SpawnEggItem(ModEntities.MONK_CAVALRY, 0xF4E8C8, 0x6B3A1F, new Item.Properties()));
	public static final Item ARTILLERIST_SPAWN_EGG = register(
		"artillerist_spawn_egg",
		new SpawnEggItem(ModEntities.ARTILLERIST, 0x8B6D4A, 0x5A4632, new Item.Properties()));
	public static final Item SPANISH_ARTILLERIST_SPAWN_EGG = register(
		"spanish_artillerist_spawn_egg",
		new SpawnEggItem(ModEntities.SPANISH_ARTILLERIST, 0xE8E4D8, 0x4A4A4A, new Item.Properties()));
	public static final Item ASHIGARU_MUSKETEER_SPAWN_EGG = register(
		"ashigaru_musketeer_spawn_egg",
		new SpawnEggItem(ModEntities.ASHIGARU_MUSKETEER, 0x8B6D4A, 0x3D4A32, new Item.Properties()));
	public static final Item SAMURAI_MUSKETEER_SPAWN_EGG = register(
		"samurai_musketeer_spawn_egg",
		new SpawnEggItem(ModEntities.SAMURAI_MUSKETEER, 0x7A1F1F, 0x3D4A32, new Item.Properties()));
	public static final Item BRITISH_MUSKETEER_SPAWN_EGG = register(
		"british_musketeer_spawn_egg",
		new SpawnEggItem(ModEntities.BRITISH_MUSKETEER, 0x8B1E2D, 0x3D4A32, new Item.Properties()));
	public static final Item CAPTAIN_SPAWN_EGG = register(
		"captain_spawn_egg",
		new SpawnEggItem(ModEntities.CAPTAIN, 0x1E3A5F, 0xC4A35A, new Item.Properties()));

	private static final List<Item> ROSTER_SPAWN_EGGS = List.of(
		NOMOSIK_SPAWN_EGG,
		ARCHER_SPAWN_EGG,
		KNIGHT_SPAWN_EGG,
		SAMURAI_SPAWN_EGG,
		SAMURAI_ARCHER_SPAWN_EGG,
		SAMURAI_CAVALRY_SPAWN_EGG,
		SAMURAI_ARCHER_CAVALRY_SPAWN_EGG,
		NOMAD_SPAWN_EGG,
		BRITISH_SPAWN_EGG,
		SPANISH_SPAWN_EGG,
		NINJA_SPAWN_EGG,
		MONK_SPAWN_EGG,
		MONK_ARCHER_SPAWN_EGG,
		MONK_CAVALRY_SPAWN_EGG,
		ARTILLERIST_SPAWN_EGG,
		SPANISH_ARTILLERIST_SPAWN_EGG,
		ASHIGARU_MUSKETEER_SPAWN_EGG,
		SAMURAI_MUSKETEER_SPAWN_EGG,
		BRITISH_MUSKETEER_SPAWN_EGG,
		CAPTAIN_SPAWN_EGG
	);

	private ModItems() {
	}

	/** The roster spawn eggs shown in creative. */
	public static List<Item> playableUnitSpawnEggs() {
		return ROSTER_SPAWN_EGGS;
	}

	/** @deprecated use {@link #playableUnitSpawnEggs()} */
	@Deprecated
	public static List<Item> raceUnitSpawnEggs() {
		return playableUnitSpawnEggs();
	}

	/**
	 * Contents of the Nomosiki creative tab: one egg per remaining unit.
	 */
	public static void appendCreativeTabItems(CreativeModeTab.Output entries) {
		entries.accept(ModBlocks.TESTER_SQUAD_SHOP);
		for (Item egg : ROSTER_SPAWN_EGGS) {
			entries.accept(egg);
		}
	}

	public static void register() {
		// Static fields register items on class load.
	}

	/** Spawn egg with {@link DataComponents#ENTITY_DATA} so {@link NomosikEntity#NBT_RACE} is applied on spawn. */
	public static ItemStack spawnEggWithRace(Item spawnEggItem, NomosikRace race) {
		ItemStack stack = new ItemStack(spawnEggItem);
		CompoundTag tag = new CompoundTag();
		tag.putByte(NomosikEntity.NBT_RACE, NomosikRace.normalizeForPlay(race).id());
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		return stack;
	}

	private static Item register(String path, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, NomosikiMod.id(path), item);
	}
}
