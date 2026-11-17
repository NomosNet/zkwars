package com.nomosiki.entity;

import com.nomosiki.entity.ai.NomosikShieldItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class NomosikShieldLogic {
	/**
	 * Defaults until {@code NomosikiServerConfig.shield} (implementation step 10).
	 * Matches raise-shields plan §7: FOLLOW allowed, mounted knights blocked.
	 */
	private static final boolean SHIELD_FEATURE_ENABLED = true;
	private static final boolean SHIELD_STANCE_ONLY_ON_HOLD = false;
	private static final boolean SHIELD_ALLOW_MOUNTED = false;
	/** Matches Player.disableShield cooldown / Recruits Shieldman default. */
	static final int SHIELD_DISABLE_TICKS = 100;

	private NomosikShieldLogic() {
	}

	static void tickCooldown(NomosikEntity entity) {
		if (entity.shieldBlockCooldown > 0) {
			entity.shieldBlockCooldown--;
		}
	}

	static boolean isShieldStance(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return entity.getEntityData().get(NomosikEntity.DATA_SHIELD_STANCE);
		}
		return entity.shieldStance;
	}

	static void setShieldStance(NomosikEntity entity, boolean up) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.shieldStance = up;
		entity.getEntityData().set(NomosikEntity.DATA_SHIELD_STANCE, up);
		if (!up && entity.isUsingItem()) {
			entity.stopUsingItem();
		}
	}

	static boolean canUseShieldStance(NomosikEntity entity) {
		if (!SHIELD_FEATURE_ENABLED) {
			return false;
		}
		if (entity.isRangedUnit()) {
			return false;
		}
		if (entity.isCavalryMounted() && !SHIELD_ALLOW_MOUNTED) {
			return false;
		}
		return NomosikShieldItems.isShieldBlockItem(entity.getItemBySlot(EquipmentSlot.OFFHAND));
	}

	static boolean shouldBlockWithShieldNow(NomosikEntity entity) {
		if (!isShieldStance(entity) || !canUseShieldStance(entity)) {
			return false;
		}
		if (entity.shieldBlockCooldown > 0) {
			return false;
		}
		if (SHIELD_STANCE_ONLY_ON_HOLD) {
			SquadOrder order = entity.getSquadOrder();
			if (order != SquadOrder.HOLD && !order.usesAttackVolume()) {
				return false;
			}
		}
		return NomosikShieldItems.isShieldBlockItem(entity.getItemBySlot(EquipmentSlot.OFFHAND));
	}

	static int getShieldBlockCooldown(NomosikEntity entity) {
		return entity.shieldBlockCooldown;
	}

	static void setShieldBlockCooldown(NomosikEntity entity, int ticks) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.shieldBlockCooldown = Math.max(0, ticks);
	}

	static void disableShieldStance(NomosikEntity entity, int ticks) {
		if (entity.level().isClientSide()) {
			return;
		}
		setShieldBlockCooldown(entity, ticks > 0 ? ticks : SHIELD_DISABLE_TICKS);
		entity.stopUsingItem();
		entity.level().broadcastEntityEvent(entity, (byte) 30);
	}

	static void hurtCurrentlyUsedShield(NomosikEntity entity, float amount) {
		ItemStack useItem = entity.getUseItem();
		if (entity.level().isClientSide() || !NomosikShieldItems.isShieldBlockItem(useItem)) {
			return;
		}
		if (amount < 3.0F) {
			return;
		}
		int damage = 1 + Mth.floor(amount);
		InteractionHand hand = entity.getUsedItemHand();
		useItem.hurtAndBreak(damage, entity, entity.getSlotForHand(hand));
		if (useItem.isEmpty()) {
			if (hand == InteractionHand.MAIN_HAND) {
				entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			} else {
				entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			}
			entity.clearUseItemField();
			entity.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + entity.level().random.nextFloat() * 0.4F);
		}
	}

	static void blockUsingShield(NomosikEntity entity, LivingEntity attacker) {
		if (attacker.canDisableShield()) {
			disableShieldStance(entity, SHIELD_DISABLE_TICKS);
		}
	}
}
