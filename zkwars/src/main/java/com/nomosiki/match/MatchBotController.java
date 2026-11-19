package com.nomosiki.match;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Minimal bot: march owned units to nearest contested/neutral beacon, else enemy HQ. */
public final class MatchBotController {
	private MatchBotController() {
	}

	public static void tick(ServerLevel level, MatchSession session) {
		if (!session.side1IsBot || session.phase != MatchPhase.RUNNING) {
			return;
		}
		Vec3 target = pickTarget(level, session);
		if (target == null) {
			return;
		}
		List<NomosikEntity> units = new ArrayList<>();
		for (UUID id : session.trackedEntities) {
			Entity entity = level.getEntity(id);
			if (entity instanceof NomosikEntity nomosik
				&& MatchSession.BOT_OWNER_ID.equals(nomosik.getOwnerUUID())
				&& nomosik.isAlive()) {
				units.add(nomosik);
			}
		}
		for (NomosikEntity unit : units) {
			if (unit.distanceToSqr(target) > 9.0) {
				unit.getNavigation().moveTo(target.x, target.y, target.z, 1.1);
			}
		}
	}

	private static Vec3 pickTarget(ServerLevel level, MatchSession session) {
		CaptureBeaconEntity best = null;
		double bestDist = Double.MAX_VALUE;
		Vec3 from = session.start1 != null ? session.start1 : session.origin;
		for (UUID id : session.beaconIds.values()) {
			Entity entity = level.getEntity(id);
			if (!(entity instanceof CaptureBeaconEntity beacon)) {
				continue;
			}
			if (beacon.ownerSide() == 1) {
				continue;
			}
			double d = beacon.distanceToSqr(from);
			if (d < bestDist) {
				bestDist = d;
				best = beacon;
			}
		}
		if (best != null) {
			return best.position();
		}
		if (session.hq0Id != null) {
			Entity hq = level.getEntity(session.hq0Id);
			if (hq instanceof BuildingHeartEntity heart && heart.isAlive()) {
				return heart.position();
			}
		}
		return null;
	}
}
