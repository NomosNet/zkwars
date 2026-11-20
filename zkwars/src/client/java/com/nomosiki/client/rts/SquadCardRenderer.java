package com.nomosiki.client.rts;

import com.nomosiki.client.SquadLabels;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.squad.SquadIcons;
import com.nomosiki.squad.SquadType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SquadCardRenderer {
	public static final int CARD_W = 54;
	public static final int CARD_H = 80;
	public static final int HEAD_SIZE = 28;
	public static final int CARD_GAP = 2;
	public static final int HEAD_GAP = 2;
	public static final int CHEVRON_W = 10;
	public static final int BADGE = 9;
	private static final double PORTRAIT_RANGE = 256.0;
	private static final int NAME_H = 11;
	private static final int ICON_SRC = 16;
	private static final int BAR_H = 3;
	private static final int HEAD_BAR_H = 2;
	private static final int BAR_PAD = 2;
	private static final int PORTRAIT_SCALE_MIN = 14;
	private static final int PORTRAIT_SCALE_MAX = 30;
	private static final int CARD_PAPER = 0xAAF4D6E0;
	private static final int CARD_PAPER_DEAD = 0x88C8B4BA;
	private static final int SELECTED_FILL = 0x66FF8FB5;
	private static final int HP_BAR_COLOR = 0xFF6B9E4E;
	private static final int HP_BAR_BG = 0xFF5A2434;

	private SquadCardRenderer() {
	}

	public static List<SquadArmyStatePayload.SquadRow> aliveRows(List<SquadArmyStatePayload.SquadRow> rows) {
		List<SquadArmyStatePayload.SquadRow> out = new ArrayList<>();
		for (SquadArmyStatePayload.SquadRow row : rows) {
			if (row.alive() > 0) {
				out.add(row);
			}
		}
		return out;
	}

	public static int resolvePrimaryIndex(List<SquadArmyStatePayload.SquadRow> rows, Optional<UUID> preferredId) {
		if (rows.isEmpty()) {
			return -1;
		}
		if (preferredId.isPresent()) {
			UUID id = preferredId.get();
			for (int i = 0; i < rows.size(); i++) {
				if (rows.get(i).squadId().equals(id)) {
					return i;
				}
			}
		}
		for (int i = 0; i < rows.size(); i++) {
			if (RtsClientState.isSelected(rows.get(i).squadId())) {
				return i;
			}
		}
		return 0;
	}

	public static StripLayout layoutStrip(
		int left,
		int top,
		int width,
		List<SquadArmyStatePayload.SquadRow> rows,
		Optional<UUID> preferredPrimaryId,
		int scroll
	) {
		int primaryIndex = resolvePrimaryIndex(rows, preferredPrimaryId);
		int headCount = primaryIndex < 0 ? 0 : Math.max(0, rows.size() - 1);
		int primaryBlock = primaryIndex < 0 ? 0 : CARD_W + (headCount > 0 ? CARD_GAP : 0);
		int headsBudget = Math.max(0, width - primaryBlock);
		boolean overflow = false;
		int visibleHeads = headCount;
		int chevronSpace = 0;
		if (headCount > 0) {
			int needed = headCount * HEAD_SIZE + Math.max(0, headCount - 1) * HEAD_GAP;
			overflow = needed > headsBudget;
			chevronSpace = overflow ? (CHEVRON_W + CARD_GAP) * 2 : 0;
			int budget = Math.max(0, headsBudget - chevronSpace);
			if (overflow) {
				visibleHeads = Math.max(1, (budget + HEAD_GAP) / (HEAD_SIZE + HEAD_GAP));
				visibleHeads = Math.min(visibleHeads, headCount);
			}
		}
		int maxScroll = Math.max(0, headCount - visibleHeads);
		int clamped = Mth.clamp(scroll, 0, maxScroll);
		int primaryX = left;
		int headsLeft;
		int leftChevronX = left;
		int rightChevronX = left;
		if (overflow) {
			headsLeft = left + primaryBlock + CHEVRON_W + CARD_GAP;
			leftChevronX = left + primaryBlock;
			rightChevronX = headsLeft + visibleHeads * HEAD_SIZE + Math.max(0, visibleHeads - 1) * HEAD_GAP + CARD_GAP;
		} else {
			headsLeft = left + primaryBlock;
		}
		return new StripLayout(
			left,
			top,
			width,
			CARD_H,
			primaryIndex,
			primaryX,
			headsLeft,
			visibleHeads,
			clamped,
			maxScroll,
			overflow,
			leftChevronX,
			rightChevronX,
			rows.size()
		);
	}

	public static Map<UUID, NomosikEntity> collectPortraits(Minecraft mc) {
		Map<UUID, NomosikEntity> best = new HashMap<>();
		Map<UUID, Double> dist = new HashMap<>();
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return best;
		}
		var box = player.getBoundingBox().inflate(PORTRAIT_RANGE, PORTRAIT_RANGE, PORTRAIT_RANGE);
		List<NomosikEntity> found = mc.level.getEntitiesOfClass(
			NomosikEntity.class,
			box,
			n -> n.isAlive()
				&& player.getUUID().equals(n.getOwnerUUID())
				&& n.getSquadIdForGui().isPresent()
		);
		for (NomosikEntity nomosik : found) {
			UUID squadId = nomosik.getSquadIdForGui().orElse(null);
			if (squadId == null) {
				continue;
			}
			double d = nomosik.distanceToSqr(player);
			Double prev = dist.get(squadId);
			if (prev == null || d < prev) {
				dist.put(squadId, d);
				best.put(squadId, nomosik);
			}
		}
		return best;
	}

	public static int renderStrip(
		GuiGraphics graphics,
		Font font,
		Minecraft mc,
		StripLayout layout,
		List<SquadArmyStatePayload.SquadRow> rows,
		int mouseX,
		int mouseY,
		java.util.Optional<UUID> activeId
	) {
		return renderStrip(graphics, font, mc, layout, rows, mouseX, mouseY, activeId, false);
	}

	public static int renderStrip(
		GuiGraphics graphics,
		Font font,
		Minecraft mc,
		StripLayout layout,
		List<SquadArmyStatePayload.SquadRow> rows,
		int mouseX,
		int mouseY,
		java.util.Optional<UUID> activeId,
		boolean showTransferBadge
	) {
		if (layout.overflow()) {
			drawChevron(graphics, layout.leftChevronX(), layout.top(), true, mouseX, mouseY, layout.scroll() > 0);
			drawChevron(graphics, layout.rightChevronX(), layout.top(), false, mouseX, mouseY, layout.scroll() < layout.maxScroll());
		}
		Map<UUID, NomosikEntity> portraits = collectPortraits(mc);
		int hoveredIndex = -1;
		if (layout.primaryIndex() >= 0 && layout.primaryIndex() < rows.size()) {
			SquadArmyStatePayload.SquadRow primary = rows.get(layout.primaryIndex());
			boolean hovered = hitRect(layout.primaryX(), layout.top(), CARD_W, CARD_H, mouseX, mouseY);
			if (hovered) {
				hoveredIndex = layout.primaryIndex();
			}
			boolean active = activeId.filter(id -> id.equals(primary.squadId())).isPresent();
			boolean selected = RtsClientState.isSelected(primary.squadId());
			drawCard(
				graphics,
				font,
				primary,
				portraits.get(primary.squadId()),
				layout.primaryX(),
				layout.top(),
				CARD_W,
				active,
				selected,
				hovered,
				mouseX,
				mouseY,
				showTransferBadge
			);
		}
		List<Integer> headIndices = headRowIndices(layout, rows.size());
		for (int slot = 0; slot < headIndices.size(); slot++) {
			int rowIndex = headIndices.get(slot);
			SquadArmyStatePayload.SquadRow row = rows.get(rowIndex);
			int x = layout.headsLeft() + slot * (HEAD_SIZE + HEAD_GAP);
			int y = layout.top() + (CARD_H - HEAD_SIZE) / 2;
			boolean hovered = hitRect(x, y, HEAD_SIZE, HEAD_SIZE, mouseX, mouseY);
			if (hovered) {
				hoveredIndex = rowIndex;
			}
			boolean selected = RtsClientState.isSelected(row.squadId());
			drawHead(graphics, row, portraits.get(row.squadId()), x, y, selected, hovered, mouseX, mouseY);
		}
		return hoveredIndex;
	}

	public static void drawCard(
		GuiGraphics graphics,
		Font font,
		SquadArmyStatePayload.SquadRow row,
		@Nullable NomosikEntity member,
		int x,
		int y,
		int cardW,
		boolean active,
		boolean selected,
		boolean hovered,
		int mouseX,
		int mouseY,
		boolean showTransferBadge
	) {
		boolean dead = row.alive() <= 0;
		int bg = dead ? CARD_PAPER_DEAD : CARD_PAPER;
		graphics.fill(x, y, x + cardW, y + CARD_H, bg);
		if (selected) {
			graphics.fill(x, y, x + cardW, y + CARD_H, SELECTED_FILL);
		}
		if (hovered && !active) {
			graphics.fill(x, y, x + cardW, y + CARD_H, 0x33FFFFFF);
		}
		int gold = frameGold(active, selected, dead);
		int hi = frameHi(active, selected, dead);
		SakuraStyle.drawWaFrame(graphics, x, y, cardW, CARD_H, dead ? 0xFF4A3840 : SakuraStyle.FRAME_INK, gold, hi);
		if (active) {
			SakuraStyle.drawWaFrame(graphics, x + 1, y + 1, cardW - 2, CARD_H - 2, SakuraStyle.FRAME_GOLD, SakuraStyle.FRAME_GOLD_HI, SakuraStyle.FRAME_GOLD_HI);
		}

		Component name = squadName(row);
		String clipped = font.plainSubstrByWidth(name.getString(), cardW - 4);
		int nameW = font.width(clipped);
		int nameColor = dead ? SakuraStyle.INK_DISABLED : SakuraStyle.INK;
		graphics.drawString(font, clipped, x + (cardW - nameW) / 2, y + 3, nameColor, false);

		int portraitX1 = x + 3;
		int portraitY1 = y + NAME_H + 1;
		int portraitX2 = x + cardW - 3;
		int portraitY2 = y + CARD_H - BAR_PAD - BAR_H - 3;
		if (member != null && member.isAlive() && !dead) {
			int scale = Mth.clamp((portraitY2 - portraitY1) / 2, PORTRAIT_SCALE_MIN, PORTRAIT_SCALE_MAX);
			graphics.enableScissor(portraitX1, portraitY1, portraitX2, portraitY2);
			try {
				InventoryScreen.renderEntityInInventoryFollowsMouse(
					graphics, portraitX1, portraitY1, portraitX2, portraitY2, scale, 0.0625F, (float) mouseX, (float) mouseY, member);
			} finally {
				graphics.disableScissor();
			}
		} else {
			drawFallbackIcon(graphics, row, portraitX1, portraitY1, portraitX2 - portraitX1, portraitY2 - portraitY1, dead);
		}

		String count = row.alive() + "/" + row.total();
		int countW = font.width(count);
		graphics.drawString(font, count, x + (cardW - countW) / 2, portraitY2 - 10, dead ? SakuraStyle.INK_DISABLED : SakuraStyle.INK, false);

		int barX = x + BAR_PAD + 2;
		int barW = cardW - (BAR_PAD + 2) * 2;
		int barY = y + CARD_H - BAR_PAD - BAR_H - 2;
		graphics.fill(barX, barY, barX + barW, barY + BAR_H, HP_BAR_BG);
		int fill = Mth.clamp(Math.round(row.avgHealthPct() * barW), 0, barW);
		if (fill > 0) {
			graphics.fill(barX, barY, barX + fill, barY + BAR_H, HP_BAR_COLOR);
		}
		if (showTransferBadge) {
			int bx = x + cardW - BADGE - 2;
			int by = y + 2;
			boolean badgeHover = hitRect(bx, by, BADGE, BADGE, mouseX, mouseY);
			graphics.fill(bx, by, bx + BADGE, by + BADGE, badgeHover ? 0xFFFFC14D : 0xFFC47A1A);
			graphics.fill(bx + 2, by + 4, bx + BADGE - 2, by + 5, 0xFF2A1608);
			graphics.fill(bx + BADGE - 4, by + 2, bx + BADGE - 3, by + 7, 0xFF2A1608);
		}
	}

	private static void drawHead(
		GuiGraphics graphics,
		SquadArmyStatePayload.SquadRow row,
		@Nullable NomosikEntity member,
		int x,
		int y,
		boolean selected,
		boolean hovered,
		int mouseX,
		int mouseY
	) {
		boolean dead = row.alive() <= 0;
		int bg = dead ? CARD_PAPER_DEAD : CARD_PAPER;
		graphics.fill(x, y, x + HEAD_SIZE, y + HEAD_SIZE, bg);
		if (selected) {
			graphics.fill(x, y, x + HEAD_SIZE, y + HEAD_SIZE, SELECTED_FILL);
		}
		if (hovered) {
			graphics.fill(x, y, x + HEAD_SIZE, y + HEAD_SIZE, 0x33FFFFFF);
		}
		int gold = frameGold(false, selected, dead);
		int hi = frameHi(false, selected, dead);
		SakuraStyle.drawWaFrame(graphics, x, y, HEAD_SIZE, HEAD_SIZE, dead ? 0xFF4A3840 : SakuraStyle.FRAME_INK, gold, hi);

		int portraitPad = 2;
		int portraitX1 = x + portraitPad;
		int portraitY1 = y + portraitPad;
		int portraitX2 = x + HEAD_SIZE - portraitPad;
		int portraitY2 = y + HEAD_SIZE - portraitPad - HEAD_BAR_H - 1;
		if (member != null && member.isAlive() && !dead) {
			int scale = Mth.clamp((portraitY2 - portraitY1) / 2, 8, 16);
			graphics.enableScissor(portraitX1, portraitY1, portraitX2, portraitY2);
			try {
				InventoryScreen.renderEntityInInventoryFollowsMouse(
					graphics, portraitX1, portraitY1, portraitX2, portraitY2, scale, 0.0625F, (float) mouseX, (float) mouseY, member);
			} finally {
				graphics.disableScissor();
			}
		} else {
			drawFallbackIcon(graphics, row, portraitX1, portraitY1, portraitX2 - portraitX1, portraitY2 - portraitY1, dead);
		}

		int barX = x + 2;
		int barW = HEAD_SIZE - 4;
		int barY = y + HEAD_SIZE - HEAD_BAR_H - 1;
		graphics.fill(barX, barY, barX + barW, barY + HEAD_BAR_H, HP_BAR_BG);
		int fill = Mth.clamp(Math.round(row.avgHealthPct() * barW), 0, barW);
		if (fill > 0) {
			graphics.fill(barX, barY, barX + fill, barY + HEAD_BAR_H, HP_BAR_COLOR);
		}
	}

	private static Component squadName(SquadArmyStatePayload.SquadRow row) {
		if (row.name().isEmpty()) {
			return Component.translatable(SquadType.fromId(row.typeId()).translationKey());
		}
		return SquadLabels.component(row.name());
	}

	public static List<Component> cardTooltip(SquadArmyStatePayload.SquadRow row) {
		List<Component> lines = new ArrayList<>();
		lines.add(squadName(row));
		lines.add(Component.translatable("overlay.nomosiki.rts.card.strength", row.alive(), row.total()));
		lines.add(Component.translatable(
			"overlay.nomosiki.rts.status_line",
			BattleHudOverlay.orderLabel(SquadOrder.fromId(row.orderId())),
			BattleHudOverlay.stanceLabel(NomosikCombatMode.fromId(row.combatModeId()))
		));
		SquadFormation formation = SquadFormation.fromId(row.formationId());
		lines.add(Component.translatable(
			"overlay.nomosiki.rts.formation_line",
			formation.formationsScreenLabel(),
			String.format("%.1f", row.formationSpacing())
		));
		lines.add(Component.translatable("overlay.nomosiki.rts.card.distance", formatDist(row.distanceToPlayer())));
		if (BattleHudOverlay.isRangedBranch(row.typeId())) {
			NomosikRangedFireMode fireMode = NomosikRangedFireMode.fromId(row.rangedFireModeId());
			lines.add(Component.translatable(
				"overlay.nomosiki.rts.ranged_line",
				BattleHudOverlay.fireModeLabel(fireMode),
				row.waypoints().size()
			));
			if (fireMode == NomosikRangedFireMode.STRATEGIC_FIRE && row.hasFireZone()) {
				lines.add(Component.translatable("overlay.nomosiki.rts.bombard_line", row.fireRadius()));
			}
		} else if (!row.waypoints().isEmpty()) {
			lines.add(Component.translatable("overlay.nomosiki.rts.waypoint_line", row.waypoints().size()));
		}
		if (row.shieldStance()) {
			lines.add(Component.translatable("overlay.nomosiki.rts.shield_line"));
		}
		return lines;
	}

	public static boolean hitStrip(StripLayout layout, double mouseX, double mouseY) {
		return mouseX >= layout.left() && mouseX < layout.left() + layout.width()
			&& mouseY >= layout.top() && mouseY < layout.top() + layout.height();
	}

	public static boolean hitLeftChevron(StripLayout layout, double mouseX, double mouseY) {
		return layout.overflow() && hitRect(layout.leftChevronX(), layout.top(), CHEVRON_W, CARD_H, mouseX, mouseY);
	}

	public static boolean hitRightChevron(StripLayout layout, double mouseX, double mouseY) {
		return layout.overflow() && hitRect(layout.rightChevronX(), layout.top(), CHEVRON_W, CARD_H, mouseX, mouseY);
	}

	public static int transferBadgeAt(StripLayout layout, double mouseX, double mouseY) {
		if (layout.primaryIndex() < 0) {
			return -1;
		}
		if (mouseY < layout.top() || mouseY >= layout.top() + CARD_H) {
			return -1;
		}
		int bx = layout.primaryX() + CARD_W - BADGE - 2;
		int by = layout.top() + 2;
		if (hitRect(bx, by, BADGE, BADGE, mouseX, mouseY)) {
			return layout.primaryIndex();
		}
		return -1;
	}

	public static int cardIndexAt(StripLayout layout, double mouseX, double mouseY) {
		if (mouseY < layout.top() || mouseY >= layout.top() + layout.height()) {
			return -1;
		}
		if (layout.primaryIndex() >= 0 && hitRect(layout.primaryX(), layout.top(), CARD_W, CARD_H, mouseX, mouseY)) {
			return layout.primaryIndex();
		}
		int headY = layout.top() + (CARD_H - HEAD_SIZE) / 2;
		if (mouseY < headY || mouseY >= headY + HEAD_SIZE) {
			return -1;
		}
		int rel = (int) mouseX - layout.headsLeft();
		if (rel < 0) {
			return -1;
		}
		int stride = HEAD_SIZE + HEAD_GAP;
		int slot = rel / stride;
		int inSlot = rel % stride;
		if (slot < 0 || slot >= layout.visibleHeads() || inSlot >= HEAD_SIZE) {
			return -1;
		}
		List<Integer> heads = headRowIndices(layout, layout.rowCount());
		if (slot >= heads.size()) {
			return -1;
		}
		return heads.get(slot);
	}

	private static List<Integer> headRowIndices(StripLayout layout, int rowCount) {
		List<Integer> indices = new ArrayList<>();
		if (layout.primaryIndex() < 0 || rowCount <= 0) {
			return indices;
		}
		int skipped = 0;
		for (int i = 0; i < rowCount; i++) {
			if (i == layout.primaryIndex()) {
				continue;
			}
			if (skipped < layout.scroll()) {
				skipped++;
				continue;
			}
			indices.add(i);
			if (indices.size() >= layout.visibleHeads()) {
				break;
			}
		}
		return indices;
	}

	private static void drawFallbackIcon(
		GuiGraphics graphics,
		SquadArmyStatePayload.SquadRow row,
		int x,
		int y,
		int w,
		int h,
		boolean dead
	) {
		int iconSize = Math.min(28, Math.min(w, h) - 2);
		if (iconSize <= 0) {
			return;
		}
		int iconX = x + (w - iconSize) / 2;
		int iconY = y + (h - iconSize) / 2 - 2;
		graphics.pose().pushPose();
		graphics.pose().translate(iconX, iconY, 0);
		float scale = iconSize / (float) ICON_SRC;
		graphics.pose().scale(scale, scale, 1.0f);
		float tint = dead ? 0.55f : 1.0f;
		graphics.setColor(tint, tint * 0.85f, tint * 0.9f, dead ? 0.7f : 1.0f);
		graphics.blit(SquadIcons.byId(row.iconId()), 0, 0, 0, 0, ICON_SRC, ICON_SRC, ICON_SRC, ICON_SRC);
		graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.pose().popPose();
	}

	private static void drawChevron(GuiGraphics graphics, int x, int y, boolean left, int mouseX, int mouseY, boolean enabled) {
		boolean hovered = enabled && hitRect(x, y, CHEVRON_W, CARD_H, mouseX, mouseY);
		SakuraStyle.drawButton(graphics, x, y, CHEVRON_W, CARD_H, hovered, enabled);
		int color = enabled ? SakuraStyle.INK : SakuraStyle.INK_DISABLED;
		int cx = x + CHEVRON_W / 2;
		int cy = y + CARD_H / 2;
		int dir = left ? -1 : 1;
		graphics.fill(cx - dir, cy - 3, cx - dir + 1, cy + 4, color);
		graphics.fill(cx, cy - 2, cx + 1, cy + 3, color);
		graphics.fill(cx + dir, cy - 1, cx + dir + 1, cy + 2, color);
	}

	private static int frameGold(boolean active, boolean selected, boolean dead) {
		if (dead) {
			return 0xFF8A7A80;
		}
		if (active) {
			return SakuraStyle.FRAME_GOLD_HI;
		}
		if (selected) {
			return SakuraStyle.ACCENT;
		}
		return SakuraStyle.FRAME_SAKURA;
	}

	private static int frameHi(boolean active, boolean selected, boolean dead) {
		if (dead) {
			return 0xFFC0B0B4;
		}
		if (active) {
			return SakuraStyle.FRAME_GOLD_HI;
		}
		if (selected) {
			return SakuraStyle.FRAME_HI;
		}
		return SakuraStyle.FRAME_HI;
	}

	private static boolean hitRect(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private static String formatDist(double blocks) {
		return blocks < 10.0 ? String.format("%.1f", blocks) : String.format("%.0f", blocks);
	}

	public record StripLayout(
		int left,
		int top,
		int width,
		int height,
		int primaryIndex,
		int primaryX,
		int headsLeft,
		int visibleHeads,
		int scroll,
		int maxScroll,
		boolean overflow,
		int leftChevronX,
		int rightChevronX,
		int rowCount
	) {
		/** Compatibility alias for previous uniform-card layout fields. */
		public int cardW() {
			return CARD_W;
		}

		public int cardsLeft() {
			return primaryX;
		}

		public int visibleCount() {
			return (primaryIndex >= 0 ? 1 : 0) + visibleHeads;
		}
	}
}
