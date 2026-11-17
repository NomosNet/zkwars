package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.rts.TacticalCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void nomosiki$hideHandInTacticalCamera(Camera camera, float partialTick, Matrix4f projectionMatrix, CallbackInfo ci) {
		if (TacticalCameraController.isActive()) {
			ci.cancel();
		}
	}
}
