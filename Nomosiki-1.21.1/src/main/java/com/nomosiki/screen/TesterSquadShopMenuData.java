package com.nomosiki.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TesterSquadShopMenuData(BlockPos pos) {
	public static final StreamCodec<RegistryFriendlyByteBuf, TesterSquadShopMenuData> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC,
		TesterSquadShopMenuData::pos,
		TesterSquadShopMenuData::new
	);
}
