package com.nomosiki.network.duel;

import com.nomosiki.duel.DuelService;
import com.nomosiki.network.duel.payload.c2s.DuelActionPayload;
import com.nomosiki.network.duel.payload.c2s.DuelOpenPayload;
import com.nomosiki.network.duel.payload.s2c.DuelScreenPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DuelNetworking {
	private DuelNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(DuelOpenPayload.TYPE, DuelOpenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DuelActionPayload.TYPE, DuelActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DuelScreenPayload.TYPE, DuelScreenPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(DuelOpenPayload.TYPE, (payload, context) ->
			context.server().execute(() -> DuelService.get(context.player().server).sendScreen(context.player())));

		ServerPlayNetworking.registerGlobalReceiver(DuelActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleAction(context.player(), payload)));
	}

	private static void handleAction(ServerPlayer player, DuelActionPayload payload) {
		DuelService duels = DuelService.get(player.server);
		Component error = switch (payload.action()) {
			case DuelActionPayload.ACCEPT -> duels.accept(player);
			case DuelActionPayload.DENY -> duels.deny(player);
			case DuelActionPayload.CANCEL -> duels.cancel(player);
			case DuelActionPayload.CHALLENGE -> {
				ServerPlayer target = player.server.getPlayerList().getPlayer(payload.target());
				if (target == null) {
					yield Component.translatable("message.nomosiki.duel.offline");
				}
				yield duels.challenge(player, target);
			}
			default -> Component.translatable("message.nomosiki.duel.none");
		};
		if (error != null) {
			player.displayClientMessage(error, false);
		}
	}
}
