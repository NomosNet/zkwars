package com.nomosiki.squad;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.entity.ai.StandCells;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stable XZ center used only for formation facing ({@link SquadFormationLayout}).
 * Without this, recomputing the centroid every tick while units walk makes slots drift.
 * <p>
 * {@link SquadOrder#FOLLOW} with a non-{@link SquadFormation#NONE} shape re-anchors slots to the
 * owner each tick interval (Recruits formation-follow: layout at player position + look yaw).
 */
public final class SquadBrain {
	private static final int TICK_INTERVAL = 10;

	/** Per squad: anchor block + centroid XZ captured when that anchor was first used for layout this session. */
	private static final Map<UUID, LayoutOrientationSnap> LAYOUT_ORIENTATION_SNAP = new ConcurrentHashMap<>();

	private SquadBrain() {
	}

	private record LayoutOrientationSnap(BlockPos anchor, double refCx, double refCz) {
	}

	/**
	 * Unit XZ vector pointing along the formation front (from saved centroid toward the anchor).
	 * Used by {@link SquadFormationShift} for forward/backward steps.
	 */
	static Vec3 formationForwardUnit(UUID squadId, BlockPos anchor, NomosikEntity facingFallback) {
		Vec3 anchorCenter = Vec3.atBottomCenterOf(anchor);
		LayoutOrientationSnap snap = LAYOUT_ORIENTATION_SNAP.get(squadId);
		if (snap != null) {
			Vec3 squadCenter = new Vec3(snap.refCx, anchorCenter.y, snap.refCz);
			Vec3 toAnchor = anchorCenter.subtract(squadCenter);
			Vec3 forward = new Vec3(toAnchor.x, 0.0, toAnchor.z);
			if (forward.lengthSqr() > 1.0e-4) {
				return forward.normalize();
			}
		}
		// No snap yet: use stored facing yaw from the last layout tick.
		float yaw = facingFallback.getSquadFormationFacingYaw();
		double rad = Math.toRadians(yaw);
		return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
	}

	public static void forceOrientationAndLayout(ServerLevel level, UUID squadId, BlockPos anchor, double behindX, double behindZ) {
		BlockPos anchorImmutable = anchor.immutable();
		LAYOUT_ORIENTATION_SNAP.put(squadId, new LayoutOrientationSnap(anchorImmutable, behindX, behindZ));
		SquadManager manager = SquadService.get(level.getServer());
		List<NomosikEntity> ordered = manager.resolveMembersInLevel(level, squadId);
		if (ordered.size() < 2) {
			return;
		}
		SquadFormation formation = ordered.get(0).getSquadFormation();
		if (formation == SquadFormation.NONE) {
			return;
		}
		Vec3 anchorCenter = Vec3.atBottomCenterOf(anchorImmutable);
		Vec3 squadCenter = new Vec3(behindX, anchorCenter.y, behindZ);
		assignComputedSlots(level, squadId, ordered, formation, anchorCenter, squadCenter, ordered.get(0).getSquadOrder());
	}

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % TICK_INTERVAL != 0) {
			return;
		}
		SquadManager manager = SquadService.get(level.getServer());
		Map<UUID, List<NomosikEntity>> bySquad = new HashMap<>();
		for (Entity e : level.getAllEntities()) {
			if (!(e instanceof NomosikEntity w)) {
				continue;
			}
			if (w.getSquadId().isEmpty()) {
				w.setSquadFormationTargetServer(null);
				w.clearFormationRouteState();
				continue;
			}
			bySquad.computeIfAbsent(w.getSquadId().get(), k -> new ArrayList<>()).add(w);
		}
		for (Map.Entry<UUID, List<NomosikEntity>> entry : bySquad.entrySet()) {
			UUID squadId = entry.getKey();
			List<NomosikEntity> group = entry.getValue();
			if (group.size() < 2) {
				clearSquadSlots(squadId, group);
				continue;
			}
			List<NomosikEntity> ordered = manager.resolveMembersInLevel(level, squadId);
			if (ordered.size() < 2) {
				clearSquadSlots(squadId, group);
				continue;
			}
			SquadFormation formation = ordered.get(0).getSquadFormation();
			if (formation == SquadFormation.NONE) {
				clearSquadSlots(squadId, ordered);
				continue;
			}
			SquadOrder order = ordered.get(0).getSquadOrder();
			if (order == SquadOrder.FOLLOW) {
				layoutFollowFormation(level, squadId, ordered, formation);
				continue;
			}
			BlockPos anchor = null;
			for (NomosikEntity w : ordered) {
				if (w.getSquadAnchorPos() != null) {
					anchor = w.getSquadAnchorPos();
					break;
				}
			}
			if (anchor == null) {
				clearSquadSlots(squadId, ordered);
				continue;
			}
			Vec3 anchorCenter = Vec3.atBottomCenterOf(anchor);
			double cx = 0.0;
			double cz = 0.0;
			for (NomosikEntity w : ordered) {
				cx += w.getX();
				cz += w.getZ();
			}
			cx /= ordered.size();
			cz /= ordered.size();
			BlockPos anchorImmutable = anchor.immutable();
			LayoutOrientationSnap snap = LAYOUT_ORIENTATION_SNAP.get(squadId);
			if (snap == null || !snap.anchor.equals(anchorImmutable)) {
				if (snap != null && SquadFormationShiftService.consumeOrientationPreserve(squadId)) {
					// Forward/backward shift: keep orientation centroid, only move anchor block.
					snap = new LayoutOrientationSnap(anchorImmutable, snap.refCx, snap.refCz);
				} else {
					snap = new LayoutOrientationSnap(anchorImmutable, cx, cz);
				}
				LAYOUT_ORIENTATION_SNAP.put(squadId, snap);
			}
			Vec3 squadCenter = new Vec3(snap.refCx, anchorCenter.y, snap.refCz);
			assignComputedSlots(level, squadId, ordered, formation, anchorCenter, squadCenter, order);
		}
	}

	/**
	 * Recruits-style formation follow: front rank at the owner, rows behind along look direction.
	 */
	private static void layoutFollowFormation(
		ServerLevel level,
		UUID squadId,
		List<NomosikEntity> ordered,
		SquadFormation formation
	) {
		LivingEntity owner = ordered.get(0).getOwner();
		if (owner == null || !owner.isAlive()) {
			clearSquadSlots(squadId, ordered);
			return;
		}
		LAYOUT_ORIENTATION_SNAP.remove(squadId);
		float yaw = owner.getYRot();
		double rad = Math.toRadians(yaw);
		Vec3 forward = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
		Vec3 ownerPos = owner.position();
		Vec3 anchorCenter = ownerPos;
		Vec3 squadCenter = ownerPos.subtract(forward);
		assignComputedSlots(level, squadId, ordered, formation, anchorCenter, squadCenter, SquadOrder.FOLLOW);
	}

	private static void assignComputedSlots(
		ServerLevel level,
		UUID squadId,
		List<NomosikEntity> ordered,
		SquadFormation formation,
		Vec3 anchorCenter,
		Vec3 squadCenter,
		SquadOrder order
	) {
		int n = ordered.size();
		double spacing = Mth.clamp(
			ordered.get(0).getSquadFormationSpacing(),
			NomosikEntity.SQUAD_FORMATION_SPACING_MIN,
			NomosikEntity.SQUAD_FORMATION_SPACING_MAX
		);
		List<Vec3> slots = SquadFormationLayout.slotsForFormation(
			formation,
			level,
			anchorCenter,
			squadCenter,
			n,
			spacing,
			Mth.clamp(
				ordered.get(0).getSquadFormationMaxInRow(),
				NomosikEntity.SQUAD_FORMATION_MAX_IN_ROW_MIN,
				NomosikEntity.SQUAD_FORMATION_MAX_IN_ROW_MAX
			)
		);
		if (slots.size() != n) {
			clearSquadSlots(squadId, ordered);
			return;
		}
		float facingYaw = SquadFormationLayout.facingYawDegrees(anchorCenter, squadCenter);
		assignSlotsToMembers(level, BlockPos.containing(anchorCenter), ordered, slots, holdFormationForOrder(order), facingYaw);
	}

	private static void clearSquadSlots(UUID squadId, List<NomosikEntity> members) {
		LAYOUT_ORIENTATION_SNAP.remove(squadId);
		for (NomosikEntity w : members) {
			w.setSquadFormationTargetServer(null);
			w.clearFormationRouteState();
		}
	}

	private static boolean holdFormationForOrder(SquadOrder order) {
		return switch (order) {
			case HOLD, ATTACK_ZONE, FOLLOW -> true;
			case MOVE_TO, ATTACK_TARGET -> false;
		};
	}

	/**
	 * Stable slot assignment: keep {@link NomosikEntity#getSquadFormationSlot} when still free, else first free index.
	 * A slot the unit cannot walk is skipped until every slot has failed, then the squad waits 5 seconds.
	 */
	private static void assignSlotsToMembers(
		ServerLevel level,
		BlockPos anchor,
		List<NomosikEntity> ordered,
		List<Vec3> slots,
		boolean holdFormation,
		float facingYaw
	) {
		for (NomosikEntity w : ordered) {
			w.noteFormationAnchor(anchor);
		}
		long now = level.getGameTime();
		int n = ordered.size();
		List<Boolean> free = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			free.add(true);
		}
		for (NomosikEntity w : ordered) {
			if (!w.isFormationRejectPaused(now)) {
				continue;
			}
			int preferred = w.getSquadFormationSlot();
			if (preferred >= 0 && preferred < n) {
				free.set(preferred, false);
			}
		}
		for (NomosikEntity w : ordered) {
			if (w.isFormationRejectPaused(now)) {
				keepCurrentSlot(w, holdFormation, facingYaw);
				continue;
			}
			if (w.rejectedFormationSlotCount() >= n) {
				w.pauseFormationRejects(now + 100L);
				reserveCurrentSlot(free, w, n);
				keepCurrentSlot(w, holdFormation, facingYaw);
				continue;
			}
			int chosen = -1;
			int preferred = w.getSquadFormationSlot();
			if (preferred >= 0 && preferred < n && free.get(preferred) && !w.isFormationSlotRejected(preferred)) {
				chosen = preferred;
			} else {
				for (int i = 0; i < n; i++) {
					if (free.get(i) && !w.isFormationSlotRejected(i)) {
						chosen = i;
						break;
					}
				}
			}
			if (chosen < 0) {
				w.pauseFormationRejects(now + 100L);
				keepCurrentSlot(w, holdFormation, facingYaw);
				continue;
			}
			free.set(chosen, false);
			w.setSquadFormationSlotServer(chosen);
			Vec3 geometric = slots.get(chosen);
			w.setSquadFormationTargetServer(resolveReachable(level, w, anchor, chosen, geometric));
			w.setSquadFormationFlagsServer(true, holdFormation);
			w.setSquadFormationFacingServer(facingYaw);
		}
	}

	private static void reserveCurrentSlot(List<Boolean> free, NomosikEntity mob, int count) {
		int preferred = mob.getSquadFormationSlot();
		if (preferred >= 0 && preferred < count && free.get(preferred)) {
			free.set(preferred, false);
		}
	}

	private static void keepCurrentSlot(NomosikEntity mob, boolean holdFormation, float facingYaw) {
		if (mob.getSquadFormationTarget() == null) {
			return;
		}
		mob.setSquadFormationFlagsServer(true, holdFormation);
		mob.setSquadFormationFacingServer(facingYaw);
	}

	private static Vec3 resolveReachable(ServerLevel level, NomosikEntity mob, BlockPos anchor, int slot, Vec3 geometric) {
		boolean repick = mob.consumeFormationSlotRepick();
		if (!repick) {
			Vec3 cached = mob.cachedReachableSlot(anchor, slot, geometric);
			if (cached != null) {
				return cached;
			}
		}
		BlockPos origin = BlockPos.containing(geometric);
		BlockPos reachable = StandCells.firstReachable(mob, StandCells.around(level, origin));
		Vec3 chosen = Vec3.atBottomCenterOf(reachable != null ? reachable : SquadFormationLayout.surfaceAt(level, origin));
		mob.cacheReachableSlot(anchor, slot, geometric, chosen);
		return chosen;
	}
}
