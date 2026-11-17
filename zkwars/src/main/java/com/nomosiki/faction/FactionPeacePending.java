package com.nomosiki.faction;

/** Unconfirmed peace offer between two factions at war. */
public record FactionPeacePending(String requesterFaction, long requestedAtMs) {
}
