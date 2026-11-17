package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionOpenScreenPayload() implements CustomPacketPayload {
	public static final Type<FactionOpenScreenPayload> TYPE = new Type<>(NomosikiMod.id("faction_open_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionOpenScreenPayload> CODEC = StreamCodec.unit(new FactionOpenScreenPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
