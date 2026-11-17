package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionInvitePayload(String targetName) implements CustomPacketPayload {
	public static final Type<FactionInvitePayload> TYPE = new Type<>(NomosikiMod.id("faction_invite"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionInvitePayload> CODEC = StreamCodec.of(
		(buf, payload) -> buf.writeUtf(payload.targetName, 32),
		buf -> new FactionInvitePayload(buf.readUtf(32))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
