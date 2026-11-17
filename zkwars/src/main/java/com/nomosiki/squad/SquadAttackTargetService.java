package com.nomosiki.squad;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps {@link SquadOrder#ATTACK_TARGET} zones locked onto the enemy squad centroid or player.
 */
public final class SquadAttackTargetService {
	private static final int TICK_INTERVAL = 10;

	private SquadAttackTargetService() {
	}

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % TICK_INTERVAL != 0) {
			return;
		}
		SquadManager manager = SquadService.get(level.getServer());
		Map<UUID, List<NomosikEntity>> attackersByTarget = new HashMap<>();
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof NomosikEntity nomosik) || !nomosik.isAlive()) {
				continue;
			}
			if (nomosik.getSquadOrder() != SquadOrder.ATTACK_TARGET) {
				continue;
			}
			Optional<UUID> targetId = nomosik.getSquadAttackTargetId();
			if (targetId.isEmpty()) {
				nomosik.convertAttackTargetToZone();
				continue;
			}
			attackersByTarget.computeIfAbsent(targetId.get(), id -> new ArrayList<>()).add(nomosik);
		}
		for (Map.Entry<UUID, List<NomosikEntity>> entry : attackersByTarget.entrySet()) {
			List<NomosikEntity> attackers = entry.getValue();
			boolean targetIsPlayer = attackers.stream().anyMatch(NomosikEntity::isSquadAttackTargetPlayer);
			Vec3 center = targetIsPlayer
				? playerCenter(level, entry.getKey())
				: squadCenter(level, manager, entry.getKey());
			if (center == null) {
				for (NomosikEntity attacker : attackers) {
					attacker.convertAttackTargetToZone();
				}
				continue;
			}
			BlockPos anchor = BlockPos.containing(center.x, center.y, center.z);
			for (NomosikEntity attacker : attackers) {
				attacker.refreshAttackTargetZone(anchor);
			}
		}
	}

	public static List<NomosikEntity> livingMembers(ServerLevel level, SquadManager manager, UUID squadId) {
		List<NomosikEntity> fromRoster = manager.resolveMembersInLevel(level, squadId);
		List<NomosikEntity> alive = new ArrayList<>();
		for (NomosikEntity member : fromRoster) {
			if (member.isAlive() && !member.isRemoved()) {
				alive.add(member);
			}
		}
		if (!alive.isEmpty()) {
			return alive;
		}
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof NomosikEntity nomosik) || !nomosik.isAlive() || nomosik.isRemoved()) {
				continue;
			}
			if (nomosik.getSquadId().filter(squadId::equals).isPresent()) {
				alive.add(nomosik);
			}
		}
		return alive;
	}

	public static Vec3 centroid(List<NomosikEntity> members) {
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;
		for (NomosikEntity member : members) {
			x += member.getX();
			y += member.getY();
			z += member.getZ();
		}
		int n = Math.max(1, members.size());
		return new Vec3(x / n, y / n, z / n);
	}

	private static Vec3 squadCenter(ServerLevel level, SquadManager manager, UUID squadId) {
		List<NomosikEntity> targets = livingMembers(level, manager, squadId);
		if (targets.isEmpty()) {
			return null;
		}
		return centroid(targets);
	}

	private static Vec3 playerCenter(ServerLevel level, UUID playerId) {
		Player player = level.getPlayerByUUID(playerId);
		if (player == null || !player.isAlive() || player.isRemoved()) {
			return null;
		}
		return player.position();
	}
}
