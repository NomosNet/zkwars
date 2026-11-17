package com.nomosiki.client;

import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.network.squad.payload.c2s.management.SquadAddMemberPayload;
import com.nomosiki.network.squad.payload.c2s.sync.SquadArmyStateRequestPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadAssignNearbyPayload;
import com.nomosiki.network.squad.payload.c2s.command.NinjaMarkPlayersPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadAttackTargetPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadCombatModePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadCommandPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadDeployFormationPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadCreatePayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadDisbandPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadEditPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadFormationPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadFormationShiftPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadJoinByEntityPayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadRemoveMemberPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadMountCommandPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadRangedFirePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadShieldStancePayload;
import com.nomosiki.network.squad.payload.c2s.management.SquadSetActivePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadWaypointPayload;
import com.nomosiki.squad.SquadType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SquadCommandClient {
	private SquadCommandClient() {
	}

	public static void send(SquadOrder order, BlockPos anchor, int zoneRadius, List<Integer> targetEntityIds, List<UUID> orderForSquadIds) {
		ClientPlayNetworking.send(new SquadCommandPayload(order.id(), anchor, zoneRadius, targetEntityIds, orderForSquadIds));
	}

	public static void sendCombatMode(NomosikCombatMode mode, List<Integer> targetEntityIds, Optional<UUID> forSquadId) {
		ClientPlayNetworking.send(new SquadCombatModePayload(mode.id(), targetEntityIds, forSquadId));
	}

	public static void sendFormation(SquadFormation formation, float spacing, List<Integer> targetEntityIds, Optional<UUID> forSquadId) {
		ClientPlayNetworking.send(new SquadFormationPayload(formation.id(), spacing, targetEntityIds, forSquadId));
	}

	public static void createSquad(String name, byte iconId, SquadType type) {
		ClientPlayNetworking.send(new SquadCreatePayload(name == null ? "" : name, iconId, type.id()));
	}

	public static void editSquad(UUID id, String name, byte iconId) {
		ClientPlayNetworking.send(new SquadEditPayload(id, name == null ? "" : name, iconId));
	}

	public static void disbandSquad(UUID id) {
		ClientPlayNetworking.send(new SquadDisbandPayload(id));
	}

	public static void addMember(UUID squadId, int entityId) {
		ClientPlayNetworking.send(new SquadAddMemberPayload(squadId, entityId));
	}

	public static void removeMember(UUID squadId, int entityId) {
		ClientPlayNetworking.send(new SquadRemoveMemberPayload(squadId, entityId));
	}

	public static void assignNearby(UUID squadId, int radius) {
		ClientPlayNetworking.send(new SquadAssignNearbyPayload(squadId, radius));
	}

	/** @param squadId {@code null} to leave any squad */
	public static void joinSquad(int entityId, @Nullable UUID squadId) {
		ClientPlayNetworking.send(new SquadJoinByEntityPayload(entityId, Optional.ofNullable(squadId)));
	}

	public static void setActiveSquad(@Nullable UUID squadId) {
		ClientPlayNetworking.send(new SquadSetActivePayload(Optional.ofNullable(squadId)));
	}

	public static void requestArmyState() {
		ClientPlayNetworking.send(new SquadArmyStateRequestPayload());
	}

	public static void requestClanRoster() {
		ClientPlayNetworking.send(new com.nomosiki.network.squad.payload.c2s.sync.ClanArmyRosterRequestPayload());
	}

	public static void watchClan(Optional<UUID> commanderId) {
		ClientPlayNetworking.send(new com.nomosiki.network.squad.payload.c2s.sync.ClanArmyWatchPayload(commanderId));
	}

	public static void giveSquad(UUID squadId, UUID targetPlayerId) {
		ClientPlayNetworking.send(new com.nomosiki.network.squad.payload.c2s.management.SquadGivePayload(squadId, targetPlayerId));
	}

	public static void sendToSquads(SquadOrder order, BlockPos anchor, int zoneRadius, List<UUID> squadIds) {
		if (squadIds.isEmpty()) {
			return;
		}
		send(order, anchor, zoneRadius, List.of(), List.copyOf(squadIds));
	}

	public static void sendAttackTarget(List<UUID> attackerSquadIds, UUID targetSquadId, int zoneRadius) {
		sendAttackTarget(attackerSquadIds, targetSquadId, zoneRadius, false);
	}

	public static void sendAttackTarget(List<UUID> attackerSquadIds, UUID targetId, int zoneRadius, boolean targetIsPlayer) {
		if (attackerSquadIds.isEmpty() || targetId == null) {
			return;
		}
		ClientPlayNetworking.send(new SquadAttackTargetPayload(
			List.copyOf(attackerSquadIds),
			targetId,
			zoneRadius,
			targetIsPlayer
		));
	}

	public static void sendNinjaMark(List<UUID> squadIds) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new NinjaMarkPlayersPayload(List.copyOf(squadIds)));
	}

	public static void sendDeployFormation(List<SquadDeployFormationPayload.Entry> entries) {
		if (entries.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadDeployFormationPayload(List.copyOf(entries)));
	}

	public static void sendCombatModeToSquads(NomosikCombatMode mode, List<UUID> squadIds) {
		for (UUID squadId : squadIds) {
			sendCombatMode(mode, List.of(), Optional.of(squadId));
		}
	}

	public static void sendFormationToSquads(SquadFormation formation, float spacing, List<UUID> squadIds) {
		for (UUID squadId : squadIds) {
			sendFormation(formation, spacing, List.of(), Optional.of(squadId));
		}
	}

	public static void addWaypoint(List<UUID> squadIds, BlockPos pos) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadWaypointPayload(
			SquadWaypointPayload.ACTION_ADD,
			List.copyOf(squadIds),
			List.of(pos)
		));
	}

	public static void clearWaypoints(List<UUID> squadIds) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadWaypointPayload(
			SquadWaypointPayload.ACTION_CLEAR,
			List.copyOf(squadIds),
			List.of()
		));
	}

	public static void sendRangedFire(List<UUID> squadIds, NomosikRangedFireMode mode) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadRangedFirePayload(List.copyOf(squadIds), mode.id(), false, BlockPos.ZERO, 0));
	}

	public static void sendShieldStance(List<UUID> squadIds, boolean up) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadShieldStancePayload(List.copyOf(squadIds), up));
	}

	public static void sendMountCommand(List<UUID> squadIds, boolean mount) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadMountCommandPayload(List.copyOf(squadIds), mount));
	}

	public static void sendStrategicFireZone(List<UUID> squadIds, BlockPos anchor, int zoneRadius) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadRangedFirePayload(
			List.copyOf(squadIds),
			NomosikRangedFireMode.STRATEGIC_FIRE.id(),
			true,
			anchor,
			zoneRadius
		));
	}

	/** Shift anchor along formation axis without changing squad order. */
	public static void sendFormationShift(byte direction, List<UUID> squadIds) {
		if (squadIds.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SquadFormationShiftPayload(direction, List.copyOf(squadIds)));
	}
}
