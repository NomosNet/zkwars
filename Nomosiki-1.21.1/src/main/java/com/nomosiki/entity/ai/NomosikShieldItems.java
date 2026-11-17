package com.nomosiki.entity.ai;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;

/**
 * Fabric-side shield detection (no Forge {@code ToolActions.SHIELD_BLOCK}).
 */
public final class NomosikShieldItems {
	private NomosikShieldItems() {
	}

	public static boolean isShieldBlockItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.getItem() instanceof ShieldItem) {
			return true;
		}
		return stack.getUseAnimation() == UseAnim.BLOCK;
	}
}
