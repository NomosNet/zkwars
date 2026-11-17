package com.nomosiki.compat.emote.network;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NomosikSquadEmoteAckS2CPayload(int applied, int skippedNonHumanoid, boolean stop)
	implements CustomPacketPayload {

	public static final Type<NomosikSquadEmoteAckS2CPayload> TYPE =
		new Type<>(NomosikiMod.id("squad_emote_ack"));

	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikSquadEmoteAckS2CPayload> CODEC = StreamCodec.of(
		(buf, p) -> {
			buf.writeVarInt(p.applied);
			buf.writeVarInt(p.skippedNonHumanoid);
			buf.writeBoolean(p.stop);
		},
		buf -> new NomosikSquadEmoteAckS2CPayload(buf.readVarInt(), buf.readVarInt(), buf.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
