package com.nomosiki.discord;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Payload for a Discord diplomacy notification. */
public record DiplomacyDiscordEvent(
	Type type,
	String factionA,
	String factionB,
	@Nullable String leaderName,
	long epochMs,
	/** Extra war pairs for cascade summary (may be empty). */
	List<String> cascadePairs,
	boolean militaryAlliance,
	/** City name, wartime clock, or empty. */
	String detail
) {
	public DiplomacyDiscordEvent {
		cascadePairs = cascadePairs == null ? List.of() : List.copyOf(cascadePairs);
		if (factionA == null) {
			factionA = "";
		}
		if (factionB == null) {
			factionB = "";
		}
		if (detail == null) {
			detail = "";
		}
	}

	public static DiplomacyDiscordEvent of(
		Type type,
		String factionA,
		String factionB,
		@Nullable String leaderName
	) {
		return new DiplomacyDiscordEvent(type, factionA, factionB, leaderName, System.currentTimeMillis(), List.of(), false, "");
	}

	public static DiplomacyDiscordEvent notice(
		Type type,
		String factionA,
		String factionB,
		@Nullable String leaderName,
		String detail
	) {
		return new DiplomacyDiscordEvent(type, factionA, factionB, leaderName, System.currentTimeMillis(), List.of(), false, detail);
	}

	public static DiplomacyDiscordEvent alliance(
		Type type,
		String factionA,
		String factionB,
		@Nullable String leaderName,
		boolean militaryAlliance
	) {
		return new DiplomacyDiscordEvent(type, factionA, factionB, leaderName, System.currentTimeMillis(), List.of(), militaryAlliance, "");
	}

	public static DiplomacyDiscordEvent war(
		String factionA,
		String factionB,
		@Nullable String leaderName,
		List<String> cascadePairs
	) {
		return new DiplomacyDiscordEvent(
			Type.WAR_DECLARED, factionA, factionB, leaderName, System.currentTimeMillis(), cascadePairs, false, "");
	}

	public enum Type {
		WAR_DECLARED,
		PEACE_DECLARED,
		PEACE_OFFERED,
		PEACE_DECLINED,
		ALLIANCE_REQUESTED,
		ALLIANCE_FORMED,
		ALLIANCE_DECLINED,
		ALLIANCE_BROKEN,
		WARTIME_STARTED,
		WARTIME_STOPPED,
		WARTIME_EXPIRED,
		CAPTURE_STARTED,
		CAPTURE_COMPLETED
	}
}
