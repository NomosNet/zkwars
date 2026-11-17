package com.nomosiki.client;

import com.nomosiki.faction.FactionColor;
import com.nomosiki.faction.FactionRelationStatus;
import com.nomosiki.faction.SquadBannerStance;
import com.nomosiki.network.faction.FactionDiplomacyRow;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FactionClientState {
	private static final Map<UUID, FactionColor> COLOR_BY_PLAYER = new ConcurrentHashMap<>();
	private static final Map<UUID, SquadBannerStance> STANCE_BY_PLAYER = new ConcurrentHashMap<>();
	private static final Map<String, FactionRelationStatus> RELATION_BY_FACTION = new ConcurrentHashMap<>();
	private static volatile FactionScreenDataPayload lastScreenData =
		new FactionScreenDataPayload("", "", ItemStack.EMPTY, false, List.of(), List.of(), List.of(), List.of());
	private static volatile boolean incomingAlliancePending;

	private FactionClientState() {
	}

	public static void updateRenderSync(Map<UUID, String> colorByPlayer, Map<UUID, Byte> stanceByPlayer) {
		COLOR_BY_PLAYER.clear();
		for (Map.Entry<UUID, String> entry : colorByPlayer.entrySet()) {
			COLOR_BY_PLAYER.put(entry.getKey(), FactionColor.fromId(entry.getValue()));
		}
		STANCE_BY_PLAYER.clear();
		if (stanceByPlayer == null) {
			return;
		}
		for (Map.Entry<UUID, Byte> entry : stanceByPlayer.entrySet()) {
			STANCE_BY_PLAYER.put(entry.getKey(), SquadBannerStance.fromId(entry.getValue()));
		}
	}

	public static SquadBannerStance stanceToward(UUID playerId) {
		if (playerId == null) {
			return SquadBannerStance.NEUTRAL;
		}
		return STANCE_BY_PLAYER.getOrDefault(playerId, SquadBannerStance.NEUTRAL);
	}

	@Nullable
	public static Integer nameRgb(UUID playerId) {
		if (playerId == null) {
			return null;
		}
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.nameRgb();
	}

	public static ResourceLocation getTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.texture();
	}

	@Nullable
	public static ResourceLocation getAshigaruTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.ashigaruTexture();
	}

	@Nullable
	public static ResourceLocation getSamuraiTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.samuraiTexture();
	}

	@Nullable
	public static ResourceLocation getBritishTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.britishTexture();
	}

	@Nullable
	public static ResourceLocation getSpanishTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.spanishTexture();
	}

	@Nullable
	public static ResourceLocation getNinjaTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.ninjaTexture();
	}

	@Nullable
	public static ResourceLocation getMonkTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.monkTexture();
	}

	@Nullable
	public static ResourceLocation getCaptainTextureForPlayer(UUID playerId) {
		FactionColor color = COLOR_BY_PLAYER.get(playerId);
		return color == null ? null : color.captainTexture();
	}

	public static void setLastScreenData(FactionScreenDataPayload payload) {
		lastScreenData = payload;
		RELATION_BY_FACTION.clear();
		boolean pending = false;
		for (FactionDiplomacyRow row : payload.diplomacy()) {
			RELATION_BY_FACTION.put(row.factionName().toLowerCase(Locale.ROOT), row.relationStatus());
			if (row.alliancePendingIncoming()) {
				pending = true;
			}
		}
		incomingAlliancePending = pending;
	}

	public static FactionScreenDataPayload lastScreenData() {
		return lastScreenData;
	}

	public static Optional<FactionDiplomacyRow> diplomacyRowFor(String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return Optional.empty();
		}
		for (FactionDiplomacyRow row : lastScreenData.diplomacy()) {
			if (row.factionName().equalsIgnoreCase(factionName)) {
				return Optional.of(row);
			}
		}
		return Optional.empty();
	}

	@Nullable
	public static FactionRelationStatus relationFor(String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return null;
		}
		return RELATION_BY_FACTION.get(factionName.toLowerCase(Locale.ROOT));
	}

	public static boolean hasActiveFactionWar() {
		return lastScreenData.diplomacy().stream()
			.anyMatch(row -> row.relationStatus() == FactionRelationStatus.AT_WAR);
	}

	public static boolean hasIncomingAllianceRequests() {
		return incomingAlliancePending;
	}

	public static boolean hasAlliedFactions() {
		return lastScreenData.diplomacy().stream()
			.anyMatch(row -> row.relationStatus().isAlliance());
	}
}
