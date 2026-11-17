package com.nomosiki.network.unit.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NomosikHirePayload(int entityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<NomosikHirePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("nomosik_hire"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikHirePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, NomosikHirePayload::entityId,
		NomosikHirePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
