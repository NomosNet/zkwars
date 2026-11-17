package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.UUID;

public record SquadJoinByEntityPayload(int entityId, Optional<UUID> squadId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadJoinByEntityPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_join_by_entity"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadJoinByEntityPayload> CODEC = StreamCodec.of(
		SquadJoinByEntityPayload::encode,
		SquadJoinByEntityPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadJoinByEntityPayload p) {
		buf.writeVarInt(p.entityId);
		buf.writeBoolean(p.squadId.isPresent());
		p.squadId.ifPresent(buf::writeUUID);
	}

	private static SquadJoinByEntityPayload decode(RegistryFriendlyByteBuf buf) {
		int entityId = buf.readVarInt();
		Optional<UUID> squad = buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty();
		return new SquadJoinByEntityPayload(entityId, squad);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
