package com.nomosiki.compat.emote.network;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record NomosikSquadEmoteC2SPayload(UUID emoteId, String emoteKey, List<UUID> squadIds)
	implements CustomPacketPayload {
	private static final int MAX_SQUADS = 16;
	private static final int MAX_KEY_LEN = 128;

	public static final Type<NomosikSquadEmoteC2SPayload> TYPE =
		new Type<>(NomosikiMod.id("squad_emote_play"));

	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikSquadEmoteC2SPayload> CODEC = StreamCodec.of(
		NomosikSquadEmoteC2SPayload::encode,
		NomosikSquadEmoteC2SPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, NomosikSquadEmoteC2SPayload p) {
		buf.writeUUID(p.emoteId);
		String key = p.emoteKey == null ? "" : p.emoteKey;
		if (key.length() > MAX_KEY_LEN) {
			key = key.substring(0, MAX_KEY_LEN);
		}
		buf.writeUtf(key, MAX_KEY_LEN);
		int squadCount = Math.min(p.squadIds.size(), MAX_SQUADS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.squadIds.get(i));
		}
	}

	private static NomosikSquadEmoteC2SPayload decode(RegistryFriendlyByteBuf buf) {
		UUID emoteId = buf.readUUID();
		String emoteKey = buf.readUtf(MAX_KEY_LEN);
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUADS);
		List<UUID> squadIds = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			squadIds.add(buf.readUUID());
		}
		return new NomosikSquadEmoteC2SPayload(emoteId, emoteKey, List.copyOf(squadIds));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
