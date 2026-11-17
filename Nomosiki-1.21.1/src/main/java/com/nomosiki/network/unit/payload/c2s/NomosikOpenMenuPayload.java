package com.nomosiki.network.unit.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NomosikOpenMenuPayload(int entityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<NomosikOpenMenuPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("nomosik_open_menu"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikOpenMenuPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, NomosikOpenMenuPayload::entityId,
		NomosikOpenMenuPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
