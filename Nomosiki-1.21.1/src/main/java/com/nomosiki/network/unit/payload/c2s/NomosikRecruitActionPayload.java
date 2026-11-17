package com.nomosiki.network.unit.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NomosikRecruitActionPayload(int entityId, byte actionId) implements CustomPacketPayload {
	public static final byte ACTION_TOGGLE_FOLLOW = 0;
	public static final byte ACTION_CLEAR_TARGET = 1;

	public static final CustomPacketPayload.Type<NomosikRecruitActionPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("nomosik_recruit_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NomosikRecruitActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, NomosikRecruitActionPayload::entityId,
		ByteBufCodecs.BYTE, NomosikRecruitActionPayload::actionId,
		NomosikRecruitActionPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
