package com.nomosiki.faction;

/**
 * Canonical unordered pair of faction names at war. {@link #factionA} sorts before
 * {@link #factionB} by {@link String#compareToIgnoreCase(String)}.
 */
public record FactionWarPair(String factionA, String factionB) {
	public static FactionWarPair of(String a, String b) {
		if (a == null || b == null || a.isBlank() || b.isBlank()) {
			throw new IllegalArgumentException("faction names required");
		}
		if (a.equalsIgnoreCase(b)) {
			throw new IllegalArgumentException("cannot pair faction with itself");
		}
		if (a.compareToIgnoreCase(b) <= 0) {
			return new FactionWarPair(a, b);
		}
		return new FactionWarPair(b, a);
	}

	public boolean matches(String a, String b) {
		return (this.factionA.equalsIgnoreCase(a) && this.factionB.equalsIgnoreCase(b))
			|| (this.factionA.equalsIgnoreCase(b) && this.factionB.equalsIgnoreCase(a));
	}

	public boolean involves(String factionName) {
		return this.factionA.equalsIgnoreCase(factionName) || this.factionB.equalsIgnoreCase(factionName);
	}
}
