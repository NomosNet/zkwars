package com.nomosiki.network.duel.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DuelOpenPayload() implements CustomPacketPayload {
	public static final Type<DuelOpenPayload> TYPE = new Type<>(NomosikiMod.id("duel_open"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DuelOpenPayload> CODEC = StreamCodec.unit(new DuelOpenPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
