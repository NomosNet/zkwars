package com.nomosiki.faction;

/** Military ally of both belligerents — pick a side or stay neutral. */
public record FactionWarSideChoicePending(
	String aggressorFaction,
	String defenderFaction,
	String conflictedFaction,
	long notifiedAtMs
) {
}
