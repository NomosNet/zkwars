package com.nomosiki.faction;

import com.nomosiki.compat.items.FactionArmorCompat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class FactionAdminCommands {
	private FactionAdminCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("faction")
			.then(Commands.literal("admin")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("leader")
					.then(factionArg("faction")
						.then(playerArg("player")
							.executes(ctx -> executeLeader(
								ctx.getSource(),
								StringArgumentType.getString(ctx, "faction"),
								StringArgumentType.getString(ctx, "player"))))))
				.then(Commands.literal("move")
					.then(playerArg("player")
						.then(greedyFaction("faction")
							.executes(ctx -> executeMove(
								ctx.getSource(),
								StringArgumentType.getString(ctx, "player"),
								StringArgumentType.getString(ctx, "faction"))))))
				.then(Commands.literal("add")
					.then(playerArg("player")
						.then(greedyFaction("faction")
							.executes(ctx -> executeAdd(
								ctx.getSource(),
								StringArgumentType.getString(ctx, "player"),
								StringArgumentType.getString(ctx, "faction"))))))
				.then(Commands.literal("kick")
					.then(playerArg("player")
						.executes(ctx -> executeKick(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
				.then(Commands.literal("disband")
					.then(greedyFaction("faction")
						.executes(ctx -> executeDisband(ctx.getSource(), StringArgumentType.getString(ctx, "faction")))))
				.then(Commands.literal("recolor")
					.executes(ctx -> executeRecolor(ctx.getSource())))
				.then(Commands.literal("list")
					.executes(ctx -> executeList(ctx.getSource())))
				.then(Commands.literal("info")
					.then(playerArg("player")
						.executes(ctx -> executeInfo(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
				.then(Commands.literal("relations")
					.then(greedyFaction("faction")
						.executes(ctx -> executeRelations(ctx.getSource(), StringArgumentType.getString(ctx, "faction")))))
				.then(Commands.literal("pending")
					.then(greedyFaction("faction")
						.executes(ctx -> executePending(ctx.getSource(), StringArgumentType.getString(ctx, "faction")))))
				.then(Commands.literal("accept")
					.then(Commands.literal("peace")
						.then(factionArg("faction")
							.then(factionArg("other")
								.executes(ctx -> executeAcceptPeace(ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "other"))))))
					.then(Commands.literal("alliance")
						.then(factionArg("faction")
							.then(factionArg("other")
								.executes(ctx -> executeAcceptAlliance(ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "other"))))))
					.then(Commands.literal("warjoin")
						.then(factionArg("faction")
							.then(factionArg("aggressor")
								.executes(ctx -> executeAcceptWarJoin(ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "aggressor")))))))
				.then(Commands.literal("decline")
					.then(Commands.literal("peace")
						.then(factionArg("faction")
							.then(factionArg("other")
								.executes(ctx -> executeDeclinePeace(ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "other"))))))
					.then(Commands.literal("alliance")
						.then(factionArg("faction")
							.then(factionArg("other")
								.executes(ctx -> executeDeclineAlliance(ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "other"))))))
					.then(Commands.literal("warjoin")
						.then(factionArg("faction")
							.then(factionArg("aggressor")
								.executes(ctx -> executeDeclineWarJoin(ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "aggressor")))))))
				.then(Commands.literal("side")
					.then(factionArg("faction")
						.then(factionArg("aggressor")
							.then(Commands.argument("choice", StringArgumentType.word())
								.suggests((ctx, builder) -> suggestChoices(builder))
								.executes(ctx -> executeSide(
									ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "aggressor"),
									StringArgumentType.getString(ctx, "choice")))))))
				.then(Commands.literal("war")
					.then(factionArg("faction")
						.then(factionArg("other")
							.executes(ctx -> executeWar(ctx.getSource(),
								StringArgumentType.getString(ctx, "faction"),
								StringArgumentType.getString(ctx, "other"))))))
				.then(Commands.literal("peace")
					.then(factionArg("faction")
						.then(factionArg("other")
							.executes(ctx -> executePeace(ctx.getSource(),
								StringArgumentType.getString(ctx, "faction"),
								StringArgumentType.getString(ctx, "other"))))))
				.then(Commands.literal("alliance")
					.then(factionArg("faction")
						.then(factionArg("other")
							.executes(ctx -> executeAlliance(ctx.getSource(),
								StringArgumentType.getString(ctx, "faction"),
								StringArgumentType.getString(ctx, "other"),
								FactionAllianceTier.REGULAR))
							.then(Commands.argument("tier", StringArgumentType.word())
								.suggests((ctx, builder) -> suggestTiers(builder))
								.executes(ctx -> executeAlliance(ctx.getSource(),
									StringArgumentType.getString(ctx, "faction"),
									StringArgumentType.getString(ctx, "other"),
									parseTier(StringArgumentType.getString(ctx, "tier"))))))))
				.then(Commands.literal("break")
					.then(factionArg("faction")
						.then(factionArg("other")
							.executes(ctx -> executeBreak(ctx.getSource(),
								StringArgumentType.getString(ctx, "faction"),
								StringArgumentType.getString(ctx, "other"))))))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> factionArg(String name) {
		return Commands.argument(name, StringArgumentType.string())
			.suggests((ctx, builder) -> suggestFactions(ctx.getSource(), builder));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> greedyFaction(String name) {
		return Commands.argument(name, StringArgumentType.greedyString())
			.suggests((ctx, builder) -> suggestFactions(ctx.getSource(), builder));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> playerArg(String name) {
		return Commands.argument(name, StringArgumentType.word())
			.suggests((ctx, builder) -> suggestPlayers(ctx.getSource(), builder));
	}

	private static CompletableFuture<Suggestions> suggestFactions(CommandSourceStack source, SuggestionsBuilder builder) {
		String token = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (String name : FactionService.get(source.getServer()).listOtherFactions(null)) {
			String shown = name.indexOf(' ') >= 0 ? "\"" + name + "\"" : name;
			if (token.isEmpty() || shown.toLowerCase(Locale.ROOT).startsWith(token) || name.toLowerCase(Locale.ROOT).startsWith(token)) {
				builder.suggest(shown);
			}
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestPlayers(CommandSourceStack source, SuggestionsBuilder builder) {
		String token = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (String name : source.getOnlinePlayerNames()) {
			if (token.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(token)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestChoices(SuggestionsBuilder builder) {
		builder.suggest("aggressor");
		builder.suggest("defender");
		builder.suggest("neutral");
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestTiers(SuggestionsBuilder builder) {
		builder.suggest("regular");
		builder.suggest("military");
		return builder.buildFuture();
	}

	private static int executeLeader(CommandSourceStack source, String faction, String player) {
		FactionManager manager = FactionService.get(source.getServer());
		UUID playerId = manager.resolvePlayerId(player);
		if (playerId == null) {
			return fail(source, "message.nomosiki.faction.admin.player_not_found");
		}
		return done(source, manager.adminSetLeader(faction, playerId, actor(source)),
			"message.nomosiki.faction.admin.leader_ok", player, faction);
	}

	private static int executeMove(CommandSourceStack source, String player, String faction) {
		FactionManager manager = FactionService.get(source.getServer());
		UUID playerId = manager.resolvePlayerId(player);
		if (playerId == null) {
			return fail(source, "message.nomosiki.faction.admin.player_not_found");
		}
		return done(source, manager.adminMove(playerId, faction, actor(source)),
			"message.nomosiki.faction.admin.move_ok", player, faction.trim());
	}

	private static int executeAdd(CommandSourceStack source, String player, String faction) {
		FactionManager manager = FactionService.get(source.getServer());
		UUID playerId = manager.resolvePlayerId(player);
		if (playerId == null) {
			return fail(source, "message.nomosiki.faction.admin.player_not_found");
		}
		return done(source, manager.adminAdd(playerId, faction, actor(source)),
			"message.nomosiki.faction.admin.add_ok", player, faction.trim());
	}

	private static int executeKick(CommandSourceStack source, String player) {
		FactionManager manager = FactionService.get(source.getServer());
		UUID playerId = manager.resolvePlayerId(player);
		if (playerId == null) {
			return fail(source, "message.nomosiki.faction.admin.player_not_found");
		}
		return done(source, manager.adminKick(playerId, actor(source)),
			"message.nomosiki.faction.admin.kick_ok", player);
	}

	private static int executeDisband(CommandSourceStack source, String faction) {
		return done(source, FactionService.get(source.getServer()).adminDisband(faction, actor(source)),
			"message.nomosiki.faction.admin.disband_ok", faction.trim());
	}

	private static int executeRecolor(CommandSourceStack source) {
		int count = FactionArmorCompat.recolorLoaded(source.getServer());
		source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.recolor", count), true);
		return Math.max(count, 1);
	}

	private static int executeList(CommandSourceStack source) {
		FactionManager manager = FactionService.get(source.getServer());
		var factions = manager.allFactions();
		if (factions.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.list_empty"), false);
			return 1;
		}
		for (FactionData faction : factions) {
			String owner = manager.memberDisplayName(faction.owner());
			boolean online = manager.isPlayerOnline(faction.owner());
			String name = faction.name();
			int count = faction.members().size();
			source.sendSuccess(() -> Component.translatable(
				"message.nomosiki.faction.admin.list_line",
				name,
				count,
				owner,
				Component.translatable(online
					? "message.nomosiki.faction.admin.online"
					: "message.nomosiki.faction.admin.offline")
			), false);
		}
		return factions.size();
	}

	private static int executeInfo(CommandSourceStack source, String player) {
		FactionManager manager = FactionService.get(source.getServer());
		UUID playerId = manager.resolvePlayerId(player);
		if (playerId == null) {
			return fail(source, "message.nomosiki.faction.admin.player_not_found");
		}
		String factionName = manager.getPlayerFaction(playerId);
		String label = manager.memberDisplayName(playerId);
		boolean online = manager.isPlayerOnline(playerId);
		Component presence = Component.translatable(online
			? "message.nomosiki.faction.admin.online"
			: "message.nomosiki.faction.admin.offline");
		if (factionName == null) {
			source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.info_none", label, presence), false);
			return 1;
		}
		boolean owner = manager.getFaction(factionName).map(faction -> playerId.equals(faction.owner())).orElse(false);
		source.sendSuccess(() -> Component.translatable(
			owner ? "message.nomosiki.faction.admin.info_owner" : "message.nomosiki.faction.admin.info_member",
			label,
			factionName,
			presence
		), false);
		return 1;
	}

	private static int executeRelations(CommandSourceStack source, String rawFaction) {
		FactionManager manager = FactionService.get(source.getServer());
		var faction = manager.getFaction(rawFaction);
		if (faction.isEmpty()) {
			return fail(source, "message.nomosiki.faction.war_enemy_not_found");
		}
		String name = faction.get().name();
		FactionDiplomacySavedData diplomacy = manager.diplomacyData();
		source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.relations_header", name), false);
		boolean any = false;
		for (FactionWarPair pair : diplomacy.allAtWarPairs()) {
			if (!pair.involves(name)) {
				continue;
			}
			any = true;
			String other = pair.factionA().equalsIgnoreCase(name) ? pair.factionB() : pair.factionA();
			source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.relations_war", other), false);
		}
		for (String ally : diplomacy.regularAlliesOf(name)) {
			any = true;
			source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.relations_ally", ally), false);
		}
		for (String ally : diplomacy.militaryAlliesOf(name)) {
			any = true;
			source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.relations_military", ally), false);
		}
		if (!any) {
			source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.relations_none"), false);
		}
		return 1;
	}

	private static int executePending(CommandSourceStack source, String rawFaction) {
		FactionManager manager = FactionService.get(source.getServer());
		var faction = manager.getFaction(rawFaction);
		if (faction.isEmpty()) {
			return fail(source, "message.nomosiki.faction.war_enemy_not_found");
		}
		String name = faction.get().name();
		FactionDiplomacySavedData diplomacy = manager.diplomacyData();
		source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.pending_header", name), false);
		boolean any = false;
		for (FactionDiplomacySavedData.PeaceInvolving offer : diplomacy.peaceInvolving(name)) {
			any = true;
			boolean incoming = !offer.requesterFaction().equalsIgnoreCase(name);
			String other = offer.otherFaction();
			source.sendSuccess(() -> Component.translatable(
				incoming ? "message.nomosiki.faction.admin.pending_peace_in" : "message.nomosiki.faction.admin.pending_peace_out",
				other
			), false);
		}
		for (FactionDiplomacySavedData.AllianceInvolving offer : diplomacy.alliancesPendingInvolving(name)) {
			any = true;
			boolean incoming = !offer.requesterFaction().equalsIgnoreCase(name);
			String other = offer.otherFaction();
			String tier = offer.tier() == FactionAllianceTier.MILITARY ? "military" : "regular";
			source.sendSuccess(() -> Component.translatable(
				incoming ? "message.nomosiki.faction.admin.pending_alliance_in" : "message.nomosiki.faction.admin.pending_alliance_out",
				other,
				tier
			), false);
		}
		for (FactionWarJoinPending pending : diplomacy.warJoinsInvolving(name)) {
			if (!pending.allyFaction().equalsIgnoreCase(name)) {
				continue;
			}
			any = true;
			source.sendSuccess(() -> Component.translatable(
				"message.nomosiki.faction.admin.pending_warjoin",
				pending.aggressorFaction(),
				pending.defenderFaction()
			), false);
		}
		for (FactionWarSideChoicePending pending : diplomacy.sideChoicesInvolving(name)) {
			if (!pending.conflictedFaction().equalsIgnoreCase(name)) {
				continue;
			}
			any = true;
			source.sendSuccess(() -> Component.translatable(
				"message.nomosiki.faction.admin.pending_side",
				pending.aggressorFaction(),
				pending.defenderFaction()
			), false);
		}
		if (!any) {
			source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.admin.pending_none"), false);
		}
		return 1;
	}

	private static int executeAcceptPeace(CommandSourceStack source, String faction, String other) {
		return done(source, FactionService.diplomacy(source.getServer()).adminAcceptPeace(faction, other, actor(source)),
			"message.nomosiki.faction.admin.peace_accept_ok", faction, other);
	}

	private static int executeDeclinePeace(CommandSourceStack source, String faction, String other) {
		return done(source, FactionService.diplomacy(source.getServer()).adminDeclinePeace(faction, other, actor(source)),
			"message.nomosiki.faction.admin.peace_decline_ok", faction, other);
	}

	private static int executeAcceptAlliance(CommandSourceStack source, String faction, String other) {
		return done(source, FactionService.diplomacy(source.getServer()).adminAcceptAlliance(faction, other, actor(source)),
			"message.nomosiki.faction.admin.alliance_accept_ok", faction, other);
	}

	private static int executeDeclineAlliance(CommandSourceStack source, String faction, String other) {
		return done(source, FactionService.diplomacy(source.getServer()).adminDeclineAlliance(faction, other, actor(source)),
			"message.nomosiki.faction.admin.alliance_decline_ok", faction, other);
	}

	private static int executeAcceptWarJoin(CommandSourceStack source, String faction, String aggressor) {
		return done(source, FactionService.diplomacy(source.getServer()).adminAcceptWarJoin(faction, aggressor, actor(source)),
			"message.nomosiki.faction.admin.warjoin_accept_ok", faction, aggressor);
	}

	private static int executeDeclineWarJoin(CommandSourceStack source, String faction, String aggressor) {
		return done(source, FactionService.diplomacy(source.getServer()).adminDeclineWarJoin(faction, aggressor, actor(source)),
			"message.nomosiki.faction.admin.warjoin_decline_ok", faction, aggressor);
	}

	private static int executeSide(CommandSourceStack source, String faction, String aggressor, String choice) {
		Byte mapped = mapChoice(choice);
		if (mapped == null) {
			return fail(source, "message.nomosiki.faction.admin.side_bad");
		}
		return done(source, FactionService.diplomacy(source.getServer()).adminResolveSide(faction, aggressor, mapped, actor(source)),
			"message.nomosiki.faction.admin.side_ok", faction, choice);
	}

	private static int executeWar(CommandSourceStack source, String faction, String other) {
		return done(source, FactionService.diplomacy(source.getServer()).adminDeclareWar(faction, other, actor(source)),
			"message.nomosiki.faction.admin.war_ok", faction, other);
	}

	private static int executePeace(CommandSourceStack source, String faction, String other) {
		return done(source, FactionService.diplomacy(source.getServer()).adminForcePeace(faction, other, actor(source)),
			"message.nomosiki.faction.admin.peace_ok", faction, other);
	}

	private static int executeAlliance(CommandSourceStack source, String faction, String other, @Nullable FactionAllianceTier tier) {
		if (tier == null) {
			return fail(source, "message.nomosiki.faction.admin.tier_bad");
		}
		return done(source, FactionService.diplomacy(source.getServer()).adminForceAlliance(faction, other, tier, actor(source)),
			"message.nomosiki.faction.admin.alliance_ok", faction, other);
	}

	private static int executeBreak(CommandSourceStack source, String faction, String other) {
		return done(source, FactionService.diplomacy(source.getServer()).adminBreakAlliance(faction, other, actor(source)),
			"message.nomosiki.faction.admin.break_ok", faction, other);
	}

	@Nullable
	private static FactionAllianceTier parseTier(String raw) {
		if (raw == null) {
			return null;
		}
		return switch (raw.toLowerCase(Locale.ROOT)) {
			case "regular", "ally" -> FactionAllianceTier.REGULAR;
			case "military" -> FactionAllianceTier.MILITARY;
			default -> null;
		};
	}

	@Nullable
	private static Byte mapChoice(String raw) {
		if (raw == null) {
			return null;
		}
		return switch (raw.toLowerCase(Locale.ROOT)) {
			case "aggressor" -> FactionWarSideChoice.SUPPORT_AGGRESSOR;
			case "defender" -> FactionWarSideChoice.SUPPORT_DEFENDER;
			case "neutral" -> FactionWarSideChoice.STAY_NEUTRAL;
			default -> null;
		};
	}

	private static String actor(CommandSourceStack source) {
		return source.getPlayer() == null ? source.getTextName() : source.getPlayer().getGameProfile().getName();
	}

	private static int done(CommandSourceStack source, String errorKey, String successKey, Object... args) {
		if (errorKey != null && !errorKey.isEmpty()) {
			return fail(source, errorKey);
		}
		source.sendSuccess(() -> Component.translatable(successKey, args), true);
		return 1;
	}

	private static int fail(CommandSourceStack source, String key) {
		source.sendFailure(Component.translatable(key));
		return 0;
	}
}
