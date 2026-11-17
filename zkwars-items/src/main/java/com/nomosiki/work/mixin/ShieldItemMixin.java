package com.nomosiki.work.mixin;

import com.nomosiki.work.weapon.TwoHandedWeapons;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShieldItem.class)
abstract class ShieldItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void zk$denyTwoHandedShield(
		Level level,
		Player player,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
	) {
		if (TwoHandedWeapons.otherHandIsTwoHanded(player, hand)) {
			cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
		}
	}
}
