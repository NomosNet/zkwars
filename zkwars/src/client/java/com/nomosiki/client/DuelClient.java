package com.nomosiki.client;

import com.nomosiki.network.duel.payload.c2s.DuelOpenPayload;
import com.nomosiki.network.duel.payload.s2c.DuelScreenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class DuelClient {
	private static DuelScreenPayload last = new DuelScreenPayload(
		0L,
		DuelScreenPayload.PHASE_IDLE,
		"",
		new UUID(0L, 0L),
		0L,
		false,
		false,
		false,
		List.of()
	);

	private DuelClient() {
	}

	public static DuelScreenPayload last() {
		return last;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(DuelScreenPayload.TYPE, (payload, context) ->
			context.client().execute(() -> apply(context.client(), payload)));
	}

	public static void open(Minecraft client, @Nullable Screen parent) {
		if (client.player == null) {
			return;
		}
		client.setScreen(new DuelScreen(parent));
		ClientPlayNetworking.send(new DuelOpenPayload());
	}

	private static void apply(Minecraft client, DuelScreenPayload payload) {
		last = payload;
		if (client.screen instanceof DuelScreen duel) {
			if (payload.closeScreen()) {
				duel.onClose();
			} else {
				duel.refresh();
			}
			return;
		}
		if (payload.requestOpen() && client.screen == null && client.player != null) {
			client.setScreen(new DuelScreen(null));
		}
	}
}
