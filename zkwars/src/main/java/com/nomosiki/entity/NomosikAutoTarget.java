package com.nomosiki.entity;

import com.nomosiki.NomosikAsync;
import com.nomosiki.combat.CombatRelation;
import com.nomosiki.squad.SquadAutoTargetService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class NomosikAutoTarget {
	private static final double AUTO_TARGET_RANGE_NEUTRAL = 24.0;
	private static final double AUTO_TARGET_RANGE_AGGRESSIVE = 40.0;
	private static final double AUTO_TARGET_RANGE_RAID = 40.0;
	private static final int SEARCH_EVERY_NO_TARGET = 20;
	private static final int SEARCH_EVERY_HAS_TARGET = 60;
	/** Switch only if new target is ~20% closer (linear); 0.8^2 = 0.64. */
	private static final double RETARGET_DIST_SQR_FACTOR = 0.64;

	private NomosikAutoTarget() {
	}

	static void tickSearch(NomosikEntity entity, ServerLevel level) {
		if (entity.autoTargetSearchInFlight) {
			return;
		}
		NomosikCombatMode mode = entity.getCombatMode();
		if (!mode.runsAutoTargetSearch()) {
			return;
		}
		Optional<UUID> squadId = entity.getSquadId();
		if (squadId.isPresent()) {
			if (!SquadAutoTargetService.isAutoTargetScout(entity, level)) {
				return;
			}
		}
		if (!shouldRunScheduledTargetSearch(entity)) {
			return;
		}
		int interval = autoTargetSearchInterval(entity);
		if (squadId.isPresent() && !SquadAutoTargetService.canSquadSearch(squadId.get(), level.getGameTime(), interval)) {
			return;
		}
		double range = autoTargetRangeForMode(mode);
		AABB box = entity.getBoundingBox().inflate(range);
		if (entity.squadOrder.usesAttackVolume() && entity.squadAttackZone != null) {
			box = box.minmax(entity.squadAttackZone);
		}
		final SquadOrder orderSnap = entity.squadOrder;
		final AABB zoneSnap = entity.squadAttackZone;
		final AABB rangedZoneSnap = entity.canUseRangedFireOrders() && entity.getRangedFireMode() == NomosikRangedFireMode.STRATEGIC_FIRE
			? entity.getEffectiveRangedFireZone()
			: null;
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != null && e.isAlive() && e != entity);
		if (squadId.isPresent()) {
			SquadAutoTargetService.recordSquadSearch(squadId.get(), level.getGameTime());
		}
		if (nearby.isEmpty()) {
			return;
		}
		final UUID ownerId = entity.getOwnerUUID();
		final UUID nomosikId = entity.getUUID();
		final double wx = entity.getX();
		final double wy = entity.getY();
		final double wz = entity.getZ();
		final NomosikCombatMode modeSnap = mode;
		final Optional<UUID> squadIdSnap = squadId;
		final int ticket = ++entity.autoTargetGeneration;
		entity.autoTargetSearchInFlight = true;
		NomosikAsync.run(level,
			() -> filterAutoTargets(nearby, ownerId, nomosikId, wx, wy, wz, modeSnap, orderSnap, zoneSnap, rangedZoneSnap),
			candidates -> onSearchDone(entity, ticket, level, squadIdSnap, candidates));
	}

	private static void onSearchDone(NomosikEntity entity, int ticket, ServerLevel level, Optional<UUID> squadId, List<LivingEntity> candidates) {
		entity.autoTargetSearchInFlight = false;
		if (!entity.isAlive()) {
			return;
		}
		if (ticket != entity.autoTargetGeneration) {
			return;
		}
		NomosikCombatMode mode = entity.getCombatMode();
		double rangeSqr = autoTargetRangeSqrForMode(mode);
		if (squadId.isPresent()) {
			if (entity.canUseRangedFireOrders() && entity.getRangedFireMode() == NomosikRangedFireMode.STRATEGIC_FIRE) {
				return;
			}
			List<LivingEntity> inRange = new ArrayList<>();
			for (LivingEntity candidate : candidates) {
				if (!candidate.isAlive() || candidate.isRemoved()) {
					continue;
				}
				if (entity.distanceToSqr(candidate) > rangeSqr) {
					continue;
				}
				if (!canApplyAutoTarget(entity, candidate)) {
					continue;
				}
				inRange.add(candidate);
			}
			if (!inRange.isEmpty()) {
				SquadAutoTargetService.assignSquadTargets(level, squadId.get(), inRange, entity);
			}
			return;
		}
		LivingEntity current = entity.getTarget();
		if (entity.isRecentAttacker(current) && canApplyAutoTarget(entity, current)) {
			return;
		}
		boolean holdCurrent = current != null
			&& current.isAlive()
			&& !current.isRemoved()
			&& entity.distanceToSqr(current) < rangeSqr;
		double currentDistSqr = holdCurrent ? entity.distanceToSqr(current) : 0.0;
		for (LivingEntity candidate : candidates) {
			if (!candidate.isAlive() || candidate.isRemoved()) {
				continue;
			}
			double candidateDistSqr = entity.distanceToSqr(candidate);
			if (candidateDistSqr > rangeSqr) {
				continue;
			}
			if (!canApplyAutoTarget(entity, candidate)) {
				continue;
			}
			if (holdCurrent) {
				if (candidate == current) {
					continue;
				}
				if (candidateDistSqr >= currentDistSqr * RETARGET_DIST_SQR_FACTOR) {
					continue;
				}
			}
			entity.setTarget(candidate);
			return;
		}
	}

	static boolean shouldClearTarget(LivingEntity current) {
		return !current.isAlive() || current.isRemoved() || isExemptPlayer(current);
	}

	/** Moderators in spectator or creative are never a combat target. */
	private static boolean isExemptPlayer(LivingEntity candidate) {
		return candidate instanceof Player player && (player.isSpectator() || player.isCreative());
	}

	static boolean canApplyAutoTarget(NomosikEntity entity, LivingEntity candidate) {
		if (candidate == null || !candidate.isAlive() || candidate.isRemoved() || isExemptPlayer(candidate)) {
			return false;
		}
		if (entity.getCombatMode() == NomosikCombatMode.PASSIVE) {
			return false;
		}
		UUID ownerId = entity.getOwnerUUID();
		NomosikCombatMode mode = entity.getCombatMode();
		return switch (mode) {
			case NEUTRAL -> canAutoHuntCandidate(mode, ownerId, candidate);
			case AGGRESSIVE -> CombatRelation.mayAttack(entity, candidate);
			case RAID -> CombatRelation.mayRaidAttack(entity, candidate);
			default -> false;
		};
	}

	private static int autoTargetSearchInterval(NomosikEntity entity) {
		LivingEntity current = entity.getTarget();
		return (current != null && current.isAlive() && !current.isRemoved())
			? SEARCH_EVERY_HAS_TARGET
			: SEARCH_EVERY_NO_TARGET;
	}

	private static boolean shouldRunScheduledTargetSearch(NomosikEntity entity) {
		int interval = autoTargetSearchInterval(entity);
		return (entity.tickCount + entity.getId()) % interval == 0;
	}

	private static double autoTargetRangeForMode(NomosikCombatMode mode) {
		return switch (mode) {
			case NEUTRAL -> AUTO_TARGET_RANGE_NEUTRAL;
			case AGGRESSIVE -> AUTO_TARGET_RANGE_AGGRESSIVE;
			case RAID -> AUTO_TARGET_RANGE_RAID;
			case PASSIVE -> 0.0;
		};
	}

	private static double autoTargetRangeSqrForMode(NomosikCombatMode mode) {
		double range = autoTargetRangeForMode(mode);
		return range * range;
	}

	private static List<LivingEntity> filterAutoTargets(
		List<LivingEntity> nearby,
		UUID ownerId,
		UUID nomosikId,
		double wx,
		double wy,
		double wz,
		NomosikCombatMode mode,
		SquadOrder orderSnap,
		@Nullable AABB zoneSnap,
		@Nullable AABB rangedZoneSnap
	) {
		List<LivingEntity> filtered = new ArrayList<>();
		for (LivingEntity e : nearby) {
			if (!e.getUUID().equals(nomosikId) && isPotentialAutoTarget(e, ownerId, mode, orderSnap, zoneSnap, rangedZoneSnap)) {
				filtered.add(e);
			}
		}
		filtered.sort(Comparator.comparingDouble(ent -> ent.distanceToSqr(wx, wy, wz)));
		int cap = Math.min(12, filtered.size());
		return List.copyOf(filtered.subList(0, cap));
	}

	/**
	 * NEUTRAL: только монстры, враги фракции при войне, мобы атакующие владельца.
	 * AGGRESSIVE: монстры, AT_WAR, NEUTRAL по дипломатии; финальная проверка — {@link CombatRelation#mayAttack}.
	 * RAID: все живые (кроме владельца/союзников — отсекаются в {@link #isPotentialAutoTarget}).
	 */
	static boolean canAutoHuntCandidate(NomosikCombatMode mode, @Nullable UUID ownerId, LivingEntity candidate) {
		if (!mode.runsAutoTargetSearch()) {
			return false;
		}
		if (NomosikiHorseMount.isNomosikiMountEntity(candidate)) {
			return false;
		}
		if (mode == NomosikCombatMode.RAID) {
			return true;
		}
		if (ownerId != null && CombatRelation.isMatchEnemyTarget(ownerId, candidate)) {
			return true;
		}
		if (ownerId != null && CombatRelation.isFactionEnemyTarget(ownerId, candidate)) {
			return true;
		}
		if (mode == NomosikCombatMode.AGGRESSIVE
			&& ownerId != null
			&& CombatRelation.isFactionNeutralTarget(ownerId, candidate)) {
			return true;
		}
		if (candidate instanceof Enemy) {
			return true;
		}
		return ownerId != null
			&& candidate instanceof Mob mob
			&& mob.getTarget() instanceof Player targetPlayer
			&& ownerId.equals(targetPlayer.getUUID());
	}

	private static boolean isPotentialAutoTarget(
		LivingEntity candidate,
		UUID ownerId,
		NomosikCombatMode mode,
		SquadOrder orderSnap,
		@Nullable AABB zoneSnap,
		@Nullable AABB rangedZoneSnap
	) {
		if (NomosikiHorseMount.isNomosikiMountEntity(candidate)) {
			return false;
		}
		if (ownerId != null && candidate instanceof Player p && p.getUUID().equals(ownerId)) {
			return false;
		}
		if (ownerId != null && candidate instanceof NomosikEntity ally && ownerId.equals(ally.getOwnerUUID())) {
			return false;
		}
		if (orderSnap.usesAttackVolume() && zoneSnap != null) {
			var p = candidate.position();
			if (!zoneSnap.contains(p.x, p.y, p.z)) {
				return false;
			}
		}
		if (rangedZoneSnap != null) {
			var p = candidate.position();
			if (!rangedZoneSnap.contains(p.x, p.y, p.z)) {
				return false;
			}
		}
		return canAutoHuntCandidate(mode, ownerId, candidate);
	}

	/**
	 * Вызов при уроне по владельцу: NEUTRAL и AGGRESSIVE получают цель-нападавшего; PASSIVE — нет.
	 */
	static void alertNomosikiOfOwnerDamage(ServerLevel level, ServerPlayer owner, LivingEntity attacker) {
		if (!attacker.isAlive() || attacker.isRemoved() || attacker == owner) {
			return;
		}
		UUID ownerId = owner.getUUID();
		double rs = NomosikEntity.REVENGE_RANGE_SQR;
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof NomosikEntity nomosik)) {
				continue;
			}
			if (!ownerId.equals(nomosik.getOwnerUUID())) {
				continue;
			}
			if (owner.distanceToSqr(nomosik) > rs) {
				continue;
			}
			if (nomosik.getCombatMode() == NomosikCombatMode.PASSIVE) {
				continue;
			}
			if (!canApplyAutoTarget(nomosik, attacker)) {
				continue;
			}
			nomosik.setTarget(attacker);
		}
	}
}
