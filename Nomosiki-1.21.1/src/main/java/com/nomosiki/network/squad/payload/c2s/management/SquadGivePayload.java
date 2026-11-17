package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SquadGivePayload(UUID squadId, UUID targetPlayerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadGivePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_give"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadGivePayload> CODEC = StreamCodec.of(
		SquadGivePayload::encode,
		SquadGivePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadGivePayload payload) {
		buf.writeUUID(payload.squadId);
		buf.writeUUID(payload.targetPlayerId);
	}

	private static SquadGivePayload decode(RegistryFriendlyByteBuf buf) {
		return new SquadGivePayload(buf.readUUID(), buf.readUUID());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
