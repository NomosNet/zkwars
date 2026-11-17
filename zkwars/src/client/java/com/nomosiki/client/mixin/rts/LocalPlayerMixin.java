package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.rts.TacticalCameraController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(
		method = "aiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V", shift = At.Shift.AFTER)
	)
	private void nomosiki$freezeInputForTacticalCamera(CallbackInfo ci) {
		TacticalCameraController.freezePlayerMovement((LocalPlayer) (Object) this);
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void nomosiki$freezeForTacticalCamera(CallbackInfo ci) {
		TacticalCameraController.freezePlayerMovement((LocalPlayer) (Object) this);
	}
}
