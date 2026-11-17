package com.nomosiki.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HireMenuData(int entityId) {
	public static final StreamCodec<RegistryFriendlyByteBuf, HireMenuData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		HireMenuData::entityId,
		HireMenuData::new
	);
}
