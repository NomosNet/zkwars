package com.nomosiki.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

public final class SakuraStyle {
	public static int INK = 0xFF5A2434;
	public static int INK_MUTED = 0xFF8A5A68;
	public static int INK_DISABLED = 0xFFA88890;
	public static int PAPER = 0x55F4D6E0;
	public static int PAPER_INNER = 0x33FADFE8;
	public static int BUTTON = 0xFFF2C4D0;
	public static int BUTTON_HOVER = 0xFFFFD0DC;
	public static int BUTTON_DISABLED = 0xFFD4C0C6;
	public static int FRAME_INK = 0xFF5A2434;
	public static int FRAME_SAKURA = 0xFFE89BB0;
	public static int FRAME_HI = 0xFFFFE4EE;
	public static int FRAME_GOLD = 0xFFD4A574;
	public static int FRAME_GOLD_HI = 0xFFFFE0A8;
	public static int FRAME_INNER = 0x665A2434;
	public static int FRAME_DISABLED = 0xFFB898A0;
	public static int FRAME_DISABLED_HI = 0xFFD0C0C4;
	public static int HANDLE = 0xFFC45C7A;
	public static int HANDLE_HOVER = 0xFFE07090;
	public static int TITLE = 0xFF5A2434;
	public static int ACCENT = 0xFFC45C7A;
	public static int DIM = 0x66101018;
	public static int PORTRAIT = 0xEEFFF8EE;
	public static int SLOT_FILL = 0xDDF8E8EE;
	public static int SLOT_INNER = 0xEEFFF5F8;
	public static int EDIT_FILL = 0xCCFFF5F8;
	public static int ROW = 0x33FADFE8;
	public static int ROW_HOVER = 0x66FFD0DC;
	public static int ROW_SELECTED = 0xAAFFD0DC;
	public static int SCROLL_TRACK = 0x66000000;
	public static final int SLOT_SIZE = 18;

	private SakuraStyle() {
	}

	public static void apply(ZkGuiTheme.Palette palette) {
		INK = palette.ink();
		INK_MUTED = palette.inkMuted();
		INK_DISABLED = palette.inkDisabled();
		PAPER = palette.paper();
		PAPER_INNER = palette.paperInner();
		BUTTON = palette.button();
		BUTTON_HOVER = palette.buttonHover();
		BUTTON_DISABLED = palette.buttonDisabled();
		FRAME_INK = palette.frameInk();
		FRAME_SAKURA = palette.frameMid();
		FRAME_HI = palette.frameHi();
		FRAME_GOLD = palette.frameGold();
		FRAME_GOLD_HI = palette.frameGoldHi();
		FRAME_INNER = palette.frameInner();
		FRAME_DISABLED = palette.frameDisabled();
		FRAME_DISABLED_HI = palette.frameDisabledHi();
		HANDLE = palette.handle();
		HANDLE_HOVER = palette.handleHover();
		TITLE = palette.title();
		ACCENT = palette.accent();
		DIM = palette.dim();
		PORTRAIT = palette.portrait();
		SLOT_FILL = palette.slotFill();
		SLOT_INNER = palette.slotInner();
		EDIT_FILL = palette.editFill();
		ROW = palette.row();
		ROW_HOVER = palette.rowHover();
		ROW_SELECTED = palette.rowSelected();
		SCROLL_TRACK = palette.scrollTrack();
	}

	public static void drawDim(GuiGraphics graphics, int width, int height) {
		graphics.fill(0, 0, width, height, DIM);
	}

	public static void drawPaperPanel(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, PAPER);
		graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, PAPER_INNER);
		drawWaFrame(graphics, x, y, w, h, FRAME_INK, FRAME_SAKURA, FRAME_HI);
		drawWaFrame(graphics, x + 2, y + 2, w - 4, h - 4, FRAME_INNER, FRAME_GOLD, FRAME_GOLD_HI);
	}

	public static void drawPortraitFrame(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, PORTRAIT);
		drawWaFrame(graphics, x, y, w, h, FRAME_INK, FRAME_SAKURA, FRAME_HI);
	}

	public static void drawSlot(GuiGraphics graphics, int x, int y) {
		graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_FILL);
		graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_INNER);
		drawWaFrame(graphics, x, y, SLOT_SIZE, SLOT_SIZE, FRAME_INK, FRAME_SAKURA, FRAME_HI);
	}

	public static void drawSlotGrid(GuiGraphics graphics, int x, int y, int cols, int rows) {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				drawSlot(graphics, x + col * SLOT_SIZE, y + row * SLOT_SIZE);
			}
		}
	}

	public static void drawSlots(GuiGraphics graphics, Iterable<Slot> slots, int leftPos, int topPos) {
		for (Slot slot : slots) {
			drawSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
		}
	}

	public static void drawListRow(GuiGraphics graphics, int x, int y, int w, int h, boolean selected, boolean hovered) {
		int fill = selected ? ROW_SELECTED : (hovered ? ROW_HOVER : ROW);
		graphics.fill(x, y, x + w, y + h, fill);
		if (selected) {
			drawWaFrame(graphics, x, y, w, h, FRAME_INK, FRAME_GOLD, FRAME_GOLD_HI);
		}
	}

	public static void drawScrollbar(GuiGraphics graphics, int x, int y, int h, int scroll, int visible, int total) {
		if (total <= visible || h <= 0 || visible <= 0) {
			return;
		}
		graphics.fill(x, y, x + 4, y + h, SCROLL_TRACK);
		int thumbH = Math.max(8, Math.min(h, h * visible / total));
		int maxScroll = total - visible;
		int travel = Math.max(0, h - thumbH);
		int thumbY = y + (maxScroll == 0 ? 0 : travel * scroll / maxScroll);
		graphics.fill(x, thumbY, x + 4, thumbY + thumbH, HANDLE);
	}

	public static void drawEditBox(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {
		graphics.fill(x, y, x + w, y + h, EDIT_FILL);
		int gold = focused ? FRAME_GOLD_HI : FRAME_SAKURA;
		drawWaFrame(graphics, x, y, w, h, FRAME_INK, gold, FRAME_HI);
	}

	public static void drawGoldSelection(GuiGraphics graphics, int x, int y, int w, int h) {
		drawWaFrame(graphics, x, y, w, h, FRAME_GOLD, FRAME_GOLD_HI, FRAME_GOLD_HI);
	}

	public static void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered, boolean active) {
		int fill = !active ? BUTTON_DISABLED : (hovered ? BUTTON_HOVER : BUTTON);
		graphics.fill(x, y, x + w, y + h, fill);
		int gold = !active ? FRAME_DISABLED : (hovered ? FRAME_GOLD_HI : FRAME_SAKURA);
		int hi = !active ? FRAME_DISABLED_HI : FRAME_HI;
		drawWaFrame(graphics, x, y, w, h, FRAME_INK, gold, hi);
	}

	public static void drawCircleButton(GuiGraphics graphics, int cx, int cy, int radius, boolean hovered, boolean active, boolean selected) {
		int fill = !active ? BUTTON_DISABLED : (hovered || selected ? BUTTON_HOVER : BUTTON);
		int ring = !active ? FRAME_DISABLED : (hovered || selected ? FRAME_GOLD_HI : FRAME_GOLD);
		int hi = !active ? FRAME_DISABLED_HI : FRAME_HI;
		fillCircle(graphics, cx, cy, radius, fill);
		strokeCircle(graphics, cx, cy, radius, FRAME_INK);
		if (radius > 2) {
			strokeCircle(graphics, cx, cy, radius - 1, ring);
		}
		if (radius > 3) {
			strokeCircle(graphics, cx, cy, radius - 2, hi);
		}
	}

	public static void drawSlider(
		GuiGraphics graphics,
		Font font,
		int x,
		int y,
		int w,
		int h,
		double value,
		Component message,
		boolean hovered,
		boolean active
	) {
		drawButton(graphics, x, y, w, h, hovered, active);
		int handleW = 8;
		int handleX = x + (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * (w - handleW));
		int handle = hovered && active ? HANDLE_HOVER : HANDLE;
		if (!active) {
			handle = INK_DISABLED;
		}
		graphics.fill(handleX + 1, y + 2, handleX + handleW - 1, y + h - 2, handle);
		int color = active ? INK : INK_DISABLED;
		int textY = y + (h - 8) / 2;
		int tw = font.width(message);
		graphics.drawString(font, message, x + (w - tw) / 2, textY, color, false);
	}

	public static void drawLabel(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
		graphics.drawString(font, text, x, y, color, false);
	}

	public static void drawCentered(GuiGraphics graphics, Font font, Component text, int cx, int y, int color) {
		int tw = font.width(text);
		graphics.drawString(font, text, cx - tw / 2, y, color, false);
	}

	public static void drawWaFrame(GuiGraphics graphics, int x, int y, int w, int h, int ink, int gold, int highlight) {
		hLine(graphics, x, y, w, ink);
		hLine(graphics, x, y + h - 1, w, ink);
		vLine(graphics, x, y, h, ink);
		vLine(graphics, x + w - 1, y, h, ink);
		if (w < 4 || h < 4) {
			return;
		}
		hLine(graphics, x + 1, y + 1, w - 2, gold);
		hLine(graphics, x + 1, y + h - 2, w - 2, gold);
		vLine(graphics, x + 1, y + 1, h - 2, gold);
		vLine(graphics, x + w - 2, y + 1, h - 2, gold);
		int arm = Math.min(5, Math.min(w, h) / 3);
		drawWaCorner(graphics, x, y, 1, 1, arm, highlight, gold);
		drawWaCorner(graphics, x + w - 1, y, -1, 1, arm, highlight, gold);
		drawWaCorner(graphics, x, y + h - 1, 1, -1, arm, highlight, gold);
		drawWaCorner(graphics, x + w - 1, y + h - 1, -1, -1, arm, highlight, gold);
	}

	private static void drawWaCorner(GuiGraphics graphics, int ox, int oy, int dx, int dy, int arm, int highlight, int gold) {
		for (int i = 0; i < arm; i++) {
			pix(graphics, ox + dx * i, oy, highlight);
			pix(graphics, ox, oy + dy * i, highlight);
		}
		pix(graphics, ox + dx, oy + dy, highlight);
		if (arm >= 4) {
			pix(graphics, ox + dx * 2, oy + dy, gold);
			pix(graphics, ox + dx, oy + dy * 2, gold);
			pix(graphics, ox + dx * 3, oy + dy, highlight);
			pix(graphics, ox, oy + dy * 3, highlight);
		}
	}

	private static void fillCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
		int r2 = radius * radius;
		for (int dy = -radius; dy <= radius; dy++) {
			int remain = r2 - dy * dy;
			if (remain < 0) {
				continue;
			}
			int dx = (int) Math.floor(Math.sqrt(remain));
			graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	private static void strokeCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
		int outer2 = radius * radius;
		int inner = Math.max(0, radius - 1);
		int inner2 = inner * inner;
		for (int dy = -radius; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				int d2 = dx * dx + dy * dy;
				if (d2 <= outer2 && d2 > inner2) {
					pix(graphics, cx + dx, cy + dy, color);
				}
			}
		}
	}

	private static void pix(GuiGraphics graphics, int x, int y, int color) {
		graphics.fill(x, y, x + 1, y + 1, color);
	}

	private static void hLine(GuiGraphics graphics, int x, int y, int w, int color) {
		graphics.fill(x, y, x + w, y + 1, color);
	}

	private static void vLine(GuiGraphics graphics, int x, int y, int h, int color) {
		graphics.fill(x, y, x + 1, y + h, color);
	}
}
