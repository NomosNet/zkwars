package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionInviteResponsePayload(String factionName, boolean accept) implements CustomPacketPayload {
	public static final Type<FactionInviteResponsePayload> TYPE = new Type<>(NomosikiMod.id("faction_invite_response"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionInviteResponsePayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeUtf(payload.factionName, 32);
			buf.writeBoolean(payload.accept);
		},
		buf -> new FactionInviteResponsePayload(buf.readUtf(32), buf.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
