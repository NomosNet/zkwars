package com.nomosiki.network.rts;

import com.nomosiki.network.rts.payload.c2s.RtsCommandModePayload;
import com.nomosiki.rts.RtsCommandModeService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class RtsNetworking {
	private RtsNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(RtsCommandModePayload.TYPE, RtsCommandModePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RtsCommandModePayload.TYPE, (payload, context) ->
			context.server().execute(() -> RtsCommandModeService.setActive(context.player(), payload.active())));
	}
}
