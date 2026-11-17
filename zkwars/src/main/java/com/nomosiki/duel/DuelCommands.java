package com.nomosiki.duel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DuelCommands {
	private DuelCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("duel")
			.then(Commands.literal("accept")
				.executes(ctx -> respond(ctx.getSource(), DuelService.get(ctx.getSource().getServer()).accept(ctx.getSource().getPlayerOrException()))))
			.then(Commands.literal("deny")
				.executes(ctx -> respond(ctx.getSource(), DuelService.get(ctx.getSource().getServer()).deny(ctx.getSource().getPlayerOrException()))))
			.then(Commands.literal("cancel")
				.executes(ctx -> respond(ctx.getSource(), DuelService.get(ctx.getSource().getServer()).cancel(ctx.getSource().getPlayerOrException()))))
			.then(Commands.argument("target", EntityArgument.player())
				.executes(ctx -> challenge(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
	}

	private static int challenge(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
		ServerPlayer self = source.getPlayerOrException();
		return respond(source, DuelService.get(source.getServer()).challenge(self, target));
	}

	private static int respond(CommandSourceStack source, Component error) {
		if (error != null) {
			source.sendFailure(error);
			return 0;
		}
		return 1;
	}
}
