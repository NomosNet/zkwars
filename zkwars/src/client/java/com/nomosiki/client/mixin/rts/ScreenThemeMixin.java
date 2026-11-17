package com.nomosiki.client.mixin.rts;

import com.nomosiki.client.gui.GuiThemeSwitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenThemeMixin {
	@Shadow
	protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
	private void zkshogun$themeButton(Minecraft minecraft, int width, int height, CallbackInfo ci) {
		if (!this.getClass().getName().startsWith("com.nomosiki.client.")) {
			return;
		}
		this.addRenderableWidget(GuiThemeSwitch.button(width - 62, 4));
	}
}
