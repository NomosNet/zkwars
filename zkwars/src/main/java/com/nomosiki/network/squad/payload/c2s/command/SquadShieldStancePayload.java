package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SquadShieldStancePayload(List<UUID> squadIds, boolean up) implements CustomPacketPayload {
	private static final int MAX_SQUADS = 16;

	public static final CustomPacketPayload.Type<SquadShieldStancePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_shield_stance"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadShieldStancePayload> CODEC = StreamCodec.of(
		SquadShieldStancePayload::encode,
		SquadShieldStancePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadShieldStancePayload p) {
		int squadCount = Math.min(p.squadIds.size(), MAX_SQUADS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.squadIds.get(i));
		}
		buf.writeBoolean(p.up);
	}

	private static SquadShieldStancePayload decode(RegistryFriendlyByteBuf buf) {
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUADS);
		List<UUID> squadIds = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			squadIds.add(buf.readUUID());
		}
		boolean up = buf.readBoolean();
		return new SquadShieldStancePayload(List.copyOf(squadIds), up);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
