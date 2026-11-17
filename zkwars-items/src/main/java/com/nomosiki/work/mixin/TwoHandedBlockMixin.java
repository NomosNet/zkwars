package com.nomosiki.work.mixin;

import com.nomosiki.work.weapon.TwoHandedWeapons;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class TwoHandedBlockMixin {
	@Unique
	private boolean zk$releasingTwoHandedShield;

	@Inject(method = "tick", at = @At("HEAD"))
	private void zk$dropShieldWhileTwoHanded(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player) {
			this.zk$releaseShield(player);
		}
	}

	@Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
	private void zk$twoHandedCannotBlock(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player && this.zk$releaseShield(player)) {
			cir.setReturnValue(false);
		}
	}

	@Unique
	private boolean zk$releaseShield(Player player) {
		if (this.zk$releasingTwoHandedShield || !player.isUsingItem()) {
			return false;
		}
		ItemStack using = player.getUseItem();
		if (using.isEmpty() || using.getItem().getUseAnimation(using) != UseAnim.BLOCK) {
			return false;
		}
		InteractionHand usedHand = player.getUsedItemHand();
		if (!TwoHandedWeapons.otherHandIsTwoHanded(player, usedHand)) {
			return false;
		}
		this.zk$releasingTwoHandedShield = true;
		try {
			player.stopUsingItem();
		} finally {
			this.zk$releasingTwoHandedShield = false;
		}
		return true;
	}
}
