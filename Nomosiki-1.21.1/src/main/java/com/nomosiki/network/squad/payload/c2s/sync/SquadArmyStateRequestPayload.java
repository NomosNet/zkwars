package com.nomosiki.network.squad.payload.c2s.sync;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SquadArmyStateRequestPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadArmyStateRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_army_state_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadArmyStateRequestPayload> CODEC =
		StreamCodec.unit(new SquadArmyStateRequestPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
