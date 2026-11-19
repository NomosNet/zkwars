package com.nomosiki.network.match.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MatchBuildPlacePayload(String buildingId, BlockPos pos) implements CustomPacketPayload {
	public static final Type<MatchBuildPlacePayload> TYPE = new Type<>(NomosikiMod.id("match_build_place"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MatchBuildPlacePayload> CODEC = StreamCodec.of(
		MatchBuildPlacePayload::encode,
		MatchBuildPlacePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, MatchBuildPlacePayload p) {
		buf.writeUtf(p.buildingId, 32);
		buf.writeBlockPos(p.pos);
	}

	private static MatchBuildPlacePayload decode(RegistryFriendlyByteBuf buf) {
		return new MatchBuildPlacePayload(buf.readUtf(32), buf.readBlockPos());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
