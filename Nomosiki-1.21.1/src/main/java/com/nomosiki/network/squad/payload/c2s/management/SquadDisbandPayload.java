package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SquadDisbandPayload(UUID squadId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadDisbandPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_disband"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadDisbandPayload> CODEC = StreamCodec.of(
		SquadDisbandPayload::encode,
		SquadDisbandPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadDisbandPayload p) {
		buf.writeUUID(p.squadId);
	}

	private static SquadDisbandPayload decode(RegistryFriendlyByteBuf buf) {
		return new SquadDisbandPayload(buf.readUUID());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
