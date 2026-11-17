package com.nomosiki.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class SakuraEditBox extends EditBox {
	public SakuraEditBox(Font font, int x, int y, int width, int height, Component message) {
		super(font, x, y, width, height, message);
		this.setBordered(false);
		this.setTextColor(SakuraStyle.INK);
		this.setTextColorUneditable(SakuraStyle.INK_DISABLED);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (!this.isVisible()) {
			return;
		}
		this.setTextColor(SakuraStyle.INK);
		this.setTextColorUneditable(SakuraStyle.INK_DISABLED);
		SakuraStyle.drawEditBox(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isFocused());
		graphics.pose().pushPose();
		graphics.pose().translate(4.0F, (this.getHeight() - 8) / 2.0F, 0.0F);
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		graphics.pose().popPose();
	}
}
