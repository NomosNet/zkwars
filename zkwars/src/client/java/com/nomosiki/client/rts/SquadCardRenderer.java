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
import java.util.UUID;

public final class SquadCardRenderer {
	public static final int CARD_W = 46;
	public static final int CARD_H = 68;
	public static final int CARD_GAP = 2;
	public static final int CHEVRON_W = 10;
	public static final int BADGE = 9;
	private static final double PORTRAIT_RANGE = 256.0;
	private static final int NAME_H = 11;
	private static final int ICON_SRC = 16;
	private static final int BAR_H = 3;
	private static final int BAR_PAD = 2;
	private static final int PORTRAIT_SCALE_MIN = 14;
	private static final int PORTRAIT_SCALE_MAX = 22;
	private static final int CARD_PAPER = 0xAAF4D6E0;
	private static final int CARD_PAPER_DEAD = 0x88C8B4BA;
	private static final int SELECTED_FILL = 0x66FF8FB5;
	private static final int HP_BAR_COLOR = 0xFF6B9E4E;
	private static final int HP_BAR_BG = 0xFF5A2434;

	private SquadCardRenderer() {
	}

	public static StripLayout layoutStrip(int left, int top, int width, int squadCount, int scroll) {
		int needed = squadCount * CARD_W + Math.max(0, squadCount - 1) * CARD_GAP;
		boolean overflow = squadCount > 0 && needed > width;
		int chevronSpace = overflow ? (CHEVRON_W + CARD_GAP) * 2 : 0;
		int cardsBudget = width - chevronSpace;
		int visibleCount = Math.max(0, squadCount);
		if (overflow) {
			visibleCount = Math.max(1, (cardsBudget + CARD_GAP) / (CARD_W + CARD_GAP));
			visibleCount = Math.min(visibleCount, squadCount);
		}
		int maxScroll = Math.max(0, squadCount - visibleCount);
		int clamped = Mth.clamp(scroll, 0, maxScroll);
		int cardsW = visibleCount <= 0 ? 0 : visibleCount * CARD_W + Math.max(0, visibleCount - 1) * CARD_GAP;
		int cardsLeft = overflow ? left + CHEVRON_W + CARD_GAP : left + Math.max(0, (width - cardsW) / 2);
		int leftChevronX = left;
		int rightChevronX = cardsLeft + cardsW + CARD_GAP;
		return new StripLayout(
			left, top, width, CARD_H, CARD_W, cardsLeft, visibleCount, clamped, maxScroll, overflow, leftChevronX, rightChevronX
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
		if (layout.overflow) {
			drawChevron(graphics, layout.leftChevronX, layout.top, true, mouseX, mouseY, layout.scroll > 0);
			drawChevron(graphics, layout.rightChevronX, layout.top, false, mouseX, mouseY, layout.scroll < layout.maxScroll);
		}
		Map<UUID, NomosikEntity> portraits = collectPortraits(mc);
		int hoveredIndex = -1;
		for (int i = 0; i < layout.visibleCount; i++) {
			int rowIndex = layout.scroll + i;
			if (rowIndex < 0 || rowIndex >= rows.size()) {
				break;
			}
			SquadArmyStatePayload.SquadRow row = rows.get(rowIndex);
			int x = layout.cardsLeft + i * (layout.cardW + CARD_GAP);
			boolean hovered = mouseX >= x && mouseX < x + layout.cardW
				&& mouseY >= layout.top && mouseY < layout.top + CARD_H;
			if (hovered) {
				hoveredIndex = rowIndex;
			}
			boolean active = activeId.filter(id -> id.equals(row.squadId())).isPresent();
			boolean selected = RtsClientState.isSelected(row.squadId());
			drawCard(graphics, font, row, portraits.get(row.squadId()), x, layout.top, layout.cardW, active, selected, hovered, mouseX, mouseY, showTransferBadge);
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

		String count = Integer.toString(row.alive());
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
		return mouseX >= layout.left && mouseX < layout.left + layout.width
			&& mouseY >= layout.top && mouseY < layout.top + layout.height;
	}

	public static boolean hitLeftChevron(StripLayout layout, double mouseX, double mouseY) {
		return layout.overflow && hitRect(layout.leftChevronX, layout.top, CHEVRON_W, CARD_H, mouseX, mouseY);
	}

	public static boolean hitRightChevron(StripLayout layout, double mouseX, double mouseY) {
		return layout.overflow && hitRect(layout.rightChevronX, layout.top, CHEVRON_W, CARD_H, mouseX, mouseY);
	}

	public static int transferBadgeAt(StripLayout layout, double mouseX, double mouseY) {
		if (mouseY < layout.top || mouseY >= layout.top + CARD_H) {
			return -1;
		}
		int rel = (int) mouseX - layout.cardsLeft;
		if (rel < 0) {
			return -1;
		}
		int stride = layout.cardW + CARD_GAP;
		int slot = rel / stride;
		int inSlot = rel % stride;
		if (slot < 0 || slot >= layout.visibleCount || inSlot >= layout.cardW) {
			return -1;
		}
		int bx = layout.cardW - BADGE - 2;
		int by = 2;
		if (inSlot < bx || inSlot >= bx + BADGE || mouseY < layout.top + by || mouseY >= layout.top + by + BADGE) {
			return -1;
		}
		return layout.scroll + slot;
	}

	public static int cardIndexAt(StripLayout layout, double mouseX, double mouseY) {
		if (mouseY < layout.top || mouseY >= layout.top + CARD_H) {
			return -1;
		}
		int rel = (int) mouseX - layout.cardsLeft;
		if (rel < 0) {
			return -1;
		}
		int stride = layout.cardW + CARD_GAP;
		int slot = rel / stride;
		int inSlot = rel % stride;
		if (slot < 0 || slot >= layout.visibleCount || inSlot >= layout.cardW) {
			return -1;
		}
		return layout.scroll + slot;
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
		int cardW,
		int cardsLeft,
		int visibleCount,
		int scroll,
		int maxScroll,
		boolean overflow,
		int leftChevronX,
		int rightChevronX
	) {
	}
}
