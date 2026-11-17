package com.nomosiki.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class SakuraButton extends Button {
	public SakuraButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
	}

	public SakuraButton withTooltip(Tooltip tooltip) {
		this.setTooltip(tooltip);
		return this;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawButton(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isHoveredOrFocused(), this.active);
		Font font = Minecraft.getInstance().font;
		int color = this.active ? SakuraStyle.INK : SakuraStyle.INK_DISABLED;
		String clipped = font.plainSubstrByWidth(this.getMessage().getString(), Math.max(8, this.getWidth() - 6));
		int tw = font.width(clipped);
		graphics.drawString(font, clipped, this.getX() + (this.getWidth() - tw) / 2, this.getY() + (this.getHeight() - 8) / 2, color, false);
	}
}
