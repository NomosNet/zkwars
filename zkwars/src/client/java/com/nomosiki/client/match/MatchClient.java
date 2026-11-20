package com.nomosiki.client.match;

import com.nomosiki.client.rts.TacticalHudScreen;
import com.nomosiki.match.MatchPhase;
import com.nomosiki.network.match.payload.s2c.MatchHudPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class MatchClient {
	private static MatchHudPayload last = idlePayload();

	private MatchClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(MatchHudPayload.TYPE, (payload, context) ->
			context.client().execute(() -> last = payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> last = idlePayload());
		HudRenderCallback.EVENT.register(MatchClient::renderHud);
	}

	public static MatchHudPayload last() {
		return last;
	}

	private static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
		MatchHudPayload hud = last;
		if (hud.phase() != MatchPhase.RUNNING.id() && !hud.showEnd()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null) {
			return;
		}
		if (mc.screen instanceof TacticalHudScreen) {
			if (hud.showEnd()) {
				Component end = hud.winnerSide() == hud.yourSide()
					? Component.translatable("hud.nomosiki.match.victory")
					: Component.translatable("hud.nomosiki.match.defeat");
				graphics.drawCenteredString(mc.font, end, mc.getWindow().getGuiScaledWidth() / 2, 40, 0xFFFF55);
			}
			return;
		}
		int x = 8;
		int y = 8;
		if (hud.phase() == MatchPhase.RUNNING.id()) {
			int min = hud.remainingSeconds() / 60;
			int sec = hud.remainingSeconds() % 60;
			graphics.drawString(mc.font, Component.translatable(
				"hud.nomosiki.match.timer",
				String.format("%d:%02d", min, sec)
			), x, y, 0xFFFFFF, true);
			y += 12;
			graphics.drawString(mc.font, Component.translatable(
				"hud.nomosiki.match.authority",
				hud.authority(),
				hud.income()
			), x, y, 0xFFE082, true);
			y += 12;
			graphics.drawString(mc.font, Component.translatable(
				"hud.nomosiki.match.points",
				hud.ownPoints(),
				hud.totalPoints()
			), x, y, 0x80CBC4, true);
			y += 12;
			graphics.drawString(mc.font, Component.translatable(
				"hud.nomosiki.match.pop",
				hud.pop(),
				hud.popCap()
			), x, y, 0xCFD8DC, true);
			y += 12;
			graphics.drawString(mc.font, Component.translatable(
				"hud.nomosiki.match.hq",
				(int) hud.ownHqHp(),
				(int) hud.ownHqMaxHp(),
				(int) hud.enemyHqHp(),
				(int) hud.enemyHqMaxHp()
			), x, y, 0xEF9A9A, true);
		}
		if (hud.showEnd()) {
			Component end = hud.winnerSide() == hud.yourSide()
				? Component.translatable("hud.nomosiki.match.victory")
				: Component.translatable("hud.nomosiki.match.defeat");
			int w = mc.getWindow().getGuiScaledWidth();
			graphics.drawCenteredString(mc.font, end, w / 2, 40, 0xFFFF55);
		}
	}

	private static MatchHudPayload idlePayload() {
		return new MatchHudPayload(
			MatchPhase.IDLE.id(),
			0, 0, 0, 0, 0,
			0, 0, 0, 0,
			(byte) 0, (byte) -1, (byte) -1,
			0, 100, false, (byte) 0
		);
	}
}
