package com.nomosiki.compat.emote.network;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Batch play/stop for many entities in one packet. */
public record NomosikSquadEmoteBatchS2CPayload(boolean stop, List<Entry> entries) implements CustomPacketPayload {
	/** Max entries per packet; callers must chunk larger lists. */
	public static final int MAX_ENTRIES = 128;
	private static final int MAX_KEY_LEN = 128;

	public record Entry(int entityId, UUID emoteId, String emoteKey, int startTick) {
	}

	public static final Type<NomosikSquadEmoteBatchS2CPayload> TYPE =
		new Type<>(NomosikiMod.id("squad_emote_batch"));

	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikSquadEmoteBatchS2CPayload> CODEC = StreamCodec.of(
		NomosikSquadEmoteBatchS2CPayload::encode,
		NomosikSquadEmoteBatchS2CPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, NomosikSquadEmoteBatchS2CPayload p) {
		buf.writeBoolean(p.stop);
		int count = Math.min(p.entries.size(), MAX_ENTRIES);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			Entry e = p.entries.get(i);
			buf.writeVarInt(e.entityId);
			buf.writeUUID(e.emoteId);
			String key = e.emoteKey == null ? "" : e.emoteKey;
			if (key.length() > MAX_KEY_LEN) {
				key = key.substring(0, MAX_KEY_LEN);
			}
			buf.writeUtf(key, MAX_KEY_LEN);
			buf.writeVarInt(e.startTick);
		}
	}

	private static NomosikSquadEmoteBatchS2CPayload decode(RegistryFriendlyByteBuf buf) {
		boolean stop = buf.readBoolean();
		int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
		List<Entry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int entityId = buf.readVarInt();
			UUID emoteId = buf.readUUID();
			String emoteKey = buf.readUtf(MAX_KEY_LEN);
			int startTick = buf.readVarInt();
			entries.add(new Entry(entityId, emoteId, emoteKey, startTick));
		}
		return new NomosikSquadEmoteBatchS2CPayload(stop, List.copyOf(entries));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
