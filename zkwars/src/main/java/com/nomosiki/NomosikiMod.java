package com.nomosiki;

import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.duel.DuelCommands;
import com.nomosiki.duel.DuelService;
import com.nomosiki.match.MatchCommands;
import com.nomosiki.match.MatchService;
import com.nomosiki.compat.emote.server.NomosikSquadEmoteService;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.FactionAdminCommands;
import com.nomosiki.faction.FactionCommands;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.WartimeService;
import com.nomosiki.squad.SquadBrain;
import com.nomosiki.squad.SquadAttackTargetService;
import com.nomosiki.squad.SquadCommands;
import com.nomosiki.squad.SquadService;
import com.nomosiki.squad.NinjaMarkService;
import com.nomosiki.squad.SquadWaypointService;
import com.nomosiki.rts.RtsCommandModeService;
import com.nomosiki.network.ModNetworking;
import com.nomosiki.network.squad.SquadClanNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.nomosiki.registry.ModBlocks;
import com.nomosiki.registry.ModEntities;
import com.nomosiki.registry.ModItemGroups;
import com.nomosiki.registry.ModItems;
import com.nomosiki.registry.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NomosikiMod implements ModInitializer {
	public static final String MOD_ID = "zkwars";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		NomosikiServerConfig.INSTANCE.loadAll();
		NomosikiAdvancements.register();
		ModEntities.register();
		ModBlocks.register();
		ModItems.register();
		ModItemGroups.register();
		ModScreenHandlers.register();
		ModNetworking.register();

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer victim)) {
				return true;
			}
			if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == victim) {
				return true;
			}
			DuelService duels = DuelService.get(victim.server);
			if (duels.mayDamage(attacker, victim)) {
				return true;
			}
			duels.notifyBlocked(attacker, victim);
			return false;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return;
			}
			if (!(player.level() instanceof ServerLevel serverLevel)) {
				return;
			}
			if (!(source.getEntity() instanceof LivingEntity attacker)) {
				return;
			}
			NomosikEntity.alertNomosikiOfOwnerDamage(serverLevel, player, attacker);
		});
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (world.getServer() == null) {
				return true;
			}
			return !MatchService.get(world.getServer()).isProtectedShell(pos);
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			FactionCommands.register(dispatcher);
			FactionAdminCommands.register(dispatcher);
			SquadCommands.register(dispatcher);
			DuelCommands.register(dispatcher);
			MatchCommands.register(dispatcher);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel serverLevel : server.getAllLevels()) {
				SquadBrain.tick(serverLevel);
				SquadAttackTargetService.tick(serverLevel);
				SquadWaypointService.tick(serverLevel);
			}
			NinjaMarkService.tick(server);
			RtsCommandModeService.tick(server);
			NomosikSquadEmoteService.serverTick(server);
			WartimeService.get(server).tick();
			DuelService.get(server).tick();
			MatchService.get(server).tick();
		});
		EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) ->
			NomosikSquadEmoteService.resendIfPlaying(trackedEntity, player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			NomosikSquadEmoteService.clearAll();
			DuelService.forget(server);
			MatchService.forget(server);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer player) {
				DuelService.get(player.server).onDeath(player);
			}
			if (entity instanceof LivingEntity living) {
				NomosikiAdvancements.onKill(living, damageSource);
			}
			if (entity instanceof NomosikEntity w && entity.level().getServer() != null) {
				SquadService.get(entity.level().getServer()).onNomosikRemoved(w);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			WartimeService.get(server).onJoin(handler.player);
			NomosikiAdvancements.onJoin(handler.player);
			if (handler.player.hasDisconnected()) {
				return;
			}
			var manager = FactionService.get(server);
			manager.sendRenderSyncTo(handler.player);
			manager.sendScreenData(handler.player);
			manager.broadcastTabName(handler.player);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			DuelService.get(server).onDisconnect(handler.player);
			MatchService.get(server).onDisconnect(handler.player);
			RtsCommandModeService.onDisconnect(handler.player);
			NinjaMarkService.onDisconnect(handler.player.getUUID());
			SquadClanNetworking.clearWatch(handler.player.getUUID());
		});
		LOGGER.info("Nomosiki mod loaded.");
	}
}
