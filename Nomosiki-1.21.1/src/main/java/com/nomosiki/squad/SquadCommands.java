package com.nomosiki.squad;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.FactionColor;
import com.nomosiki.faction.FactionService;
import com.nomosiki.faction.WartimeService;
import com.nomosiki.network.squad.SquadNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class SquadCommands {
	private static final List<String> TYPE_SUGGESTIONS = List.of(
		"infantry", "archer", "cavalry",
		"samurai", "samurai_archer", "samurai_cavalry", "samurai_archer_cavalry", "nomad",
		"british", "spanish", "ninja", "monk", "monk_archer", "monk_cavalry",
		"ashigaru_musketeer", "samurai_musketeer", "british_musketeer", "captain"
	);
	private static final List<String> GEAR_TYPE_SUGGESTIONS = List.of(
		"samurai", "samurai_archer", "samurai_cavalry", "samurai_archer_cavalry", "samurai_musketeer"
	);

	private SquadCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nomosik")
			.then(Commands.literal("squad")
				.then(Commands.literal("create")
					.then(Commands.argument("type", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TYPE_SUGGESTIONS, builder))
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							SquadType type = parseType(StringArgumentType.getString(ctx, "type"));
							if (type == null) {
								ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_type"));
								return 0;
							}
							return finishCreate(ctx, player, type, null);
						})
						.then(Commands.argument("name", StringArgumentType.greedyString())
							.executes(ctx -> {
								ServerPlayer player = ctx.getSource().getPlayerOrException();
								SquadType type = parseType(StringArgumentType.getString(ctx, "type"));
								if (type == null) {
									ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_type"));
									return 0;
								}
								String name = StringArgumentType.getString(ctx, "name");
								return finishCreate(ctx, player, type, name);
							}))))
				.then(Commands.literal("list").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					List<NomosikSquad> squads = SquadService.get(player.server).listCommanderSquads(player.getUUID());
					if (squads.isEmpty()) {
						ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.list_empty"), false);
						return 1;
					}
					for (int i = 0; i < squads.size(); i++) {
						NomosikSquad s = squads.get(i);
						int idx = i + 1;
						String dispName = displaySquadName(s);
						ctx.getSource().sendSuccess(() -> Component.translatable(
							"message.nomosiki.squad.list_line",
							idx,
							dispName,
							(int) s.iconId(),
							Component.translatable(s.type().translationKey()),
							s.memberCount(),
							s.id().toString().substring(0, 8)
						), false);
					}
					return 1;
				}))
				.then(Commands.literal("rename")
					.then(Commands.argument("index", IntegerArgumentType.integer(1))
						.then(Commands.argument("newName", StringArgumentType.greedyString())
							.executes(ctx -> {
								ServerPlayer player = ctx.getSource().getPlayerOrException();
								int index = IntegerArgumentType.getInteger(ctx, "index");
								String newName = StringArgumentType.getString(ctx, "newName");
								SquadManager mgr = SquadService.get(player.server);
								NomosikSquad squad = squadAtIndex(player, mgr, index);
								if (squad == null) {
									ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_index"));
									return 0;
								}
								Optional<String> err = mgr.tryEditSquad(player, squad.id(), newName, squad.iconId());
								if (err.isPresent()) {
									ctx.getSource().sendFailure(Component.translatable(err.get()));
									return 0;
								}
								SquadNetworking.sendSquadState(player, mgr);
								ctx.getSource().sendSuccess(() -> Component.translatable(
									"message.nomosiki.squad.renamed", index, newName.trim()), false);
								return 1;
							}))))
				.then(Commands.literal("icon")
					.then(Commands.argument("index", IntegerArgumentType.integer(1))
						.then(Commands.argument("iconId", IntegerArgumentType.integer(0, SquadManager.SQUAD_ICON_COUNT - 1))
							.executes(ctx -> {
								ServerPlayer player = ctx.getSource().getPlayerOrException();
								int index = IntegerArgumentType.getInteger(ctx, "index");
								int icon = IntegerArgumentType.getInteger(ctx, "iconId");
								SquadManager mgr = SquadService.get(player.server);
								NomosikSquad squad = squadAtIndex(player, mgr, index);
								if (squad == null) {
									ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_index"));
									return 0;
								}
								Optional<String> err = mgr.tryEditSquad(player, squad.id(), "", (byte) icon);
								if (err.isPresent()) {
									ctx.getSource().sendFailure(Component.translatable(err.get()));
									return 0;
								}
								SquadNetworking.sendSquadState(player, mgr);
								ctx.getSource().sendSuccess(() -> Component.translatable(
									"message.nomosiki.squad.icon_changed", index, icon), false);
								return 1;
							}))))
				.then(Commands.literal("disband")
					.then(Commands.argument("index", IntegerArgumentType.integer(1))
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							int index = IntegerArgumentType.getInteger(ctx, "index");
							SquadManager mgr = SquadService.get(player.server);
							List<NomosikSquad> squads = mgr.listCommanderSquads(player.getUUID());
							if (index > squads.size()) {
								ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_index"));
								return 0;
							}
							UUID sid = squads.get(index - 1).id();
							if (!mgr.disbandSquad(player, sid)) {
								ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.disband_failed"));
								return 0;
							}
							SquadNetworking.sendSquadState(player, mgr);
							ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.disbanded"), false);
							return 1;
						})))
				.then(Commands.literal("add")
					.then(Commands.argument("index", IntegerArgumentType.integer(1))
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							int index = IntegerArgumentType.getInteger(ctx, "index");
							SquadManager mgr = SquadService.get(player.server);
							List<NomosikSquad> squads = mgr.listCommanderSquads(player.getUUID());
							if (index > squads.size()) {
								ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_index"));
								return 0;
							}
							UUID sid = squads.get(index - 1).id();
							NomosikEntity recruit = SquadManager.findOwnedNomosikInLook(player, 8.0);
							if (recruit == null) {
								ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.no_target"));
								return 0;
							}
							String err = mgr.tryAddOrTransferMember(player, sid, recruit);
							if (!err.isEmpty()) {
								ctx.getSource().sendFailure(Component.translatable(err));
								return 0;
							}
							SquadNetworking.sendSquadState(player, mgr);
							ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.added"), false);
							return 1;
						})))
				.then(Commands.literal("add_nearby")
					.then(Commands.argument("index", IntegerArgumentType.integer(1))
						.executes(ctx -> runAddNearby(ctx, IntegerArgumentType.getInteger(ctx, "index"), 24))
						.then(Commands.argument("radius", IntegerArgumentType.integer(4, 64))
							.executes(ctx -> runAddNearby(ctx, IntegerArgumentType.getInteger(ctx, "index"),
								IntegerArgumentType.getInteger(ctx, "radius"))))))
				.then(Commands.literal("active")
					.then(Commands.argument("index", IntegerArgumentType.integer(0))
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							int index = IntegerArgumentType.getInteger(ctx, "index");
							SquadManager mgr = SquadService.get(player.server);
							if (index == 0) {
								mgr.setActiveSquad(player, null);
								SquadNetworking.sendSquadState(player, mgr);
								ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.active_cleared"), false);
								return 1;
							}
							List<NomosikSquad> squads = mgr.listCommanderSquads(player.getUUID());
							if (index > squads.size()) {
								ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_index"));
								return 0;
							}
							mgr.setActiveSquad(player, squads.get(index - 1).id());
							SquadNetworking.sendSquadState(player, mgr);
							ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.active_set", index), false);
							return 1;
						})))
				.then(Commands.literal("leave").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					NomosikEntity recruit = SquadManager.findOwnedNomosikInLook(player, 8.0);
					if (recruit == null) {
						ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.no_target"));
						return 0;
					}
					if (recruit.getSquadId().isEmpty()) {
						ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.not_in_squad"));
						return 0;
					}
					SquadManager mgr = SquadService.get(player.server);
					mgr.removeNomosikFromSquad(recruit);
					SquadNetworking.sendSquadState(player, mgr);
					ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.left_unit"), false);
					return 1;
				}))
				.then(Commands.literal("sync").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					SquadNetworking.sendSquadState(player, SquadService.get(player.server));
					ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.synced"), false);
					return 1;
				}))
				.then(Commands.literal("give")
					.then(Commands.argument("index", IntegerArgumentType.integer(1))
						.then(Commands.argument("player", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
							.executes(ctx -> {
								ServerPlayer player = ctx.getSource().getPlayerOrException();
								return transferOwned(
									ctx.getSource(),
									player.getUUID(),
									IntegerArgumentType.getInteger(ctx, "index"),
									StringArgumentType.getString(ctx, "player"),
									false
								);
							}))))
				.then(Commands.literal("admin")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("list")
						.then(Commands.argument("player", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
							.executes(ctx -> executeAdminList(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
					.then(Commands.literal("give")
						.then(Commands.argument("from", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
							.then(Commands.argument("index", IntegerArgumentType.integer(1))
								.then(Commands.argument("to", StringArgumentType.word())
									.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
									.executes(ctx -> {
										CommandSourceStack source = ctx.getSource();
										UUID from = resolvePlayer(source, StringArgumentType.getString(ctx, "from"));
										if (from == null) {
											return 0;
										}
										return transferOwned(
											source,
											from,
											IntegerArgumentType.getInteger(ctx, "index"),
											StringArgumentType.getString(ctx, "to"),
											true
										);
									})))))
					.then(Commands.literal("giveall")
						.then(Commands.argument("from", StringArgumentType.word())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
							.then(Commands.argument("to", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder))
								.executes(ctx -> executeGiveAll(
									ctx.getSource(),
									StringArgumentType.getString(ctx, "from"),
									StringArgumentType.getString(ctx, "to")))))))
				.then(Commands.literal("spawn_test")
					.requires(source -> source.hasPermission(2))
					.executes(ctx -> runSpawnTest(ctx, "infantry", 10, null))
					.then(Commands.argument("type", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TYPE_SUGGESTIONS, builder))
						.executes(ctx -> runSpawnTest(ctx,
							StringArgumentType.getString(ctx, "type"),
							10,
							null))
						.then(Commands.argument("count", IntegerArgumentType.integer(1, NomosikSquad.MAX_MEMBERS))
							.executes(ctx -> runSpawnTest(ctx,
								StringArgumentType.getString(ctx, "type"),
								IntegerArgumentType.getInteger(ctx, "count"),
								null))
							.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(ctx -> runSpawnTest(ctx,
									StringArgumentType.getString(ctx, "type"),
									IntegerArgumentType.getInteger(ctx, "count"),
									StringArgumentType.getString(ctx, "name")))))))
				.then(Commands.literal("spawn_gear")
					.requires(source -> source.hasPermission(2))
					.then(Commands.argument("type", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(GEAR_TYPE_SUGGESTIONS, builder))
						.executes(ctx -> runSpawnGear(ctx,
							StringArgumentType.getString(ctx, "type"),
							10,
							null))
						.then(Commands.argument("count", IntegerArgumentType.integer(1, NomosikSquad.MAX_MEMBERS))
							.executes(ctx -> runSpawnGear(ctx,
								StringArgumentType.getString(ctx, "type"),
								IntegerArgumentType.getInteger(ctx, "count"),
								null))
							.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(ctx -> runSpawnGear(ctx,
									StringArgumentType.getString(ctx, "type"),
									IntegerArgumentType.getInteger(ctx, "count"),
									StringArgumentType.getString(ctx, "name")))))))
				.then(spawnColorsCommand())
				.then(spawnAllCommand())));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> spawnAllCommand() {
		return Commands.literal("spawn_all")
			.requires(source -> source.hasPermission(2))
			.executes(ctx -> runSpawnAll(ctx, SquadTestSpawn.COLOR_PARADE_DEFAULT_MEMBERS))
			.then(Commands.argument("count", IntegerArgumentType.integer(1, SquadTestSpawn.COLOR_PARADE_MAX_MEMBERS))
				.executes(ctx -> runSpawnAll(ctx, IntegerArgumentType.getInteger(ctx, "count"))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> spawnColorsCommand() {
		return Commands.literal("spawn_colors")
			.requires(source -> source.hasPermission(2))
			.executes(ctx -> runSpawnColors(ctx, "infantry", SquadTestSpawn.COLOR_PARADE_DEFAULT_MEMBERS))
			.then(Commands.argument("count", IntegerArgumentType.integer(1, SquadTestSpawn.COLOR_PARADE_MAX_MEMBERS))
				.executes(ctx -> runSpawnColors(ctx, "infantry", IntegerArgumentType.getInteger(ctx, "count"))))
			.then(Commands.argument("type", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TYPE_SUGGESTIONS, builder))
				.executes(ctx -> runSpawnColors(ctx,
					StringArgumentType.getString(ctx, "type"),
					SquadTestSpawn.COLOR_PARADE_DEFAULT_MEMBERS))
				.then(Commands.argument("count", IntegerArgumentType.integer(1, SquadTestSpawn.COLOR_PARADE_MAX_MEMBERS))
					.executes(ctx -> runSpawnColors(ctx,
						StringArgumentType.getString(ctx, "type"),
						IntegerArgumentType.getInteger(ctx, "count")))));
	}

	private static int runSpawnTest(
		com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
		String typeRaw,
		int count,
		@Nullable String name
	) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		SquadType type = parseType(typeRaw);
		if (type == null || !type.isPlayable()) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_type"));
			return 0;
		}
		int spawned = SquadTestSpawn.spawnTestSquad(player, type, count, name);
		if (spawned < 0) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.spawn_test_failed"));
			return 0;
		}
		String displayName = name == null || name.isBlank()
			? type.name()
			: name.trim();
		int finalSpawned = spawned;
		ctx.getSource().sendSuccess(() -> Component.translatable(
			"message.nomosiki.squad.spawn_test_ok",
			finalSpawned,
			Component.translatable(type.translationKey()),
			displayName
		), true);
		return spawned;
	}

	private static int runSpawnGear(
		com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
		String typeRaw,
		int count,
		@Nullable String name
	) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		SquadType type = parseType(typeRaw);
		if (type == null || !SamuraiGearLoadout.supports(type)) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.spawn_gear_bad_type"));
			return 0;
		}
		if (!SamuraiGearLoadout.isAvailable()) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.spawn_gear_missing_items"));
			return 0;
		}
		int spawned = SquadTestSpawn.spawnGearedSquad(player, type, count, name);
		if (spawned < 0) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.spawn_test_failed"));
			return 0;
		}
		String displayName = name == null || name.isBlank()
			? type.name()
			: name.trim();
		int finalSpawned = spawned;
		ctx.getSource().sendSuccess(() -> Component.translatable(
			"message.nomosiki.squad.spawn_gear_ok",
			finalSpawned,
			Component.translatable(type.translationKey()),
			displayName
		), true);
		return spawned;
	}

	private static int runSpawnColors(
		com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
		String typeRaw,
		int count
	) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		SquadType type = parseType(typeRaw);
		if (type == null || !type.isPlayable()) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_type"));
			return 0;
		}
		int spawned = SquadTestSpawn.spawnColorParade(player, count, type);
		if (spawned <= 0) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.spawn_colors_failed"));
			return 0;
		}
		int squads = FactionColor.ashigaruColors().size();
		ctx.getSource().sendSuccess(() -> Component.translatable(
			"message.nomosiki.squad.spawn_colors_ok",
			squads,
			Component.translatable(type.translationKey()),
			count,
			spawned
		), true);
		return spawned;
	}

	private static int runSpawnAll(
		com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
		int count
	) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		int spawned = SquadTestSpawn.spawnAllTypesParade(player, count);
		if (spawned <= 0) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.spawn_all_failed"));
			return 0;
		}
		int types = SquadType.playableValues().size();
		ctx.getSource().sendSuccess(() -> Component.translatable(
			"message.nomosiki.squad.spawn_all_ok",
			types,
			count,
			spawned
		), true);
		return spawned;
	}

	private static int runAddNearby(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, int index, int radius)
		throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		SquadManager mgr = SquadService.get(player.server);
		List<NomosikSquad> squads = mgr.listCommanderSquads(player.getUUID());
		if (index > squads.size()) {
			ctx.getSource().sendFailure(Component.translatable("message.nomosiki.squad.bad_index"));
			return 0;
		}
		UUID sid = squads.get(index - 1).id();
		mgr.assignNearbyOwnedToSquad(player, sid, radius);
		SquadNetworking.sendSquadState(player, mgr);
		ctx.getSource().sendSuccess(() -> Component.translatable("message.nomosiki.squad.assigned_nearby", index, radius), false);
		return 1;
	}

	private static int finishCreate(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
		ServerPlayer player, SquadType type, @Nullable String rawName) {
		SquadManager mgr = SquadService.get(player.server);
		String nameArg = rawName == null ? "" : rawName;
		Optional<String> err = mgr.createNamedSquad(player, nameArg, (byte) 0, type);
		if (err.isPresent()) {
			ctx.getSource().sendFailure(Component.translatable(err.get()));
			return 0;
		}
		SquadNetworking.sendSquadState(player, mgr);
		Optional<UUID> activeId = mgr.getActiveSquadId(player.getUUID());
		String shortId = activeId.map(id -> id.toString().substring(0, 8)).orElse("?");
		Optional<NomosikSquad> created = activeId.flatMap(mgr.data()::getSquad);
		String squadName = created.map(NomosikSquad::name).filter(n -> !n.isEmpty()).orElse(shortId);
		int icon = created.map(s -> (int) s.iconId()).orElse(0);
		ctx.getSource().sendSuccess(() -> Component.translatable(
			"message.nomosiki.squad.created_named",
			squadName,
			Component.translatable(type.translationKey()),
			icon,
			shortId
		), false);
		return 1;
	}

	private static int transferOwned(CommandSourceStack source, UUID fromId, int index, String toName, boolean admin) {
		if (!admin && WartimeService.get(source.getServer()).isActive()) {
			source.sendFailure(Component.translatable("message.nomosiki.squad.transfer_wartime"));
			return 0;
		}
		UUID toId = FactionService.get(source.getServer()).resolvePlayerId(toName);
		if (toId == null) {
			source.sendFailure(Component.translatable("message.nomosiki.faction.admin.player_not_found"));
			return 0;
		}
		SquadManager mgr = SquadService.get(source.getServer());
		List<NomosikSquad> squads = mgr.listCommanderSquads(fromId);
		if (index < 1 || index > squads.size()) {
			source.sendFailure(Component.translatable("message.nomosiki.squad.bad_index"));
			return 0;
		}
		NomosikSquad squad = squads.get(index - 1);
		if (!admin && source.getPlayer() != null && !source.getPlayer().getUUID().equals(squad.commanderId())) {
			source.sendFailure(Component.translatable("message.nomosiki.squad.not_commander"));
			return 0;
		}
		String error = mgr.transferSquad(squad.id(), toId);
		if (!error.isEmpty()) {
			source.sendFailure(Component.translatable(error));
			return 0;
		}
		announceTransfer(source, squad, fromId, toId);
		return 1;
	}

	private static int executeGiveAll(CommandSourceStack source, String fromName, String toName) {
		UUID fromId = resolvePlayer(source, fromName);
		if (fromId == null) {
			return 0;
		}
		UUID toId = FactionService.get(source.getServer()).resolvePlayerId(toName);
		if (toId == null) {
			source.sendFailure(Component.translatable("message.nomosiki.faction.admin.player_not_found"));
			return 0;
		}
		SquadManager mgr = SquadService.get(source.getServer());
		List<NomosikSquad> squads = new ArrayList<>(mgr.listCommanderSquads(fromId));
		if (squads.isEmpty()) {
			source.sendFailure(Component.translatable("message.nomosiki.squad.list_empty"));
			return 0;
		}
		int moved = 0;
		for (NomosikSquad squad : squads) {
			String error = mgr.transferSquad(squad.id(), toId);
			if (!error.isEmpty()) {
				source.sendFailure(Component.translatable(error));
				return moved;
			}
			moved++;
		}
		int count = moved;
		source.sendSuccess(() -> Component.translatable("message.nomosiki.squad.transfer_all", count, toName), true);
		notifyPlayer(source.getServer(), fromId, Component.translatable("message.nomosiki.squad.transfer_all_lost", count, toName));
		notifyPlayer(source.getServer(), toId, Component.translatable("message.nomosiki.squad.transfer_all_got", count, fromName));
		return count;
	}

	private static int executeAdminList(CommandSourceStack source, String playerName) {
		UUID playerId = resolvePlayer(source, playerName);
		if (playerId == null) {
			return 0;
		}
		SquadManager mgr = SquadService.get(source.getServer());
		List<NomosikSquad> squads = mgr.listCommanderSquads(playerId);
		if (squads.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("message.nomosiki.squad.list_empty"), false);
			return 1;
		}
		for (int i = 0; i < squads.size(); i++) {
			NomosikSquad squad = squads.get(i);
			int idx = i + 1;
			String dispName = displaySquadName(squad);
			source.sendSuccess(() -> Component.translatable(
				"message.nomosiki.squad.list_line",
				idx,
				dispName,
				(int) squad.iconId(),
				Component.translatable(squad.type().translationKey()),
				squad.memberCount(),
				squad.id().toString().substring(0, 8)
			), false);
		}
		return squads.size();
	}

	@Nullable
	private static UUID resolvePlayer(CommandSourceStack source, String name) {
		UUID id = FactionService.get(source.getServer()).resolvePlayerId(name);
		if (id == null) {
			source.sendFailure(Component.translatable("message.nomosiki.faction.admin.player_not_found"));
		}
		return id;
	}

	private static void announceTransfer(CommandSourceStack source, NomosikSquad squad, UUID fromId, UUID toId) {
		String squadName = displaySquadName(squad);
		String toName = FactionService.get(source.getServer()).memberDisplayName(toId);
		String fromName = FactionService.get(source.getServer()).memberDisplayName(fromId);
		source.sendSuccess(() -> Component.translatable("message.nomosiki.squad.transfer_ok", squadName, toName), true);
		notifyPlayer(source.getServer(), fromId, Component.translatable("message.nomosiki.squad.transfer_lost", squadName, toName));
		notifyPlayer(source.getServer(), toId, Component.translatable("message.nomosiki.squad.transfer_got", squadName, fromName));
	}

	private static void notifyPlayer(net.minecraft.server.MinecraftServer server, UUID playerId, Component message) {
		ServerPlayer online = server.getPlayerList().getPlayer(playerId);
		if (online != null) {
			online.displayClientMessage(message, false);
		}
	}

	private static @Nullable NomosikSquad squadAtIndex(ServerPlayer player, SquadManager mgr, int index) {
		List<NomosikSquad> squads = mgr.listCommanderSquads(player.getUUID());
		if (index < 1 || index > squads.size()) {
			return null;
		}
		return squads.get(index - 1);
	}

	private static String displaySquadName(NomosikSquad s) {
		String n = s.name();
		String base = (n == null || n.isEmpty()) ? "—" : n;
		if (s.reservePool()) {
			return "[auto] " + base;
		}
		return base;
	}

	private static @Nullable SquadType parseType(String raw) {
		if (raw == null) {
			return null;
		}
		return switch (raw.toLowerCase(Locale.ROOT)) {
			case "infantry", "inf" -> SquadType.INFANTRY;
			case "samurai", "sam" -> SquadType.SAMURAI;
			case "samurai_archer", "sarcher" -> SquadType.SAMURAI_ARCHER;
			case "samurai_cavalry", "scav", "samurai_knight" -> SquadType.SAMURAI_CAVALRY;
			case "samurai_archer_cavalry", "sarchercav", "mounted_archer" -> SquadType.SAMURAI_ARCHER_CAVALRY;
			case "british", "brit" -> SquadType.BRITISH;
			case "spanish", "span" -> SquadType.SPANISH;
			case "ninja" -> SquadType.NINJA;
			case "monk" -> SquadType.MONK;
			case "monk_archer" -> SquadType.MONK_ARCHER;
			case "monk_cavalry" -> SquadType.MONK_CAVALRY;
			case "artillerist", "artillery", "ashigaru_artillerist" -> SquadType.ARTILLERIST;
			case "spanish_artillerist", "spanart", "gunner" -> SquadType.SPANISH_ARTILLERIST;
			case "ashigaru_musketeer", "musketeer", "musket" -> SquadType.ASHIGARU_MUSKETEER;
			case "samurai_musketeer", "samusket" -> SquadType.SAMURAI_MUSKETEER;
			case "british_musketeer", "britmusket" -> SquadType.BRITISH_MUSKETEER;
			case "captain" -> SquadType.CAPTAIN;
			case "archers", "archer" -> SquadType.ARCHER;
			case "cavalry", "cav", "knight" -> SquadType.KNIGHT;
			case "nomad" -> SquadType.NOMAD;
			default -> null;
		};
	}
}
