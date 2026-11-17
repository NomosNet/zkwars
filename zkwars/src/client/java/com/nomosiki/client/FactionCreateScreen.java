package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraEditBox;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.faction.FactionBannerHelper;
import com.nomosiki.network.faction.payload.c2s.FactionCreatePayload;
import com.nomosiki.network.faction.payload.c2s.FactionOpenScreenPayload;
import com.nomosiki.screen.FactionCreateMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class FactionCreateScreen extends AbstractContainerScreen<FactionCreateMenu> {
	private EditBox factionNameBox;
	private Button createButton;
	private final FactionBannerRenderer bannerRenderer = new FactionBannerRenderer();

	public FactionCreateScreen(FactionCreateMenu handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = FactionCreateMenu.IMAGE_WIDTH;
		this.imageHeight = FactionCreateMenu.IMAGE_HEIGHT;
		this.titleLabelX = 68;
		this.titleLabelY = 8;
		this.inventoryLabelX = FactionCreateMenu.PLAYER_LEFT;
		this.inventoryLabelY = FactionCreateMenu.PLAYER_TOP - 11;
	}

	@Override
	protected void init() {
		super.init();
		int maxTop = Math.max(2, this.height - this.imageHeight - 4);
		if (this.topPos > maxTop) {
			this.topPos = maxTop;
		}
		if (this.topPos < 2) {
			this.topPos = 2;
		}

		this.factionNameBox = new SakuraEditBox(this.font, this.leftPos + 8, this.topPos + 56, 160, 18,
			Component.translatable("screen.nomosiki.faction.name"));
		this.factionNameBox.setMaxLength(20);
		this.factionNameBox.setHint(Component.translatable("screen.nomosiki.faction.name"));
		this.addRenderableWidget(this.factionNameBox);
		this.setInitialFocus(this.factionNameBox);
		this.createButton = this.addRenderableWidget(new SakuraButton(this.leftPos + 8, this.topPos + 78, 160, 20,
			Component.translatable("button.nomosiki.faction.create"), b -> {
				ClientPlayNetworking.send(new FactionCreatePayload(this.factionNameBox.getValue(), ""));
				ClientPlayNetworking.send(new FactionOpenScreenPayload());
			}));
		this.addRenderableWidget(new SakuraButton(this.leftPos + 8, this.topPos + 6, 56, 16,
			Component.translatable("button.nomosiki.faction.back"), b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new FactionListScreen());
					this.minecraft.player.closeContainer();
				}
			}));
		this.updateCreateButtonState();
	}

	@Override
	protected void setInitialFocus() {
		if (this.factionNameBox != null) {
			this.setInitialFocus(this.factionNameBox);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.closeContainer();
		}
		if (this.factionNameBox != null
			&& (this.factionNameBox.keyPressed(keyCode, scanCode, modifiers) || this.factionNameBox.canConsumeInput())) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.factionNameBox != null && !this.factionNameBox.isMouseOver(mouseX, mouseY)) {
			this.factionNameBox.setFocused(false);
			if (this.getFocused() == this.factionNameBox) {
				this.setFocused(null);
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void updateCreateButtonState() {
		if (this.createButton == null) {
			return;
		}
		ItemStack banner = this.menu.getBannerStack();
		boolean valid = !this.factionNameBox.getValue().isBlank()
			&& !banner.isEmpty()
			&& FactionBannerHelper.hasPatterns(banner);
		this.createButton.active = valid;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		SakuraStyle.drawPaperPanel(graphics, x, y, this.imageWidth, this.imageHeight);
		SakuraStyle.drawSlots(graphics, this.menu.slots, x, y);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, SakuraStyle.TITLE, false);
		Component bannerHint = Component.translatable("screen.nomosiki.faction.banner_slot");
		String bannerHintText = this.font.plainSubstrByWidth(bannerHint.getString(), 108);
		graphics.drawString(this.font, bannerHintText, 8, 24, SakuraStyle.INK, false);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
			SakuraStyle.INK_MUTED, false);
		ItemStack banner = this.menu.getBannerStack();
		if (!banner.isEmpty() && FactionBannerHelper.hasPatterns(banner)) {
			DyeColor dye = FactionBannerHelper.getBaseColor(banner);
			graphics.drawString(
				this.font,
				Component.translatable("screen.nomosiki.faction.unit_color_from_banner",
					Component.translatable("color.minecraft." + dye.getName())),
				8,
				102,
				SakuraStyle.INK_MUTED,
				false
			);
		} else if (!banner.isEmpty()) {
			graphics.drawString(
				this.font,
				Component.translatable("screen.nomosiki.faction.banner_needs_patterns"),
				8,
				102,
				SakuraStyle.ACCENT,
				false
			);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		ItemStack banner = this.menu.getBannerStack();
		this.bannerRenderer.setBannerStack(banner);
		if (this.bannerRenderer.hasBanner()) {
			this.bannerRenderer.render(graphics, this.leftPos + 118, this.topPos + 28, 1.5F);
		}
		this.renderTooltip(graphics, mouseX, mouseY);
		this.updateCreateButtonState();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.updateCreateButtonState();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
