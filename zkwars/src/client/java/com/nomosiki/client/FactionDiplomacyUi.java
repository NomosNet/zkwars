package com.nomosiki.client;

import com.nomosiki.NomosikiMod;
import com.nomosiki.faction.FactionAllianceTier;
import com.nomosiki.faction.FactionRelationStatus;
import com.nomosiki.network.faction.FactionDiplomacyRow;
import com.nomosiki.network.faction.FactionRelationEdge;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FactionDiplomacyUi {
	public static final ResourceLocation GUI_TEXTURE = NomosikiMod.id("textures/gui/diplomacy/diplomacy_gui.png");
	/** Full texture atlas size (padding on right/bottom is empty). */
	public static final int TEX_SIZE = 256;
	public static final int PANEL_WIDTH = 420;
	public static final int PANEL_HEIGHT = 236;
	public static final int PAD = 6;
	public static final int COL_WIDTH = 116;
	public static final int CENTER_WIDTH = 172;
	public static final int CENTER_X = PAD + COL_WIDTH + 2;
	public static final int RIGHT_X = CENTER_X + CENTER_WIDTH + 2;
	public static final int FLAG_MAX = 4;
	public static final int FLAG_SLOT = 16;
	public static final float FLAG_SCALE = 0.85F;
	public static final float CREST_SCALE = 2.5F;

	private static final ResourceLocation ICON_ALLY = NomosikiMod.id("textures/gui/diplomacy/ally.png");
	private static final ResourceLocation ICON_WAR_ALLY = NomosikiMod.id("textures/gui/diplomacy/war_ally.png");
	private static final ResourceLocation ICON_NEUTRAL = NomosikiMod.id("textures/gui/diplomacy/neutral.png");
	private static final ResourceLocation ICON_WAR = NomosikiMod.id("textures/gui/diplomacy/war.png");

	public record FlagLook(String name, ItemStack banner, String colorId) {
	}

	private FactionDiplomacyUi() {
	}

	public static ResourceLocation statusIcon(FactionDiplomacyRow row) {
		if (row.warSideChoicePendingIncoming() || row.warJoinPendingIncoming()) {
			return ICON_WAR;
		}
		if (row.alliancePendingIncoming() || row.alliancePendingOutgoing()) {
			return row.alliancePendingTier() == FactionAllianceTier.MILITARY ? ICON_WAR_ALLY : ICON_ALLY;
		}
		return statusIcon(row.relationStatus());
	}

	public static ResourceLocation statusIcon(FactionRelationStatus status) {
		return switch (status) {
			case ALLY -> ICON_ALLY;
			case MILITARY_ALLY -> ICON_WAR_ALLY;
			case AT_WAR -> ICON_WAR;
			case NEUTRAL -> ICON_NEUTRAL;
		};
	}

	public static Component diplomacyLine(FactionDiplomacyRow row) {
		if (row.warSideChoicePendingIncoming()) {
			return Component.translatable("screen.nomosiki.faction.diplomacy_war_side_choice",
				row.factionName(), row.warSideDefenderName());
		}
		if (row.warJoinPendingIncoming()) {
			return Component.translatable("screen.nomosiki.faction.diplomacy_war_join", row.factionName());
		}
		if (row.alliancePendingIncoming()) {
			String key = row.alliancePendingTier() == FactionAllianceTier.MILITARY
				? "screen.nomosiki.faction.diplomacy_pending_incoming_military"
				: "screen.nomosiki.faction.diplomacy_pending_incoming";
			return Component.translatable(key, row.factionName());
		}
		if (row.alliancePendingOutgoing()) {
			String key = row.alliancePendingTier() == FactionAllianceTier.MILITARY
				? "screen.nomosiki.faction.diplomacy_pending_outgoing_military"
				: "screen.nomosiki.faction.diplomacy_pending_outgoing";
			return Component.translatable(key, row.factionName());
		}
		if (row.peacePendingIncoming()) {
			return Component.translatable("screen.nomosiki.faction.diplomacy_peace_incoming", row.factionName());
		}
		if (row.peacePendingOutgoing()) {
			return Component.translatable("screen.nomosiki.faction.diplomacy_peace_outgoing", row.factionName());
		}
		String key = switch (row.relationStatus()) {
			case ALLY -> "screen.nomosiki.faction.diplomacy_ally";
			case MILITARY_ALLY -> "screen.nomosiki.faction.diplomacy_military_ally";
			case AT_WAR -> "screen.nomosiki.faction.diplomacy_at_war";
			case NEUTRAL -> "screen.nomosiki.faction.diplomacy_neutral";
		};
		return Component.translatable(key, row.factionName());
	}

	public static int diplomacyColor(FactionDiplomacyRow row) {
		if (row.warSideChoicePendingIncoming() || row.warJoinPendingIncoming()) {
			return 0xFFE07060;
		}
		if (row.alliancePendingIncoming() || row.alliancePendingOutgoing()) {
			return 0xFFD4A574;
		}
		return switch (row.relationStatus()) {
			case ALLY -> 0xFFB06080;
			case MILITARY_ALLY -> 0xFFC45C7A;
			case AT_WAR -> 0xFF8B2040;
			case NEUTRAL -> 0xFF8A5A68;
		};
	}

	public static Component listStatusLine(FactionDiplomacyRow row) {
		if (row.warSideChoicePendingIncoming() || row.warJoinPendingIncoming()
			|| row.alliancePendingIncoming() || row.alliancePendingOutgoing()
			|| row.peacePendingIncoming() || row.peacePendingOutgoing()) {
			return diplomacyLine(row);
		}
		String key = switch (row.relationStatus()) {
			case ALLY -> "screen.nomosiki.faction.list_status_ally";
			case MILITARY_ALLY -> "screen.nomosiki.faction.list_status_military";
			case AT_WAR -> "screen.nomosiki.faction.list_status_war";
			case NEUTRAL -> "screen.nomosiki.faction.list_status_neutral";
		};
		return Component.translatable(key);
	}

	public static Component rowLabel(FactionRelationStatus status) {
		String key = switch (status) {
			case ALLY -> "screen.nomosiki.faction.diplomacy_row_ally";
			case MILITARY_ALLY -> "screen.nomosiki.faction.diplomacy_row_military";
			case AT_WAR -> "screen.nomosiki.faction.diplomacy_row_war";
			case NEUTRAL -> "screen.nomosiki.faction.list_status_neutral";
		};
		return Component.translatable(key);
	}

	public static Optional<FlagLook> lookFor(FactionScreenDataPayload data, String factionName) {
		if (factionName == null || factionName.isBlank()) {
			return Optional.empty();
		}
		if (data.factionName().equalsIgnoreCase(factionName)) {
			return Optional.of(new FlagLook(data.factionName(), data.myBanner(), data.colorId()));
		}
		for (FactionDiplomacyRow row : data.diplomacy()) {
			if (row.factionName().equalsIgnoreCase(factionName)) {
				return Optional.of(new FlagLook(row.factionName(), row.banner(), row.colorId()));
			}
		}
		return Optional.empty();
	}

	public static List<FlagLook> partners(
		FactionScreenDataPayload data,
		String factionName,
		FactionRelationStatus status
	) {
		List<FlagLook> result = new ArrayList<>();
		if (factionName == null || factionName.isBlank()) {
			return result;
		}
		for (FactionRelationEdge edge : data.relationEdges()) {
			if (edge.relationStatus() != status) {
				continue;
			}
			String other = edge.otherOf(factionName);
			if (other == null) {
				continue;
			}
			lookFor(data, other).ifPresent(result::add);
		}
		result.sort(Comparator.comparing(FlagLook::name, String.CASE_INSENSITIVE_ORDER));
		return result;
	}
}
