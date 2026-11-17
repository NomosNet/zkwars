package com.nomosiki.network.faction;

import com.nomosiki.faction.FactionManager;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.FactionAllianceTier;
import com.nomosiki.faction.FactionDiplomacyService;
import com.nomosiki.faction.FactionWarSideChoice;
import com.nomosiki.network.faction.payload.c2s.FactionAllianceActionPayload;
import com.nomosiki.network.faction.payload.c2s.FactionCreatePayload;
import com.nomosiki.network.faction.payload.c2s.FactionDeclarePeacePayload;
import com.nomosiki.network.faction.payload.c2s.FactionDeclareWarPayload;
import com.nomosiki.network.faction.payload.c2s.FactionInvitePayload;
import com.nomosiki.network.faction.payload.c2s.FactionInviteResponsePayload;
import com.nomosiki.network.faction.payload.c2s.FactionLeavePayload;
import com.nomosiki.network.faction.payload.c2s.FactionOpenCreateMenuPayload;
import com.nomosiki.network.faction.payload.c2s.FactionOpenScreenPayload;
import com.nomosiki.network.faction.payload.s2c.FactionRenderSyncPayload;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import com.nomosiki.screen.FactionCreateMenu;
import com.nomosiki.screen.FactionCreateMenuProvider;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FactionNetworking {
	private FactionNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(FactionOpenScreenPayload.TYPE, FactionOpenScreenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionOpenCreateMenuPayload.TYPE, FactionOpenCreateMenuPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionCreatePayload.TYPE, FactionCreatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionInvitePayload.TYPE, FactionInvitePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionInviteResponsePayload.TYPE, FactionInviteResponsePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionLeavePayload.TYPE, FactionLeavePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionDeclareWarPayload.TYPE, FactionDeclareWarPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionDeclarePeacePayload.TYPE, FactionDeclarePeacePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FactionAllianceActionPayload.TYPE, FactionAllianceActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FactionRenderSyncPayload.TYPE, FactionRenderSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FactionScreenDataPayload.TYPE, FactionScreenDataPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FactionOpenScreenPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			FactionManager manager = FactionService.get(player.server);
			manager.sendScreenData(player);
			manager.sendRenderSyncTo(player);
		});

		ServerPlayNetworking.registerGlobalReceiver(FactionOpenCreateMenuPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			FactionManager manager = FactionService.get(player.server);
			if (manager.getPlayerFaction(player.getUUID()) != null) {
				player.displayClientMessage(Component.translatable("message.nomosiki.faction.already_in_faction"), true);
				return;
			}
			player.openMenu(new FactionCreateMenuProvider());
		});

		ServerPlayNetworking.registerGlobalReceiver(FactionCreatePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof FactionCreateMenu menu)) {
				player.displayClientMessage(Component.translatable("message.nomosiki.faction.nobanner"), true);
				return;
			}
			FactionManager manager = FactionService.get(player.server);
			String messageKey = manager.createFaction(player, payload.factionName(), menu);
			if (!messageKey.isEmpty()) {
				player.displayClientMessage(Component.translatable(messageKey), true);
			} else {
				player.closeContainer();
				player.displayClientMessage(Component.translatable("message.nomosiki.faction.created"), true);
			}
			manager.sendScreenData(player);
		});

		ServerPlayNetworking.registerGlobalReceiver(FactionInvitePayload.TYPE, (payload, context) ->
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				FactionManager manager = FactionService.get(player.server);
				String messageKey = manager.invite(player, payload.targetName());
				if (!messageKey.isEmpty()) {
					player.displayClientMessage(Component.translatable(messageKey), true);
				} else {
					player.displayClientMessage(Component.translatable("message.nomosiki.faction.invite_sent", payload.targetName()), true);
				}
				manager.sendScreenData(player);
			}));

		ServerPlayNetworking.registerGlobalReceiver(FactionInviteResponsePayload.TYPE, (payload, context) ->
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				FactionManager manager = FactionService.get(player.server);
				String messageKey = manager.respondInvite(player, payload.factionName(), payload.accept());
				if (!messageKey.isEmpty()) {
					player.displayClientMessage(Component.translatable(messageKey), true);
				}
				manager.sendScreenData(player);
			}));

		ServerPlayNetworking.registerGlobalReceiver(FactionLeavePayload.TYPE, (payload, context) ->
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				FactionManager manager = FactionService.get(player.server);
				String messageKey = manager.leaveFaction(player);
				if (!messageKey.isEmpty()) {
					player.displayClientMessage(Component.translatable(messageKey), true);
				} else {
					player.displayClientMessage(Component.translatable("message.nomosiki.faction.left"), true);
				}
				manager.sendScreenData(player);
			}));

		ServerPlayNetworking.registerGlobalReceiver(FactionDeclareWarPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleDeclareWar(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(FactionDeclarePeacePayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleDeclarePeace(payload, context.player())));

		ServerPlayNetworking.registerGlobalReceiver(FactionAllianceActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> handleAllianceAction(payload, context.player())));
	}

	private static void handleDeclareWar(FactionDeclareWarPayload payload, ServerPlayer player) {
		FactionDiplomacyService diplomacy = FactionService.diplomacy(player.server);
		String messageKey = diplomacy.declareWar(player, payload.enemyFactionName());
		if (!messageKey.isEmpty()) {
			player.displayClientMessage(Component.translatable(messageKey), true);
		} else {
			player.displayClientMessage(
				Component.translatable("message.nomosiki.faction.war_success", payload.enemyFactionName()),
				true
			);
		}
	}

	private static void handleDeclarePeace(FactionDeclarePeacePayload payload, ServerPlayer player) {
		FactionDiplomacyService diplomacy = FactionService.diplomacy(player.server);
		String messageKey = diplomacy.declarePeace(player, payload.enemyFactionName());
		if (!messageKey.isEmpty()) {
			player.displayClientMessage(Component.translatable(messageKey), true);
		} else {
			player.displayClientMessage(
				Component.translatable("message.nomosiki.faction.peace_offer_sent", payload.enemyFactionName()),
				true
			);
		}
	}

	private static void handleAllianceAction(FactionAllianceActionPayload payload, ServerPlayer player) {
		FactionDiplomacyService diplomacy = FactionService.diplomacy(player.server);
		String messageKey = switch (payload.actionId()) {
			case FactionAllianceActionPayload.REQUEST ->
				diplomacy.requestAlliance(player, payload.targetFactionName(), FactionAllianceTier.REGULAR);
			case FactionAllianceActionPayload.REQUEST_MILITARY ->
				diplomacy.requestAlliance(player, payload.targetFactionName(), FactionAllianceTier.MILITARY);
			case FactionAllianceActionPayload.ACCEPT -> diplomacy.acceptAlliance(player, payload.targetFactionName());
			case FactionAllianceActionPayload.DECLINE -> diplomacy.declineAlliance(player, payload.targetFactionName());
			case FactionAllianceActionPayload.CANCEL -> diplomacy.cancelAllianceRequest(player, payload.targetFactionName());
			case FactionAllianceActionPayload.BREAK -> diplomacy.breakAlliance(player, payload.targetFactionName());
			case FactionAllianceActionPayload.UPGRADE_TO_MILITARY ->
				diplomacy.requestAllianceUpgrade(player, payload.targetFactionName(), FactionAllianceTier.MILITARY);
			case FactionAllianceActionPayload.DOWNGRADE_TO_REGULAR ->
				diplomacy.requestAllianceUpgrade(player, payload.targetFactionName(), FactionAllianceTier.REGULAR);
			case FactionAllianceActionPayload.JOIN_WAR -> diplomacy.acceptWarJoin(player, payload.targetFactionName());
			case FactionAllianceActionPayload.DECLINE_WAR_JOIN -> diplomacy.declineWarJoin(player, payload.targetFactionName());
			case FactionAllianceActionPayload.WAR_SIDE_SUPPORT_AGGRESSOR ->
				diplomacy.resolveWarSideChoice(player, payload.targetFactionName(), FactionWarSideChoice.SUPPORT_AGGRESSOR);
			case FactionAllianceActionPayload.WAR_SIDE_SUPPORT_DEFENDER ->
				diplomacy.resolveWarSideChoice(player, payload.targetFactionName(), FactionWarSideChoice.SUPPORT_DEFENDER);
			case FactionAllianceActionPayload.WAR_SIDE_STAY_NEUTRAL ->
				diplomacy.resolveWarSideChoice(player, payload.targetFactionName(), FactionWarSideChoice.STAY_NEUTRAL);
			case FactionAllianceActionPayload.REQUEST_PEACE -> diplomacy.declarePeace(player, payload.targetFactionName());
			case FactionAllianceActionPayload.ACCEPT_PEACE -> diplomacy.acceptPeace(player, payload.targetFactionName());
			case FactionAllianceActionPayload.DECLINE_PEACE -> diplomacy.declinePeace(player, payload.targetFactionName());
			case FactionAllianceActionPayload.CANCEL_PEACE -> diplomacy.cancelPeace(player, payload.targetFactionName());
			default -> "message.nomosiki.faction.alliance_no_pending";
		};
		if (!messageKey.isEmpty()) {
			player.displayClientMessage(Component.translatable(messageKey), true);
			return;
		}
		Component success = switch (payload.actionId()) {
			case FactionAllianceActionPayload.REQUEST ->
				Component.translatable("message.nomosiki.faction.alliance_request_success", payload.targetFactionName());
			case FactionAllianceActionPayload.REQUEST_MILITARY ->
				Component.translatable("message.nomosiki.faction.military_alliance_request_success", payload.targetFactionName());
			case FactionAllianceActionPayload.ACCEPT ->
				Component.translatable("message.nomosiki.faction.alliance_accept_success", payload.targetFactionName());
			case FactionAllianceActionPayload.DECLINE ->
				Component.translatable("message.nomosiki.faction.alliance_decline_success", payload.targetFactionName());
			case FactionAllianceActionPayload.CANCEL ->
				Component.translatable("message.nomosiki.faction.alliance_cancel_success", payload.targetFactionName());
			case FactionAllianceActionPayload.BREAK ->
				Component.translatable("message.nomosiki.faction.alliance_break_success", payload.targetFactionName());
			case FactionAllianceActionPayload.UPGRADE_TO_MILITARY ->
				Component.translatable("message.nomosiki.faction.military_alliance_upgrade_sent", payload.targetFactionName());
			case FactionAllianceActionPayload.DOWNGRADE_TO_REGULAR ->
				Component.translatable("message.nomosiki.faction.alliance_downgrade_sent", payload.targetFactionName());
			case FactionAllianceActionPayload.JOIN_WAR ->
				Component.translatable("message.nomosiki.faction.war_join_success", payload.targetFactionName());
			case FactionAllianceActionPayload.DECLINE_WAR_JOIN ->
				Component.translatable("message.nomosiki.faction.war_join_declined", payload.targetFactionName());
			case FactionAllianceActionPayload.WAR_SIDE_SUPPORT_AGGRESSOR, FactionAllianceActionPayload.WAR_SIDE_SUPPORT_DEFENDER ->
				Component.translatable("message.nomosiki.faction.war_side_choice_success");
			case FactionAllianceActionPayload.WAR_SIDE_STAY_NEUTRAL ->
				Component.translatable("message.nomosiki.faction.war_side_stay_success");
			case FactionAllianceActionPayload.REQUEST_PEACE ->
				Component.translatable("message.nomosiki.faction.peace_offer_success", payload.targetFactionName());
			case FactionAllianceActionPayload.ACCEPT_PEACE ->
				Component.translatable("message.nomosiki.faction.peace_success", payload.targetFactionName());
			case FactionAllianceActionPayload.DECLINE_PEACE ->
				Component.translatable("message.nomosiki.faction.peace_decline_success", payload.targetFactionName());
			case FactionAllianceActionPayload.CANCEL_PEACE ->
				Component.translatable("message.nomosiki.faction.peace_cancel_success", payload.targetFactionName());
			default -> Component.empty();
		};
		if (!success.getString().isEmpty()) {
			player.displayClientMessage(success, true);
		}
	}
}
