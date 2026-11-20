package com.nomosiki.network.match.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record MatchReinforcePayload(UUID squadId) implements CustomPacketPayload {
	public static final Type<MatchReinforcePayload> TYPE = new Type<>(NomosikiMod.id("match_reinforce"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MatchReinforcePayload> CODEC = StreamCodec.of(
		(buf, p) -> buf.writeUUID(p.squadId),
		buf -> new MatchReinforcePayload(buf.readUUID())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
