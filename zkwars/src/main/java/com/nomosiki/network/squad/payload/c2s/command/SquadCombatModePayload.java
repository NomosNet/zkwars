package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.NomosikCombatMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SquadCombatModePayload(byte modeId, List<Integer> targetEntityIds, Optional<UUID> forSquadId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SquadCombatModePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_combat_mode"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadCombatModePayload> CODEC = StreamCodec.of(
		SquadCombatModePayload::encode,
		SquadCombatModePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadCombatModePayload p) {
		buf.writeByte(p.modeId);
		buf.writeVarInt(p.targetEntityIds.size());
		for (int id : p.targetEntityIds) {
			buf.writeVarInt(id);
		}
		buf.writeBoolean(p.forSquadId.isPresent());
		p.forSquadId.ifPresent(buf::writeUUID);
	}

	private static SquadCombatModePayload decode(RegistryFriendlyByteBuf buf) {
		byte modeId = buf.readByte();
		int n = buf.readVarInt();
		List<Integer> ids = new ArrayList<>(Math.min(n, 4096));
		for (int i = 0; i < n; i++) {
			ids.add(buf.readVarInt());
		}
		Optional<UUID> squad = Optional.empty();
		if (buf.readableBytes() >= 1) {
			if (buf.readBoolean() && buf.readableBytes() >= 16) {
				squad = Optional.of(buf.readUUID());
			}
		}
		return new SquadCombatModePayload(modeId, List.copyOf(ids), squad);
	}

	public NomosikCombatMode mode() {
		return NomosikCombatMode.fromId(this.modeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
