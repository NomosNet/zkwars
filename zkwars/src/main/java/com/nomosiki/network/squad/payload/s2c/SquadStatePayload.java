package com.nomosiki.network.squad.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SquadStatePayload(Optional<UUID> activeSquadId, List<SquadStatePayload.Brief> squads) implements CustomPacketPayload {
	public record Brief(UUID squadId, byte typeId, int memberCount, String name, byte iconId) {
	}

	public static final CustomPacketPayload.Type<SquadStatePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_state"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadStatePayload> CODEC = StreamCodec.of(
		SquadStatePayload::encode,
		SquadStatePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadStatePayload p) {
		buf.writeBoolean(p.activeSquadId.isPresent());
		p.activeSquadId.ifPresent(buf::writeUUID);
		buf.writeVarInt(p.squads.size());
		for (Brief b : p.squads) {
			buf.writeUUID(b.squadId);
			buf.writeByte(b.typeId);
			buf.writeVarInt(b.memberCount);
			buf.writeUtf(b.name(), 32);
			buf.writeByte(b.iconId());
		}
	}

	private static SquadStatePayload decode(RegistryFriendlyByteBuf buf) {
		Optional<UUID> active = buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty();
		int n = Math.min(buf.readVarInt(), 64);
		List<Brief> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			list.add(new Brief(
				buf.readUUID(),
				buf.readByte(),
				buf.readVarInt(),
				buf.readUtf(32),
				buf.readByte()
			));
		}
		return new SquadStatePayload(active, List.copyOf(list));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
