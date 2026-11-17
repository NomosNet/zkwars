package com.nomosiki.faction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Faction RGB for {@link Player#getDisplayName()}. The dedicated server reads membership directly.
 * The client supplies colors from the render sync via {@link #setClientLookup}.
 */
public final class FactionNameColors {
	@FunctionalInterface
	public interface ClientLookup {
		@Nullable Integer rgb(UUID playerId);
	}

	private static volatile ClientLookup clientLookup = ignored -> null;

	private FactionNameColors() {
	}

	public static void setClientLookup(ClientLookup lookup) {
		clientLookup = lookup == null ? ignored -> null : lookup;
	}

	@Nullable
	public static Integer rgb(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			FactionColor color = FactionService.get(serverPlayer.server).getFactionColorByOwner(serverPlayer.getUUID());
			return color == null ? null : color.nameRgb();
		}
		if (player.level().isClientSide()) {
			return clientLookup.rgb(player.getUUID());
		}
		return null;
	}
}
