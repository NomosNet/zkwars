package com.nomosiki.match;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import com.nomosiki.squad.SquadType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Match race passives: dwarf beer spirit near own buildings, captain/elite aura,
 * orc authority burst when a building falls.
 */
public final class MatchRaceAbilities {
	private MatchRaceAbilities() {
	}

	public static void tick(ServerLevel level, MatchSession session) {
		if (session.phase != MatchPhase.RUNNING) {
			return;
		}
		for (UUID id : session.trackedEntities) {
			Entity entity = level.getEntity(id);
			if (!(entity instanceof NomosikEntity unit) || !unit.isAlive()) {
				continue;
			}
			byte side = session.sideOfOwner(unit.getOwnerUUID());
			if (side == CaptureBeaconEntity.SIDE_NONE) {
				continue;
			}
			if (session.raceOfSide(side) == NomosikRace.DWARF) {
				tickBeerSpirit(level, session, unit, side);
			}
			if (unit.getTags().contains("zkwars_match_elite") || unit.getSquadBranchType() == SquadType.CAPTAIN) {
				tickCaptainAura(level, unit);
			}
		}
	}

	private static void tickBeerSpirit(ServerLevel level, MatchSession session, NomosikEntity unit, byte side) {
		boolean nearOwn = false;
		for (UUID id : session.trackedEntities) {
			Entity entity = level.getEntity(id);
			if (entity instanceof BuildingHeartEntity heart
				&& heart.isAlive()
				&& heart.matchSide() == side
				&& heart.distanceToSqr(unit) <= 256.0) {
				nearOwn = true;
				break;
			}
		}
		if (nearOwn && unit.getHealth() < unit.getMaxHealth()) {
			unit.heal(0.25f);
		}
	}

	private static void tickCaptainAura(ServerLevel level, NomosikEntity captain) {
		UUID owner = captain.getOwnerUUID();
		if (owner == null) {
			return;
		}
		AABB box = captain.getBoundingBox().inflate(12.0);
		for (NomosikEntity ally : level.getEntitiesOfClass(NomosikEntity.class, box, e ->
			e.isAlive() && owner.equals(e.getOwnerUUID()) && e != captain)) {
			if (ally.getHealth() < ally.getMaxHealth()) {
				ally.heal(0.15f);
			}
		}
	}

	public static void onBuildingDestroyed(MatchSession session, byte killerSide) {
		if (killerSide == CaptureBeaconEntity.SIDE_NONE) {
			return;
		}
		if (session.raceOfSide(killerSide) == NomosikRace.ORC) {
			session.addAuthority(killerSide, 15);
		}
	}
}
