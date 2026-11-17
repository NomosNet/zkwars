package com.nomosiki.client.rts;

import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.squad.SquadType;
import net.minecraft.network.chat.Component;

public final class BattleHudOverlay {
	private BattleHudOverlay() {
	}

	static Component orderLabel(SquadOrder order) {
		return Component.translatable(switch (order) {
			case FOLLOW -> "button.nomosiki.squad_follow";
			case HOLD -> "button.nomosiki.squad_hold";
			case MOVE_TO -> "button.nomosiki.squad_move";
			case ATTACK_ZONE -> "button.nomosiki.squad_attack_zone";
			case ATTACK_TARGET -> "button.nomosiki.squad_attack_target";
		});
	}

	public static boolean isRangedBranch(byte typeId) {
		SquadType type = SquadType.fromId(typeId);
		return type == SquadType.ARCHER || type == SquadType.SAMURAI_ARCHER
			|| type == SquadType.SAMURAI_ARCHER_CAVALRY || type == SquadType.NOMAD || type == SquadType.NINJA
			|| type == SquadType.MONK_ARCHER
			|| type == SquadType.ARTILLERIST || type == SquadType.SPANISH_ARTILLERIST
			|| type.isMusketeer();
	}

	static Component fireModeLabel(NomosikRangedFireMode mode) {
		return Component.translatable(switch (mode) {
			case HOLD_FIRE -> "nomosiki.rts.fire_mode.hold_fire";
			case FIRE_AT_WILL -> "nomosiki.rts.fire_mode.fire_at_will";
			case STRATEGIC_FIRE -> "nomosiki.rts.fire_mode.strategic_fire";
		});
	}

	static Component stanceLabel(NomosikCombatMode mode) {
		return Component.translatable(switch (mode) {
			case PASSIVE -> "button.nomosiki.combat_mode.passive";
			case NEUTRAL -> "button.nomosiki.combat_mode.neutral";
			case AGGRESSIVE -> "button.nomosiki.combat_mode.aggressive";
			case RAID -> "button.nomosiki.combat_mode.raid";
		});
	}
}
