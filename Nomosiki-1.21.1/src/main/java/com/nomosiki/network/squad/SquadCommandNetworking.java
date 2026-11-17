package com.nomosiki.network.squad;

import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.combat.CombatRelation;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikiHorseMount;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.squad.SquadFormationShift;
import com.nomosiki.squad.SquadFormationShiftService;
import com.nomosiki.squad.NinjaMarkService;
import com.nomosiki.squad.SquadAttackTargetService;
import com.nomosiki.squad.SquadBrain;
import com.nomosiki.squad.SquadManager;
import com.nomosiki.squad.SquadService;
import com.nomosiki.network.squad.payload.c2s.command.NinjaMarkPlayersPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadAttackTargetPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadCombatModePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadCommandPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadDeployFormationPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadFormationPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadFormationShiftPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadRangedFirePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadMountCommandPayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadShieldStancePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadWaypointPayload;
import com.nomosiki.network.squad.payload.s2c.SquadOrderAckPayload;
import com.nomosiki.squad.SquadWaypointService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SquadCommandNetworking {
	private static final double RECRUIT_GUI_RANGE = 64.0;
	private static final double RECRUIT_GUI_RANGE_SQR = RECRUIT_GUI_RANGE * RECRUIT_GUI_RANGE;
	private static final double MAX_ANCHOR_DISTANCE = 512.0;
	private static final double MAX_ANCHOR_DISTANCE_SQR = MAX_ANCHOR_DISTANCE * MAX_ANCHOR_DISTANCE;
	private static final long MIN_ORDER_INTERVAL_MS = 200L;

	private static final Map<UUID, Long> LAST_ORDER_MS = new HashMap<>();

	private SquadCommandNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SquadCommandPayload.TYPE, SquadCommandPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadCombatModePayload.TYPE, SquadCombatModePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadFormationPayload.TYPE, SquadFormationPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadWaypointPayload.TYPE, SquadWaypointPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadRangedFirePayload.TYPE, SquadRangedFirePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadShieldStancePayload.TYPE, SquadShieldStancePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadMountCommandPayload.TYPE, SquadMountCommandPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadFormationShiftPayload.TYPE, SquadFormationShiftPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadDeployFormationPayload.TYPE, SquadDeployFormationPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SquadAttackTargetPayload.TYPE, SquadAttackTargetPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(NinjaMarkPlayersPayload.TYPE, NinjaMarkPlayersPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SquadFormationPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleSquadFormation(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadCommandPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleSquadCommand(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadCombatModePayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleSquadCombatMode(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadWaypointPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleWaypoint(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadRangedFirePayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleRangedFire(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadShieldStancePayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleShieldStance(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadMountCommandPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleMountCommand(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadFormationShiftPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleFormationShift(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadDeployFormationPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleDeployFormation(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(SquadAttackTargetPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleAttackTarget(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(NinjaMarkPlayersPayload.TYPE, (payload, context) ->
			context.server().execute(() -> NinjaMarkService.handleMark(payload, context.player())));
	}

	public static double recruitGuiRangeSqr() {
		return RECRUIT_GUI_RANGE_SQR;
	}

	private static double squadCommandRangeSqr() {
		double r = NomosikiServerConfig.INSTANCE.rtsCommandRange;
		return r * r;
	}

	public static void handleSquadCommand(SquadCommandPayload payload, ServerPlayer player) {
		if (!rateLimit(player)) {
			return;
		}
		SquadOrder order = payload.order();
		BlockPos anchor = payload.anchor();
		if (order != SquadOrder.FOLLOW && !anchorValid(player, anchor)) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.anchor_too_far"), true);
			return;
		}

		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		List<UUID> squadIds = payload.orderForSquadIds();

		if (!squadIds.isEmpty()) {
			SquadManager squads = SquadService.get(player.server);
			int affected = 0;
			int squadsHit = 0;
			Optional<UUID> ackSquad = squadIds.size() == 1 ? Optional.of(squadIds.get(0)) : Optional.empty();
			for (UUID squadId : squadIds) {
				if (!squads.isCommanderOf(player, squadId)) {
					continue;
				}
				SquadWaypointService.clearOnDirectOrder(squadId);
				int before = affected;
				for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
					if (!ownerId.equals(nomosik.getOwnerUUID())) {
						continue;
					}
					if (player.distanceToSqr(nomosik) > squadCommandRangeSqr()) {
						continue;
					}
					nomosik.applySquadOrder(order, anchor, payload.zoneRadius());
					affected++;
				}
				if (affected > before) {
					squadsHit++;
				}
			}
			if (affected == 0) {
				player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
				return;
			}
			NomosikiAdvancements.onOrder(player, order, squadsHit);
			sendAck(player, order, anchor, payload.zoneRadius(), affected, ackSquad);
			SquadNetworking.sendArmyStateAfterOrder(player, squads);
			return;
		}

		List<Integer> filter = payload.targetEntityIds();
		Set<Integer> idFilter = filter.isEmpty() ? null : new HashSet<>(filter);
		int affected = 0;
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof NomosikEntity nomosik)) {
				continue;
			}
			if (!ownerId.equals(nomosik.getOwnerUUID())) {
				continue;
			}
			if (player.distanceToSqr(nomosik) > squadCommandRangeSqr()) {
				continue;
			}
			if (idFilter != null && !idFilter.contains(entity.getId())) {
				continue;
			}
			nomosik.applySquadOrder(order, anchor, payload.zoneRadius());
			affected++;
		}
		if (affected > 0) {
			NomosikiAdvancements.onOrder(player, order, 0);
			sendAck(player, order, anchor, payload.zoneRadius(), affected, Optional.empty());
		}
		SquadNetworking.sendArmyStateAfterOrder(player, SquadService.get(player.server));
	}

	public static void handleAttackTarget(SquadAttackTargetPayload payload, ServerPlayer player) {
		if (!rateLimit(player)) {
			return;
		}
		List<UUID> attackerIds = payload.attackerSquadIds();
		UUID targetId = payload.targetSquadId();
		if (attackerIds.isEmpty() || targetId == null) {
			return;
		}
		if (payload.targetIsPlayer()) {
			handleAttackPlayer(player, attackerIds, targetId, payload.zoneRadius());
			return;
		}
		ServerLevel level = player.serverLevel();
		SquadManager squads = SquadService.get(player.server);
		List<NomosikEntity> targetMembers = SquadAttackTargetService.livingMembers(level, squads, targetId);
		if (targetMembers.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.attack_target_gone"), true);
			return;
		}
		UUID ownerId = player.getUUID();
		for (NomosikEntity target : targetMembers) {
			if (ownerId.equals(target.getOwnerUUID())) {
				return;
			}
		}
		Vec3 center = SquadAttackTargetService.centroid(targetMembers);
		BlockPos anchor = BlockPos.containing(center.x, center.y, center.z);
		if (!anchorValid(player, anchor)) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.anchor_too_far"), true);
			return;
		}
		int zoneRadius = Mth.clamp(payload.zoneRadius(), 4, 24);
		List<NomosikEntity> inRange = attackersInRange(player, squads, attackerIds, targetId);
		if (inRange.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
			return;
		}
		boolean mayAttack = false;
		for (NomosikEntity attacker : inRange) {
			for (NomosikEntity target : targetMembers) {
				if (CombatRelation.mayAttack(attacker, target)) {
					mayAttack = true;
					break;
				}
			}
			if (mayAttack) {
				break;
			}
		}
		if (!mayAttack) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.cannot_attack_squad"), true);
			return;
		}
		issueAttackTarget(player, squads, attackerIds, inRange, targetId, anchor, zoneRadius, false);
	}

	private static void handleAttackPlayer(ServerPlayer player, List<UUID> attackerIds, UUID targetPlayerId, int rawZoneRadius) {
		if (player.getUUID().equals(targetPlayerId)) {
			return;
		}
		ServerLevel level = player.serverLevel();
		Player target = level.getPlayerByUUID(targetPlayerId);
		if (target == null || !target.isAlive() || target.isRemoved()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.attack_player_gone"), true);
			return;
		}
		BlockPos anchor = BlockPos.containing(target.getX(), target.getY(), target.getZ());
		if (!anchorValid(player, anchor)) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.anchor_too_far"), true);
			return;
		}
		SquadManager squads = SquadService.get(player.server);
		int zoneRadius = Mth.clamp(rawZoneRadius, 4, 24);
		List<NomosikEntity> inRange = attackersInRange(player, squads, attackerIds, null);
		if (inRange.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
			return;
		}
		boolean mayAttack = false;
		for (NomosikEntity attacker : inRange) {
			if (CombatRelation.mayAttack(attacker, target)) {
				mayAttack = true;
				break;
			}
		}
		if (!mayAttack) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.cannot_attack_player"), true);
			return;
		}
		issueAttackTarget(player, squads, attackerIds, inRange, targetPlayerId, anchor, zoneRadius, true);
	}

	private static List<NomosikEntity> attackersInRange(
		ServerPlayer player,
		SquadManager squads,
		List<UUID> attackerIds,
		@Nullable UUID excludedTargetSquadId
	) {
		UUID ownerId = player.getUUID();
		double rangeSqr = squadCommandRangeSqr();
		ServerLevel level = player.serverLevel();
		List<NomosikEntity> inRange = new ArrayList<>();
		for (UUID squadId : attackerIds) {
			if (squadId.equals(excludedTargetSquadId) || !squads.isCommanderOf(player, squadId)) {
				continue;
			}
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				inRange.add(nomosik);
			}
		}
		return inRange;
	}

	private static void issueAttackTarget(
		ServerPlayer player,
		SquadManager squads,
		List<UUID> attackerIds,
		List<NomosikEntity> inRange,
		UUID targetId,
		BlockPos anchor,
		int zoneRadius,
		boolean targetIsPlayer
	) {
		Set<UUID> cleared = new HashSet<>();
		int affected = 0;
		for (NomosikEntity nomosik : inRange) {
			Optional<UUID> squadId = nomosik.getSquadId();
			if (squadId.isPresent() && cleared.add(squadId.get())) {
				SquadWaypointService.clearOnDirectOrder(squadId.get());
			}
			nomosik.applySquadAttackTarget(targetId, anchor, zoneRadius, targetIsPlayer);
			affected++;
		}
		if (affected > 0) {
			NomosikiAdvancements.onAttackOrder(player, cleared.size());
		}
		Optional<UUID> ackSquad = attackerIds.size() == 1 ? Optional.of(attackerIds.get(0)) : Optional.empty();
		sendAck(player, SquadOrder.ATTACK_TARGET, anchor, zoneRadius, affected, ackSquad);
		SquadNetworking.sendArmyStateAfterOrder(player, squads);
	}

	public static void handleSquadCombatMode(SquadCombatModePayload payload, ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		NomosikCombatMode mode = payload.mode();
		Optional<UUID> squadOpt = payload.forSquadId();
		double rangeSqr = squadCommandRangeSqr();

		if (squadOpt.isPresent()) {
			SquadManager squads = SquadService.get(player.server);
			UUID squadId = squadOpt.get();
			if (!squads.isCommanderOf(player, squadId)) {
				return;
			}
			int affected = 0;
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				nomosik.setCombatMode(mode);
				affected++;
			}
			if (affected == 0) {
				player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
			} else {
				NomosikiAdvancements.grant(player, NomosikiActions.STANCE);
				SquadNetworking.sendArmyStateAfterOrder(player, squads);
			}
			return;
		}

		List<Integer> filter = payload.targetEntityIds();
		Set<Integer> idFilter = filter.isEmpty() ? null : new HashSet<>(filter);
		int affected = 0;
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof NomosikEntity nomosik)) {
				continue;
			}
			if (!ownerId.equals(nomosik.getOwnerUUID())) {
				continue;
			}
			if (player.distanceToSqr(nomosik) > rangeSqr) {
				continue;
			}
			if (idFilter != null && !idFilter.contains(entity.getId())) {
				continue;
			}
			nomosik.setCombatMode(mode);
			affected++;
		}
		if (affected > 0) {
			NomosikiAdvancements.grant(player, NomosikiActions.STANCE);
		}
	}

	public static void handleSquadFormation(SquadFormationPayload payload, ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		var formation = payload.formation();
		float spacing = payload.spacingOrDefault();
		Optional<UUID> squadOpt = payload.forSquadId();
		double rangeSqr = squadCommandRangeSqr();

		if (squadOpt.isPresent()) {
			SquadManager squads = SquadService.get(player.server);
			UUID squadId = squadOpt.get();
			if (!squads.isCommanderOf(player, squadId)) {
				return;
			}
			int affected = 0;
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				nomosik.setSquadFormationServer(formation);
				nomosik.setSquadFormationSpacingServer(spacing);
				affected++;
			}
			if (affected == 0) {
				player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
			} else {
				NomosikiAdvancements.grant(player, NomosikiActions.FORMATION);
				SquadNetworking.sendArmyStateAfterOrder(player, squads);
			}
			return;
		}

		List<Integer> filter = payload.targetEntityIds();
		Set<Integer> idFilter = filter.isEmpty() ? null : new HashSet<>(filter);
		int loose = 0;
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof NomosikEntity nomosik)) {
				continue;
			}
			if (!ownerId.equals(nomosik.getOwnerUUID())) {
				continue;
			}
			if (player.distanceToSqr(nomosik) > rangeSqr) {
				continue;
			}
			if (idFilter != null && !idFilter.contains(entity.getId())) {
				continue;
			}
			nomosik.setSquadFormationServer(formation);
			nomosik.setSquadFormationSpacingServer(spacing);
			loose++;
		}
		if (loose > 0) {
			NomosikiAdvancements.grant(player, NomosikiActions.FORMATION);
		}
	}

	private static boolean rateLimit(ServerPlayer player) {
		long now = System.currentTimeMillis();
		UUID id = player.getUUID();
		Long last = LAST_ORDER_MS.get(id);
		if (last != null && now - last < MIN_ORDER_INTERVAL_MS) {
			return false;
		}
		LAST_ORDER_MS.put(id, now);
		return true;
	}

	private static boolean anchorValid(ServerPlayer player, BlockPos anchor) {
		if (anchor == null || anchor.equals(BlockPos.ZERO)) {
			return false;
		}
		Vec3 center = Vec3.atCenterOf(anchor);
		return player.position().distanceToSqr(center) <= MAX_ANCHOR_DISTANCE_SQR;
	}

	private static void sendAck(
		ServerPlayer player,
		SquadOrder order,
		BlockPos anchor,
		int zoneRadius,
		int membersAffected,
		Optional<UUID> squadId
	) {
		ServerPlayNetworking.send(player, new SquadOrderAckPayload(
			order.id(),
			anchor,
			zoneRadius,
			membersAffected,
			squadId
		));
	}

	public static void handleWaypoint(SquadWaypointPayload payload, ServerPlayer player) {
		if (!rateLimit(player)) {
			return;
		}
		if (payload.squadIds().isEmpty()) {
			return;
		}
		ServerLevel level = player.serverLevel();
		SquadManager squads = SquadService.get(player.server);
		boolean changed = false;
		boolean routed = false;
		for (UUID squadId : payload.squadIds()) {
			boolean step = switch (payload.actionId()) {
				case SquadWaypointPayload.ACTION_ADD -> {
					if (payload.points().isEmpty()) {
						yield false;
					}
					yield SquadWaypointService.addWaypoint(player, level, squads, squadId, payload.points().get(0));
				}
				case SquadWaypointPayload.ACTION_CLEAR -> {
					SquadWaypointService.clearWaypoints(player, level, squads, squadId);
					yield true;
				}
				case SquadWaypointPayload.ACTION_REPLACE_ALL -> SquadWaypointService.replaceAll(
					player, level, squads, squadId, payload.points()
				);
				default -> false;
			};
			changed |= step;
			if (step && payload.actionId() == SquadWaypointPayload.ACTION_ADD) {
				routed = true;
			}
			if (step && payload.actionId() == SquadWaypointPayload.ACTION_REPLACE_ALL && !payload.points().isEmpty()) {
				routed = true;
			}
		}
		if (routed) {
			NomosikiAdvancements.grant(player, NomosikiActions.WAYPOINT);
		}
		if (changed) {
			SquadNetworking.sendArmyState(player, squads, true);
		}
	}

	/**
	 * Shifts squad anchor(s) along the formation axis (Recruits forward/backward).
	 * Does not clear waypoint queues or change {@link SquadOrder}.
	 */
	public static void handleFormationShift(SquadFormationShiftPayload payload, ServerPlayer player) {
		if (!rateLimit(player)) {
			return;
		}
		if (payload.squadIds().isEmpty()) {
			return;
		}
		int signedStep = switch (payload.direction()) {
			case SquadFormationShiftPayload.STEP_FORWARD ->
				NomosikiServerConfig.INSTANCE.rtsFormationStepBlocks;
			case SquadFormationShiftPayload.STEP_BACKWARD ->
				-NomosikiServerConfig.INSTANCE.rtsFormationStepBlocks;
			default -> 0;
		};
		if (signedStep == 0) {
			return;
		}

		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		SquadManager squads = SquadService.get(player.server);
		double rangeSqr = squadCommandRangeSqr();
		int membersShifted = 0;
		Optional<UUID> ackSquad = payload.squadIds().size() == 1
			? Optional.of(payload.squadIds().get(0))
			: Optional.empty();
		BlockPos lastAnchor = BlockPos.ZERO;
		SquadOrder lastOrder = SquadOrder.HOLD;
		int lastZoneRadius = 8;

		for (UUID squadId : payload.squadIds()) {
			if (!squads.isCommanderOf(player, squadId)) {
				continue;
			}
			List<NomosikEntity> members = squads.resolveMembersInLevel(level, squadId);
			if (members.isEmpty()) {
				continue;
			}
			NomosikEntity lead = members.get(0);
			SquadOrder order = lead.getSquadOrder();
			if (order == SquadOrder.FOLLOW) {
				continue;
			}
			if (lead.getSquadFormation() == SquadFormation.NONE) {
				continue;
			}
			BlockPos anchor = lead.getSquadAnchorPos();
			if (anchor == null) {
				continue;
			}
			Optional<BlockPos> shifted = SquadFormationShift.shiftAnchor(
				level, anchor, squadId, lead, signedStep
			);
			if (shifted.isEmpty() || !anchorValid(player, shifted.get())) {
				continue;
			}
			boolean inRange = false;
			for (NomosikEntity nomosik : members) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				inRange = true;
				break;
			}
			if (!inRange) {
				continue;
			}
			SquadFormationShiftService.markOrientationPreserve(squadId);
			BlockPos newAnchor = shifted.get();
			int zoneRadius = lead.getSquadZoneRadiusForGui();
			for (NomosikEntity nomosik : members) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				nomosik.applySquadOrder(order, newAnchor, zoneRadius);
				membersShifted++;
			}
			lastAnchor = newAnchor;
			lastOrder = order;
			lastZoneRadius = zoneRadius;
		}

		if (membersShifted == 0) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.formation_shift_failed"), true);
			return;
		}
		sendAck(player, lastOrder, lastAnchor, lastZoneRadius, membersShifted, ackSquad);
		Component msg = signedStep > 0
			? Component.translatable("message.nomosiki.rts.formation_shift_forward", Math.abs(signedStep))
			: Component.translatable("message.nomosiki.rts.formation_shift_backward", Math.abs(signedStep));
		player.displayClientMessage(msg, true);
		SquadNetworking.sendArmyStateAfterOrder(player, squads);
	}

	public static void handleRangedFire(SquadRangedFirePayload payload, ServerPlayer player) {
		if (payload.squadIds().isEmpty()) {
			return;
		}
		if (payload.hasZone() && !anchorValid(player, payload.zoneAnchor())) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.anchor_too_far"), true);
			return;
		}
		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		SquadManager squads = SquadService.get(player.server);
		double rangeSqr = squadCommandRangeSqr();
		int affected = 0;
		for (UUID squadId : payload.squadIds()) {
			if (!squads.isCommanderOf(player, squadId)) {
				continue;
			}
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID()) || !nomosik.canUseRangedFireOrders()) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				if (payload.hasZone()) {
					nomosik.setRangedFireZone(payload.zoneAnchor(), payload.zoneRadius());
				} else {
					nomosik.setRangedFireMode(payload.mode());
				}
				affected++;
			}
		}
		if (affected == 0) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
			return;
		}
		if (payload.hasZone()) {
			NomosikiAdvancements.grant(player, NomosikiActions.BOMBARD);
		}
		SquadNetworking.sendArmyState(player, squads, true);
	}

	public static void handleShieldStance(SquadShieldStancePayload payload, ServerPlayer player) {
		if (payload.squadIds().isEmpty()) {
			return;
		}
		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		SquadManager squads = SquadService.get(player.server);
		double rangeSqr = squadCommandRangeSqr();
		int inRange = 0;
		int affected = 0;
		for (UUID squadId : payload.squadIds()) {
			if (!squads.isCommanderOf(player, squadId)) {
				continue;
			}
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				inRange++;
				if (!nomosik.canUseShieldStance()) {
					continue;
				}
				nomosik.setShieldStance(payload.up());
				affected++;
			}
		}
		if (affected == 0) {
			player.displayClientMessage(Component.translatable(
				inRange == 0 ? "message.nomosiki.rts.out_of_range" : "message.nomosiki.rts.no_shields"
			), true);
			return;
		}
		SquadNetworking.sendArmyState(player, squads, true);
	}

	public static void handleMountCommand(SquadMountCommandPayload payload, ServerPlayer player) {
		if (payload.squadIds().isEmpty()) {
			return;
		}
		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		SquadManager squads = SquadService.get(player.server);
		double rangeSqr = squadCommandRangeSqr();
		List<NomosikEntity> members = new ArrayList<>();
		for (UUID squadId : payload.squadIds()) {
			if (!squads.isCommanderOf(player, squadId)) {
				continue;
			}
			for (NomosikEntity nomosik : squads.resolveMembersInLevel(level, squadId)) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > rangeSqr) {
					continue;
				}
				members.add(nomosik);
			}
		}
		if (members.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
			return;
		}
		if (payload.mount()) {
			java.util.List<NomosikEntity> riders = new java.util.ArrayList<>();
			for (NomosikEntity nomosik : members) {
				if (nomosik.isCaptain()) {
					nomosik.setWantsHelm(true);
					com.nomosiki.compat.ship.ZkShipCompat.claimNearbyHelm(
						nomosik, com.nomosiki.entity.ai.CaptainHelmGoal.CLAIM_REACH);
				} else {
					riders.add(nomosik);
				}
			}
			com.nomosiki.compat.ship.ZkShipCompat.trySeatNearby(riders, level, NomosikiHorseMount.MOUNT_SEARCH_RADIUS);
			java.util.List<NomosikEntity> horses = new java.util.ArrayList<>();
			java.util.List<NomosikEntity> guns = new java.util.ArrayList<>();
			for (NomosikEntity nomosik : riders) {
				if (nomosik.isArtillerist()) {
					guns.add(nomosik);
				} else {
					horses.add(nomosik);
				}
			}
			NomosikiHorseMount.tryMountNearby(horses, level, NomosikiHorseMount.MOUNT_SEARCH_RADIUS);
			com.nomosiki.compat.siege.SiegeMachinesCompat.tryMountNearby(guns, level, NomosikiHorseMount.MOUNT_SEARCH_RADIUS);
		} else {
			for (NomosikEntity nomosik : members) {
				if (nomosik.isCaptain()) {
					nomosik.setWantsHelm(false);
					com.nomosiki.compat.ship.ZkShipCompat.releaseHelm(nomosik);
					nomosik.getNavigation().stop();
				} else if (nomosik.isArtillerist()) {
					nomosik.setRangedFireMode(NomosikRangedFireMode.FIRE_AT_WILL);
					com.nomosiki.compat.siege.SiegeMachinesCompat.dismount(nomosik);
					nomosik.getNavigation().stop();
				} else {
					NomosikiHorseMount.dismountKeepMount(nomosik);
				}
			}
		}
		SquadNetworking.sendArmyState(player, squads, true);
	}

	public static void handleDeployFormation(SquadDeployFormationPayload payload, ServerPlayer player) {
		if (!rateLimit(player) || payload.entries().isEmpty()) {
			return;
		}
		ServerLevel level = player.serverLevel();
		UUID ownerId = player.getUUID();
		SquadManager squads = SquadService.get(player.server);
		int affected = 0;
		SquadOrder lastOrder = SquadOrder.MOVE_TO;
		BlockPos lastAnchor = BlockPos.ZERO;
		int lastZone = 8;
		Optional<UUID> ackSquad = payload.entries().size() == 1
			? Optional.of(payload.entries().get(0).squadId())
			: Optional.empty();
		for (SquadDeployFormationPayload.Entry entry : payload.entries()) {
			if (!squads.isCommanderOf(player, entry.squadId())) {
				continue;
			}
			if (!anchorValid(player, entry.anchor())) {
				continue;
			}
			SquadWaypointService.clearOnDirectOrder(entry.squadId());
			List<NomosikEntity> members = squads.resolveMembersInLevel(level, entry.squadId());
			int local = 0;
			SquadOrder order = SquadOrder.fromId(entry.orderId());
			for (NomosikEntity nomosik : members) {
				if (!ownerId.equals(nomosik.getOwnerUUID())) {
					continue;
				}
				if (player.distanceToSqr(nomosik) > squadCommandRangeSqr()) {
					continue;
				}
				if (nomosik.getSquadFormation() == SquadFormation.NONE) {
					nomosik.setSquadFormationServer(SquadFormation.LINE);
				}
				nomosik.setSquadFormationSpacingServer(entry.spacing());
				nomosik.setSquadFormationMaxInRowServer(entry.maxInRow());
				nomosik.applySquadOrder(order, entry.anchor(), entry.zoneRadius());
				local++;
			}
			if (local == 0) {
				continue;
			}
			affected += local;
			lastOrder = order;
			lastAnchor = entry.anchor();
			lastZone = entry.zoneRadius();
			double rad = Math.toRadians(entry.facingYaw());
			Vec3 forward = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
			if (forward.lengthSqr() < 1.0e-6) {
				forward = new Vec3(0.0, 0.0, 1.0);
			} else {
				forward = forward.normalize();
			}
			Vec3 anchorCenter = Vec3.atBottomCenterOf(entry.anchor());
			Vec3 behind = anchorCenter.subtract(forward);
			SquadBrain.forceOrientationAndLayout(level, entry.squadId(), entry.anchor(), behind.x, behind.z);
		}
		if (affected == 0) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.out_of_range"), true);
			return;
		}
		sendAck(player, lastOrder, lastAnchor, lastZone, affected, ackSquad);
		SquadNetworking.sendArmyStateAfterOrder(player, squads);
	}
}
