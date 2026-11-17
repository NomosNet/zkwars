package com.nomosiki.client.rts;

import com.nomosiki.client.SquadClientState;
import com.nomosiki.client.SquadCommandClient;
import com.nomosiki.compat.gun.GunsCompat;
import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadFormationShiftPayload;
import com.nomosiki.squad.SquadFormationLayout;
import com.nomosiki.squad.SquadType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class RtsOrderInputHandler {
	public static final float[] FORMATION_SPACING_PRESETS = {1.0F, 2.0F, 3.0F};

	private static final SquadFormation[] FORMATION_CYCLE = {
		SquadFormation.NONE, SquadFormation.LINE, SquadFormation.SQUARE,
		SquadFormation.TRIANGLE, SquadFormation.HOLLOW_CIRCLE, SquadFormation.HOLLOW_SQUARE,
		SquadFormation.V, SquadFormation.CIRCLE, SquadFormation.MOVEMENT
	};
	private static final NomosikCombatMode[] COMBAT_MODE_CYCLE = {
		NomosikCombatMode.PASSIVE, NomosikCombatMode.NEUTRAL, NomosikCombatMode.AGGRESSIVE, NomosikCombatMode.RAID
	};

	private RtsOrderInputHandler() {
	}

	public static boolean issueCursorOrder(Minecraft mc, BlockPos anchor) {
		Player player = mc.player;
		if (player == null || anchor == null) {
			return false;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty() || RtsClientState.totalAliveInSquads(squadIds) <= 0) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return false;
		}
		NomosikiConfig cfg = NomosikiConfig.INSTANCE;
		int zoneRadius = Mth.clamp(cfg.defaultAttackZoneRadius, 4, 24);

		if ((RtsClientState.bombardDesignate() || strategicFireModifier(mc))
			&& TacticalCommandBar.hasRangedBombardSelection()) {
			confirmBombard(mc, anchor);
			return true;
		}

		if (Screen.hasShiftDown() && !Screen.hasControlDown()) {
			SquadCommandClient.addWaypoint(squadIds, anchor);
			OrderMarkerRenderer.spawnParticles(mc, anchor);
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.waypoint_added"), true);
			return true;
		}

		SquadOrder order = resolveOrderFromModifiers();
		int members = Math.max(1, RtsClientState.totalAliveInSquads(squadIds));
		SquadCommandClient.sendToSquads(order, anchor, zoneRadius, squadIds);
		RtsClientState.onLocalOrderIssued(order, anchor, zoneRadius, members);
		OrderMarkerRenderer.spawnParticles(mc, anchor);
		player.displayClientMessage(Component.translatable("message.nomosiki.rts.order_sent_local", orderLabel(order)), true);
		return true;
	}

	public static void holdAtPlayerFeet() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		Player player = mc.player;
		BlockPos anchor = TacticalCameraController.isActive()
			? TacticalCameraController.focusBlockPos()
			: player.blockPosition();
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return;
		}
		int members = Math.max(1, RtsClientState.totalAliveInSquads(squadIds));
		SquadCommandClient.sendToSquads(SquadOrder.HOLD, anchor, NomosikiConfig.INSTANCE.defaultAttackZoneRadius, squadIds);
		RtsClientState.onLocalOrderIssued(SquadOrder.HOLD, anchor, NomosikiConfig.INSTANCE.defaultAttackZoneRadius, members);
		OrderMarkerRenderer.spawnParticles(mc, anchor);
		player.displayClientMessage(Component.translatable("message.nomosiki.rts.order_sent_local", orderLabel(SquadOrder.HOLD)), true);
	}

	/** Formation step forward along the squad axis (key {@code ]}). */
	public static void formationStepForward() {
		formationStep(SquadFormationShiftPayload.STEP_FORWARD);
	}

	/** Formation step backward along the squad axis (key {@code [}). */
	public static void formationStepBackward() {
		formationStep(SquadFormationShiftPayload.STEP_BACKWARD);
	}

	private static void formationStep(byte direction) {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueFormationShift(mc)) {
			if (mc.player != null) {
				mc.player.displayClientMessage(
					Component.translatable("message.nomosiki.rts.formation_shift_failed"),
					true
				);
			}
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		SquadCommandClient.sendFormationShift(direction, squadIds);
		if (mc.player == null) {
			return;
		}
		Component msg = direction == SquadFormationShiftPayload.STEP_FORWARD
			? Component.translatable("message.nomosiki.rts.formation_shift_forward_local")
			: Component.translatable("message.nomosiki.rts.formation_shift_backward_local");
		mc.player.displayClientMessage(msg, true);
	}

	public static void cycleFormation() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return;
		}
		SquadFormation current = RtsClientState.activeRow()
			.map(row -> SquadFormation.fromId(row.formationId()))
			.orElseGet(() -> {
				Optional<UUID> activeId = SquadClientState.activeSquadId();
				return activeId.flatMap(id -> findNearestSquadMember(mc, id))
					.map(NomosikEntity::getSquadFormationForGui)
					.orElse(SquadFormation.LINE);
			});
		SquadFormation next = nextFormation(current);
		for (UUID squadId : squadIds) {
			float spacing = RtsClientState.squadRow(squadId)
				.map(SquadArmyStatePayload.SquadRow::formationSpacing)
				.orElseGet(() -> findNearestSquadMember(mc, squadId)
					.map(n -> (float) n.getSquadFormationSpacingForGui())
					.orElse((float) SquadFormationLayout.DEFAULT_SPACING));
			SquadCommandClient.sendFormation(next, spacing, List.of(), Optional.of(squadId));
		}
		mc.player.displayClientMessage(
			Component.translatable("message.nomosiki.rts.formation_cycled", next.formationsScreenLabel()),
			true
		);
	}

	public static void cycleBattlePreset() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		Optional<UUID> squadId = SquadClientState.activeSquadId();
		if (squadId.isEmpty()) {
			return;
		}
		SquadType type = RtsClientState.activeRow()
			.map(row -> SquadType.fromId(row.typeId()))
			.orElseGet(() -> SquadClientState.squadTypeFor(squadId.get()).orElse(SquadType.INFANTRY));
		RtsBattlePreset preset = RtsBattlePreset.nextFor(type);
		SquadCommandClient.sendFormation(preset.formation(), preset.spacing(), List.of(), squadId);
		mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.preset_applied", Component.translatable(preset.labelKey())), true);
	}

	public static void toggleRangedFireMode() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return;
		}
		NomosikRangedFireMode current = RtsClientState.activeRow()
			.map(row -> NomosikRangedFireMode.fromId(row.rangedFireModeId()))
			.orElse(NomosikRangedFireMode.FIRE_AT_WILL);
		NomosikRangedFireMode next = current == NomosikRangedFireMode.HOLD_FIRE
			? NomosikRangedFireMode.FIRE_AT_WILL
			: NomosikRangedFireMode.HOLD_FIRE;
		SquadCommandClient.sendRangedFire(squadIds, next);
		mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.fire_mode_set", fireModeLabel(next)), true);
	}

	public static void setCombatMode(NomosikCombatMode mode) {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		Optional<UUID> squadId = SquadClientState.activeSquadId();
		if (squadId.isEmpty()) {
			return;
		}
		SquadCommandClient.sendCombatMode(mode, List.of(), squadId);
		mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.stance_set", combatModeLabel(mode)), true);
	}

	public static void cycleFormationSpacing() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return;
		}
		float current = RtsClientState.activeRow()
			.map(SquadArmyStatePayload.SquadRow::formationSpacing)
			.orElse(FORMATION_SPACING_PRESETS[1]);
		float next = nextFormationSpacingPreset(current);
		for (UUID squadId : squadIds) {
			SquadFormation formation = RtsClientState.squadRow(squadId)
				.map(row -> SquadFormation.fromId(row.formationId()))
				.orElse(SquadFormation.LINE);
			SquadCommandClient.sendFormation(formation, next, List.of(), Optional.of(squadId));
		}
		mc.player.displayClientMessage(
			Component.translatable("message.nomosiki.rts.formation_spacing_set", formatSpacing(next)),
			true
		);
	}

	public static void cycleCombatMode() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return;
		}
		NomosikCombatMode current = RtsClientState.activeRow()
			.map(row -> NomosikCombatMode.fromId(row.combatModeId()))
			.orElse(NomosikCombatMode.NEUTRAL);
		NomosikCombatMode next = nextCombatMode(current);
		SquadCommandClient.sendCombatModeToSquads(next, squadIds);
		mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.stance_set", combatModeLabel(next)), true);
	}

	public static void toggleShieldStance() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc)) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return;
		}
		if (!TacticalCommandBar.hasShieldCapableSelection(mc)) {
			mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_shields"), true);
			return;
		}
		boolean up = RtsClientState.activeRow()
			.map(row -> !row.shieldStance())
			.orElse(true);
		SquadCommandClient.sendShieldStance(squadIds, up);
		mc.player.displayClientMessage(
			Component.translatable(up
				? "message.nomosiki.squad.shield_stance_on"
				: "message.nomosiki.squad.shield_stance_off"),
			true
		);
	}

	public static void onBombardButton() {
		Minecraft mc = Minecraft.getInstance();
		if (!canIssueWorldOrder(mc) || !TacticalCommandBar.hasRangedBombardSelection()) {
			return;
		}
		if (RtsClientState.bombardDesignate()) {
			cancelBombardDesignate();
			return;
		}
		if (RtsClientState.activeBombardRadius().isPresent()) {
			clearBombardment(mc);
			return;
		}
		RtsClientState.setBombardDesignate(true);
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.bombard_aim"), true);
		}
	}

	public static void clearBombardment(Minecraft mc) {
		if (mc == null || mc.player == null) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		SquadCommandClient.sendRangedFire(squadIds, NomosikRangedFireMode.FIRE_AT_WILL);
		RtsClientState.setBombardDesignate(false);
		mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.bombard_cleared"), true);
	}

	/** While the press is held, grow the zone from the drag centre to this ground point. A short move keeps the slider radius. */
	public static void stretchBombardRadius(BlockPos edge) {
		BlockPos center = RtsClientState.bombardDragCenter();
		if (center == null || edge == null) {
			return;
		}
		double dx = edge.getX() - center.getX();
		double dz = edge.getZ() - center.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < FormationDragMath.CLICK_WORLD_THRESHOLD) {
			return;
		}
		RtsClientState.setBombardRadius((int) Math.round(dist));
	}

	public static void cancelBombardDesignate() {
		if (!RtsClientState.bombardDesignate()) {
			return;
		}
		RtsClientState.setBombardDesignate(false);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.bombard_cancel"), true);
		}
	}

	public static void confirmBombard(Minecraft mc, BlockPos anchor) {
		Player player = mc.player;
		if (player == null || anchor == null) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty() || !TacticalCommandBar.hasRangedBombardSelection()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		int zoneRadius = RtsClientState.bombardRadius();
		SquadCommandClient.sendStrategicFireZone(squadIds, anchor, zoneRadius);
		BombardZoneRenderer.notePlaced(anchor, zoneRadius);
		RtsClientState.setBombardDesignate(false);
		OrderMarkerRenderer.spawnParticles(mc, anchor);
		if (musketeersBeyondRange(anchor)) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.bombard_out_of_range"), true);
			return;
		}
		player.displayClientMessage(Component.translatable("message.nomosiki.rts.bombard_set", zoneRadius), true);
	}

	private static boolean musketeersBeyondRange(BlockPos anchor) {
		double maxSqr = GunsCompat.RADIUS_MUSKET * GunsCompat.RADIUS_MUSKET;
		Vec3 target = Vec3.atCenterOf(anchor);
		for (UUID squadId : RtsClientState.orderTargetSquads()) {
			SquadArmyStatePayload.SquadRow row = RtsClientState.squadRow(squadId).orElse(null);
			if (row == null || !SquadType.fromId(row.typeId()).isMusketeer()) {
				continue;
			}
			if (target.distanceToSqr(row.centerX(), row.centerY(), row.centerZ()) > maxSqr) {
				return true;
			}
		}
		return false;
	}

	public static void toggleSiegeMount(Minecraft mc) {
		if (!canIssueWorldOrder(mc) || !TacticalCommandBar.hasArtilleristSelection()) {
			return;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return;
		}
		boolean dismount = TacticalCommandBar.hasSiegeMountedSelection(mc);
		SquadCommandClient.sendMountCommand(squadIds, !dismount);
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable(
				dismount ? "message.nomosiki.squad.siege_dismount" : "message.nomosiki.squad.siege_mount"
			), true);
		}
	}

	public static int formationSpacingPresetIndex(float spacing) {
		int best = 0;
		float bestDist = Float.MAX_VALUE;
		for (int i = 0; i < FORMATION_SPACING_PRESETS.length; i++) {
			float dist = Math.abs(spacing - FORMATION_SPACING_PRESETS[i]);
			if (dist < bestDist) {
				bestDist = dist;
				best = i;
			}
		}
		return best;
	}

	public static float nextFormationSpacingPreset(float current) {
		int next = (formationSpacingPresetIndex(current) + 1) % FORMATION_SPACING_PRESETS.length;
		return FORMATION_SPACING_PRESETS[next];
	}

	@Nullable
	public static BlockPos raycastAnchor() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return null;
		}
		if (TacticalCameraController.isActive()) {
			return TacticalCameraController.raycastAnchor(mc);
		}
		Player player = mc.player;
		Vec3 eye = player.getEyePosition(1f);
		Vec3 reach = eye.add(player.getViewVector(1f).scale(64.0));
		BlockHitResult hit = mc.level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) {
			return null;
		}
		return hit.getBlockPos();
	}

	private static boolean canIssueWorldOrder(Minecraft mc) {
		if (!NomosikiConfig.INSTANCE.rtsEnabled || mc.player == null) {
			return false;
		}
		if (mc.screen != null
			&& !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
			&& !(mc.screen instanceof TacticalHudScreen)) {
			return false;
		}
		List<UUID> targets = RtsClientState.orderTargetSquads();
		return !targets.isEmpty() && RtsClientState.totalAliveInSquads(targets) > 0;
	}

	/** Client-side guard: formation shift needs a non-loose formation and a stationary squad order. */
	private static boolean canIssueFormationShift(Minecraft mc) {
		if (!canIssueWorldOrder(mc)) {
			return false;
		}
		Optional<SquadArmyStatePayload.SquadRow> row = RtsClientState.activeRow();
		if (row.isEmpty()) {
			return true;
		}
		SquadArmyStatePayload.SquadRow active = row.get();
		if (SquadFormation.fromId(active.formationId()) == SquadFormation.NONE) {
			return false;
		}
		SquadOrder order = SquadOrder.fromId(active.orderId());
		return order == SquadOrder.HOLD || order == SquadOrder.MOVE_TO || order == SquadOrder.ATTACK_ZONE;
	}

	private static boolean strategicFireModifier(Minecraft mc) {
		return Screen.hasControlDown() && mc.getWindow() != null
			&& GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_J) == GLFW.GLFW_PRESS;
	}

	private static SquadOrder resolveOrderFromModifiers() {
		if (Screen.hasControlDown()) {
			return SquadOrder.ATTACK_ZONE;
		}
		return SquadOrder.MOVE_TO;
	}

	private static SquadFormation nextFormation(SquadFormation current) {
		for (int i = 0; i < FORMATION_CYCLE.length; i++) {
			if (FORMATION_CYCLE[i] == current) {
				return FORMATION_CYCLE[(i + 1) % FORMATION_CYCLE.length];
			}
		}
		return SquadFormation.LINE;
	}

	private static NomosikCombatMode nextCombatMode(NomosikCombatMode current) {
		for (int i = 0; i < COMBAT_MODE_CYCLE.length; i++) {
			if (COMBAT_MODE_CYCLE[i] == current) {
				return COMBAT_MODE_CYCLE[(i + 1) % COMBAT_MODE_CYCLE.length];
			}
		}
		return NomosikCombatMode.NEUTRAL;
	}

	private static Component combatModeLabel(NomosikCombatMode mode) {
		return TacticalCommandBar.combatModeLabel(mode);
	}

	private static String formatSpacing(float spacing) {
		if (Math.abs(spacing - Math.rint(spacing)) < 0.01F) {
			return String.valueOf((int) Math.rint(spacing));
		}
		return String.format("%.1f", spacing);
	}

	private static Optional<NomosikEntity> findNearestSquadMember(Minecraft mc, UUID squadId) {
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return Optional.empty();
		}
		var box = player.getBoundingBox().inflate(128.0, 128.0, 128.0);
		List<NomosikEntity> found = mc.level.getEntitiesOfClass(
			NomosikEntity.class,
			box,
			n -> player.getUUID().equals(n.getOwnerUUID()) && n.getSquadIdForGui().filter(squadId::equals).isPresent()
		);
		return found.stream().min(Comparator.comparingDouble(player::distanceTo));
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

	private static Component fireModeLabel(NomosikRangedFireMode mode) {
		return Component.translatable(switch (mode) {
			case HOLD_FIRE -> "nomosiki.rts.fire_mode.hold_fire";
			case FIRE_AT_WILL -> "nomosiki.rts.fire_mode.fire_at_will";
			case STRATEGIC_FIRE -> "nomosiki.rts.fire_mode.strategic_fire";
		});
	}
}
