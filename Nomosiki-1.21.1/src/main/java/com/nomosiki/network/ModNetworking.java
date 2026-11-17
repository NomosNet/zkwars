package com.nomosiki.network;

import com.nomosiki.network.duel.DuelNetworking;
import com.nomosiki.network.faction.FactionNetworking;
import com.nomosiki.network.rts.RtsNetworking;
import com.nomosiki.network.shop.TesterSquadShopNetworking;
import com.nomosiki.network.squad.SquadCommandNetworking;
import com.nomosiki.network.squad.SquadManagementNetworking;
import com.nomosiki.compat.emote.network.NomosikEmoteNetworking;
import com.nomosiki.network.squad.SquadNetworking;
import com.nomosiki.network.unit.UnitNetworking;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		FactionNetworking.register();
		SquadNetworking.register();
		SquadCommandNetworking.register();
		SquadManagementNetworking.register();
		UnitNetworking.register();
		RtsNetworking.register();
		NomosikEmoteNetworking.register();
		TesterSquadShopNetworking.register();
		DuelNetworking.register();
	}
}
