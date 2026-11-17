package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.rts.TacticalCameraController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(
		method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void nomosiki$hideCrosshairInTactical(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (TacticalCameraController.isActive()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "renderHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void nomosiki$hideHotbarInTactical(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (TacticalCameraController.isActive()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void nomosiki$hideExperienceInTactical(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (TacticalCameraController.isActive()) {
			ci.cancel();
		}
	}
}
