package com.nomosiki.match;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NomosikRace;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MatchCommands {
	private MatchCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("match")
			.then(Commands.literal("solo")
				.executes(ctx -> solo(ctx.getSource(), NomosikRace.HUMAN))
				.then(Commands.argument("race", StringArgumentType.word())
					.executes(ctx -> solo(ctx.getSource(), parseRace(StringArgumentType.getString(ctx, "race"))))))
			.then(Commands.literal("forfeit")
				.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer()).forfeit(ctx.getSource().getPlayerOrException()))))
			.then(Commands.literal("end")
				.requires(src -> src.hasPermission(2))
				.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer()).forceEnd())))
			.then(Commands.literal("cleanup")
				.requires(src -> src.hasPermission(2))
				.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer()).cleanupOnly(ctx.getSource().getPlayerOrException()))))
			.then(Commands.literal("status")
				.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer()).status(ctx.getSource().getPlayerOrException()))))
			.then(Commands.literal("recruit")
				.then(Commands.argument("role", StringArgumentType.word())
					.executes(ctx -> recruit(ctx.getSource(), StringArgumentType.getString(ctx, "role")))))
			.then(Commands.literal("timer")
				.requires(src -> src.hasPermission(2) && NomosikiServerConfig.INSTANCE.match.debugCommands)
				.then(Commands.literal("set")
					.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 7200))
						.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer())
							.setTimerSeconds(IntegerArgumentType.getInteger(ctx, "seconds")))))))
			.then(Commands.literal("authority")
				.requires(src -> src.hasPermission(2) && NomosikiServerConfig.INSTANCE.match.debugCommands)
				.then(Commands.literal("give")
					.then(Commands.argument("amount", IntegerArgumentType.integer(1, 800))
						.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer())
							.giveAuthority(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount")))))))
			.then(Commands.literal("point")
				.requires(src -> src.hasPermission(2) && NomosikiServerConfig.INSTANCE.match.debugCommands)
				.then(Commands.literal("own")
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer())
							.ownPoint(StringArgumentType.getString(ctx, "id"), ctx.getSource().getPlayerOrException())))))
				.then(Commands.literal("progress")
					.then(Commands.argument("id", StringArgumentType.word())
						.then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
							.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer())
								.setPointProgress(
									StringArgumentType.getString(ctx, "id"),
									FloatArgumentType.getFloat(ctx, "value"))))))))
			.then(Commands.literal("damage")
				.requires(src -> src.hasPermission(2) && NomosikiServerConfig.INSTANCE.match.debugCommands)
				.then(Commands.literal("hq")
					.then(Commands.argument("side", IntegerArgumentType.integer(0, 1))
						.then(Commands.argument("amount", FloatArgumentType.floatArg(1.0f, 2000.0f))
							.executes(ctx -> respond(ctx.getSource(), MatchService.get(ctx.getSource().getServer())
								.damageHq(
									(byte) IntegerArgumentType.getInteger(ctx, "side"),
									FloatArgumentType.getFloat(ctx, "amount")))))))
			)
		);
	}

	private static int solo(CommandSourceStack source, NomosikRace race) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		return respond(source, MatchService.get(source.getServer()).startSolo(player, race));
	}

	private static int recruit(CommandSourceStack source, String roleId) throws CommandSyntaxException {
		MatchRecruitment.Role role = MatchRecruitment.Role.fromId(roleId);
		if (role == null) {
			source.sendFailure(Component.translatable("message.nomosiki.match.unknown_role", roleId));
			return 0;
		}
		return respond(source, MatchService.get(source.getServer()).recruit(source.getPlayerOrException(), role));
	}

	private static NomosikRace parseRace(String raw) {
		return switch (raw.toLowerCase()) {
			case "orc", "orcs" -> NomosikRace.ORC;
			case "dwarf", "dwarves", "gnom", "гном" -> NomosikRace.DWARF;
			default -> NomosikRace.HUMAN;
		};
	}

	private static int respond(CommandSourceStack source, Component message) {
		source.sendSuccess(() -> message, false);
		return 1;
	}
}
