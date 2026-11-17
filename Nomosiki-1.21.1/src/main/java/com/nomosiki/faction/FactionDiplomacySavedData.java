package com.nomosiki.faction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted symmetric diplomacy between faction names: stance + pending alliance requests.
 */
public final class FactionDiplomacySavedData extends SavedData {
	public static final String DATA_ID = "nomosiki_faction_diplomacy";

	private final Map<FactionWarPair, FactionRelationStatus> relations = new HashMap<>();
	private final Map<FactionWarPair, FactionAlliancePending> pendingAlliances = new HashMap<>();
	private final Map<FactionWarPair, FactionPeacePending> pendingPeace = new HashMap<>();
	private final List<FactionWarJoinPending> warJoinPending = new ArrayList<>();
	private final List<FactionWarSideChoicePending> warSideChoicePending = new ArrayList<>();
	private boolean migratedFromWarData;

	public static Factory<FactionDiplomacySavedData> factory() {
		return new Factory<>(FactionDiplomacySavedData::new, FactionDiplomacySavedData::fromNbt, DataFixTypes.LEVEL);
	}

	private static FactionDiplomacySavedData fromNbt(CompoundTag tag, HolderLookup.Provider lookup) {
		FactionDiplomacySavedData data = new FactionDiplomacySavedData();
		data.migratedFromWarData = tag.getBoolean("MigratedFromWarData");
		ListTag relations = tag.getList("Relations", Tag.TAG_COMPOUND);
		for (int i = 0; i < relations.size(); i++) {
			CompoundTag row = relations.getCompound(i);
			String a = row.getString("A");
			String b = row.getString("B");
			FactionRelationStatus status = FactionRelationStatus.fromId(row.getByte("Status"));
			if (a.isBlank() || b.isBlank() || a.equalsIgnoreCase(b) || status == FactionRelationStatus.NEUTRAL) {
				continue;
			}
			try {
				data.relations.put(FactionWarPair.of(a, b), status);
			} catch (IllegalArgumentException ignored) {
			}
		}
		ListTag pending = tag.getList("PendingAlliances", Tag.TAG_COMPOUND);
		for (int i = 0; i < pending.size(); i++) {
			CompoundTag row = pending.getCompound(i);
			String a = row.getString("A");
			String b = row.getString("B");
			String requester = row.getString("Requester");
			long at = row.getLong("RequestedAt");
			FactionAllianceTier tier = row.contains("Tier")
				? FactionAllianceTier.fromId(row.getByte("Tier"))
				: FactionAllianceTier.REGULAR;
			if (a.isBlank() || b.isBlank() || requester.isBlank()) {
				continue;
			}
			try {
				data.pendingAlliances.put(FactionWarPair.of(a, b), new FactionAlliancePending(requester, at, tier));
			} catch (IllegalArgumentException ignored) {
			}
		}
		ListTag peace = tag.getList("PendingPeace", Tag.TAG_COMPOUND);
		for (int i = 0; i < peace.size(); i++) {
			CompoundTag row = peace.getCompound(i);
			String a = row.getString("A");
			String b = row.getString("B");
			String requester = row.getString("Requester");
			long at = row.getLong("RequestedAt");
			if (a.isBlank() || b.isBlank() || requester.isBlank()) {
				continue;
			}
			try {
				data.pendingPeace.put(FactionWarPair.of(a, b), new FactionPeacePending(requester, at));
			} catch (IllegalArgumentException ignored) {
			}
		}
		ListTag warJoin = tag.getList("WarJoinPending", Tag.TAG_COMPOUND);
		for (int i = 0; i < warJoin.size(); i++) {
			CompoundTag row = warJoin.getCompound(i);
			String aggressor = row.getString("Aggressor");
			String defender = row.getString("Defender");
			String ally = row.getString("Ally");
			long at = row.getLong("NotifiedAt");
			if (aggressor.isBlank() || defender.isBlank() || ally.isBlank()) {
				continue;
			}
			data.warJoinPending.add(new FactionWarJoinPending(aggressor, defender, ally, at));
		}
		ListTag sideChoice = tag.getList("WarSideChoicePending", Tag.TAG_COMPOUND);
		for (int i = 0; i < sideChoice.size(); i++) {
			CompoundTag row = sideChoice.getCompound(i);
			String aggressor = row.getString("Aggressor");
			String defender = row.getString("Defender");
			String conflicted = row.getString("Conflicted");
			long at = row.getLong("NotifiedAt");
			if (aggressor.isBlank() || defender.isBlank() || conflicted.isBlank()) {
				continue;
			}
			data.warSideChoicePending.add(new FactionWarSideChoicePending(aggressor, defender, conflicted, at));
		}
		return data;
	}

	public boolean isMigratedFromWarData() {
		return this.migratedFromWarData;
	}

	public void migrateFromWarData(FactionWarSavedData warData) {
		if (this.migratedFromWarData) {
			return;
		}
		for (FactionWarPair pair : warData.allPairs()) {
			this.relations.put(pair, FactionRelationStatus.AT_WAR);
		}
		this.migratedFromWarData = true;
		this.setDirty();
	}

	public FactionRelationStatus getRelation(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return FactionRelationStatus.NEUTRAL;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return FactionRelationStatus.NEUTRAL;
		}
		FactionRelationStatus status = this.relations.get(FactionWarPair.of(factionA, factionB));
		return status == null ? FactionRelationStatus.NEUTRAL : status;
	}

	public void setRelation(String factionA, String factionB, FactionRelationStatus status) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return;
		}
		FactionWarPair pair = FactionWarPair.of(factionA, factionB);
		if (status == null || status == FactionRelationStatus.NEUTRAL) {
			boolean changed = this.relations.remove(pair) != null;
			changed |= this.pendingPeace.remove(pair) != null;
			if (changed) {
				this.setDirty();
			}
			return;
		}
		FactionRelationStatus previous = this.relations.put(pair, status);
		if (previous != status) {
			if (status.isAlliance() || status == FactionRelationStatus.AT_WAR) {
				this.pendingAlliances.remove(pair);
			}
			this.pendingPeace.remove(pair);
			this.setDirty();
		}
	}

	@Nullable
	public FactionAlliancePending getPendingAlliance(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return null;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return null;
		}
		return this.pendingAlliances.get(FactionWarPair.of(factionA, factionB));
	}

	public void setPendingAlliance(String factionA, String factionB, FactionAlliancePending pending) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank() || pending == null) {
			return;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return;
		}
		this.pendingAlliances.put(FactionWarPair.of(factionA, factionB), pending);
		this.setDirty();
	}

	@Nullable
	public FactionPeacePending getPendingPeace(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank() || factionA.equalsIgnoreCase(factionB)) {
			return null;
		}
		return this.pendingPeace.get(FactionWarPair.of(factionA, factionB));
	}

	public void setPendingPeace(String factionA, String factionB, FactionPeacePending pending) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank() || pending == null) {
			return;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return;
		}
		this.pendingPeace.put(FactionWarPair.of(factionA, factionB), pending);
		this.setDirty();
	}

	public void clearPendingPeace(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank() || factionA.equalsIgnoreCase(factionB)) {
			return;
		}
		if (this.pendingPeace.remove(FactionWarPair.of(factionA, factionB)) != null) {
			this.setDirty();
		}
	}

	public boolean hasIncomingPeace(String myFaction, String otherFaction) {
		FactionPeacePending pending = getPendingPeace(myFaction, otherFaction);
		return pending != null && !pending.requesterFaction().equalsIgnoreCase(myFaction);
	}

	public boolean hasOutgoingPeace(String myFaction, String otherFaction) {
		FactionPeacePending pending = getPendingPeace(myFaction, otherFaction);
		return pending != null && pending.requesterFaction().equalsIgnoreCase(myFaction);
	}

	public void clearPendingAlliance(String factionA, String factionB) {
		if (factionA == null || factionB == null || factionA.isBlank() || factionB.isBlank()) {
			return;
		}
		if (factionA.equalsIgnoreCase(factionB)) {
			return;
		}
		if (this.pendingAlliances.remove(FactionWarPair.of(factionA, factionB)) != null) {
			this.setDirty();
		}
	}

	public boolean hasIncomingAllianceRequest(String myFaction, String otherFaction) {
		FactionAlliancePending pending = getPendingAlliance(myFaction, otherFaction);
		return pending != null && !pending.requesterFaction().equalsIgnoreCase(myFaction);
	}

	public boolean hasOutgoingAllianceRequest(String myFaction, String otherFaction) {
		FactionAlliancePending pending = getPendingAlliance(myFaction, otherFaction);
		return pending != null && pending.requesterFaction().equalsIgnoreCase(myFaction);
	}

	public void addWarJoinPending(FactionWarJoinPending pending) {
		this.warJoinPending.removeIf(existing -> existing.allyFaction().equalsIgnoreCase(pending.allyFaction())
			&& existing.aggressorFaction().equalsIgnoreCase(pending.aggressorFaction())
			&& existing.defenderFaction().equalsIgnoreCase(pending.defenderFaction()));
		this.warJoinPending.add(pending);
		this.setDirty();
	}

	@Nullable
	public FactionWarJoinPending findWarJoinPending(String allyFaction, String aggressorFaction) {
		for (FactionWarJoinPending pending : this.warJoinPending) {
			if (pending.allyFaction().equalsIgnoreCase(allyFaction)
				&& pending.aggressorFaction().equalsIgnoreCase(aggressorFaction)) {
				return pending;
			}
		}
		return null;
	}

	public boolean hasWarJoinPending(String allyFaction) {
		for (FactionWarJoinPending pending : this.warJoinPending) {
			if (pending.allyFaction().equalsIgnoreCase(allyFaction)) {
				return true;
			}
		}
		return false;
	}

	public void clearWarJoinPending(String allyFaction, String aggressorFaction) {
		if (this.warJoinPending.removeIf(pending ->
			pending.allyFaction().equalsIgnoreCase(allyFaction)
				&& pending.aggressorFaction().equalsIgnoreCase(aggressorFaction))) {
			this.setDirty();
		}
	}

	public void addWarSideChoicePending(FactionWarSideChoicePending pending) {
		this.warSideChoicePending.removeIf(existing -> existing.conflictedFaction().equalsIgnoreCase(pending.conflictedFaction())
			&& existing.aggressorFaction().equalsIgnoreCase(pending.aggressorFaction())
			&& existing.defenderFaction().equalsIgnoreCase(pending.defenderFaction()));
		this.warSideChoicePending.add(pending);
		this.setDirty();
	}

	@Nullable
	public FactionWarSideChoicePending findWarSideChoicePending(String conflictedFaction, String aggressorFaction) {
		for (FactionWarSideChoicePending pending : this.warSideChoicePending) {
			if (pending.conflictedFaction().equalsIgnoreCase(conflictedFaction)
				&& pending.aggressorFaction().equalsIgnoreCase(aggressorFaction)) {
				return pending;
			}
		}
		return null;
	}

	public boolean hasWarSideChoicePending(String conflictedFaction) {
		for (FactionWarSideChoicePending pending : this.warSideChoicePending) {
			if (pending.conflictedFaction().equalsIgnoreCase(conflictedFaction)) {
				return true;
			}
		}
		return false;
	}

	public void clearWarSideChoicePending(String conflictedFaction, String aggressorFaction) {
		if (this.warSideChoicePending.removeIf(pending ->
			pending.conflictedFaction().equalsIgnoreCase(conflictedFaction)
				&& pending.aggressorFaction().equalsIgnoreCase(aggressorFaction))) {
			this.setDirty();
		}
	}

	public List<String> militaryAlliesOf(String factionName) {
		List<String> result = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionRelationStatus> entry : this.relations.entrySet()) {
			if (entry.getValue() != FactionRelationStatus.MILITARY_ALLY) {
				continue;
			}
			FactionWarPair pair = entry.getKey();
			if (pair.factionA().equalsIgnoreCase(factionName)) {
				result.add(pair.factionB());
			} else if (pair.factionB().equalsIgnoreCase(factionName)) {
				result.add(pair.factionA());
			}
		}
		return List.copyOf(result);
	}

	public List<String> regularAlliesOf(String factionName) {
		List<String> result = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionRelationStatus> entry : this.relations.entrySet()) {
			if (entry.getValue() != FactionRelationStatus.ALLY) {
				continue;
			}
			FactionWarPair pair = entry.getKey();
			if (pair.factionA().equalsIgnoreCase(factionName)) {
				result.add(pair.factionB());
			} else if (pair.factionB().equalsIgnoreCase(factionName)) {
				result.add(pair.factionA());
			}
		}
		return List.copyOf(result);
	}

	public void removeFaction(String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return;
		}
		boolean changed = this.relations.keySet().removeIf(pair -> pair.involves(factionName));
		changed |= this.pendingAlliances.keySet().removeIf(pair -> pair.involves(factionName));
		changed |= this.pendingPeace.keySet().removeIf(pair -> pair.involves(factionName));
		changed |= this.warJoinPending.removeIf(pending ->
			pending.aggressorFaction().equalsIgnoreCase(factionName)
				|| pending.defenderFaction().equalsIgnoreCase(factionName)
				|| pending.allyFaction().equalsIgnoreCase(factionName));
		changed |= this.warSideChoicePending.removeIf(pending ->
			pending.aggressorFaction().equalsIgnoreCase(factionName)
				|| pending.defenderFaction().equalsIgnoreCase(factionName)
				|| pending.conflictedFaction().equalsIgnoreCase(factionName));
		if (changed) {
			this.setDirty();
		}
	}

	public List<PeaceInvolving> peaceInvolving(String factionName) {
		List<PeaceInvolving> result = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionPeacePending> entry : this.pendingPeace.entrySet()) {
			if (!entry.getKey().involves(factionName)) {
				continue;
			}
			String other = entry.getKey().factionA().equalsIgnoreCase(factionName)
				? entry.getKey().factionB()
				: entry.getKey().factionA();
			result.add(new PeaceInvolving(other, entry.getValue().requesterFaction()));
		}
		return List.copyOf(result);
	}

	public List<AllianceInvolving> alliancesPendingInvolving(String factionName) {
		List<AllianceInvolving> result = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionAlliancePending> entry : this.pendingAlliances.entrySet()) {
			if (!entry.getKey().involves(factionName)) {
				continue;
			}
			String other = entry.getKey().factionA().equalsIgnoreCase(factionName)
				? entry.getKey().factionB()
				: entry.getKey().factionA();
			result.add(new AllianceInvolving(other, entry.getValue().requesterFaction(), entry.getValue().tier()));
		}
		return List.copyOf(result);
	}

	public List<FactionWarJoinPending> warJoinsInvolving(String factionName) {
		List<FactionWarJoinPending> result = new ArrayList<>();
		for (FactionWarJoinPending pending : this.warJoinPending) {
			if (pending.allyFaction().equalsIgnoreCase(factionName)
				|| pending.aggressorFaction().equalsIgnoreCase(factionName)
				|| pending.defenderFaction().equalsIgnoreCase(factionName)) {
				result.add(pending);
			}
		}
		return List.copyOf(result);
	}

	public List<FactionWarSideChoicePending> sideChoicesInvolving(String factionName) {
		List<FactionWarSideChoicePending> result = new ArrayList<>();
		for (FactionWarSideChoicePending pending : this.warSideChoicePending) {
			if (pending.conflictedFaction().equalsIgnoreCase(factionName)
				|| pending.aggressorFaction().equalsIgnoreCase(factionName)
				|| pending.defenderFaction().equalsIgnoreCase(factionName)) {
				result.add(pending);
			}
		}
		return List.copyOf(result);
	}

	public record PeaceInvolving(String otherFaction, String requesterFaction) {
	}

	public record AllianceInvolving(String otherFaction, String requesterFaction, FactionAllianceTier tier) {
	}

	public List<FactionWarPair> allAtWarPairs() {
		List<FactionWarPair> result = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionRelationStatus> entry : this.relations.entrySet()) {
			if (entry.getValue() == FactionRelationStatus.AT_WAR) {
				result.add(entry.getKey());
			}
		}
		return List.copyOf(result);
	}

	public List<FactionWarPair> allAlliedPairs() {
		List<FactionWarPair> result = new ArrayList<>();
		for (Map.Entry<FactionWarPair, FactionRelationStatus> entry : this.relations.entrySet()) {
			if (entry.getValue().isAlliance()) {
				result.add(entry.getKey());
			}
		}
		return List.copyOf(result);
	}

	public Map<FactionWarPair, FactionRelationStatus> nonNeutralRelations() {
		return Map.copyOf(this.relations);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putBoolean("MigratedFromWarData", this.migratedFromWarData);
		ListTag relationsTag = new ListTag();
		for (Map.Entry<FactionWarPair, FactionRelationStatus> entry : this.relations.entrySet()) {
			if (entry.getValue() == FactionRelationStatus.NEUTRAL) {
				continue;
			}
			CompoundTag row = new CompoundTag();
			row.putString("A", entry.getKey().factionA());
			row.putString("B", entry.getKey().factionB());
			row.putByte("Status", entry.getValue().id());
			relationsTag.add(row);
		}
		tag.put("Relations", relationsTag);
		ListTag pendingTag = new ListTag();
		for (Map.Entry<FactionWarPair, FactionAlliancePending> entry : this.pendingAlliances.entrySet()) {
			CompoundTag row = new CompoundTag();
			row.putString("A", entry.getKey().factionA());
			row.putString("B", entry.getKey().factionB());
			row.putString("Requester", entry.getValue().requesterFaction());
			row.putLong("RequestedAt", entry.getValue().requestedAtMs());
			row.putByte("Tier", entry.getValue().tier().id());
			pendingTag.add(row);
		}
		tag.put("PendingAlliances", pendingTag);
		ListTag peaceTag = new ListTag();
		for (Map.Entry<FactionWarPair, FactionPeacePending> entry : this.pendingPeace.entrySet()) {
			CompoundTag row = new CompoundTag();
			row.putString("A", entry.getKey().factionA());
			row.putString("B", entry.getKey().factionB());
			row.putString("Requester", entry.getValue().requesterFaction());
			row.putLong("RequestedAt", entry.getValue().requestedAtMs());
			peaceTag.add(row);
		}
		tag.put("PendingPeace", peaceTag);
		ListTag warJoinTag = new ListTag();
		for (FactionWarJoinPending pending : this.warJoinPending) {
			CompoundTag row = new CompoundTag();
			row.putString("Aggressor", pending.aggressorFaction());
			row.putString("Defender", pending.defenderFaction());
			row.putString("Ally", pending.allyFaction());
			row.putLong("NotifiedAt", pending.notifiedAtMs());
			warJoinTag.add(row);
		}
		tag.put("WarJoinPending", warJoinTag);
		ListTag sideChoiceTag = new ListTag();
		for (FactionWarSideChoicePending pending : this.warSideChoicePending) {
			CompoundTag row = new CompoundTag();
			row.putString("Aggressor", pending.aggressorFaction());
			row.putString("Defender", pending.defenderFaction());
			row.putString("Conflicted", pending.conflictedFaction());
			row.putLong("NotifiedAt", pending.notifiedAtMs());
			sideChoiceTag.add(row);
		}
		tag.put("WarSideChoicePending", sideChoiceTag);
		return tag;
	}
}
