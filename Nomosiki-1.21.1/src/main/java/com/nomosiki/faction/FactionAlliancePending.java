package com.nomosiki.faction;

/** Unconfirmed alliance request between two factions. */
public record FactionAlliancePending(String requesterFaction, long requestedAtMs, FactionAllianceTier tier) {
	public FactionAlliancePending(String requesterFaction, long requestedAtMs) {
		this(requesterFaction, requestedAtMs, FactionAllianceTier.REGULAR);
	}
}
