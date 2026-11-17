package com.nomosiki.work.registry;

import com.nomosiki.work.NomosikiWork;
import com.nomosiki.work.gun.GunItems;
import com.nomosiki.work.weapon.KunaiItem;
import com.nomosiki.work.weapon.ShurikenItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	private static final List<Item> ALL = new ArrayList<>();

	public static Item STEEL_INGOT;
	public static Item STEEL_NUGGET;
	public static Item CLOTH;
	public static Item KATANA_HANDLE;
	public static Item SPEAR_HANDLE;
	public static Item KNIVE_HANDLE;

	public static Item STEEL_SAMURAI_HELMET;
	public static Item STEEL_SAMURAI_CHESTPLATE;
	public static Item STEEL_SAMURAI_LEGGINGS;
	public static Item STEEL_SAMURAI_BOOTS;
	public static Item IRON_SAMURAI_HELMET;
	public static Item IRON_SAMURAI_CHESTPLATE;
	public static Item IRON_SAMURAI_LEGGINGS;
	public static Item IRON_SAMURAI_BOOTS;
	public static Item NETHERITE_SAMURAI_HELMET;
	public static Item NETHERITE_SAMURAI_CHESTPLATE;
	public static Item NETHERITE_SAMURAI_LEGGINGS;
	public static Item NETHERITE_SAMURAI_BOOTS;
	public static Item LIGHT_SAMURAI_HELMET;
	public static Item LIGHT_SAMURAI_CHESTPLATE;
	public static Item LIGHT_SAMURAI_LEGGINGS;
	public static Item LIGHT_SAMURAI_BOOTS;
	public static Item STEEL_NINJA_HELMET;
	public static Item STEEL_NINJA_CHESTPLATE;
	public static Item NINJA_LEGGINGS;
	public static Item STEEL_NINJA_BOOTS;
	public static Item NETHERITE_NINJA_HELMET;
	public static Item NETHERITE_NINJA_CHESTPLATE;
	public static Item NETHERITE_NINJA_BOOTS;
	public static Item KIMONO;
	public static Item STRAW_HAT;

	public static Item KATANA;
	public static Item WAKIZASHI;
	public static Item ODACHI;
	public static Item NAGINATA;
	public static Item NAGAMAKI;
	public static Item KAMAYARI;
	public static Item TONBUKIRI;
	public static Item TETSUBO;
	public static Item MASAKARI;
	public static Item KABUTOWARI;
	public static Item KUNAI;
	public static Item KAMA;
	public static Item SAI;
	public static Item SHUKO;
	public static Item SHURIKEN;
	public static Item YUMI;

	public static Item KATANA_NETHERITE;
	public static Item WAKIZASHI_NETHERITE;
	public static Item ODACHI_NETHERITE;
	public static Item NAGINATA_NETHERITE;
	public static Item NAGAMAKI_NETHERITE;
	public static Item KAMAYARI_NETHERITE;
	public static Item TONBUKIRI_NETHERITE;
	public static Item TETSUBO_NETHERITE;
	public static Item MASAKARI_NETHERITE;
	public static Item KABUTOWARI_NETHERITE;
	public static Item KUNAI_NETHERITE;
	public static Item KAMA_NETHERITE;
	public static Item SAI_NETHERITE;
	public static Item SHUKO_NETHERITE;

	private ModItems() {
	}

	public static List<Item> allItems() {
		return List.copyOf(ALL);
	}

	public static void register() {
		STEEL_INGOT = item("steel_ingot", new Item(new Item.Properties()));
		STEEL_NUGGET = item("steel_nugget", new Item(new Item.Properties()));
		CLOTH = item("cloth", new Item(new Item.Properties()));
		KATANA_HANDLE = item("katana_handle", new Item(new Item.Properties()));
		SPEAR_HANDLE = item("spear_handle", new Item(new Item.Properties()));
		KNIVE_HANDLE = item("knive_handle", new Item(new Item.Properties()));

		STEEL_SAMURAI_HELMET = armor("steel_samurai_helmet", ModArmorMaterials.SAMURAI_STEEL, ArmorItem.Type.HELMET, 33, false);
		STEEL_SAMURAI_CHESTPLATE = armor("steel_samurai_chestplate", ModArmorMaterials.SAMURAI_STEEL, ArmorItem.Type.CHESTPLATE, 33, false);
		STEEL_SAMURAI_LEGGINGS = armor("steel_samurai_leggings", ModArmorMaterials.SAMURAI_STEEL, ArmorItem.Type.LEGGINGS, 33, false);
		STEEL_SAMURAI_BOOTS = armor("steel_samurai_boots", ModArmorMaterials.SAMURAI_STEEL, ArmorItem.Type.BOOTS, 33, false);

		IRON_SAMURAI_HELMET = armor("iron_samurai_helmet", ModArmorMaterials.SAMURAI_IRON, ArmorItem.Type.HELMET, 15, false);
		IRON_SAMURAI_CHESTPLATE = armor("iron_samurai_chestplate", ModArmorMaterials.SAMURAI_IRON, ArmorItem.Type.CHESTPLATE, 15, false);
		IRON_SAMURAI_LEGGINGS = armor("iron_samurai_leggings", ModArmorMaterials.SAMURAI_IRON, ArmorItem.Type.LEGGINGS, 15, false);
		IRON_SAMURAI_BOOTS = armor("iron_samurai_boots", ModArmorMaterials.SAMURAI_IRON, ArmorItem.Type.BOOTS, 15, false);

		NETHERITE_SAMURAI_HELMET = armor("netherite_samurai_helmet", ModArmorMaterials.SAMURAI_NETHERITE, ArmorItem.Type.HELMET, 37, true);
		NETHERITE_SAMURAI_CHESTPLATE = armor("netherite_samurai_chestplate", ModArmorMaterials.SAMURAI_NETHERITE, ArmorItem.Type.CHESTPLATE, 37, true);
		NETHERITE_SAMURAI_LEGGINGS = armor("netherite_samurai_leggings", ModArmorMaterials.SAMURAI_NETHERITE, ArmorItem.Type.LEGGINGS, 37, true);
		NETHERITE_SAMURAI_BOOTS = armor("netherite_samurai_boots", ModArmorMaterials.SAMURAI_NETHERITE, ArmorItem.Type.BOOTS, 37, true);

		LIGHT_SAMURAI_HELMET = armor("light_samurai_helmet", ModArmorMaterials.SAMURAI_RONIN, ArmorItem.Type.HELMET, 26, false);
		LIGHT_SAMURAI_CHESTPLATE = armor("light_samurai_chestplate", ModArmorMaterials.SAMURAI_RONIN, ArmorItem.Type.CHESTPLATE, 26, false);
		LIGHT_SAMURAI_LEGGINGS = armor("light_samurai_leggings", ModArmorMaterials.SAMURAI_RONIN, ArmorItem.Type.LEGGINGS, 26, false);
		LIGHT_SAMURAI_BOOTS = armor("light_samurai_boots", ModArmorMaterials.SAMURAI_RONIN, ArmorItem.Type.BOOTS, 26, false);

		STEEL_NINJA_HELMET = armor("steel_ninja_helmet", ModArmorMaterials.NINJA_STEEL, ArmorItem.Type.HELMET, 33, false);
		STEEL_NINJA_CHESTPLATE = armor("steel_ninja_chestplate", ModArmorMaterials.NINJA_STEEL, ArmorItem.Type.CHESTPLATE, 33, false);
		NINJA_LEGGINGS = armor("ninja_leggings", ModArmorMaterials.CLOTH, ArmorItem.Type.LEGGINGS, 26, false);
		STEEL_NINJA_BOOTS = armor("steel_ninja_boots", ModArmorMaterials.NINJA_STEEL, ArmorItem.Type.BOOTS, 33, false);

		NETHERITE_NINJA_HELMET = armor("netherite_ninja_helmet", ModArmorMaterials.NINJA_NETHERITE, ArmorItem.Type.HELMET, 37, true);
		NETHERITE_NINJA_CHESTPLATE = armor("netherite_ninja_chestplate", ModArmorMaterials.NINJA_NETHERITE, ArmorItem.Type.CHESTPLATE, 37, true);
		NETHERITE_NINJA_BOOTS = armor("netherite_ninja_boots", ModArmorMaterials.NINJA_NETHERITE, ArmorItem.Type.BOOTS, 37, true);

		KIMONO = armor("kimono", ModArmorMaterials.CLOTH, ArmorItem.Type.CHESTPLATE, 26, false);
		STRAW_HAT = armor("straw_hat", ModArmorMaterials.STRAW, ArmorItem.Type.HELMET, 15, false);

		KATANA = sword("katana", ModToolTiers.STEEL, 1, -2.0F, false);
		WAKIZASHI = sword("wakizashi", ModToolTiers.STEEL, 0, -1.7F, false);
		ODACHI = sword("odachi", ModToolTiers.STEEL, 3, -2.4F, false);
		NAGINATA = sword("naginata", ModToolTiers.STEEL, 3, -2.5F, false, 1.0F, 0.0F);
		NAGAMAKI = sword("nagamaki", ModToolTiers.STEEL, 2, -2.3F, false, 0.5F, 0.0F);
		KAMAYARI = sword("kamayari", ModToolTiers.STEEL, 2, -2.5F, false, 1.0F, 0.4F);
		TONBUKIRI = sword("tonbukiri", ModToolTiers.STEEL, 2, -2.7F, false, 1.5F, 0.5F);
		TETSUBO = sword("tetsubo", ModToolTiers.STEEL, 7, -3.5F, false, 0.0F, 1.0F);
		MASAKARI = sword("masakari", ModToolTiers.STEEL, 3, -2.8F, false, 0.0F, 0.4F);
		KABUTOWARI = sword("kabutowari", ModToolTiers.STEEL, 0, -1.8F, false);
		KUNAI = kunai("kunai", ModToolTiers.STEEL, -1, -2.0F, false, 6.0F);
		KAMA = sword("kama", ModToolTiers.STEEL, -1, -1.4F, false);
		SAI = sword("sai", ModToolTiers.STEEL, -2, -1.2F, false);
		SHUKO = sword("shuko", ModToolTiers.STEEL, -1, -1.6F, false);
		SHURIKEN = item("shuriken", new ShurikenItem(new Item.Properties().stacksTo(16)));
		YUMI = item("yumi", new BowItem(new Item.Properties().durability(930)));

		KATANA_NETHERITE = sword("katana_netherite", ModToolTiers.NETHERITE, 2, -2.0F, true);
		WAKIZASHI_NETHERITE = sword("wakizashi_netherite", ModToolTiers.NETHERITE, 1, -1.7F, true);
		ODACHI_NETHERITE = sword("odachi_netherite", ModToolTiers.NETHERITE, 4, -2.4F, true);
		NAGINATA_NETHERITE = sword("naginata_netherite", ModToolTiers.NETHERITE, 4, -2.5F, true, 1.0F, 0.0F);
		NAGAMAKI_NETHERITE = sword("nagamaki_netherite", ModToolTiers.NETHERITE, 3, -2.3F, true, 0.5F, 0.0F);
		KAMAYARI_NETHERITE = sword("kamayari_netherite", ModToolTiers.NETHERITE, 3, -2.5F, true, 1.0F, 0.4F);
		TONBUKIRI_NETHERITE = sword("tonbukiri_netherite", ModToolTiers.NETHERITE, 3, -2.7F, true, 1.5F, 0.5F);
		TETSUBO_NETHERITE = sword("tetsubo_netherite", ModToolTiers.NETHERITE, 8, -3.5F, true, 0.0F, 1.0F);
		MASAKARI_NETHERITE = sword("masakari_netherite", ModToolTiers.NETHERITE, 4, -2.8F, true, 0.0F, 0.4F);
		KABUTOWARI_NETHERITE = sword("kabutowari_netherite", ModToolTiers.NETHERITE, 1, -1.8F, true);
		KUNAI_NETHERITE = kunai("kunai_netherite", ModToolTiers.NETHERITE, 0, -2.0F, true, 8.0F);
		KAMA_NETHERITE = sword("kama_netherite", ModToolTiers.NETHERITE, 0, -1.4F, true);
		SAI_NETHERITE = sword("sai_netherite", ModToolTiers.NETHERITE, -1, -1.2F, true);
		SHUKO_NETHERITE = sword("shuko_netherite", ModToolTiers.NETHERITE, 0, -1.6F, true);
	}

	public static void appendCreativeTabItems(net.minecraft.world.item.CreativeModeTab.Output entries) {
		for (Item item : ALL) {
			entries.accept(item);
		}
		GunItems.addToCreativeTab(entries);
	}

	private static Item armor(
		String name,
		net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material,
		ArmorItem.Type type,
		int durabilityFactor,
		boolean fireResistant
	) {
		Item.Properties properties = new Item.Properties()
			.durability(type.getDurability(durabilityFactor))
			.attributes(armorAttributes(material, type));
		if (fireResistant) {
			properties.fireResistant();
		}
		return item(name, new ArmorItem(material, type, properties));
	}

	private static ItemAttributeModifiers armorAttributes(Holder<ArmorMaterial> material, ArmorItem.Type type) {
		ArmorMaterial value = material.value();
		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
		ResourceLocation id = ResourceLocation.withDefaultNamespace("armor." + type.getName());
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
			.add(Attributes.ARMOR, new AttributeModifier(id, value.getDefense(type), AttributeModifier.Operation.ADD_VALUE), group)
			.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, value.toughness(), AttributeModifier.Operation.ADD_VALUE), group);
		float knockback = value.knockbackResistance();
		if (knockback > 0.0F) {
			builder.add(
				Attributes.KNOCKBACK_RESISTANCE,
				new AttributeModifier(id, knockback, AttributeModifier.Operation.ADD_VALUE),
				group);
		}
		return builder.build();
	}

	private static Item sword(String name, ModToolTiers tier, int attack, float speed, boolean fireResistant) {
		return sword(name, tier, attack, speed, fireResistant, 0.0F, 0.0F);
	}

	private static Item sword(String name, ModToolTiers tier, int attack, float speed, boolean fireResistant, float reach, float knockback) {
		return item(name, new SwordItem(tier, swordProperties(tier, attack, speed, fireResistant, reach, knockback)));
	}

	private static Item kunai(String name, ModToolTiers tier, int attack, float speed, boolean fireResistant, float throwDamage) {
		return item(name, new KunaiItem(tier, throwDamage, swordProperties(tier, attack, speed, fireResistant, 0.0F, 0.0F)));
	}

	private static Item.Properties swordProperties(
		ModToolTiers tier,
		int attack,
		float speed,
		boolean fireResistant,
		float reach,
		float knockback
	) {
		ItemAttributeModifiers attributes = SwordItem.createAttributes(tier, attack, speed);
		if (reach != 0.0F) {
			attributes = attributes.withModifierAdded(
				Attributes.ENTITY_INTERACTION_RANGE,
				new AttributeModifier(NomosikiWork.id("weapon_reach"), reach, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND);
		}
		if (knockback != 0.0F) {
			attributes = attributes.withModifierAdded(
				Attributes.ATTACK_KNOCKBACK,
				new AttributeModifier(NomosikiWork.id("weapon_knockback"), knockback, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND);
		}
		Item.Properties properties = new Item.Properties().attributes(attributes);
		if (fireResistant) {
			properties.fireResistant();
		}
		return properties;
	}

	private static Item item(String name, Item item) {
		Item registered = Registry.register(BuiltInRegistries.ITEM, NomosikiWork.id(name), item);
		ALL.add(registered);
		return registered;
	}
}
