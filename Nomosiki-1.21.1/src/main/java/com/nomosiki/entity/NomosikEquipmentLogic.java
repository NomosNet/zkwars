package com.nomosiki.entity;

import com.nomosiki.compat.items.FactionArmorCompat;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

final class NomosikEquipmentLogic {
	private NomosikEquipmentLogic() {
	}

	static void registerEquipmentListener(NomosikEntity entity) {
		entity.equipmentInventory.addListener(container -> syncVanillaEquipmentFromContainer(entity));
	}

	static void syncVanillaEquipmentFromContainer(NomosikEntity entity) {
		if (entity.equipmentInventory == null || entity.syncingVanillaEquipment) {
			return;
		}
		entity.syncingVanillaEquipment = true;
		try {
			mirrorSlot(entity, EquipmentSlot.HEAD, 0);
			mirrorSlot(entity, EquipmentSlot.CHEST, 1);
			mirrorSlot(entity, EquipmentSlot.LEGS, 2);
			mirrorSlot(entity, EquipmentSlot.FEET, 3);
			mirrorSlot(entity, EquipmentSlot.MAINHAND, 4);
			mirrorSlot(entity, EquipmentSlot.OFFHAND, 5);
		} finally {
			entity.syncingVanillaEquipment = false;
		}
	}

	private static void mirrorSlot(NomosikEntity entity, EquipmentSlot slot, int index) {
		entity.setDropChance(slot, 0.0F);
		ItemStack stack = entity.equipmentInventory.getItem(index);
		ItemStack before = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		if (!stack.isEmpty()
			&& !entity.level().isClientSide()
			&& slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
			FactionArmorCompat.apply(entity, slot, stack);
		}
		boolean dyeChanged = !ItemStack.matches(before, stack);
		if (dyeChanged || !ItemStack.matches(entity.superGetItemBySlot(slot), stack)) {
			entity.superSetItemSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
		}
	}

	static void dropCustomDeathLoot(NomosikEntity entity, ServerLevel serverLevel, DamageSource damageSource, boolean recentlyHit) {
		if (suppressEquipmentDrops(entity)) {
			clearEquipmentSilent(entity);
			return;
		}
		entity.superDropCustomDeathLoot(serverLevel, damageSource, recentlyHit);
		for (int i = 0; i < entity.equipmentInventory.getContainerSize(); i++) {
			ItemStack stack = entity.equipmentInventory.getItem(i);
			if (!stack.isEmpty()) {
				entity.spawnAtLocation(stack);
				entity.equipmentInventory.setItem(i, ItemStack.EMPTY);
			}
		}
	}

	static boolean suppressEquipmentDrops(NomosikEntity entity) {
		return false;
	}

	static void clearEquipmentSilent(NomosikEntity entity) {
		for (int i = 0; i < entity.equipmentInventory.getContainerSize(); i++) {
			entity.equipmentInventory.setItem(i, ItemStack.EMPTY);
		}
	}

	static NonNullList<ItemStack> equipmentAsNonNullList(NomosikEntity entity) {
		NonNullList<ItemStack> list = NonNullList.withSize(NomosikEntity.SLOT_COUNT, ItemStack.EMPTY);
		for (int i = 0; i < NomosikEntity.SLOT_COUNT; i++) {
			list.set(i, entity.equipmentInventory.getItem(i).copy());
		}
		return list;
	}

	static ItemStack getItemBySlot(NomosikEntity entity, EquipmentSlot slot) {
		if (entity.equipmentInventory == null) {
			return entity.superGetItemBySlot(slot);
		}
		return switch (slot) {
			case HEAD -> entity.equipmentInventory.getItem(0);
			case CHEST -> entity.equipmentInventory.getItem(1);
			case LEGS -> entity.equipmentInventory.getItem(2);
			case FEET -> entity.equipmentInventory.getItem(3);
			case MAINHAND -> entity.equipmentInventory.getItem(4);
			case OFFHAND -> entity.equipmentInventory.getItem(5);
			default -> entity.superGetItemBySlot(slot);
		};
	}

	static void setItemSlot(NomosikEntity entity, EquipmentSlot slot, ItemStack stack) {
		if (entity.equipmentInventory == null) {
			entity.superSetItemSlot(slot, stack);
			return;
		}
		validateAndSetSlot(entity, slot, stack);
	}

	static void insertIntoHands(NomosikEntity entity, ItemStack remaining) {
		if (remaining == null || remaining.isEmpty()) {
			return;
		}
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND }) {
			if (remaining.isEmpty()) {
				return;
			}
			ItemStack held = entity.getItemBySlot(slot);
			if (held.isEmpty()) {
				int put = Math.min(remaining.getCount(), remaining.getMaxStackSize());
				entity.setItemSlot(slot, remaining.split(put));
			} else if (ItemStack.isSameItemSameComponents(held, remaining)) {
				int room = held.getMaxStackSize() - held.getCount();
				if (room > 0) {
					int put = Math.min(room, remaining.getCount());
					held.grow(put);
					remaining.shrink(put);
				}
			}
		}
	}

	static void validateAndSetSlot(NomosikEntity entity, EquipmentSlot slot, ItemStack stack) {
		entity.setDropChance(slot, 0.0F);
		switch (slot) {
			case HEAD -> entity.equipmentInventory.setItem(0, stack);
			case CHEST -> entity.equipmentInventory.setItem(1, stack);
			case LEGS -> entity.equipmentInventory.setItem(2, stack);
			case FEET -> entity.equipmentInventory.setItem(3, stack);
			case MAINHAND -> entity.equipmentInventory.setItem(4, stack);
			case OFFHAND -> entity.equipmentInventory.setItem(5, stack);
			default -> entity.superSetItemSlot(slot, stack);
		}
	}
}
