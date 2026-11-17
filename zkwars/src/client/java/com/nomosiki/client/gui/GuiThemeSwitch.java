package com.nomosiki.client.gui;

import net.minecraft.client.gui.components.Button;

public final class GuiThemeSwitch {
	private GuiThemeSwitch() {
	}

	public static Button button(int x, int y) {
		ZkGuiTheme.reloadIfChanged();
		return new SakuraButton(x, y, 56, 16, ZkGuiTheme.switchLabel(), pressed -> {
			ZkGuiTheme.toggle();
			pressed.setMessage(ZkGuiTheme.switchLabel());
		});
	}
}
