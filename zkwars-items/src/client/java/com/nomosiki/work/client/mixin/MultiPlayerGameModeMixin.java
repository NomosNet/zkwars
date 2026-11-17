package com.nomosiki.work.client.mixin;

import com.nomosiki.work.gun.ClientUtilities;
import com.nomosiki.work.gun.GunItem;
import com.nomosiki.work.gun.ScopedMusketItem;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
	@Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
	private void nomosikiInteractAt(
		Player player,
		Entity entity,
		EntityHitResult hitResult,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> ci
	) {
		if (ScopedMusketItem.isScoping) {
			ci.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void nomosikiInteract(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
		if (ScopedMusketItem.isScoping) {
			ci.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void nomosikiUseItemOn(
		LocalPlayer player,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> ci
	) {
		if (ScopedMusketItem.isScoping) {
			ci.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void nomosikiUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
		ItemStack stack = player.getItemInHand(hand);
		if (ClientUtilities.canUseScope && hand == InteractionHand.MAIN_HAND && GunItem.isReady(stack)) {
			ClientUtilities.setScoping(player, true);
			if (ClientUtilities.attackKeyDown) {
				return;
			}
		}
		if (ScopedMusketItem.isScoping) {
			ci.setReturnValue(InteractionResult.FAIL);
			return;
		}
		if (stack.getItem() instanceof GunItem gun && GunItem.isReady(stack)
			&& gun.canUseFrom(player, hand) && ClientUtilities.preventFiring) {
			ci.setReturnValue(InteractionResult.FAIL);
		}
	}
}
