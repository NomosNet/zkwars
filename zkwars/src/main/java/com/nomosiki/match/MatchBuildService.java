package com.nomosiki.match;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import com.nomosiki.match.schematic.LitematicaSchematic;
import com.nomosiki.match.schematic.SchematicLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Iterator;
import java.util.UUID;

public final class MatchBuildService {
	public static final int BLOCKS_PER_TICK = 12;
	private static final double ARRIVE_DIST_SQR = 2.5 * 2.5;

	private MatchBuildService() {
	}

	public static Component place(
		MatchService match,
		ServerPlayer player,
		MatchBuildingCatalog.Kind kind,
		BlockPos site
	) {
		MatchSession session = match.session();
		Byte side = match.sideOfPlayer(player.getUUID());
		if (side == null || session.phase != MatchPhase.RUNNING) {
			return Component.translatable("message.nomosiki.match.not_in_match");
		}
		NomosikRace race = session.raceOfSide(side);
		if (!kind.allowedFor(race)) {
			return Component.translatable("message.nomosiki.match.build_denied");
		}
		ServerLevel level = player.serverLevel();
		SchematicLibrary.Result schematic = SchematicLibrary.get(level, race, kind.id());
		if (!schematic.ok() || schematic.schematic() == null) {
			return schematic.message();
		}
		int cost = kind.cost(race);
		if (session.getAuthority(side) < cost) {
			return Component.translatable("message.nomosiki.match.need_authority", cost);
		}
		Vec3Site surface = surfaceAt(level, site);
		if (!isSiteValid(level, session, surface.pos, schematic.schematic())) {
			return Component.translatable("message.nomosiki.match.build_bad_site");
		}
		if (kind == MatchBuildingCatalog.Kind.OUTPOST) {
			CaptureBeaconEntity beacon = findOwnBeaconNear(level, session, side, surface.pos, 12.0);
			if (beacon == null) {
				return Component.translatable("message.nomosiki.match.build_outpost_need_point");
			}
			if (session.outpostByBeacon.containsKey(beacon.pointId())) {
				return Component.translatable("message.nomosiki.match.build_outpost_exists");
			}
		}
		NomosikEntity worker = findFreeWorker(level, session, side);
		if (worker == null) {
			return Component.translatable("message.nomosiki.match.build_no_worker");
		}
		session.addAuthority(side, -cost);
		MatchBuildOrder order = new MatchBuildOrder(side, kind, surface.pos, cost);
		order.workerId = worker.getUUID();
		session.buildOrders.add(order);
		worker.getNavigation().moveTo(surface.pos.getX() + 0.5, surface.pos.getY(), surface.pos.getZ() + 0.5, 1.15);
		match.syncHudPublic(false);
		return Component.translatable("message.nomosiki.match.build_started", kind.id());
	}

	public static void tick(MatchService match, ServerLevel level) {
		MatchSession session = match.session();
		if (session.phase != MatchPhase.RUNNING) {
			return;
		}
		boolean repath = level.getServer().getTickCount() % 20 == 0;
		Iterator<MatchBuildOrder> it = session.buildOrders.iterator();
		while (it.hasNext()) {
			MatchBuildOrder order = it.next();
			if (order.workerId == null) {
				it.remove();
				continue;
			}
			Entity entity = level.getEntity(order.workerId);
			if (!(entity instanceof NomosikEntity worker) || !worker.isAlive()) {
				session.addAuthority(order.side, order.paidAuthority / 2);
				it.remove();
				continue;
			}
			double dist = worker.distanceToSqr(order.site.getX() + 0.5, order.site.getY(), order.site.getZ() + 0.5);
			if (dist > ARRIVE_DIST_SQR) {
				if (repath) {
					worker.getNavigation().moveTo(order.site.getX() + 0.5, order.site.getY(), order.site.getZ() + 0.5, 1.15);
				}
				continue;
			}
			BuildingHeartEntity heart = MatchSpawner.spawnBuilding(
				level,
				session,
				order.side,
				order.kind.heartType(),
				order.kind.id(),
				order.site
			);
			if (heart != null && order.kind == MatchBuildingCatalog.Kind.OUTPOST) {
				CaptureBeaconEntity beacon = findOwnBeaconNear(level, session, order.side, order.site, 12.0);
				if (beacon != null) {
					session.outpostByBeacon.put(beacon.pointId(), heart.getUUID());
				}
			}
			it.remove();
			match.syncHudPublic(false);
		}
		boolean finished = false;
		for (UUID id : session.trackedEntities) {
			Entity entity = level.getEntity(id);
			if (entity instanceof BuildingHeartEntity heart && heart.isConstructing()) {
				if (heart.tickConstruction(level, BLOCKS_PER_TICK, session)) {
					finished = true;
				}
			}
		}
		if (finished) {
			match.syncHudPublic(false);
		}
	}

	public static boolean sideHasBuilding(ServerLevel level, MatchSession session, byte side, MatchBuildingCatalog.Kind kind) {
		for (UUID id : session.trackedEntities) {
			Entity entity = level.getEntity(id);
			if (entity instanceof BuildingHeartEntity heart
				&& heart.isAlive()
				&& heart.isComplete()
				&& heart.matchSide() == side
				&& kind.id().equals(heart.buildKind())) {
				return true;
			}
		}
		return false;
	}

	public static boolean canRecruitRole(ServerLevel level, MatchSession session, byte side, MatchRecruitment.Role role) {
		if (MatchBuildingCatalog.alwaysUnlocked(role)) {
			return true;
		}
		MatchBuildingCatalog.Kind required = MatchBuildingCatalog.requiredBuilding(role);
		if (required == null) {
			return true;
		}
		return sideHasBuilding(level, session, side, required);
	}

	private static boolean isSiteValid(ServerLevel level, MatchSession session, BlockPos site, LitematicaSchematic schematic) {
		for (UUID id : session.trackedEntities) {
			Entity entity = level.getEntity(id);
			if (entity instanceof BuildingHeartEntity heart
				&& heart.isAlive()
				&& heart.overlaps(site, schematic.sizeX(), schematic.sizeY(), schematic.sizeZ())) {
				return false;
			}
		}
		for (MatchBuildOrder order : session.buildOrders) {
			SchematicLibrary.Result other = SchematicLibrary.get(level, session.raceOfSide(order.side), order.kind.id());
			if (other.ok() && other.schematic() != null && boxesOverlap(site, schematic, order.site, other.schematic())) {
				return false;
			}
		}
		return true;
	}

	private static boolean boxesOverlap(BlockPos a, LitematicaSchematic left, BlockPos b, LitematicaSchematic right) {
		return axisOverlap(a.getX(), left.sizeX(), b.getX(), right.sizeX())
			&& axisOverlap(a.getY(), left.sizeY(), b.getY(), right.sizeY())
			&& axisOverlap(a.getZ(), left.sizeZ(), b.getZ(), right.sizeZ());
	}

	private static boolean axisOverlap(int originA, int sizeA, int originB, int sizeB) {
		return originA <= originB + sizeB - 1 && originB <= originA + sizeA - 1;
	}

	private record Vec3Site(BlockPos pos) {
	}

	private static Vec3Site surfaceAt(ServerLevel level, BlockPos site) {
		net.minecraft.world.phys.Vec3 snapped = MatchSpawner.onSurface(level, net.minecraft.world.phys.Vec3.atCenterOf(site));
		return new Vec3Site(BlockPos.containing(snapped));
	}

	private static NomosikEntity findFreeWorker(ServerLevel level, MatchSession session, byte side) {
		UUID owner = session.ownerOfSide(side);
		for (UUID id : session.workerIds) {
			Entity entity = level.getEntity(id);
			if (!(entity instanceof NomosikEntity worker) || !worker.isAlive()) {
				continue;
			}
			if (owner == null || !owner.equals(worker.getOwnerUUID())) {
				continue;
			}
			boolean busy = false;
			for (MatchBuildOrder order : session.buildOrders) {
				if (id.equals(order.workerId)) {
					busy = true;
					break;
				}
			}
			if (!busy) {
				return worker;
			}
		}
		return null;
	}

	private static CaptureBeaconEntity findOwnBeaconNear(
		ServerLevel level,
		MatchSession session,
		byte side,
		BlockPos site,
		double radius
	) {
		CaptureBeaconEntity best = null;
		double bestDist = radius * radius;
		for (UUID id : session.beaconIds.values()) {
			Entity entity = level.getEntity(id);
			if (!(entity instanceof CaptureBeaconEntity beacon) || beacon.ownerSide() != side) {
				continue;
			}
			double d = beacon.distanceToSqr(site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
			if (d <= bestDist) {
				bestDist = d;
				best = beacon;
			}
		}
		return best;
	}
}
