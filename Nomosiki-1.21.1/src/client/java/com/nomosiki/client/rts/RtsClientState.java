package com.nomosiki.client.rts;

import com.nomosiki.client.SquadClientState;
import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.network.squad.payload.s2c.NinjaMarkedPlayersPayload;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.network.squad.payload.s2c.SquadOrderAckPayload;
import com.nomosiki.network.squad.payload.s2c.SquadStatePayload;
import com.nomosiki.squad.SquadType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RtsClientState {
	private static final long ORDER_MESSAGE_TTL_MS = 3000L;

	private static volatile boolean hudVisible = true;
	private static volatile Component lastOrderMessage = Component.empty();
	private static volatile long lastOrderTimeMs;
	private static volatile BlockPos lastOrderAnchor = BlockPos.ZERO;
	private static volatile byte lastOrderId = SquadOrder.FOLLOW.id();
	private static volatile int lastOrderZoneRadius;

	private static volatile List<SquadArmyStatePayload.SquadRow> armySquads = List.of();
	private static volatile int armyTickId;
	private static volatile RtsArmyClassFilter classFilter = RtsArmyClassFilter.ALL;
	private static final Set<UUID> multiSelected = ConcurrentHashMap.newKeySet();
	private static volatile boolean allowEmptySelection;
	private static volatile int rangeAnchorIndex = -1;

	public static final int BOMBARD_RADIUS_MIN = 4;
	public static final int BOMBARD_RADIUS_MAX = 24;

	private static volatile boolean bombardDesignate;
	private static volatile int bombardRadius = -1;
	private static volatile List<NinjaMarkedPlayersPayload.MarkedPlayerRow> markedPlayers = List.of();

	private RtsClientState() {
	}

	public static boolean bombardDesignate() {
		return bombardDesignate;
	}

	public static void setBombardDesignate(boolean value) {
		bombardDesignate = value;
		if (!value) {
			clearBombardDrag();
		}
	}

	public static void toggleBombardDesignate() {
		setBombardDesignate(!bombardDesignate);
	}

	public static int bombardRadius() {
		if (bombardRadius < BOMBARD_RADIUS_MIN) {
			bombardRadius = Mth.clamp(
				NomosikiConfig.INSTANCE.defaultAttackZoneRadius,
				BOMBARD_RADIUS_MIN,
				BOMBARD_RADIUS_MAX
			);
		}
		return bombardRadius;
	}

	public static void setBombardRadius(int radius) {
		bombardRadius = Mth.clamp(radius, BOMBARD_RADIUS_MIN, BOMBARD_RADIUS_MAX);
	}

	private static volatile BlockPos bombardDragCenter;
	private static volatile boolean bombardDragActive;
	private static volatile long bombardDragTick;
	private static volatile BlockPos bombardHover;

	public static void beginBombardDrag(BlockPos center, long gameTime) {
		bombardDragCenter = center;
		bombardDragActive = center != null;
		bombardDragTick = gameTime;
	}

	public static boolean bombardDragActive() {
		return bombardDragActive && bombardDragCenter != null;
	}

	public static BlockPos bombardDragCenter() {
		return bombardDragCenter;
	}

	public static long bombardDragTick() {
		return bombardDragTick;
	}

	public static void clearBombardDrag() {
		bombardDragCenter = null;
		bombardDragActive = false;
		bombardDragTick = 0L;
		bombardHover = null;
	}

	public static void setBombardHover(BlockPos pos) {
		bombardHover = pos;
	}

	public static BlockPos bombardHover() {
		return bombardHover;
	}

	/** Radius of a strategic-fire zone already standing on one of the selected squads. */
	public static OptionalInt activeBombardRadius() {
		for (UUID id : orderTargetSquads()) {
			Optional<SquadArmyStatePayload.SquadRow> row = squadRow(id);
			if (row.isEmpty() || !row.get().hasFireZone()) {
				continue;
			}
			if (NomosikRangedFireMode.fromId(row.get().rangedFireModeId()) != NomosikRangedFireMode.STRATEGIC_FIRE) {
				continue;
			}
			return OptionalInt.of(Math.max(1, row.get().fireRadius()));
		}
		return OptionalInt.empty();
	}

	public static List<NinjaMarkedPlayersPayload.MarkedPlayerRow> markedPlayers() {
		return markedPlayers;
	}

	public static void applyMarkedPlayers(NinjaMarkedPlayersPayload payload) {
		markedPlayers = List.copyOf(payload.players());
	}

	public static void clearMarkedPlayers() {
		markedPlayers = List.of();
	}

	public static void initFromConfig(boolean showHudDefault) {
		hudVisible = showHudDefault;
	}

	public static boolean hudVisible() {
		return hudVisible;
	}

	public static void toggleHud() {
		hudVisible = !hudVisible;
	}

	public static Optional<UUID> selectedSquadId() {
		return SquadClientState.activeSquadId();
	}

	public static Set<UUID> multiSelectedSquads() {
		return Set.copyOf(multiSelected);
	}

	public static List<SquadArmyStatePayload.SquadRow> armySquads() {
		return armySquads;
	}

	public static Optional<SquadArmyStatePayload.SquadRow> squadRow(UUID squadId) {
		return armySquads.stream().filter(r -> r.squadId().equals(squadId)).findFirst();
	}

	public static List<BlockPos> waypointsForActiveSquad() {
		return SquadClientState.activeSquadId()
			.flatMap(RtsClientState::squadRow)
			.map(SquadArmyStatePayload.SquadRow::waypoints)
			.orElse(List.of());
	}

	public static Optional<SquadArmyStatePayload.SquadRow> activeRow() {
		return SquadClientState.activeSquadId().flatMap(RtsClientState::squadRow);
	}

	public static int armyTickId() {
		return armyTickId;
	}

	public static void applyArmyState(SquadArmyStatePayload payload) {
		armySquads = List.copyOf(payload.squads());
		armyTickId = payload.tickId();
		syncSquadClientState(payload);
		pruneSelection();
		ensureDefaultSelection();
	}

	public static RtsArmyClassFilter classFilter() {
		return classFilter;
	}

	public static void cycleClassFilter() {
		classFilter = classFilter.next();
	}

	public static List<UUID> orderTargetSquads() {
		List<UUID> raw = new ArrayList<>();
		for (SquadArmyStatePayload.SquadRow row : armySquads) {
			if (multiSelected.contains(row.squadId())) {
				raw.add(row.squadId());
			}
		}
		if (classFilter == RtsArmyClassFilter.ALL) {
			return List.copyOf(raw);
		}
		return raw.stream()
			.filter(id -> squadTypeFor(id).map(classFilter::matches).orElse(false))
			.toList();
	}

	public static int totalAliveInSquads(List<UUID> squadIds) {
		int total = 0;
		for (UUID squadId : squadIds) {
			total += memberCountFor(squadId).orElse(0);
		}
		return total;
	}

	private static void ensureDefaultSelection() {
		if (allowEmptySelection || !multiSelected.isEmpty()) {
			return;
		}
		SquadClientState.activeSquadId().ifPresent(multiSelected::add);
	}

	private static void syncSquadClientState(SquadArmyStatePayload payload) {
		List<SquadStatePayload.Brief> briefs = payload.squads().stream()
			.map(r -> new SquadStatePayload.Brief(r.squadId(), r.typeId(), r.alive(), r.name(), r.iconId()))
			.toList();
		SquadClientState.apply(new SquadStatePayload(payload.activeSquadId(), briefs));
	}

	private static void pruneSelection() {
		Set<UUID> valid = new HashSet<>();
		for (SquadArmyStatePayload.SquadRow row : armySquads) {
			valid.add(row.squadId());
		}
		multiSelected.retainAll(valid);
	}

	public static boolean isSelected(UUID squadId) {
		return multiSelected.contains(squadId);
	}

	public static void toggleSelected(UUID squadId) {
		if (multiSelected.contains(squadId)) {
			multiSelected.remove(squadId);
			if (multiSelected.isEmpty()) {
				allowEmptySelection = true;
			}
		} else {
			allowEmptySelection = false;
			multiSelected.add(squadId);
		}
	}

	public static void selectExclusive(UUID squadId) {
		allowEmptySelection = false;
		multiSelected.clear();
		multiSelected.add(squadId);
	}

	public static void clearSelection() {
		multiSelected.clear();
		allowEmptySelection = true;
		rangeAnchorIndex = -1;
	}

	public static void replaceSelection(List<UUID> squadIds) {
		multiSelected.clear();
		if (squadIds == null || squadIds.isEmpty()) {
			allowEmptySelection = true;
			return;
		}
		allowEmptySelection = false;
		multiSelected.addAll(squadIds);
	}

	public static void selectRange(int fromIndex, int toIndex) {
		List<SquadArmyStatePayload.SquadRow> rows = armySquads;
		if (rows.isEmpty()) {
			return;
		}
		int a = Mth.clamp(Math.min(fromIndex, toIndex), 0, rows.size() - 1);
		int b = Mth.clamp(Math.max(fromIndex, toIndex), 0, rows.size() - 1);
		allowEmptySelection = false;
		multiSelected.clear();
		for (int i = a; i <= b; i++) {
			multiSelected.add(rows.get(i).squadId());
		}
	}

	public static void selectAllOfType(byte typeId) {
		allowEmptySelection = false;
		multiSelected.clear();
		for (SquadArmyStatePayload.SquadRow row : armySquads) {
			if (row.typeId() == typeId) {
				multiSelected.add(row.squadId());
			}
		}
		if (multiSelected.isEmpty()) {
			allowEmptySelection = true;
		}
	}

	public static void markRangeAnchor(int index) {
		rangeAnchorIndex = index;
	}

	public static int rangeAnchorIndex() {
		return rangeAnchorIndex;
	}

	public static void ensureSelectionAfterTacticalExit() {
		allowEmptySelection = false;
		ensureDefaultSelection();
	}

	public static void setAllSelected(boolean selected) {
		multiSelected.clear();
		if (selected) {
			allowEmptySelection = false;
			for (SquadArmyStatePayload.SquadRow row : armySquads) {
				multiSelected.add(row.squadId());
			}
		} else {
			allowEmptySelection = true;
		}
	}

	public static Optional<Integer> memberCountFor(UUID squadId) {
		return squadRow(squadId).map(SquadArmyStatePayload.SquadRow::alive);
	}

	public static Optional<SquadType> squadTypeFor(UUID squadId) {
		return squadRow(squadId).map(r -> SquadType.fromId(r.typeId()));
	}

	public static void onOrderAck(SquadOrderAckPayload payload) {
		SquadOrder order = payload.order();
		lastOrderId = order.id();
		lastOrderAnchor = payload.anchor();
		lastOrderZoneRadius = payload.zoneRadius();
		lastOrderTimeMs = System.currentTimeMillis();
		Component orderLabel = orderLabel(order);
		lastOrderMessage = Component.translatable("message.nomosiki.rts.order_ack", payload.membersAffected(), orderLabel);
	}

	public static void onLocalOrderIssued(SquadOrder order, BlockPos anchor, int zoneRadius, int memberCount) {
		lastOrderId = order.id();
		lastOrderAnchor = anchor;
		lastOrderZoneRadius = zoneRadius;
		lastOrderTimeMs = System.currentTimeMillis();
		lastOrderMessage = Component.translatable("message.nomosiki.rts.order_ack", memberCount, orderLabel(order));
	}

	public static Optional<Component> lastOrderMessage() {
		if (System.currentTimeMillis() - lastOrderTimeMs > ORDER_MESSAGE_TTL_MS) {
			return Optional.empty();
		}
		return Optional.of(lastOrderMessage);
	}

	public static Optional<BlockPos> lastOrderAnchor() {
		if (System.currentTimeMillis() - lastOrderTimeMs > ORDER_MESSAGE_TTL_MS) {
			return Optional.empty();
		}
		return Optional.of(lastOrderAnchor);
	}

	public static SquadOrder lastOrder() {
		return SquadOrder.fromId(lastOrderId);
	}

	public static int lastOrderZoneRadius() {
		return lastOrderZoneRadius;
	}

	private static Component orderLabel(SquadOrder order) {
		return Component.translatable(switch (order) {
			case FOLLOW -> "button.nomosiki.squad_follow";
			case HOLD -> "button.nomosiki.squad_hold";
			case MOVE_TO -> "button.nomosiki.squad_move";
			case ATTACK_ZONE -> "button.nomosiki.squad_attack_zone";
			case ATTACK_TARGET -> "button.nomosiki.squad_attack_target";
		});
	}
}
