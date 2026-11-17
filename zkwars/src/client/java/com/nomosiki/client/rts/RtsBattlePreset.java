package com.nomosiki.client.rts;

import com.nomosiki.entity.SquadFormation;
import com.nomosiki.squad.SquadType;

public record RtsBattlePreset(SquadType squadType, SquadFormation formation, float spacing, String labelKey) {
	private static final RtsBattlePreset[] PRESETS = {
		new RtsBattlePreset(SquadType.INFANTRY, SquadFormation.LINE, 2.0f, "nomosiki.rts.preset.infantry_line"),
		new RtsBattlePreset(SquadType.INFANTRY, SquadFormation.V, 1.5f, "nomosiki.rts.preset.infantry_wedge"),
		new RtsBattlePreset(SquadType.ARCHER, SquadFormation.LINE, 2.5f, "nomosiki.rts.preset.archer_line"),
		new RtsBattlePreset(SquadType.NOMAD, SquadFormation.V, 3.0f, "nomosiki.rts.preset.nomad_wedge"),
		new RtsBattlePreset(SquadType.INFANTRY, SquadFormation.HOLLOW_SQUARE, 1.75f, "nomosiki.rts.preset.infantry_defense"),
	};

	private static int cycleIndex;

	public static RtsBattlePreset nextFor(SquadType type) {
		SquadType match = switch (type) {
			case KNIGHT, CAPTAIN -> SquadType.INFANTRY;
			case ARTILLERIST -> SquadType.ARCHER;
			default -> type;
		};
		for (int step = 0; step < PRESETS.length; step++) {
			cycleIndex = (cycleIndex + 1) % PRESETS.length;
			if (PRESETS[cycleIndex].squadType() == match) {
				return PRESETS[cycleIndex];
			}
		}
		return PRESETS[0];
	}
}
