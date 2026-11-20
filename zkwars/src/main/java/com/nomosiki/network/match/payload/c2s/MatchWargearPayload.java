package com.nomosiki.network.match.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record MatchWargearPayload(UUID squadId, String upgradeId) implements CustomPacketPayload {
	public static final Type<MatchWargearPayload> TYPE = new Type<>(NomosikiMod.id("match_wargear"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MatchWargearPayload> CODEC = StreamCodec.of(
		(buf, p) -> {
			buf.writeUUID(p.squadId);
			buf.writeUtf(p.upgradeId, 32);
		},
		buf -> new MatchWargearPayload(buf.readUUID(), buf.readUtf(32))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
