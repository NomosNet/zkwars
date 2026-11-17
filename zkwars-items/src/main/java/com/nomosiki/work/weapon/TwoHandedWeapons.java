package com.nomosiki.work.weapon;

import com.nomosiki.work.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TwoHandedWeapons {
	private TwoHandedWeapons() {
	}

	public static boolean isTwoHanded(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		Item item = stack.getItem();
		return item == ModItems.ODACHI
			|| item == ModItems.NAGINATA
			|| item == ModItems.NAGAMAKI
			|| item == ModItems.TETSUBO
			|| item == ModItems.KAMAYARI
			|| item == ModItems.TONBUKIRI
			|| item == ModItems.ODACHI_NETHERITE
			|| item == ModItems.NAGINATA_NETHERITE
			|| item == ModItems.NAGAMAKI_NETHERITE
			|| item == ModItems.TETSUBO_NETHERITE
			|| item == ModItems.KAMAYARI_NETHERITE
			|| item == ModItems.TONBUKIRI_NETHERITE;
	}

	public static boolean otherHandIsTwoHanded(LivingEntity entity, InteractionHand hand) {
		InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		return isTwoHanded(entity.getItemInHand(other));
	}
}
