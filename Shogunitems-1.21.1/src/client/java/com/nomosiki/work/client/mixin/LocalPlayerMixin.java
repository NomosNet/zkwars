package com.nomosiki.work.client.mixin;

import com.nomosiki.work.gun.ScopedMusketItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
	@Unique
	private boolean nomosikiInsideAiStep;

	@Inject(method = "canStartSprinting", at = @At("HEAD"), cancellable = true)
	private void nomosikiBlockSprintWhileScoping(CallbackInfoReturnable<Boolean> ci) {
		if (ScopedMusketItem.isScoping) {
			ci.setReturnValue(false);
		}
	}

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void nomosikiAiStepHead(CallbackInfo ci) {
		nomosikiInsideAiStep = true;
	}

	@Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
	private void nomosikiSlowWhileScoping(CallbackInfoReturnable<Boolean> ci) {
		if (nomosikiInsideAiStep && ScopedMusketItem.isScoping) {
			ci.setReturnValue(true);
		}
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void nomosikiAiStepTail(CallbackInfo ci) {
		nomosikiInsideAiStep = false;
		if (ScopedMusketItem.recoilTicks > 0) {
			ScopedMusketItem.recoilTicks--;
		}
	}

	@Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
	private void nomosikiScopeRecoil(float dt, CallbackInfoReturnable<Float> ci) {
		if (ScopedMusketItem.isScoping && ScopedMusketItem.recoilTicks > 0) {
			LocalPlayer player = (LocalPlayer) (Object) this;
			float xRot = player.getXRot() - (ScopedMusketItem.recoilTicks - dt) * ScopedMusketItem.RECOIL_AMOUNT;
			ci.setReturnValue(xRot);
		}
	}
}
