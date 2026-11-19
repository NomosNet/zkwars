package com.nomosiki.match;

import com.nomosiki.entity.NomosikRace;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MatchSession {
	public static final UUID BOT_OWNER_ID = UUID.nameUUIDFromBytes("zkwars-match-bot".getBytes());

	public MatchPhase phase = MatchPhase.IDLE;
	public MatchEndReason endReason = MatchEndReason.NONE;
	public byte winnerSide = CaptureBeaconEntity.SIDE_NONE;

	@Nullable
	public UUID side0PlayerId;
	@Nullable
	public UUID side1PlayerId;
	public boolean side1IsBot;
	public NomosikRace race0 = NomosikRace.HUMAN;
	public NomosikRace race1 = NomosikRace.ORC;
	public boolean ready0;
	public boolean ready1;

	public int authority0 = 60;
	public int authority1 = 60;
	public int income0 = 2;
	public int income1 = 2;
	public int pop0;
	public int pop1;

	public long endsAtMs;
	public long startedAtMs;
	public int countdownLeft;

	@Nullable
	public UUID hq0Id;
	@Nullable
	public UUID hq1Id;
	public final Map<String, UUID> beaconIds = new HashMap<>();
	public final Set<UUID> trackedEntities = new HashSet<>();
	public final Set<BlockPos> trackedBlocks = new HashSet<>();
	public final Set<UUID> workerIds = new HashSet<>();
	public final List<MatchBuildOrder> buildOrders = new ArrayList<>();
	/** Beacon point id -> outpost heart UUID. */
	public final Map<String, UUID> outpostByBeacon = new HashMap<>();

	public Vec3 origin = Vec3.ZERO;
	@Nullable
	public Vec3 start0;
	@Nullable
	public Vec3 start1;

	public byte sideOfOwner(@Nullable UUID ownerId) {
		if (ownerId == null) {
			return CaptureBeaconEntity.SIDE_NONE;
		}
		if (ownerId.equals(this.side0PlayerId)) {
			return 0;
		}
		if (this.side1IsBot && ownerId.equals(BOT_OWNER_ID)) {
			return 1;
		}
		if (ownerId.equals(this.side1PlayerId)) {
			return 1;
		}
		return CaptureBeaconEntity.SIDE_NONE;
	}

	public UUID ownerOfSide(byte side) {
		if (side == 0) {
			return this.side0PlayerId;
		}
		if (side == 1) {
			return this.side1IsBot ? BOT_OWNER_ID : this.side1PlayerId;
		}
		return null;
	}

	public NomosikRace raceOfSide(byte side) {
		return side == 1 ? this.race1 : this.race0;
	}

	public int getAuthority(byte side) {
		return side == 1 ? this.authority1 : this.authority0;
	}

	public void setAuthority(byte side, int value) {
		value = Math.max(0, Math.min(800, value));
		if (side == 1) {
			this.authority1 = value;
		} else {
			this.authority0 = value;
		}
	}

	public void addAuthority(byte side, int delta) {
		this.setAuthority(side, this.getAuthority(side) + delta);
	}

	public int getIncome(byte side) {
		return side == 1 ? this.income1 : this.income0;
	}

	public void setIncome(byte side, int value) {
		if (side == 1) {
			this.income1 = value;
		} else {
			this.income0 = value;
		}
	}

	public int getPop(byte side) {
		return side == 1 ? this.pop1 : this.pop0;
	}

	public void setPop(byte side, int value) {
		if (side == 1) {
			this.pop1 = value;
		} else {
			this.pop0 = value;
		}
	}

	public void track(UUID entityId) {
		if (entityId != null) {
			this.trackedEntities.add(entityId);
		}
	}

	public void reset() {
		this.phase = MatchPhase.IDLE;
		this.endReason = MatchEndReason.NONE;
		this.winnerSide = CaptureBeaconEntity.SIDE_NONE;
		this.side0PlayerId = null;
		this.side1PlayerId = null;
		this.side1IsBot = false;
		this.race0 = NomosikRace.HUMAN;
		this.race1 = NomosikRace.ORC;
		this.ready0 = false;
		this.ready1 = false;
		this.authority0 = 60;
		this.authority1 = 60;
		this.income0 = 2;
		this.income1 = 2;
		this.pop0 = 0;
		this.pop1 = 0;
		this.endsAtMs = 0L;
		this.startedAtMs = 0L;
		this.countdownLeft = 0;
		this.hq0Id = null;
		this.hq1Id = null;
		this.beaconIds.clear();
		this.trackedEntities.clear();
		this.trackedBlocks.clear();
		this.workerIds.clear();
		this.buildOrders.clear();
		this.outpostByBeacon.clear();
		this.origin = Vec3.ZERO;
		this.start0 = null;
		this.start1 = null;
	}
}
