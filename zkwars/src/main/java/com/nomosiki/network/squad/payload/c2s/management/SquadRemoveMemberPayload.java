package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SquadRemoveMemberPayload(UUID squadId, int entityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadRemoveMemberPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_remove_member"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadRemoveMemberPayload> CODEC = StreamCodec.of(
		SquadRemoveMemberPayload::encode,
		SquadRemoveMemberPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadRemoveMemberPayload p) {
		buf.writeUUID(p.squadId);
		buf.writeVarInt(p.entityId);
	}

	private static SquadRemoveMemberPayload decode(RegistryFriendlyByteBuf buf) {
		return new SquadRemoveMemberPayload(buf.readUUID(), buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
