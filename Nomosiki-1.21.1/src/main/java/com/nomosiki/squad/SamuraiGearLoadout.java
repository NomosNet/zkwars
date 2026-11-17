package com.nomosiki.squad;

import com.nomosiki.compat.gun.GunsCompat;
import com.nomosiki.compat.items.FactionArmorCompat;
import com.nomosiki.entity.ArcherEntity;
import com.nomosiki.entity.MusketeerEntity;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.FactionColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Admin spawn kit from {@code zk-nomosiki-items}: steel samurai armor plus katana or yumi.
 */
public final class SamuraiGearLoadout {
	private static final ResourceLocation KATANA = id("katana");
	private static final ResourceLocation YUMI = id("yumi");
	private static final ResourceLocation HELMET = id("steel_samurai_helmet");
	private static final ResourceLocation CHESTPLATE = id("steel_samurai_chestplate");
	private static final ResourceLocation LEGGINGS = id("steel_samurai_leggings");
	private static final ResourceLocation BOOTS = id("steel_samurai_boots");

	private SamuraiGearLoadout() {
	}

	public static boolean supports(SquadType type) {
		return type == SquadType.SAMURAI
			|| type == SquadType.SAMURAI_ARCHER
			|| type == SquadType.SAMURAI_CAVALRY
			|| type == SquadType.SAMURAI_ARCHER_CAVALRY
			|| type == SquadType.SAMURAI_MUSKETEER;
	}

	public static boolean isAvailable() {
		return item(KATANA) != Items.AIR
			&& item(YUMI) != Items.AIR
			&& item(HELMET) != Items.AIR
			&& item(CHESTPLATE) != Items.AIR
			&& item(LEGGINGS) != Items.AIR
			&& item(BOOTS) != Items.AIR;
	}

	public static void equip(NomosikEntity entity) {
		equip(entity, null);
	}

	public static void equip(NomosikEntity entity, @Nullable FactionColor color) {
		entity.setItemSlot(EquipmentSlot.HEAD, dyedArmor(HELMET, color));
		entity.setItemSlot(EquipmentSlot.CHEST, dyedArmor(CHESTPLATE, color));
		entity.setItemSlot(EquipmentSlot.LEGS, dyedArmor(LEGGINGS, color));
		entity.setItemSlot(EquipmentSlot.FEET, dyedArmor(BOOTS, color));
		if (entity instanceof ArcherEntity) {
			entity.setItemSlot(EquipmentSlot.MAINHAND, stack(YUMI));
			entity.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 64));
		} else if (entity instanceof MusketeerEntity) {
			GunsCompat.giveDefaultLoadout(entity);
		} else {
			entity.setItemSlot(EquipmentSlot.MAINHAND, stack(KATANA));
			entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	}

	public static void dyeEquippedArmor(NomosikEntity entity, @Nullable FactionColor color) {
		dyeIfPresent(entity, EquipmentSlot.HEAD, color);
		dyeIfPresent(entity, EquipmentSlot.CHEST, color);
		dyeIfPresent(entity, EquipmentSlot.LEGS, color);
		dyeIfPresent(entity, EquipmentSlot.FEET, color);
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("zk-nomosiki-items", path);
	}

	private static Item item(ResourceLocation id) {
		return BuiltInRegistries.ITEM.get(id);
	}

	private static ItemStack stack(ResourceLocation id) {
		Item found = item(id);
		return found == Items.AIR ? ItemStack.EMPTY : new ItemStack(found);
	}

	private static ItemStack dyedArmor(ResourceLocation id, @Nullable FactionColor color) {
		ItemStack stack = stack(id);
		dyeArmor(stack, color);
		return stack;
	}

	private static void dyeIfPresent(NomosikEntity entity, EquipmentSlot slot, @Nullable FactionColor color) {
		ItemStack stack = entity.getItemBySlot(slot);
		if (stack.isEmpty()) {
			return;
		}
		dyeArmor(stack, color);
	}

	private static void dyeArmor(ItemStack stack, @Nullable FactionColor color) {
		if (stack.isEmpty()) {
			return;
		}
		DyeColor dye = color == null ? DyeColor.RED : color.toVanillaDye();
		FactionArmorCompat.dye(stack, dye);
	}
}
