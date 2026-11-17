package com.nomosiki.network.squad.payload.c2s.command;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order selected squads to attack and pursue a specific enemy squad or player.
 */
public record SquadAttackTargetPayload(
	List<UUID> attackerSquadIds,
	UUID targetSquadId,
	int zoneRadius,
	boolean targetIsPlayer
) implements CustomPacketPayload {

	private static final int MAX_SQUAD_TARGETS = 16;

	public static final Type<SquadAttackTargetPayload> TYPE =
		new Type<>(NomosikiMod.id("squad_attack_target"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadAttackTargetPayload> CODEC = StreamCodec.of(
		SquadAttackTargetPayload::encode,
		SquadAttackTargetPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadAttackTargetPayload p) {
		int squadCount = Math.min(p.attackerSquadIds.size(), MAX_SQUAD_TARGETS);
		buf.writeVarInt(squadCount);
		for (int i = 0; i < squadCount; i++) {
			buf.writeUUID(p.attackerSquadIds.get(i));
		}
		buf.writeUUID(p.targetSquadId);
		buf.writeVarInt(p.zoneRadius);
		buf.writeBoolean(p.targetIsPlayer);
	}

	private static SquadAttackTargetPayload decode(RegistryFriendlyByteBuf buf) {
		int squadCount = Math.min(buf.readVarInt(), MAX_SQUAD_TARGETS);
		List<UUID> attackers = new ArrayList<>(squadCount);
		for (int i = 0; i < squadCount; i++) {
			attackers.add(buf.readUUID());
		}
		UUID target = buf.readUUID();
		int zoneRadius = buf.readVarInt();
		boolean targetIsPlayer = buf.readBoolean();
		return new SquadAttackTargetPayload(List.copyOf(attackers), target, zoneRadius, targetIsPlayer);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
