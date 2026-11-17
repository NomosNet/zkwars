package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraEditBox;
import com.nomosiki.client.gui.SakuraSlider;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.hire.NomosikHire;
import com.nomosiki.network.shop.payload.c2s.TesterSquadShopBuyPayload;
import com.nomosiki.screen.TesterSquadShopScreenHandler;
import com.nomosiki.shop.TesterSquadShop;
import com.nomosiki.squad.SquadType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class TesterSquadShopScreen extends AbstractContainerScreen<TesterSquadShopScreenHandler> {
	private EditBox nameBox;
	private Button buyButton;
	private int typeOrdinal;
	private int unitCount = TesterSquadShop.DEFAULT_UNITS;

	public TesterSquadShopScreen(TesterSquadShopScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = TesterSquadShopScreenHandler.IMAGE_WIDTH;
		this.imageHeight = TesterSquadShopScreenHandler.IMAGE_HEIGHT;
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = TesterSquadShopScreenHandler.PLAYER_LEFT;
		this.inventoryLabelY = TesterSquadShopScreenHandler.INVENTORY_LABEL_Y;
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

		int x = this.leftPos + 8;
		int width = this.imageWidth - 16;

		this.nameBox = new SakuraEditBox(this.font, x, this.topPos + 28, width, 18,
			Component.translatable("screen.nomosiki.tester_shop.name_label"));
		this.nameBox.setMaxLength(32);
		this.nameBox.setHint(Component.translatable("screen.nomosiki.tester_shop.name_placeholder"));
		this.addRenderableWidget(this.nameBox);

		this.addRenderableWidget(new SakuraButton(x, this.topPos + 62, width, 20,
			typeButtonLabel(), b -> {
				this.typeOrdinal = (this.typeOrdinal + 1) % SquadType.playableValues().size();
				b.setMessage(typeButtonLabel());
				this.updateBuyButtonState();
			}));

		double normalized = countToNormalized(this.unitCount);
		this.addRenderableWidget(new SakuraSlider(x, this.topPos + 86, width, 20, normalized, new SakuraSlider.Listener() {
			@Override
			public Component label(double value) {
				return Component.translatable("screen.nomosiki.tester_shop.count", countFromNormalized(value));
			}

			@Override
			public void apply(double value) {
				TesterSquadShopScreen.this.unitCount = countFromNormalized(value);
				TesterSquadShopScreen.this.updateBuyButtonState();
			}
		}));

		this.buyButton = this.addRenderableWidget(new SakuraButton(x, this.topPos + 126, width, 20,
			Component.translatable("button.nomosiki.tester_shop.buy"), b -> this.buy()));
		this.updateBuyButtonState();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.closeContainer();
		}
		if (this.nameBox != null && (this.nameBox.keyPressed(keyCode, scanCode, modifiers) || this.nameBox.canConsumeInput())) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void buy() {
		ClientPlayNetworking.send(new TesterSquadShopBuyPayload(
			this.menu.getShopPos(),
			this.nameBox.getValue(),
			selectedType().id(),
			this.unitCount
		));
	}

	private Component typeButtonLabel() {
		return Component.translatable(
			"screen.nomosiki.tester_shop.type_next",
			Component.translatable(selectedType().translationKey())
		);
	}

	private SquadType selectedType() {
		var playable = SquadType.playableValues();
		return playable.get(Mth.clamp(this.typeOrdinal, 0, playable.size() - 1));
	}

	private static int countFromNormalized(double normalized) {
		int max = TesterSquadShop.MAX_UNITS;
		return 1 + (int) Math.round(Mth.clamp(normalized, 0.0, 1.0) * (max - 1));
	}

	private static double countToNormalized(int count) {
		int clamped = TesterSquadShop.clampCount(count);
		return (clamped - 1) / (double) (TesterSquadShop.MAX_UNITS - 1);
	}

	private void updateBuyButtonState() {
		if (this.buyButton == null || this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		int cost = TesterSquadShop.costFor(this.unitCount);
		boolean canAfford = NomosikHire.countEmeralds(this.minecraft.player.getInventory()) >= cost;
		this.buyButton.active = canAfford;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		SakuraStyle.drawPaperPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
		SakuraStyle.drawSlots(graphics, this.menu.slots, this.leftPos, this.topPos);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, SakuraStyle.TITLE, false);
		SakuraStyle.drawLabel(graphics, this.font,
			Component.translatable("screen.nomosiki.tester_shop.name_label"),
			8, 18, SakuraStyle.INK_MUTED);
		SakuraStyle.drawLabel(graphics, this.font,
			Component.translatable("screen.nomosiki.tester_shop.type_label"),
			8, 52, SakuraStyle.INK_MUTED);

		int emeraldCount = this.minecraft != null && this.minecraft.player != null
			? NomosikHire.countEmeralds(this.minecraft.player.getInventory())
			: 0;
		int cost = TesterSquadShop.costFor(this.unitCount);
		int priceColor = emeraldCount >= cost ? SakuraStyle.ACCENT : SakuraStyle.INK_MUTED;
		SakuraStyle.drawCentered(graphics, this.font,
			Component.translatable("screen.nomosiki.tester_shop.price", cost),
			this.imageWidth / 2, 108, priceColor);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, SakuraStyle.INK_MUTED, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.updateBuyButtonState();
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		int emeraldCount = this.minecraft != null && this.minecraft.player != null
			? NomosikHire.countEmeralds(this.minecraft.player.getInventory())
			: 0;
		SakuraStyle.drawCentered(graphics, this.font,
			Component.translatable("screen.nomosiki.hire.emeralds_owned", emeraldCount),
			this.leftPos + this.imageWidth / 2, this.topPos + 118, SakuraStyle.INK_MUTED);
		this.renderTooltip(graphics, mouseX, mouseY);
	}
}
