package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.rts.TacticalCameraController;
import com.nomosiki.client.rts.TacticalHudScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void nomosiki$captureScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (TacticalHudScreen.shouldConsumeScroll(mc)) {
			return;
		}
		TacticalCameraController.onMouseScroll(vertical);
	}

	@Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
	private void nomosiki$keepCursorFreeInTacticalHud(CallbackInfo ci) {
		if (TacticalHudScreen.isOpen(Minecraft.getInstance())) {
			ci.cancel();
		}
	}
}
