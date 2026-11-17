package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionLeavePayload() implements CustomPacketPayload {
	public static final Type<FactionLeavePayload> TYPE = new Type<>(NomosikiMod.id("faction_leave"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionLeavePayload> CODEC = StreamCodec.unit(new FactionLeavePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
