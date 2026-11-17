package com.nomosiki.network.shop;

import com.nomosiki.network.shop.payload.c2s.TesterSquadShopBuyPayload;
import com.nomosiki.shop.TesterSquadShop;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class TesterSquadShopNetworking {
	private TesterSquadShopNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(TesterSquadShopBuyPayload.TYPE, TesterSquadShopBuyPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TesterSquadShopBuyPayload.TYPE, (payload, context) ->
			TesterSquadShop.tryBuy(context.player(), payload.pos(), payload.name(), payload.typeId(), payload.count()));
	}
}
