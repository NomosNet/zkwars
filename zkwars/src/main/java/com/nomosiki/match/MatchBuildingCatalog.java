package com.nomosiki.match;

import com.nomosiki.entity.NomosikRace;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MatchBuildingCatalog {
	public enum Kind {
		BARRACKS("barracks", BuildingHeartType.BARRACKS, 80, MatchRecruitment.Role.INFANTRY, MatchRecruitment.Role.ANTI_ARMOR),
		ARCHERY("archery", BuildingHeartType.BARRACKS, 80, MatchRecruitment.Role.ARCHER),
		STABLE("stable", BuildingHeartType.BARRACKS, 120, MatchRecruitment.Role.CAVALRY),
		SIEGE_WORKS("siege_works", BuildingHeartType.BARRACKS, 160, MatchRecruitment.Role.SIEGE),
		OUTPOST("outpost", BuildingHeartType.OUTPOST, 80),
		TOWER("tower", BuildingHeartType.TOWER, 140);

		private final String id;
		private final BuildingHeartType heartType;
		private final int baseCost;
		private final MatchRecruitment.Role[] unlocks;

		Kind(String id, BuildingHeartType heartType, int baseCost, MatchRecruitment.Role... unlocks) {
			this.id = id;
			this.heartType = heartType;
			this.baseCost = baseCost;
			this.unlocks = unlocks;
		}

		public String id() {
			return this.id;
		}

		public BuildingHeartType heartType() {
			return this.heartType;
		}

		public MatchRecruitment.Role[] unlocks() {
			return this.unlocks;
		}

		public int cost(NomosikRace race) {
			double mult = switch (race) {
				case ORC -> 0.75;
				case DWARF -> 1.3;
				default -> 1.0;
			};
			int raw = (int) Math.round(this.baseCost * mult);
			return Math.max(5, ((raw + 2) / 5) * 5);
		}

		public boolean allowedFor(NomosikRace race) {
			return !(this == STABLE && race == NomosikRace.DWARF);
		}

		@Nullable
		public static Kind fromId(String raw) {
			String id = raw == null ? "" : raw.trim().toLowerCase();
			for (Kind kind : values()) {
				if (kind.id.equals(id) || kind.name().equalsIgnoreCase(id)) {
					return kind;
				}
			}
			return null;
		}

		public static List<Kind> all() {
			return List.of(values());
		}
	}

	private MatchBuildingCatalog() {
	}

	/** Roles always available from HQ. */
	public static boolean alwaysUnlocked(MatchRecruitment.Role role) {
		return role == MatchRecruitment.Role.WORKER
			|| role == MatchRecruitment.Role.INFANTRY
			|| role == MatchRecruitment.Role.SUPPORT
			|| role == MatchRecruitment.Role.ELITE;
	}

	@Nullable
	public static Kind requiredBuilding(MatchRecruitment.Role role) {
		return switch (role) {
			case ANTI_ARMOR -> Kind.BARRACKS;
			case ARCHER -> Kind.ARCHERY;
			case CAVALRY -> Kind.STABLE;
			case SIEGE -> Kind.SIEGE_WORKS;
			default -> null;
		};
	}
}
