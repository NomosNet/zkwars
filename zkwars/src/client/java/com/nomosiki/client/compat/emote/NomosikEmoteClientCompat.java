package com.nomosiki.client.compat.emote;

import com.nomosiki.compat.emote.network.NomosikSquadEmoteAckS2CPayload;
import com.nomosiki.compat.emote.network.NomosikSquadEmoteBatchS2CPayload;
import com.nomosiki.compat.emote.network.NomosikSquadEmoteC2SPayload;
import com.nomosiki.compat.emote.network.NomosikSquadEmoteStopC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;

public final class NomosikEmoteClientCompat {
	private NomosikEmoteClientCompat() {
	}

	public static void init() {
		NomosikEmoteCatalog.ensureLoaded();

		ClientPlayNetworking.registerGlobalReceiver(NomosikSquadEmoteBatchS2CPayload.TYPE, (payload, context) ->
			context.client().execute(() -> applyBatch(context.client(), payload)));

		ClientPlayNetworking.registerGlobalReceiver(NomosikSquadEmoteAckS2CPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				Minecraft mc = context.client();
				if (mc.player == null) {
					return;
				}
				Component msg = payload.stop()
					? Component.translatable("message.nomosiki.emote.stopped", payload.applied(), payload.skippedNonHumanoid())
					: Component.translatable("message.nomosiki.emote.applied", payload.applied(), payload.skippedNonHumanoid());
				mc.player.displayClientMessage(msg, true);
			}));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			client.execute(NomosikEmoteClientState::clearAll));
	}

	public static void sendPlay(UUID emoteId, String emoteKey, List<UUID> squadIds) {
		ClientPlayNetworking.send(new NomosikSquadEmoteC2SPayload(emoteId, emoteKey, squadIds));
	}

	public static void sendStop(List<UUID> squadIds) {
		ClientPlayNetworking.send(new NomosikSquadEmoteStopC2SPayload(squadIds));
	}

	private static void applyBatch(Minecraft mc, NomosikSquadEmoteBatchS2CPayload payload) {
		if (mc.level == null) {
			return;
		}
		for (NomosikSquadEmoteBatchS2CPayload.Entry entry : payload.entries()) {
			Entity entity = mc.level.getEntity(entry.entityId());
			if (entity == null) {
				continue;
			}
			UUID uuid = entity.getUUID();
			if (payload.stop()) {
				NomosikEmoteClientState.stop(uuid);
				continue;
			}
			NomosikEmoteCatalog.find(entry.emoteId(), entry.emoteKey()).ifPresent(found ->
				NomosikEmoteClientState.start(uuid, found.definition(), entry.startTick(), entity.tickCount));
		}
	}
}
