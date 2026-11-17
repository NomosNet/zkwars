package com.nomosiki.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record NomosikMenuData(int entityId) {
	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikMenuData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		NomosikMenuData::entityId,
		NomosikMenuData::new
	);
}
