package com.nomosiki.match;

import com.nomosiki.entity.NomosikRace;
import com.nomosiki.squad.SquadType;
import org.jetbrains.annotations.Nullable;

public final class MatchRecruitment {
	public enum Role {
		WORKER("worker", 40, 1, null),
		INFANTRY("infantry", 50, 1, SquadType.INFANTRY),
		ANTI_ARMOR("anti_armor", 50, 1, SquadType.INFANTRY),
		ARCHER("archer", 50, 1, SquadType.ARCHER),
		CAVALRY("cavalry", 120, 2, SquadType.KNIGHT),
		SIEGE("siege", 160, 3, SquadType.ARTILLERIST),
		SUPPORT("support", 90, 2, SquadType.INFANTRY),
		ELITE("elite", 280, 3, SquadType.CAPTAIN);

		private final String id;
		private final int baseCost;
		private final int pop;
		@Nullable
		private final SquadType squadType;

		Role(String id, int baseCost, int pop, @Nullable SquadType squadType) {
			this.id = id;
			this.baseCost = baseCost;
			this.pop = pop;
			this.squadType = squadType;
		}

		public String id() {
			return this.id;
		}

		public int pop() {
			return this.pop;
		}

		@Nullable
		public SquadType squadType() {
			return this.squadType;
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

		@Nullable
		public static Role fromId(String raw) {
			String id = raw == null ? "" : raw.trim().toLowerCase();
			for (Role role : values()) {
				if (role.id.equals(id) || role.name().equalsIgnoreCase(id)) {
					return role;
				}
			}
			return null;
		}

		public boolean allowedFor(NomosikRace race) {
			return !(this == CAVALRY && race == NomosikRace.DWARF);
		}
	}

	private MatchRecruitment() {
	}

	public static int squadSize(Role role) {
		return role == Role.WORKER ? 1 : 6;
	}
}
