package com.nomosiki.client;

import com.nomosiki.faction.FactionNameColors;
import com.nomosiki.client.rts.ClanArmyClientState;
import com.nomosiki.client.rts.TacticalCameraController;
import com.nomosiki.client.rts.TacticalHudScreen;
import com.nomosiki.client.rts.OrderMarkerRenderer;
import com.nomosiki.client.rts.RtsClientState;
import com.nomosiki.client.rts.RtsOrderInputHandler;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.client.gui.ZkGuiTheme;
import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.network.faction.payload.c2s.FactionOpenScreenPayload;
import com.nomosiki.network.faction.payload.s2c.FactionRenderSyncPayload;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import com.nomosiki.network.squad.payload.s2c.ClanArmyRosterPayload;
import com.nomosiki.network.squad.payload.s2c.ClanArmyStatePayload;
import com.nomosiki.network.squad.payload.s2c.NinjaMarkedPlayersPayload;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.network.squad.payload.s2c.SquadOrderAckPayload;
import com.nomosiki.network.squad.payload.s2c.SquadStatePayload;
import com.nomosiki.network.unit.payload.c2s.NomosikOpenMenuPayload;
import com.nomosiki.registry.ModEntities;
import com.nomosiki.registry.ModItems;
import com.nomosiki.registry.ModScreenHandlers;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.nomosiki.squad.SquadType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class NomosikiModClient implements ClientModInitializer {
	private static final KeyMapping OPEN_NOMOSIK_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.open_nomosik_gui",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_R,
		"category.nomosiki.main"
	));
	private static final KeyMapping OPEN_FACTION_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.open_faction_gui",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_O,
		"category.nomosiki.main"
	));

	private static final KeyMapping OPEN_SQUAD_LIST = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.open_squad_list",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_J,
		"category.nomosiki.main"
	));

	private static final KeyMapping OPEN_DUEL_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.open_duel_gui",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_K,
		"category.nomosiki.main"
	));

	private static final KeyMapping RTS_TOGGLE_HUD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.toggle_hud",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_U,
		"category.nomosiki.rts"
	));

	private static final KeyMapping RTS_CYCLE_FORMATION = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.cycle_formation",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_UNKNOWN,
		"category.nomosiki.rts"
	));

	private static final KeyMapping RTS_HOLD_POSITION = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.hold_position",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_F,
		"category.nomosiki.rts"
	));

	private static final KeyMapping RTS_TOGGLE_FIRE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.toggle_fire",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_G,
		"category.nomosiki.rts"
	));

	private static final KeyMapping RTS_CYCLE_PRESET = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.cycle_preset",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_B,
		"category.nomosiki.rts"
	));

	private static final KeyMapping RTS_TACTICAL_CAMERA = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.tactical_camera",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_T,
		"category.nomosiki.rts"
	));

	private static final KeyMapping RTS_CYCLE_CLASS_FILTER = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.cycle_class_filter",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_Y,
		"category.nomosiki.rts"
	));

	/** Shift formation anchor forward along the squad axis (Recruits forward). */
	private static final KeyMapping RTS_FORMATION_FORWARD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.formation_forward",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_RIGHT_BRACKET,
		"category.nomosiki.rts"
	));

	/** Shift formation anchor backward along the squad axis (Recruits backward). */
	private static final KeyMapping RTS_FORMATION_BACKWARD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		"key.nomosiki.rts.formation_backward",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_LEFT_BRACKET,
		"category.nomosiki.rts"
	));

	private static final boolean[] KEY_WAS_DOWN = new boolean[512];

	@Override
	public void onInitializeClient() {
		FactionNameColors.setClientLookup(FactionClientState::nameRgb);
		NomosikiConfig.INSTANCE.load();
		NomosikiConfig.INSTANCE.saveIfAbsent();
		ZkGuiTheme.bind(SakuraStyle::apply);
		RtsClientState.initFromConfig(NomosikiConfig.INSTANCE.rtsShowBattleHud);
		com.nomosiki.client.compat.emote.NomosikEmoteClientCompat.init();
		DuelClient.register();

		MenuScreens.register(ModScreenHandlers.NOMOSIK, NomosikScreen::new);
		MenuScreens.register(ModScreenHandlers.HIRE, HireScreen::new);
		MenuScreens.register(ModScreenHandlers.FACTION_CREATE, FactionCreateScreen::new);
		MenuScreens.register(ModScreenHandlers.TESTER_SQUAD_SHOP, TesterSquadShopScreen::new);

		EntityRendererRegistry.register(ModEntities.NOMOSIK, ctx -> new NomosikRenderer<>(ctx, NomosikTextures.Profile.DEFAULT));
		EntityRendererRegistry.register(ModEntities.ARCHER, ctx -> new NomosikRenderer<>(ctx, NomosikTextures.Profile.ARCHER));
		EntityRendererRegistry.register(ModEntities.KNIGHT, ctx -> new NomosikRenderer<>(ctx, NomosikTextures.Profile.KNIGHT));
		EntityRendererRegistry.register(ModEntities.NOMAD, ctx -> new NomosikRenderer<>(ctx, NomosikTextures.Profile.NOMAD));
		EntityRendererRegistry.register(ModEntities.ARTILLERIST, ctx -> new NomosikRenderer<>(ctx, NomosikTextures.Profile.ARTILLERIST));
		EntityRendererRegistry.register(ModEntities.CAPTAIN, ctx -> new NomosikRenderer<>(ctx, NomosikTextures.Profile.CAPTAIN));

		// Custom portrait textures must not use SpawnEggItem shell/spot tinting.
		List<Item> spawnEggs = new ArrayList<>(ModItems.playableUnitSpawnEggs());
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> -1, spawnEggs.toArray(Item[]::new));

		net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
			com.nomosiki.client.rts.FormationGhostRenderer::render);
		net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
			com.nomosiki.client.rts.BombardZoneRenderer::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_NOMOSIK_GUI.consumeClick()) {
				openSquadOrNomosikScreen(client);
			}
			while (OPEN_FACTION_GUI.consumeClick()) {
				ClientPlayNetworking.send(new FactionOpenScreenPayload());
				client.setScreen(new FactionListScreen());
			}
			while (OPEN_SQUAD_LIST.consumeClick()) {
				if (client.player != null) {
					client.setScreen(new SquadListScreen(null));
				}
			}
			while (OPEN_DUEL_GUI.consumeClick()) {
				if (client.player != null && client.screen == null) {
					DuelClient.open(client, null);
				}
			}
			while (RTS_TOGGLE_HUD.consumeClick()) {
				RtsClientState.toggleHud();
			}
			while (RTS_CYCLE_FORMATION.consumeClick()) {
				RtsOrderInputHandler.cycleFormation();
			}
			while (RTS_HOLD_POSITION.consumeClick()) {
				RtsOrderInputHandler.holdAtPlayerFeet();
			}
			while (RTS_TOGGLE_FIRE.consumeClick()) {
				RtsOrderInputHandler.toggleRangedFireMode();
			}
			while (RTS_CYCLE_PRESET.consumeClick()) {
				RtsOrderInputHandler.cycleBattlePreset();
			}
			while (RTS_TACTICAL_CAMERA.consumeClick()) {
				if (client.player != null) {
					boolean enabled = TacticalCameraController.toggle(client);
					client.player.displayClientMessage(
						Component.translatable(enabled
							? "message.nomosiki.rts.tactical_on"
							: "message.nomosiki.rts.tactical_off"),
						true
					);
				}
			}
			while (RTS_CYCLE_CLASS_FILTER.consumeClick()) {
				if (client.player != null) {
					RtsClientState.cycleClassFilter();
					client.player.displayClientMessage(
						Component.translatable("message.nomosiki.rts.class_filter_set", RtsClientState.classFilter().label()),
						true
					);
				}
			}
			while (RTS_FORMATION_FORWARD.consumeClick()) {
				RtsOrderInputHandler.formationStepForward();
			}
			while (RTS_FORMATION_BACKWARD.consumeClick()) {
				RtsOrderInputHandler.formationStepBackward();
			}
			TacticalCameraController.tick(client);
			if (client.player != null
				&& RtsClientState.bombardDesignate()
				&& !TacticalHudScreen.isOpen(client)) {
				client.player.displayClientMessage(
					Component.translatable("overlay.nomosiki.rts.bombard_hint", RtsClientState.bombardRadius()),
					true
				);
			}
			handleAltStanceHotkeys(client);
			handleCtrlSquadHotkeys(client);
			OrderMarkerRenderer.tick(client);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			client.execute(SquadCommandClient::requestArmyState));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			client.execute(() -> {
				TacticalCameraController.deactivate(client);
				RtsClientState.setBombardDesignate(false);
				RtsClientState.clearMarkedPlayers();
				ClanArmyClientState.clear();
			}));

		ClientPlayNetworking.registerGlobalReceiver(FactionRenderSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> FactionClientState.updateRenderSync(payload.colorByPlayer(), payload.stanceByPlayer())));
		ClientPlayNetworking.registerGlobalReceiver(FactionScreenDataPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				FactionClientState.setLastScreenData(payload);
				if (context.client().screen instanceof FactionListScreen) {
					context.client().setScreen(new FactionListScreen());
				} else if (context.client().screen instanceof FactionDiplomacyScreen diplomacy) {
					context.client().setScreen(new FactionDiplomacyScreen(diplomacy.targetFaction()));
				} else if (context.client().screen instanceof FactionScreen) {
					context.client().setScreen(new FactionScreen());
				} else if (context.client().screen instanceof FactionCreateScreen) {
					context.client().setScreen(new FactionListScreen());
				}
			}));
		ClientPlayNetworking.registerGlobalReceiver(SquadStatePayload.TYPE, (payload, context) ->
			context.client().execute(() -> SquadClientState.apply(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ClanArmyRosterPayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClanArmyClientState.applyRoster(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ClanArmyStatePayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClanArmyClientState.applyWatched(payload)));
		ClientPlayNetworking.registerGlobalReceiver(SquadArmyStatePayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				RtsClientState.applyArmyState(payload);
				OrderMarkerRenderer.setRoutePreview(RtsClientState.waypointsForActiveSquad());
				if (context.client().screen instanceof CommandSquadScreen screen) {
					screen.onArmyStateUpdated();
				}
			}));
		ClientPlayNetworking.registerGlobalReceiver(SquadOrderAckPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				RtsClientState.onOrderAck(payload);
				OrderMarkerRenderer.spawnParticles(context.client(), payload.anchor());
			}));
		ClientPlayNetworking.registerGlobalReceiver(NinjaMarkedPlayersPayload.TYPE, (payload, context) ->
			context.client().execute(() -> RtsClientState.applyMarkedPlayers(payload)));
	}

	private static void handleCtrlSquadHotkeys(Minecraft client) {
		if (client.player == null || !Screen.hasControlDown()) {
			return;
		}
		if (client.screen != null && !TacticalHudScreen.isOpen(client)) {
			return;
		}
		List<SquadArmyStatePayload.SquadRow> rows = RtsClientState.armySquads();
		if (rows.isEmpty()) {
			return;
		}
		for (int i = 0; i < 9; i++) {
			if (!keyClick(client, GLFW.GLFW_KEY_1 + i) || i >= rows.size()) {
				continue;
			}
			SquadArmyStatePayload.SquadRow row = rows.get(i);
			RtsClientState.selectExclusive(row.squadId());
			RtsClientState.markRangeAnchor(i);
			SquadCommandClient.setActiveSquad(row.squadId());
			String name = row.name().isEmpty()
				? SquadType.fromId(row.typeId()).name()
				: SquadLabels.text(row.name());
			client.player.displayClientMessage(
				Component.translatable("message.nomosiki.rts.active_squad_set", name),
				true
			);
		}
	}

	private static void handleAltStanceHotkeys(Minecraft client) {
		if (!NomosikiConfig.INSTANCE.rtsEnabled || client.player == null) {
			return;
		}
		if (client.screen != null && !TacticalHudScreen.isOpen(client)) {
			return;
		}
		if (!Screen.hasAltDown()) {
			return;
		}
		if (keyClick(client, GLFW.GLFW_KEY_1)) {
			RtsOrderInputHandler.setCombatMode(NomosikCombatMode.PASSIVE);
		}
		if (keyClick(client, GLFW.GLFW_KEY_2)) {
			RtsOrderInputHandler.setCombatMode(NomosikCombatMode.NEUTRAL);
		}
		if (keyClick(client, GLFW.GLFW_KEY_3)) {
			RtsOrderInputHandler.setCombatMode(NomosikCombatMode.AGGRESSIVE);
		}
		if (keyClick(client, GLFW.GLFW_KEY_4)) {
			RtsOrderInputHandler.setCombatMode(NomosikCombatMode.RAID);
		}
	}

	public static boolean handleTacticalHudKey(Minecraft client, int keyCode, int scanCode) {
		if (RTS_TACTICAL_CAMERA.matches(keyCode, scanCode)) {
			if (client.player != null) {
				boolean enabled = TacticalCameraController.toggle(client);
				client.player.displayClientMessage(
					Component.translatable(enabled
						? "message.nomosiki.rts.tactical_on"
						: "message.nomosiki.rts.tactical_off"),
					true
				);
			}
			return true;
		}
		if (OPEN_NOMOSIK_GUI.matches(keyCode, scanCode)) {
			client.setScreen(new CommandSquadScreen());
			return true;
		}
		if (RTS_TOGGLE_HUD.matches(keyCode, scanCode)) {
			RtsClientState.toggleHud();
			return true;
		}
		if (RTS_CYCLE_FORMATION.matches(keyCode, scanCode)) {
			RtsOrderInputHandler.cycleFormation();
			return true;
		}
		if (RTS_HOLD_POSITION.matches(keyCode, scanCode)) {
			RtsOrderInputHandler.holdAtPlayerFeet();
			return true;
		}
		if (RTS_TOGGLE_FIRE.matches(keyCode, scanCode)) {
			RtsOrderInputHandler.toggleRangedFireMode();
			return true;
		}
		if (RTS_CYCLE_PRESET.matches(keyCode, scanCode)) {
			RtsOrderInputHandler.cycleBattlePreset();
			return true;
		}
		if (RTS_CYCLE_CLASS_FILTER.matches(keyCode, scanCode)) {
			RtsClientState.cycleClassFilter();
			if (client.player != null) {
				client.player.displayClientMessage(
					Component.translatable("message.nomosiki.rts.class_filter_set", RtsClientState.classFilter().label()),
					true
				);
			}
			return true;
		}
		if (RTS_FORMATION_FORWARD.matches(keyCode, scanCode)) {
			RtsOrderInputHandler.formationStepForward();
			return true;
		}
		if (RTS_FORMATION_BACKWARD.matches(keyCode, scanCode)) {
			RtsOrderInputHandler.formationStepBackward();
			return true;
		}
		return false;
	}

	private static boolean keyClick(Minecraft client, int glfwKey) {
		if (glfwKey < 0 || glfwKey >= KEY_WAS_DOWN.length) {
			return false;
		}
		long window = client.getWindow().getWindow();
		boolean down = GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS;
		boolean click = down && !KEY_WAS_DOWN[glfwKey];
		KEY_WAS_DOWN[glfwKey] = down;
		return click;
	}

	private static void openSquadOrNomosikScreen(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		if (TacticalCameraController.isActive()) {
			client.setScreen(new CommandSquadScreen());
			return;
		}
		if (!(client.hitResult instanceof EntityHitResult entityHit)) {
			client.setScreen(new CommandSquadScreen());
			return;
		}
		if (!(entityHit.getEntity() instanceof NomosikEntity nomosik)) {
			client.setScreen(new CommandSquadScreen());
			return;
		}
		if (!client.player.getUUID().equals(nomosik.getOwnerUUID())) {
			client.setScreen(new CommandSquadScreen());
			return;
		}
		ClientPlayNetworking.send(new NomosikOpenMenuPayload(nomosik.getId()));
	}
}
