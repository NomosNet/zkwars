package com.nomosiki.network.faction;

import com.nomosiki.faction.FactionRelationStatus;
import org.jetbrains.annotations.Nullable;

/** Compact non-neutral stance between two factions (symmetric). */
public record FactionRelationEdge(String factionA, String factionB, byte relationStatusId) {
	public FactionRelationStatus relationStatus() {
		return FactionRelationStatus.fromId(this.relationStatusId);
	}

	@Nullable
	public String otherOf(String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return null;
		}
		if (this.factionA.equalsIgnoreCase(factionName)) {
			return this.factionB;
		}
		if (this.factionB.equalsIgnoreCase(factionName)) {
			return this.factionA;
		}
		return null;
	}
}
