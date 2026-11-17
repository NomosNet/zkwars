package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.hire.NomosikHire;
import com.nomosiki.network.unit.payload.c2s.NomosikHirePayload;
import com.nomosiki.screen.HireScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HireScreen extends AbstractContainerScreen<HireScreenHandler> {
	private static final int PANEL_SIZE = 256;

	private Button hireButton;

	public HireScreen(HireScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = PANEL_SIZE;
		this.imageHeight = PANEL_SIZE;
		this.inventoryLabelX = HireScreenHandler.PLAYER_LEFT;
		this.inventoryLabelY = HireScreenHandler.INVENTORY_LABEL_Y;
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

		int entityId = this.menu.getNomosik().getId();
		this.hireButton = this.addRenderableWidget(new SakuraButton(this.leftPos + 80, this.topPos + 118, 96, 20,
			Component.translatable("button.nomosiki.hire.confirm"), b ->
				ClientPlayNetworking.send(new NomosikHirePayload(entityId))));
		this.updateHireButtonState();
	}

	private void updateHireButtonState() {
		if (this.hireButton == null || this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		boolean canAfford = NomosikHire.countEmeralds(this.minecraft.player.getInventory()) >= NomosikHire.EMERALD_COST;
		this.hireButton.active = canAfford;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		SakuraStyle.drawPaperPanel(graphics, x, y, this.imageWidth, this.imageHeight);
		SakuraStyle.drawPortraitFrame(graphics, x + 15, y + 20, 100, 85);
		SakuraStyle.drawSlots(graphics, this.menu.slots, x, y);
		NomosikEntity nomosik = this.menu.getNomosik();
		if (nomosik.isAlive()) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 15, y + 20, x + 115, y + 105, 32,
				0.0625F, (float) mouseX, (float) mouseY, nomosik);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		NomosikEntity nomosik = this.menu.getNomosik();
		int sx = 132;
		int sy = 28;
		graphics.drawString(this.font, nomosik.getDisplayName(), sx, sy, SakuraStyle.TITLE, false);
		sy += 14;
		graphics.drawString(this.font, Component.translatable("screen.nomosiki.hire.stat_race",
			Component.translatable(nomosik.getNomosikRace().translationKey())), sx, sy, SakuraStyle.INK, false);
		sy += 12;
		graphics.drawString(this.font, Component.translatable("screen.nomosiki.hire.stat_hp",
			String.format("%.0f / %.0f", nomosik.getHealth(), nomosik.getMaxHealth())), sx, sy, SakuraStyle.INK, false);
		sy += 12;
		graphics.drawString(this.font, Component.translatable("screen.nomosiki.hire.stat_armor",
			String.valueOf(nomosik.getArmorValue())), sx, sy, SakuraStyle.INK, false);

		int emeraldCount = this.minecraft != null && this.minecraft.player != null
			? NomosikHire.countEmeralds(this.minecraft.player.getInventory())
			: 0;
		int priceColor = emeraldCount >= NomosikHire.EMERALD_COST ? SakuraStyle.ACCENT : SakuraStyle.INK_MUTED;
		SakuraStyle.drawCentered(graphics, this.font,
			Component.translatable("screen.nomosiki.hire.price", NomosikHire.EMERALD_COST),
			128, 108, priceColor);
		SakuraStyle.drawCentered(graphics, this.font,
			Component.translatable("screen.nomosiki.hire.emeralds_owned", emeraldCount),
			128, 132, SakuraStyle.INK_MUTED);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, SakuraStyle.INK_MUTED, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.updateHireButtonState();
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}
}
