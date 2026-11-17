package com.nomosiki.faction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class FactionCommands {
	private FactionCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		var factionArg = Commands.argument("faction", StringArgumentType.greedyString())
			.suggests((ctx, builder) -> suggestOtherFactions(ctx.getSource(), builder));

		dispatcher.register(Commands.literal("faction")
			.executes(ctx -> {
				var source = ctx.getSource();
				if (source.getPlayer() == null) {
					source.sendFailure(Component.literal("Player only."));
					return 0;
				}
				FactionService.get(source.getServer()).sendScreenData(source.getPlayer());
				source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.command_synced"), false);
				return 1;
			})
			.then(Commands.literal("join")
				.then(Commands.argument("faction", StringArgumentType.greedyString())
					.suggests((ctx, builder) -> suggestPendingInvites(ctx.getSource(), builder))
					.executes(ctx -> executeInviteResponse(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), true))))
			.then(Commands.literal("decline")
				.then(Commands.argument("faction", StringArgumentType.greedyString())
					.suggests((ctx, builder) -> suggestPendingInvites(ctx.getSource(), builder))
					.executes(ctx -> executeInviteResponse(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), false))))
			.then(Commands.literal("leave")
				.executes(ctx -> executeLeave(ctx.getSource())))
			.then(Commands.literal("war")
				.then(factionArg.executes(ctx -> executeWarPeace(ctx.getSource(), StringArgumentType.getString(ctx, "faction"), true))))
			.then(Commands.literal("peace")
				.then(Commands.literal("accept")
					.then(factionArg.executes(ctx -> executePeace(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), PeaceAction.ACCEPT))))
				.then(Commands.literal("decline")
					.then(factionArg.executes(ctx -> executePeace(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), PeaceAction.DECLINE))))
				.then(Commands.literal("cancel")
					.then(factionArg.executes(ctx -> executePeace(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), PeaceAction.CANCEL))))
				.then(factionArg.executes(ctx -> executePeace(
					ctx.getSource(), StringArgumentType.getString(ctx, "faction"), PeaceAction.REQUEST))))
			.then(Commands.literal("wartime")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("start")
					.then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
						.then(Commands.argument("factions", StringArgumentType.greedyString())
							.suggests((ctx, builder) -> suggestWartimeFactions(ctx.getSource(), builder))
							.executes(ctx -> executeWartimeStart(
								ctx.getSource(),
								IntegerArgumentType.getInteger(ctx, "minutes"),
								StringArgumentType.getString(ctx, "factions"))))))
				.then(Commands.literal("stop")
					.executes(ctx -> executeWartimeStop(ctx.getSource())))
				.then(Commands.literal("status")
					.executes(ctx -> executeWartimeStatus(ctx.getSource()))))
			.then(Commands.literal("members")
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("faction", StringArgumentType.greedyString())
					.suggests((ctx, builder) -> suggestAllFactions(ctx.getSource(), builder))
					.executes(ctx -> executeMembers(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction")))))
			.then(Commands.literal("wars")
				.executes(ctx -> {
					var source = ctx.getSource();
					FactionDiplomacySavedData diplomacyData = FactionService.get(source.getServer()).diplomacyData();
					if (diplomacyData.allAtWarPairs().isEmpty()) {
						source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.wars_none"), false);
						return 1;
					}
					for (FactionWarPair pair : diplomacyData.allAtWarPairs()) {
						source.sendSuccess(() -> Component.translatable(
							"message.nomosiki.faction.wars_line",
							pair.factionA(),
							pair.factionB()
						), false);
					}
					return 1;
				}))
			.then(Commands.literal("alliance")
				.then(Commands.literal("request")
					.then(factionArg.executes(ctx -> executeAlliance(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), AllianceAction.REQUEST))))
				.then(Commands.literal("accept")
					.then(factionArg.executes(ctx -> executeAlliance(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), AllianceAction.ACCEPT))))
				.then(Commands.literal("decline")
					.then(factionArg.executes(ctx -> executeAlliance(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), AllianceAction.DECLINE))))
				.then(Commands.literal("cancel")
					.then(factionArg.executes(ctx -> executeAlliance(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), AllianceAction.CANCEL))))
				.then(Commands.literal("break")
					.then(factionArg.executes(ctx -> executeAlliance(
						ctx.getSource(), StringArgumentType.getString(ctx, "faction"), AllianceAction.BREAK))))));
	}

	private static CompletableFuture<Suggestions> suggestWartimeFactions(
		CommandSourceStack source,
		SuggestionsBuilder builder
	) {
		String remaining = builder.getRemaining();
		int tokenStart = 0;
		int comma = remaining.lastIndexOf(',');
		if (comma >= 0) {
			tokenStart = comma + 1;
			while (tokenStart < remaining.length() && remaining.charAt(tokenStart) == ' ') {
				tokenStart++;
			}
		}
		SuggestionsBuilder tail = builder.createOffset(builder.getStart() + tokenStart);
		String token = remaining.substring(tokenStart).toLowerCase(Locale.ROOT);
		List<String> names = FactionService.get(source.getServer()).listOtherFactions(null);
		for (String name : names) {
			if (token.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(token)) {
				tail.suggest(name);
			}
		}
		return tail.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestPendingInvites(
		CommandSourceStack source,
		SuggestionsBuilder builder
	) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (CommandSyntaxException e) {
			return builder.buildFuture();
		}
		return SharedSuggestionProvider.suggest(
			FactionService.get(source.getServer()).pendingInvites(player.getUUID()),
			builder
		);
	}

	private static int executeLeave(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		FactionManager manager = FactionService.get(source.getServer());
		String messageKey = manager.leaveFaction(player);
		if (!messageKey.isEmpty()) {
			source.sendFailure(Component.translatable(messageKey));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("message.nomosiki.faction.left"), false);
		manager.sendScreenData(player);
		return 1;
	}

	private static int executeInviteResponse(CommandSourceStack source, String factionName, boolean accept) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		FactionManager manager = FactionService.get(source.getServer());
		String messageKey = manager.respondInvite(player, factionName, accept);
		if (!messageKey.isEmpty()) {
			source.sendFailure(Component.translatable(messageKey));
			return 0;
		}
		manager.sendScreenData(player);
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestAllFactions(
		CommandSourceStack source,
		SuggestionsBuilder builder
	) {
		return SharedSuggestionProvider.suggest(
			FactionService.get(source.getServer()).listOtherFactions(null),
			builder
		);
	}

	private static int executeMembers(CommandSourceStack source, String factionName) {
		FactionManager manager = FactionService.get(source.getServer());
		var faction = manager.getFaction(factionName);
		if (faction.isEmpty()) {
			source.sendFailure(Component.translatable("message.nomosiki.faction.war_enemy_not_found"));
			return 0;
		}
		var data = faction.get();
		List<UUID> members = new ArrayList<>(data.members());
		members.sort((a, b) -> manager.memberDisplayName(a).compareToIgnoreCase(manager.memberDisplayName(b)));
		String resolvedName = data.name();
		source.sendSuccess(() -> Component.translatable(
			"message.nomosiki.faction.members_header",
			resolvedName,
			members.size()
		), false);
		for (UUID memberId : members) {
			String label = manager.memberDisplayName(memberId);
			boolean owner = memberId.equals(data.owner());
			source.sendSuccess(() -> Component.translatable(
				owner ? "message.nomosiki.faction.members_owner" : "message.nomosiki.faction.members_line",
				label
			), false);
		}
		return members.size();
	}

	private static CompletableFuture<Suggestions> suggestOtherFactions(
		CommandSourceStack source,
		SuggestionsBuilder builder
	) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (CommandSyntaxException e) {
			return builder.buildFuture();
		}
		String myFaction = FactionService.get(source.getServer()).getPlayerFaction(player.getUUID());
		return SharedSuggestionProvider.suggest(
			FactionService.get(source.getServer()).listOtherFactions(myFaction),
			builder
		);
	}

	private enum AllianceAction {
		REQUEST, ACCEPT, DECLINE, CANCEL, BREAK
	}

	private enum PeaceAction {
		REQUEST, ACCEPT, DECLINE, CANCEL
	}

	private static int executeWarPeace(CommandSourceStack source, String enemyFaction, boolean declareWar) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		FactionDiplomacyService diplomacy = FactionService.diplomacy(source.getServer());
		String messageKey = declareWar ? diplomacy.declareWar(player, enemyFaction) : diplomacy.declarePeace(player, enemyFaction);
		if (!messageKey.isEmpty()) {
			source.sendFailure(Component.translatable(messageKey));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable(
			declareWar ? "message.nomosiki.faction.war_success" : "message.nomosiki.faction.peace_success",
			enemyFaction
		), true);
		return 1;
	}

	private static int executeAlliance(CommandSourceStack source, String otherFaction, AllianceAction action) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		FactionDiplomacyService diplomacy = FactionService.diplomacy(source.getServer());
		String messageKey = switch (action) {
			case REQUEST -> diplomacy.requestAlliance(player, otherFaction, FactionAllianceTier.REGULAR);
			case ACCEPT -> diplomacy.acceptAlliance(player, otherFaction);
			case DECLINE -> diplomacy.declineAlliance(player, otherFaction);
			case CANCEL -> diplomacy.cancelAllianceRequest(player, otherFaction);
			case BREAK -> diplomacy.breakAlliance(player, otherFaction);
		};
		if (!messageKey.isEmpty()) {
			source.sendFailure(Component.translatable(messageKey));
			return 0;
		}
		Component success = switch (action) {
			case REQUEST -> Component.translatable("message.nomosiki.faction.alliance_request_success", otherFaction);
			case ACCEPT -> Component.translatable("message.nomosiki.faction.alliance_accept_success", otherFaction);
			case DECLINE -> Component.translatable("message.nomosiki.faction.alliance_decline_success", otherFaction);
			case CANCEL -> Component.translatable("message.nomosiki.faction.alliance_cancel_success", otherFaction);
			case BREAK -> Component.translatable("message.nomosiki.faction.alliance_break_success", otherFaction);
		};
		source.sendSuccess(() -> success, true);
		return 1;
	}

	private static int executePeace(CommandSourceStack source, String otherFaction, PeaceAction action) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		FactionDiplomacyService diplomacy = FactionService.diplomacy(source.getServer());
		String messageKey = switch (action) {
			case REQUEST -> diplomacy.declarePeace(player, otherFaction);
			case ACCEPT -> diplomacy.acceptPeace(player, otherFaction);
			case DECLINE -> diplomacy.declinePeace(player, otherFaction);
			case CANCEL -> diplomacy.cancelPeace(player, otherFaction);
		};
		if (!messageKey.isEmpty()) {
			source.sendFailure(Component.translatable(messageKey));
			return 0;
		}
		String successKey = switch (action) {
			case REQUEST -> "message.nomosiki.faction.peace_offer_success";
			case ACCEPT -> "message.nomosiki.faction.peace_success";
			case DECLINE -> "message.nomosiki.faction.peace_decline_success";
			case CANCEL -> "message.nomosiki.faction.peace_cancel_success";
		};
		source.sendSuccess(() -> Component.translatable(successKey, otherFaction), true);
		return 1;
	}

	private static int executeWartimeStart(CommandSourceStack source, int minutes, String factions) {
		String actor = source.getPlayer() == null ? source.getTextName() : source.getPlayer().getGameProfile().getName();
		Component error = WartimeService.get(source.getServer()).start(minutes, factions, actor);
		if (error != null) {
			source.sendFailure(error);
			return 0;
		}
		return 1;
	}

	private static int executeWartimeStop(CommandSourceStack source) {
		String actor = source.getPlayer() == null ? source.getTextName() : source.getPlayer().getGameProfile().getName();
		Component error = WartimeService.get(source.getServer()).stop(actor);
		if (error != null) {
			source.sendFailure(error);
			return 0;
		}
		return 1;
	}

	private static int executeWartimeStatus(CommandSourceStack source) {
		WartimeService wartime = WartimeService.get(source.getServer());
		if (!wartime.isActive()) {
			source.sendSuccess(() -> Component.translatable("message.nomosiki.wartime.not_active"), false);
			return 1;
		}
		String names = String.join(", ", wartime.namedFactions());
		source.sendSuccess(() -> Component.translatable(
			"message.nomosiki.wartime.status",
			WartimeService.formatTime(wartime.endsAtMs()),
			names
		), false);
		return 1;
	}
}
