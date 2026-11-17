package com.nomosiki.faction;

/** Regular ally may opt in when their ally is attacked. */
public record FactionWarJoinPending(
	String aggressorFaction,
	String defenderFaction,
	String allyFaction,
	long notifiedAtMs
) {
}
