package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.network.duel.payload.c2s.DuelActionPayload;
import com.nomosiki.network.duel.payload.s2c.DuelScreenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DuelScreen extends Screen {
	private static final int ROW_HEIGHT = 22;
	private static final int BUTTON_HEIGHT = 20;

	@Nullable
	private final Screen parent;
	private int scroll;
	private int listLeft;
	private int listTop;
	private int listWidth;
	private int listHeight;
	private int visibleRows = 1;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public DuelScreen(@Nullable Screen parent) {
		super(Component.translatable("screen.nomosiki.duel.title"));
		this.parent = parent;
	}

	public void refresh() {
		this.rebuildWidgets();
	}

	@Override
	protected void init() {
		DuelScreenPayload data = DuelClient.last();
		this.panelWidth = 280;
		this.panelLeft = this.width / 2 - this.panelWidth / 2;
		this.panelTop = 8;
		this.panelHeight = this.height - 16;
		this.listLeft = this.panelLeft + 12;
		this.listWidth = this.panelWidth - 24;

		int y = 46;
		if (data.phase() == DuelScreenPayload.PHASE_INCOMING) {
			int half = this.listWidth / 2 - 2;
			this.addRenderableWidget(new SakuraButton(this.listLeft, y, half, BUTTON_HEIGHT,
				Component.translatable("screen.nomosiki.duel.accept"),
				b -> ClientPlayNetworking.send(new DuelActionPayload(DuelActionPayload.ACCEPT, data.otherId()))));
			this.addRenderableWidget(new SakuraButton(this.listLeft + half + 4, y, half, BUTTON_HEIGHT,
				Component.translatable("screen.nomosiki.duel.deny"),
				b -> ClientPlayNetworking.send(new DuelActionPayload(DuelActionPayload.DENY, data.otherId()))));
			y += BUTTON_HEIGHT + 6;
		} else if (data.phase() == DuelScreenPayload.PHASE_OUTGOING) {
			this.addRenderableWidget(new SakuraButton(this.listLeft, y, this.listWidth, BUTTON_HEIGHT,
				Component.translatable("screen.nomosiki.duel.cancel"),
				b -> ClientPlayNetworking.send(new DuelActionPayload(DuelActionPayload.CANCEL, data.otherId()))));
			y += BUTTON_HEIGHT + 6;
		}

		int doneY = this.panelTop + this.panelHeight - BUTTON_HEIGHT - 8;
		boolean showList = data.phase() == DuelScreenPayload.PHASE_IDLE || data.phase() == DuelScreenPayload.PHASE_INCOMING;
		this.listTop = y;
		this.listHeight = Math.max(ROW_HEIGHT, doneY - 8 - this.listTop);
		this.visibleRows = Math.max(1, this.listHeight / ROW_HEIGHT);
		if (showList) {
			List<DuelScreenPayload.Row> rows = data.rows();
			int maxScroll = Math.max(0, rows.size() - this.visibleRows);
			this.scroll = Mth.clamp(this.scroll, 0, maxScroll);
			for (int i = 0; i < this.visibleRows; i++) {
				int index = i + this.scroll;
				if (index >= rows.size()) {
					break;
				}
				DuelScreenPayload.Row row = rows.get(index);
				int rowY = this.listTop + i * ROW_HEIGHT;
				SakuraButton button = new SakuraButton(this.listLeft + this.listWidth - 72, rowY, 72, 18,
					Component.translatable("screen.nomosiki.duel.challenge"),
					b -> ClientPlayNetworking.send(new DuelActionPayload(DuelActionPayload.CHALLENGE, row.id())));
				button.active = row.canChallenge() && !data.wartime();
				this.addRenderableWidget(button);
			}
		}

		Component done = this.parent == null
			? Component.translatable("gui.done")
			: Component.translatable("screen.nomosiki.duel.back");
		this.addRenderableWidget(new SakuraButton(this.width / 2 - 50, doneY, 100, BUTTON_HEIGHT, done, b -> this.onClose()));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		DuelScreenPayload data = DuelClient.last();
		if (data.phase() != DuelScreenPayload.PHASE_IDLE && data.phase() != DuelScreenPayload.PHASE_INCOMING) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		int maxScroll = Math.max(0, data.rows().size() - this.visibleRows);
		if (maxScroll > 0 && mouseX >= this.panelLeft && mouseX < this.panelLeft + this.panelWidth
			&& mouseY >= this.listTop && mouseY < this.listTop + this.listHeight) {
			this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, maxScroll);
			this.rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawDim(graphics, this.width, this.height);
		SakuraStyle.drawPaperPanel(graphics, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		DuelScreenPayload data = DuelClient.last();
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, this.panelTop + 8, SakuraStyle.TITLE);
		Component status = statusLine(data);
		String clipped = this.font.plainSubstrByWidth(status.getString(), this.listWidth);
		graphics.drawString(this.font, clipped, this.listLeft, this.panelTop + 26, SakuraStyle.INK, false);

		boolean showList = data.phase() == DuelScreenPayload.PHASE_IDLE || data.phase() == DuelScreenPayload.PHASE_INCOMING;
		if (!showList) {
			return;
		}
		List<DuelScreenPayload.Row> rows = data.rows();
		if (rows.isEmpty()) {
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.duel.empty"),
				this.listLeft, this.listTop + 4, SakuraStyle.INK_MUTED);
			return;
		}
		graphics.enableScissor(this.panelLeft + 4, this.listTop, this.panelLeft + this.panelWidth - 4, this.listTop + this.listHeight);
		for (int i = 0; i < this.visibleRows; i++) {
			int index = i + this.scroll;
			if (index >= rows.size()) {
				break;
			}
			DuelScreenPayload.Row row = rows.get(index);
			int rowY = this.listTop + i * ROW_HEIGHT + 5;
			String faction = row.faction().isBlank()
				? Component.translatable("screen.nomosiki.duel.no_faction").getString()
				: row.faction();
			String line = this.font.plainSubstrByWidth(row.name() + " — " + faction, this.listWidth - 78);
			graphics.drawString(this.font, line, this.listLeft, rowY, SakuraStyle.INK, false);
		}
		graphics.disableScissor();
		SakuraStyle.drawScrollbar(graphics, this.panelLeft + this.panelWidth - 8, this.listTop, this.listHeight,
			this.scroll, this.visibleRows, rows.size());
	}

	private static Component statusLine(DuelScreenPayload data) {
		if (data.wartime() && data.phase() == DuelScreenPayload.PHASE_IDLE) {
			return Component.translatable("screen.nomosiki.duel.status_wartime");
		}
		return switch (data.phase()) {
			case DuelScreenPayload.PHASE_OUTGOING -> Component.translatable("screen.nomosiki.duel.status_outgoing", data.otherName());
			case DuelScreenPayload.PHASE_INCOMING -> Component.translatable("screen.nomosiki.duel.status_incoming", data.otherName());
			case DuelScreenPayload.PHASE_FIGHTING -> Component.translatable("screen.nomosiki.duel.status_fighting", data.otherName());
			default -> cooldownOrReady(data.cooldownUntilMs());
		};
	}

	private static Component cooldownOrReady(long untilMs) {
		long left = untilMs - System.currentTimeMillis();
		if (left <= 0L) {
			return Component.translatable("screen.nomosiki.duel.status_ready");
		}
		long hours = left / 3_600_000L;
		long minutes = (left % 3_600_000L) / 60_000L;
		if (hours == 0L && minutes == 0L) {
			minutes = 1L;
		}
		return Component.translatable("screen.nomosiki.duel.status_cooldown", hours, minutes);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
