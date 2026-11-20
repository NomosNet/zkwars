package com.nomosiki.client.rts;

import com.nomosiki.client.match.MatchClient;
import com.nomosiki.network.match.payload.s2c.MatchHudPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Top strip of match authority, population and points. Hidden outside a running match. */
public final class TacticalResourceBar {
	private TacticalResourceBar() {
	}

	public static void render(GuiGraphics graphics, Font font, int screenWidth) {
		if (!TacticalChrome.matchRunning()) {
			return;
		}
		MatchHudPayload hud = MatchClient.last();
		graphics.fill(0, 0, screenWidth, TacticalChrome.TOP_H, 0xCC1A100C);
		graphics.fill(0, TacticalChrome.TOP_H - 1, screenWidth, TacticalChrome.TOP_H, 0xFFD4A574);
		int min = hud.remainingSeconds() / 60;
		int sec = hud.remainingSeconds() % 60;
		int enemyPoints = Math.max(0, hud.totalPoints() - hud.ownPoints());
		String text = Component.translatable("hud.nomosiki.match.timer", String.format("%d:%02d", min, sec)).getString()
			+ "   "
			+ Component.translatable("hud.nomosiki.match.authority", hud.authority(), hud.income()).getString()
			+ "   "
			+ Component.translatable("hud.nomosiki.match.pop", hud.pop(), hud.popCap()).getString()
			+ "   "
			+ Component.translatable("hud.nomosiki.match.points_split", hud.ownPoints(), enemyPoints).getString();
		graphics.drawString(font, text, 8, 4, 0xFFFFE0A8, false);
	}
}
