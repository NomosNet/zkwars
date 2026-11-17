package com.nomosiki.network.rts.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RtsCommandModePayload(boolean active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RtsCommandModePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("rts_command_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RtsCommandModePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, RtsCommandModePayload::active,
		RtsCommandModePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
