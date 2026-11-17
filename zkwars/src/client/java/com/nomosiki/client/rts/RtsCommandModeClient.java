package com.nomosiki.client.rts;

import com.nomosiki.network.rts.payload.c2s.RtsCommandModePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RtsCommandModeClient {
	private RtsCommandModeClient() {
	}

	public static void syncActive(boolean active) {
		ClientPlayNetworking.send(new RtsCommandModePayload(active));
	}
}
