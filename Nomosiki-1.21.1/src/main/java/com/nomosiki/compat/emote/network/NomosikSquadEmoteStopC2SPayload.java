package com.nomosiki.compat.emote.network;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record NomosikSquadEmoteStopC2SPayload(List<UUID> squadIds) implements CustomPacketPayload {
	private static final int MAX_SQUADS = 16;

	public static final Type<NomosikSquadEmoteStopC2SPayload> TYPE =
		new Type<>(NomosikiMod.id("squad_emote_stop"));

	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikSquadEmoteStopC2SPayload> CODEC = StreamCodec.of(
		NomosikSquadEmoteStopC2SPayload::encode,
		NomosikSquadEmoteStopC2SPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, NomosikSquadEmoteStopC2SPayload p) {
		int squadCount = Math.min(p.squadIds.size(), MAX_SQUADS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.squadIds.get(i));
		}
	}

	private static NomosikSquadEmoteStopC2SPayload decode(RegistryFriendlyByteBuf buf) {
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUADS);
		List<UUID> squadIds = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			squadIds.add(buf.readUUID());
		}
		return new NomosikSquadEmoteStopC2SPayload(List.copyOf(squadIds));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
