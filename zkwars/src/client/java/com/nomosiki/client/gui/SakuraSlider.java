package com.nomosiki.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class SakuraSlider extends AbstractSliderButton {
	@FunctionalInterface
	public interface Listener {
		Component label(double normalized);

		default void apply(double normalized) {
		}
	}

	private final Listener listener;

	public SakuraSlider(int x, int y, int width, int height, double value, Listener listener) {
		super(x, y, width, height, Component.empty(), Mth.clamp(value, 0.0, 1.0));
		this.listener = listener;
		this.updateMessage();
	}

	public void setNormalized(double value) {
		this.value = Mth.clamp(value, 0.0, 1.0);
		this.updateMessage();
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.listener.label(this.value));
	}

	@Override
	protected void applyValue() {
		this.listener.apply(this.value);
		this.updateMessage();
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawSlider(
			graphics,
			Minecraft.getInstance().font,
			this.getX(),
			this.getY(),
			this.getWidth(),
			this.getHeight(),
			this.value,
			this.getMessage(),
			this.isHoveredOrFocused(),
			this.active
		);
	}
}
