package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Rules and province choice for buying an extra province chunk. */
public class ChunkClaimScreen extends Screen {
	private static final int BUTTON_HEIGHT = 20;
	private static final String[] RULES = {
		"screen.nomosiki.chunk_claim.line1",
		"screen.nomosiki.chunk_claim.line2",
		"screen.nomosiki.chunk_claim.line3",
		"screen.nomosiki.chunk_claim.line4"
	};

	private final @Nullable Screen returnTo;
	private List<String> shownChoices = List.of();
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;
	private int rulesX;
	private int rulesY;
	private int rulesWidth;

	public ChunkClaimScreen(@Nullable Screen returnTo) {
		super(Component.translatable("screen.nomosiki.chunk_claim.open"));
		this.returnTo = returnTo;
	}

	@Override
	protected void init() {
		List<String> names = CityChunkClaim.choices();
		this.shownChoices = List.copyOf(names);

		this.panelWidth = Math.min(320, this.width - 24);
		this.rulesWidth = this.panelWidth - 24;
		int rulesHeight = this.rulesHeight();
		int choiceBlock = names.isEmpty() ? 0 : 8 + names.size() * (BUTTON_HEIGHT + 4) - 4;
		this.panelHeight = 28 + rulesHeight + 8 + BUTTON_HEIGHT + choiceBlock + 8 + BUTTON_HEIGHT + 12;
		this.panelLeft = this.width / 2 - this.panelWidth / 2;
		this.panelTop = Math.max(8, (this.height - this.panelHeight) / 2);
		if (this.panelTop + this.panelHeight > this.height - 8) {
			this.panelTop = 8;
			this.panelHeight = this.height - 16;
		}

		this.rulesX = this.panelLeft + 12;
		this.rulesY = this.panelTop + 28;
		int y = this.rulesY + rulesHeight + 8;
		this.addRenderableWidget(new SakuraButton(this.rulesX, y, this.rulesWidth, BUTTON_HEIGHT,
			Component.translatable("screen.nomosiki.chunk_claim.open"),
			b -> CityChunkClaim.claim("")));

		y += BUTTON_HEIGHT + 8;
		int limit = this.panelTop + this.panelHeight - BUTTON_HEIGHT - 16;
		for (String name : names) {
			if (y + BUTTON_HEIGHT > limit) {
				break;
			}
			String label = name;
			this.addRenderableWidget(new SakuraButton(this.rulesX, y, this.rulesWidth, BUTTON_HEIGHT,
				Component.literal(this.font.plainSubstrByWidth(label, this.rulesWidth - 6)),
				pressed -> CityChunkClaim.claim(label)));
			y += BUTTON_HEIGHT + 4;
		}

		int doneY = this.panelTop + this.panelHeight - BUTTON_HEIGHT - 8;
		this.addRenderableWidget(new SakuraButton(this.width / 2 - 50, doneY, 100, BUTTON_HEIGHT,
			Component.translatable("gui.done"), b -> this.onClose()));
	}

	@Override
	public void tick() {
		if (!CityChunkClaim.choices().equals(this.shownChoices)) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawDim(graphics, this.width, this.height);
		SakuraStyle.drawPaperPanel(graphics, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, this.panelTop + 10, SakuraStyle.TITLE);
		int y = this.rulesY;
		for (String key : RULES) {
			for (FormattedCharSequence line : this.font.split(Component.translatable(key), this.rulesWidth)) {
				graphics.drawString(this.font, line, this.rulesX, y, SakuraStyle.INK_MUTED, false);
				y += this.font.lineHeight + 1;
			}
		}
	}

	private int rulesHeight() {
		int height = 0;
		for (String key : RULES) {
			height += this.font.split(Component.translatable(key), this.rulesWidth).size() * (this.font.lineHeight + 1);
		}
		return height;
	}

	@Override
	public void onClose() {
		CityChunkClaim.clear();
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.returnTo);
			return;
		}
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
