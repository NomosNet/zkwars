package com.nomosiki.squad;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadOrder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Один «дозорный» на отряд для {@link NomosikEntity} auto-target (§4.2.2): −90% spatial query.
 * Назначение целей — по линии фронта (не один target на весь отряд).
 * Server-only; без сетевых пакетов.
 */
public final class SquadAutoTargetService {
	private static final int MAX_ATTACKERS_PER_TARGET = 2;
	/** Switch only if new target is ~20% closer (linear); 0.8^2 = 0.64. */
	private static final double RETARGET_DIST_SQR_FACTOR = 0.64;

	private static final Map<UUID, Long> lastSquadSearchTick = new ConcurrentHashMap<>();
	private static final Map<ScoutCacheKey, Integer> scoutEntityIdBySquad = new ConcurrentHashMap<>();
	private static long scoutCacheGameTime = -1L;

	private SquadAutoTargetService() {
	}

	private record ScoutCacheKey(UUID squadId, ResourceKey<Level> dimension) {
	}

	private record LateralEntity<T extends LivingEntity>(T entity, double lateral) {
	}

	/**
	 * Без отряда — ищет сам; в отряде — только юнит с минимальным {@code entityId} среди не-PASSIVE с автопоиском.
	 */
	public static boolean isAutoTargetScout(NomosikEntity self, ServerLevel level) {
		if (self.getSquadId().isEmpty()) {
			return true;
		}
		invalidateScoutCacheIfNewTick(level.getGameTime());
		UUID squadId = self.getSquadId().get();
		ScoutCacheKey key = new ScoutCacheKey(squadId, level.dimension());
		int scoutId = scoutEntityIdBySquad.computeIfAbsent(key, k -> computeScoutEntityId(level, squadId));
		return scoutId >= 0 && scoutId == self.getId();
	}

	public static boolean canSquadSearch(UUID squadId, long gameTime, int intervalTicks) {
		Long last = lastSquadSearchTick.get(squadId);
		return last == null || gameTime - last >= intervalTicks;
	}

	public static void recordSquadSearch(UUID squadId, long gameTime) {
		lastSquadSearchTick.put(squadId, gameTime);
	}

	public static void clearSquad(UUID squadId) {
		lastSquadSearchTick.remove(squadId);
		scoutEntityIdBySquad.keySet().removeIf(key -> key.squadId().equals(squadId));
	}

	/**
	 * Раздаёт цели членам отряда слева направо по оси фронта, с лимитом атакующих на одну цель.
	 */
	public static void assignSquadTargets(
		ServerLevel level,
		UUID squadId,
		List<LivingEntity> candidates,
		NomosikEntity scout
	) {
		if (candidates == null || candidates.isEmpty()) {
			return;
		}
		List<LivingEntity> enemies = new ArrayList<>(candidates.size());
		for (LivingEntity c : candidates) {
			if (c != null && c.isAlive() && !c.isRemoved()) {
				enemies.add(c);
			}
		}
		if (enemies.isEmpty()) {
			return;
		}
		UUID focusId = scout.getSquadAttackTargetId().orElse(null);
		if (focusId != null && scout.getSquadOrder() == SquadOrder.ATTACK_TARGET) {
			List<LivingEntity> preferred = new ArrayList<>();
			boolean focusPlayer = scout.isSquadAttackTargetPlayer();
			for (LivingEntity enemy : enemies) {
				if (focusPlayer) {
					if (enemy instanceof Player player && focusId.equals(player.getUUID())) {
						preferred.add(enemy);
					}
				} else if (enemy instanceof NomosikEntity nomosik
					&& nomosik.getSquadId().filter(focusId::equals).isPresent()) {
					preferred.add(enemy);
				}
			}
			if (!preferred.isEmpty()) {
				enemies = preferred;
			}
		}

		List<NomosikEntity> members = new ArrayList<>();
		for (NomosikEntity member : SquadService.get(level.getServer()).resolveMembersInLevel(level, squadId)) {
			if (!member.isAlive() || member.getCombatMode() == NomosikCombatMode.PASSIVE) {
				continue;
			}
			if (member.isRangedUnit() && member.getRangedFireMode() == NomosikRangedFireMode.STRATEGIC_FIRE) {
				continue;
			}
			members.add(member);
		}
		if (members.isEmpty()) {
			return;
		}

		double cx = 0.0;
		double cz = 0.0;
		for (NomosikEntity m : members) {
			cx += m.getX();
			cz += m.getZ();
		}
		cx /= members.size();
		cz /= members.size();
		Vec3 center = new Vec3(cx, 0.0, cz);

		float yaw = scout.getSquadFormationFacingYaw();
		double rad = Math.toRadians(yaw);
		Vec3 forward = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
		Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
		if (right.lengthSqr() < 1.0e-8) {
			right = new Vec3(1.0, 0.0, 0.0);
		} else {
			right = right.normalize();
		}

		List<LateralEntity<NomosikEntity>> sortedMembers = sortByLateral(members, center, right);
		List<LateralEntity<LivingEntity>> sortedEnemies = sortByLateral(enemies, center, right);
		int enemyCount = sortedEnemies.size();
		int[] load = new int[enemyCount];

		for (int mi = 0; mi < sortedMembers.size(); mi++) {
			NomosikEntity member = sortedMembers.get(mi).entity();
			int start = (mi * enemyCount) / sortedMembers.size();
			int chosenIdx = findEnemyIndex(member, sortedEnemies, load, start, true);
			if (chosenIdx < 0) {
				chosenIdx = findEnemyIndex(member, sortedEnemies, load, start, false);
			}
			if (chosenIdx < 0) {
				continue;
			}
			load[chosenIdx]++;
			applyTargetWithHysteresis(member, sortedEnemies.get(chosenIdx).entity(), members);
		}
	}

	private static <T extends LivingEntity> List<LateralEntity<T>> sortByLateral(
		List<T> entities,
		Vec3 center,
		Vec3 right
	) {
		List<LateralEntity<T>> sorted = new ArrayList<>(entities.size());
		for (T e : entities) {
			double lateral = (e.getX() - center.x) * right.x + (e.getZ() - center.z) * right.z;
			sorted.add(new LateralEntity<>(e, lateral));
		}
		sorted.sort(Comparator.comparingDouble(LateralEntity::lateral));
		return sorted;
	}

	/**
	 * @param respectCap when true, skip enemies already at {@link #MAX_ATTACKERS_PER_TARGET}.
	 */
	private static int findEnemyIndex(
		NomosikEntity member,
		List<LateralEntity<LivingEntity>> enemies,
		int[] load,
		int start,
		boolean respectCap
	) {
		int n = enemies.size();
		for (int step = 0; step < n; step++) {
			int ei = (start + step) % n;
			if (respectCap && load[ei] >= MAX_ATTACKERS_PER_TARGET) {
				continue;
			}
			LivingEntity enemy = enemies.get(ei).entity();
			if (!member.canApplyAutoTarget(enemy)) {
				continue;
			}
			return ei;
		}
		return -1;
	}

	private static void applyTargetWithHysteresis(
		NomosikEntity member,
		LivingEntity assigned,
		List<NomosikEntity> squadMembers
	) {
		if (!member.canApplyAutoTarget(assigned)) {
			return;
		}
		double rangeSqr = autoTargetRangeSqr(member.getCombatMode());
		LivingEntity current = member.getTarget();
		if (current != null && member.isRecentAttacker(current) && member.canApplyAutoTarget(current)) {
			return;
		}
		boolean holdCurrent = current != null
			&& current.isAlive()
			&& !current.isRemoved()
			&& member.distanceToSqr(current) < rangeSqr;
		if (holdCurrent) {
			if (current == assigned) {
				return;
			}
			// Break dogpile: if too many squadmates already focus current, force retarget.
			int focusing = countFocusing(squadMembers, current);
			if (focusing <= MAX_ATTACKERS_PER_TARGET) {
				double assignedDistSqr = member.distanceToSqr(assigned);
				if (assignedDistSqr >= member.distanceToSqr(current) * RETARGET_DIST_SQR_FACTOR) {
					return;
				}
			}
		}
		member.setTarget(assigned);
	}

	private static int countFocusing(List<NomosikEntity> squadMembers, LivingEntity target) {
		int count = 0;
		for (NomosikEntity m : squadMembers) {
			if (m.getTarget() == target) {
				count++;
			}
		}
		return count;
	}

	private static double autoTargetRangeSqr(NomosikCombatMode mode) {
		double range = switch (mode) {
			case NEUTRAL -> 24.0;
			case AGGRESSIVE, RAID -> 40.0;
			case PASSIVE -> 0.0;
		};
		return range * range;
	}

	private static void invalidateScoutCacheIfNewTick(long gameTime) {
		if (gameTime != scoutCacheGameTime) {
			scoutEntityIdBySquad.clear();
			scoutCacheGameTime = gameTime;
		}
	}

	private static int computeScoutEntityId(ServerLevel level, UUID squadId) {
		List<NomosikEntity> members = SquadService.get(level.getServer()).resolveMembersInLevel(level, squadId);
		int bestId = Integer.MAX_VALUE;
		boolean found = false;
		for (NomosikEntity member : members) {
			if (!member.isAlive()) {
				continue;
			}
			if (!member.getCombatMode().runsAutoTargetSearch()) {
				continue;
			}
			int id = member.getId();
			if (id < bestId) {
				bestId = id;
				found = true;
			}
		}
		return found ? bestId : -1;
	}
}
