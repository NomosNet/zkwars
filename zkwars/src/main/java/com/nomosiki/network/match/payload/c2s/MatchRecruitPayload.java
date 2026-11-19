package com.nomosiki.network.match.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MatchRecruitPayload(String roleId) implements CustomPacketPayload {
	public static final Type<MatchRecruitPayload> TYPE = new Type<>(NomosikiMod.id("match_recruit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MatchRecruitPayload> CODEC = StreamCodec.of(
		(buf, p) -> buf.writeUtf(p.roleId, 32),
		buf -> new MatchRecruitPayload(buf.readUtf(32))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
