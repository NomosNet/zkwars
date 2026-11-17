package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.rts.RtsClientState;
import com.nomosiki.client.rts.RtsOrderInputHandler;
import com.nomosiki.client.rts.TacticalCameraController;
import com.nomosiki.client.rts.TacticalHudScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void nomosiki$reserveQEForTacticalCamera(
		long windowPointer,
		int key,
		int scanCode,
		int action,
		int modifiers,
		CallbackInfo ci
	) {
		if (key == GLFW.GLFW_KEY_ESCAPE
			&& action == GLFW.GLFW_PRESS
			&& RtsClientState.bombardDesignate()) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen == null
				|| mc.screen instanceof TacticalHudScreen
				|| mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen) {
				RtsOrderInputHandler.cancelBombardDesignate();
				ci.cancel();
				return;
			}
		}
		if (!TacticalCameraController.isActive()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null
			&& !(mc.screen instanceof TacticalHudScreen)
			&& !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
			return;
		}
		if (key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_E) {
			ci.cancel();
		}
	}
}
