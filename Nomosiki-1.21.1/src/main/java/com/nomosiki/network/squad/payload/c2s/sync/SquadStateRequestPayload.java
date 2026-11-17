package com.nomosiki.network.squad.payload.c2s.sync;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SquadStateRequestPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadStateRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_state_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadStateRequestPayload> CODEC = StreamCodec.unit(new SquadStateRequestPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
