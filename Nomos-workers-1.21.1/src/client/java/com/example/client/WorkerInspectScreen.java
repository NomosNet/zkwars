package com.example.client;

import com.example.client.gui.GuiButton;
import com.example.client.gui.GuiStyle;
import com.example.client.gui.ZkGuiTheme;
import com.example.entity.HumanWorkerEntity;
import com.example.inventory.WorkerInspectMenu;
import com.example.item.CourierScrollItem;
import com.example.item.ScrollData;
import com.example.item.SmelterScrollItem;
import com.example.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class WorkerInspectScreen extends AbstractContainerScreen<WorkerInspectMenu> {
	public WorkerInspectScreen(WorkerInspectMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = WorkerInspectMenu.IMAGE_WIDTH;
		this.imageHeight = WorkerInspectMenu.IMAGE_HEIGHT;
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = WorkerInspectMenu.PLAYER_X;
		this.inventoryLabelY = WorkerInspectMenu.INVENTORY_LABEL_Y;
	}

	@Override
	protected void init() {
		super.init();
		ZkGuiTheme.reloadIfChanged();
		this.addRenderableWidget(new GuiButton(this.width - 62, 4, 56, 16, ZkGuiTheme.switchLabel(), button -> {
			ZkGuiTheme.toggle();
			button.setMessage(ZkGuiTheme.switchLabel());
		}));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		GuiStyle.drawPaperPanel(graphics, x, y, this.imageWidth, this.imageHeight);
		int portraitX = x + WorkerInspectMenu.PORTRAIT_X;
		int portraitY = y + WorkerInspectMenu.PORTRAIT_Y;
		GuiStyle.drawPortraitFrame(graphics, portraitX, portraitY, WorkerInspectMenu.PORTRAIT_W, WorkerInspectMenu.PORTRAIT_H);
		for (Slot slot : this.menu.slots) {
			GuiStyle.drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
		}
		HumanWorkerEntity worker = this.menu.worker();
		if (worker != null && worker.isAlive()) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(
					graphics,
					portraitX + 2,
					portraitY + 2,
					portraitX + WorkerInspectMenu.PORTRAIT_W - 2,
					portraitY + WorkerInspectMenu.PORTRAIT_H - 2,
					26,
					0.0625F,
					mouseX,
					mouseY,
					worker
			);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, GuiStyle.TITLE, false);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, GuiStyle.INK_MUTED, false);
		Component heldLabel = Component.translatable(this.menu.data().courier() || this.menu.data().smelter()
				? "gui.zk-workers.inspect.cargo"
				: "gui.zk-workers.inspect.tool");
		graphics.drawString(this.font, heldLabel, WorkerInspectMenu.HELD_X, WorkerInspectMenu.HELD_Y - 12, GuiStyle.INK_MUTED, false);
		graphics.drawString(this.font, Component.translatable("gui.zk-workers.inspect.scroll"), WorkerInspectMenu.SCROLL_X, WorkerInspectMenu.SCROLL_Y - 12, GuiStyle.INK_MUTED, false);
		int lineY = WorkerInspectMenu.TEXT_Y;
		graphics.drawString(this.font, Component.translatable(this.menu.data().statusKey()), WorkerInspectMenu.TEXT_X, lineY, GuiStyle.INK, false);
		lineY += WorkerInspectMenu.LINE;
		ItemStack scroll = this.menu.getSlot(this.menu.scrollSlot()).getItem();
		if (scroll.isEmpty()) {
			graphics.drawString(this.font, Component.translatable("gui.zk-workers.inspect.no_order"), WorkerInspectMenu.TEXT_X, lineY, GuiStyle.INK, false);
			return;
		}
		for (Component line : this.orderLines(scroll)) {
			graphics.drawString(this.font, line, WorkerInspectMenu.TEXT_X, lineY, GuiStyle.INK, false);
			lineY += WorkerInspectMenu.LINE;
		}
	}

	private Component[] orderLines(ItemStack scroll) {
		CompoundTag tag = ScrollData.get(scroll);
		if (scroll.getItem() instanceof SmelterScrollItem) {
			return new Component[] {
					posLine(ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS), "gui.zk-workers.inspect.work_block", "gui.zk-workers.inspect.no_work_block"),
					Component.translatable("gui.zk-workers.inspect.furnaces", ScrollData.furnaceList(tag).size()),
					posLine(ScrollData.pos(tag, ScrollData.BED_POS), "gui.zk-workers.inspect.bed", "gui.zk-workers.inspect.no_bed")
			};
		}
		if (scroll.getItem() instanceof CourierScrollItem) {
			return new Component[] {
					posLine(ScrollData.pos(tag, ScrollData.CHEST_POS), "gui.zk-workers.inspect.chest", "gui.zk-workers.inspect.no_chest"),
					Component.translatable("gui.zk-workers.inspect.route", ScrollData.blockList(tag).size()),
					posLine(ScrollData.pos(tag, ScrollData.BED_POS), "gui.zk-workers.inspect.bed", "gui.zk-workers.inspect.no_bed"),
					Component.translatable(orderKey(scroll))
			};
		}
		return new Component[] {
				posLine(ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS), "gui.zk-workers.inspect.work_block", "gui.zk-workers.inspect.no_work_block"),
				posLine(ScrollData.pos(tag, ScrollData.BED_POS), "gui.zk-workers.inspect.bed", "gui.zk-workers.inspect.no_bed")
		};
	}

	private static String orderKey(ItemStack scroll) {
		if (scroll.is(ModItems.COURIER_SORT_SCROLL)) {
			return "gui.zk-workers.inspect.sort";
		}
		if (scroll.is(ModItems.COURIER_SUPPLY_SCROLL)) {
			return "gui.zk-workers.inspect.supply";
		}
		return "gui.zk-workers.inspect.delivery";
	}

	private static Component posLine(Optional<BlockPos> pos, String presentKey, String missingKey) {
		return pos.map(value -> Component.translatable(presentKey, value.getX(), value.getY(), value.getZ()))
				.orElseGet(() -> Component.translatable(missingKey));
	}
}
