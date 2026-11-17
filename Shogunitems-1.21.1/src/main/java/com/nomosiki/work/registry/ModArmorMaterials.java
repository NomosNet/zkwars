package com.nomosiki.work.registry;

import com.nomosiki.work.NomosikiWork;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
	public static Holder<ArmorMaterial> SAMURAI_STEEL;
	public static Holder<ArmorMaterial> SAMURAI_IRON;
	public static Holder<ArmorMaterial> SAMURAI_NETHERITE;
	public static Holder<ArmorMaterial> SAMURAI_RONIN;
	public static Holder<ArmorMaterial> NINJA_STEEL;
	public static Holder<ArmorMaterial> NINJA_NETHERITE;
	public static Holder<ArmorMaterial> STRAW;
	public static Holder<ArmorMaterial> CLOTH;

	private ModArmorMaterials() {
	}

	public static void register() {
		SAMURAI_STEEL = registerMaterial(
			"samurai_steel",
			defense(3, 8, 6, 3),
			10,
			SoundEvents.ARMOR_EQUIP_IRON,
			() -> Ingredient.of(ModItems.STEEL_INGOT),
			List.of(
				layer("samurai_steel"),
				new ArmorMaterial.Layer(NomosikiWork.id("samurai_steel"), "_dyeable", true)),
			2.0F,
			0.0F);
		SAMURAI_IRON = registerMaterial(
			"samurai_iron",
			defense(2, 6, 5, 2),
			9,
			SoundEvents.ARMOR_EQUIP_IRON,
			() -> Ingredient.of(Items.IRON_INGOT),
			List.of(layer("samurai_iron")),
			0.0F,
			0.0F);
		SAMURAI_NETHERITE = registerMaterial(
			"samurai_netherite",
			defense(3, 8, 6, 3),
			25,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			() -> Ingredient.of(Items.NETHERITE_INGOT),
			List.of(layer("samurai_netherite")),
			3.0F,
			0.1F);
		SAMURAI_RONIN = registerMaterial(
			"samurai_ronin",
			defense(2, 6, 5, 2),
			12,
			SoundEvents.ARMOR_EQUIP_IRON,
			() -> Ingredient.of(ModItems.STEEL_INGOT),
			List.of(layer("samurai_ronin")),
			0.5F,
			0.0F);
		NINJA_STEEL = registerMaterial(
			"ninja_steel",
			defense(2, 6, 3, 2),
			12,
			SoundEvents.ARMOR_EQUIP_IRON,
			() -> Ingredient.of(ModItems.STEEL_INGOT),
			List.of(layer("ninja_steel")),
			0.5F,
			0.0F);
		NINJA_NETHERITE = registerMaterial(
			"ninja_netherite",
			defense(3, 8, 6, 3),
			25,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			() -> Ingredient.of(Items.NETHERITE_INGOT),
			List.of(layer("ninja_netherite")),
			3.0F,
			0.1F);
		STRAW = registerMaterial(
			"straw",
			defense(1, 1, 1, 1),
			7,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			() -> Ingredient.of(Items.WHEAT),
			List.of(layer("straw")),
			0.0F,
			0.0F);
		CLOTH = registerMaterial(
			"cloth",
			defense(1, 3, 2, 1),
			12,
			SoundEvents.ARMOR_EQUIP_LEATHER,
			() -> Ingredient.of(ModItems.CLOTH),
			List.of(layer("cloth")),
			0.0F,
			0.0F);
	}

	private static EnumMap<ArmorItem.Type, Integer> defense(int helmet, int chest, int legs, int boots) {
		return Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
			map.put(ArmorItem.Type.HELMET, helmet);
			map.put(ArmorItem.Type.CHESTPLATE, chest);
			map.put(ArmorItem.Type.LEGGINGS, legs);
			map.put(ArmorItem.Type.BOOTS, boots);
			map.put(ArmorItem.Type.BODY, chest);
		});
	}

	private static ArmorMaterial.Layer layer(String name) {
		return new ArmorMaterial.Layer(NomosikiWork.id(name));
	}

	private static Holder<ArmorMaterial> registerMaterial(
		String name,
		EnumMap<ArmorItem.Type, Integer> defense,
		int enchantmentValue,
		Holder<net.minecraft.sounds.SoundEvent> equipSound,
		java.util.function.Supplier<Ingredient> repair,
		List<ArmorMaterial.Layer> layers,
		float toughness,
		float knockbackResistance
	) {
		ArmorMaterial material = new ArmorMaterial(
			defense,
			enchantmentValue,
			equipSound,
			repair,
			layers,
			toughness,
			knockbackResistance);
		return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, NomosikiWork.id(name), material);
	}
}
