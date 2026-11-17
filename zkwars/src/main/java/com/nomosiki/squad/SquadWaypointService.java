package com.nomosiki.squad;

import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SquadWaypointService {
	private static final double REACH_THRESHOLD = 6.0;
	private static final double REACH_THRESHOLD_SQR = REACH_THRESHOLD * REACH_THRESHOLD;

	private static final Map<UUID, SquadWaypointQueue> QUEUES = new ConcurrentHashMap<>();

	private SquadWaypointService() {
	}

	public static SquadWaypointQueue getOrCreate(UUID squadId) {
		return QUEUES.computeIfAbsent(squadId, id -> new SquadWaypointQueue());
	}

	public static List<BlockPos> snapshot(UUID squadId) {
		SquadWaypointQueue queue = QUEUES.get(squadId);
		return queue == null ? List.of() : queue.snapshot();
	}

	public static void clear(UUID squadId) {
		QUEUES.remove(squadId);
	}

	public static void clearOnDirectOrder(UUID squadId) {
		QUEUES.remove(squadId);
	}

	public static boolean addWaypoint(
		ServerPlayer player,
		ServerLevel level,
		SquadManager manager,
		UUID squadId,
		BlockPos pos
	) {
		if (!manager.isCommanderOf(player, squadId)) {
			return false;
		}
		int max = maxWaypoints();
		SquadWaypointQueue queue = getOrCreate(squadId);
		boolean wasEmpty = queue.isEmpty();
		if (!queue.enqueue(pos, max)) {
			return false;
		}
		List<NomosikEntity> members = inCommandRange(player, level, manager, squadId);
		if (members.isEmpty()) {
			return false;
		}
		if (wasEmpty) {
			applyMoveTo(members, queue.current().orElse(pos));
		}
		return true;
	}

	public static void clearWaypoints(ServerPlayer player, ServerLevel level, SquadManager manager, UUID squadId) {
		if (!manager.isCommanderOf(player, squadId)) {
			return;
		}
		clear(squadId);
	}

	public static boolean replaceAll(
		ServerPlayer player,
		ServerLevel level,
		SquadManager manager,
		UUID squadId,
		List<BlockPos> points
	) {
		if (!manager.isCommanderOf(player, squadId)) {
			return false;
		}
		List<NomosikEntity> members = inCommandRange(player, level, manager, squadId);
		if (members.isEmpty()) {
			return false;
		}
		SquadWaypointQueue queue = getOrCreate(squadId);
		queue.replaceAll(points, maxWaypoints());
		Optional<BlockPos> current = queue.current();
		if (current.isPresent()) {
			applyMoveTo(members, current.get());
			return true;
		}
		clear(squadId);
		return false;
	}

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % 10L != 0L) {
			return;
		}
		SquadManager manager = SquadService.get(level.getServer());
		for (Map.Entry<UUID, SquadWaypointQueue> entry : QUEUES.entrySet()) {
			UUID squadId = entry.getKey();
			SquadWaypointQueue queue = entry.getValue();
			Optional<BlockPos> current = queue.current();
			if (current.isEmpty()) {
				continue;
			}
			List<NomosikEntity> members = manager.resolveMembersInLevel(level, squadId);
			int alive = 0;
			Vec3 centerSum = Vec3.ZERO;
			for (NomosikEntity member : members) {
				if (!member.isAlive()) {
					continue;
				}
				alive++;
				centerSum = centerSum.add(member.position());
			}
			if (alive == 0) {
				continue;
			}
			Vec3 center = centerSum.scale(1.0 / alive);
			Vec3 target = Vec3.atBottomCenterOf(current.get());
			if (center.distanceToSqr(target) > REACH_THRESHOLD_SQR) {
				continue;
			}
			queue.advance();
			Optional<BlockPos> next = queue.current();
			if (next.isPresent()) {
				applyMoveTo(members, next.get());
			} else {
				applyIdleWhenDone(members);
				QUEUES.remove(squadId);
			}
		}
	}

	private static void applyMoveTo(List<NomosikEntity> members, BlockPos anchor) {
		for (NomosikEntity member : members) {
			if (!member.isAlive()) {
				continue;
			}
			member.applySquadOrder(SquadOrder.MOVE_TO, anchor, 8);
		}
	}

	private static void applyIdleWhenDone(List<NomosikEntity> members) {
		SquadOrder idle = idleOrderAfterRoute();
		for (NomosikEntity member : members) {
			if (!member.isAlive()) {
				continue;
			}
			if (idle == SquadOrder.FOLLOW) {
				member.applySquadOrder(SquadOrder.FOLLOW, null, 8);
			} else {
				BlockPos hold = member.blockPosition();
				member.applySquadOrder(SquadOrder.HOLD, hold, 8);
			}
		}
	}

	private static SquadOrder idleOrderAfterRoute() {
		return NomosikiServerConfig.INSTANCE.rtsWaypointIdleFollow ? SquadOrder.FOLLOW : SquadOrder.HOLD;
	}

	private static int maxWaypoints() {
		return Math.max(1, Math.min(8, NomosikiServerConfig.INSTANCE.rtsMaxWaypoints));
	}

	private static List<NomosikEntity> inCommandRange(
		ServerPlayer player,
		ServerLevel level,
		SquadManager manager,
		UUID squadId
	) {
		double commandRangeSqr = com.nomosiki.config.NomosikiServerConfig.INSTANCE.rtsCommandRange;
		final double rangeSqr = commandRangeSqr * commandRangeSqr;
		UUID ownerId = player.getUUID();
		List<NomosikEntity> found = manager.resolveMembersInLevel(level, squadId);
		return found.stream()
			.filter(n -> ownerId.equals(n.getOwnerUUID()))
			.filter(n -> player.distanceToSqr(n) <= rangeSqr)
			.toList();
	}
}
