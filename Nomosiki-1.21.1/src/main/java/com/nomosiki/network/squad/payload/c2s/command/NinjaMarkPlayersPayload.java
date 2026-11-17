package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order selected ninja squads to mark nearby enemy players on the tactical map.
 */
public record NinjaMarkPlayersPayload(List<UUID> squadIds) implements CustomPacketPayload {

	private static final int MAX_SQUAD_TARGETS = 16;

	public static final Type<NinjaMarkPlayersPayload> TYPE =
		new Type<>(NomosikiMod.id("ninja_mark_players"));

	public static final StreamCodec<RegistryFriendlyByteBuf, NinjaMarkPlayersPayload> CODEC = StreamCodec.of(
		NinjaMarkPlayersPayload::encode,
		NinjaMarkPlayersPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, NinjaMarkPlayersPayload p) {
		int squadCount = Math.min(p.squadIds.size(), MAX_SQUAD_TARGETS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.squadIds.get(i));
		}
	}

	private static NinjaMarkPlayersPayload decode(RegistryFriendlyByteBuf buf) {
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUAD_TARGETS);
		List<UUID> squads = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			squads.add(buf.readUUID());
		}
		return new NinjaMarkPlayersPayload(List.copyOf(squads));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
