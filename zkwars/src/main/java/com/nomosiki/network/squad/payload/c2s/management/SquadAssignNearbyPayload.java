package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SquadAssignNearbyPayload(UUID squadId, int radius) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadAssignNearbyPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_assign_nearby"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadAssignNearbyPayload> CODEC = StreamCodec.of(
		SquadAssignNearbyPayload::encode,
		SquadAssignNearbyPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadAssignNearbyPayload p) {
		buf.writeUUID(p.squadId);
		buf.writeVarInt(p.radius);
	}

	private static SquadAssignNearbyPayload decode(RegistryFriendlyByteBuf buf) {
		return new SquadAssignNearbyPayload(buf.readUUID(), buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
