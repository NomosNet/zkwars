package com.nomosiki.work.client.mixin;

import com.nomosiki.work.gun.ScopedMusketItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerMixin {
	@Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
	private void nomosikiIsScoping(CallbackInfoReturnable<Boolean> ci) {
		if (ScopedMusketItem.isScoping) {
			ci.setReturnValue(true);
		}
	}
}
