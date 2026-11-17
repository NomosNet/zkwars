package com.nomosiki.network.squad.payload.c2s.management;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SquadAddMemberPayload(UUID squadId, int entityId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadAddMemberPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_add_member"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SquadAddMemberPayload> CODEC = StreamCodec.of(
		SquadAddMemberPayload::encode,
		SquadAddMemberPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadAddMemberPayload p) {
		buf.writeUUID(p.squadId);
		buf.writeVarInt(p.entityId);
	}

	private static SquadAddMemberPayload decode(RegistryFriendlyByteBuf buf) {
		return new SquadAddMemberPayload(buf.readUUID(), buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
