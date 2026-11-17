package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionOpenCreateMenuPayload() implements CustomPacketPayload {
	public static final Type<FactionOpenCreateMenuPayload> TYPE = new Type<>(NomosikiMod.id("faction_open_create_menu"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionOpenCreateMenuPayload> CODEC = StreamCodec.unit(new FactionOpenCreateMenuPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
