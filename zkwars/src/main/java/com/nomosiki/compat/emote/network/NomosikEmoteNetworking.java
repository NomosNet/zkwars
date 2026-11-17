package com.nomosiki.compat.emote.network;

import com.nomosiki.compat.emote.server.NomosikSquadEmoteService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class NomosikEmoteNetworking {
	private NomosikEmoteNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(NomosikSquadEmoteC2SPayload.TYPE, NomosikSquadEmoteC2SPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(NomosikSquadEmoteStopC2SPayload.TYPE, NomosikSquadEmoteStopC2SPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NomosikSquadEmoteBatchS2CPayload.TYPE, NomosikSquadEmoteBatchS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NomosikSquadEmoteAckS2CPayload.TYPE, NomosikSquadEmoteAckS2CPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(NomosikSquadEmoteC2SPayload.TYPE, (payload, context) ->
			context.server().execute(() -> NomosikSquadEmoteService.play(
				context.player(), payload.emoteId(), payload.emoteKey(), payload.squadIds())));

		ServerPlayNetworking.registerGlobalReceiver(NomosikSquadEmoteStopC2SPayload.TYPE, (payload, context) ->
			context.server().execute(() -> NomosikSquadEmoteService.stopSquads(
				context.player(), payload.squadIds())));
	}
}
