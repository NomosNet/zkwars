package com.example.client;

import com.example.block.WorkerKind;
import com.example.block.work.PenWork;
import com.example.block.work.QuarryBounds;
import com.example.block.work.WorkArea;
import com.example.client.gui.GuiButton;
import com.example.client.gui.GuiEditBox;
import com.example.client.gui.GuiStyle;
import com.example.client.gui.ZkGuiTheme;
import com.example.inventory.WorkerMenu;
import com.example.network.PenLimitPayload;
import com.example.network.QuarryBoundsPayload;
import com.example.network.WorkRadiusPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public class WorkerScreen extends AbstractContainerScreen<WorkerMenu> {
	private EditBox minX;
	private EditBox maxX;
	private EditBox minY;
	private EditBox maxY;
	private EditBox minZ;
	private EditBox maxZ;
	private EditBox radiusBox;
	private EditBox limitBox;
	private @Nullable String keptRadius;
	private @Nullable String keptLimit;

	public WorkerScreen(WorkerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		if (menu.kind() == WorkerKind.SMELTER) {
			this.imageHeight = WorkerMenu.SMELTER_IMAGE_HEIGHT;
			this.inventoryLabelY = WorkerMenu.SMELTER_INVENTORY_LABEL_Y;
		} else if (menu.kind() == WorkerKind.CRAFTER) {
			this.imageHeight = WorkerMenu.CRAFTER_IMAGE_HEIGHT;
			this.inventoryLabelY = WorkerMenu.CRAFTER_INVENTORY_LABEL_Y;
		} else if (menu.kind() == WorkerKind.QUARRY) {
			this.imageHeight = WorkerMenu.QUARRY_IMAGE_HEIGHT;
			this.inventoryLabelY = WorkerMenu.QUARRY_INVENTORY_LABEL_Y;
		} else if (menu.kind().editableRadius() || menu.kind() == WorkerKind.BUILDER) {
			this.imageHeight = WorkerMenu.RADIUS_IMAGE_HEIGHT;
			this.inventoryLabelY = WorkerMenu.RADIUS_INVENTORY_LABEL_Y;
		}
	}

	@Override
	protected void init() {
		String[] kept = this.draft();
		String radiusText = this.radiusBox == null ? this.keptRadius : this.radiusBox.getValue();
		String limitText = this.limitBox == null ? this.keptLimit : this.limitBox.getValue();
		super.init();
		ZkGuiTheme.reloadIfChanged();
		this.addRenderableWidget(new GuiButton(this.width - 62, 4, 56, 16, ZkGuiTheme.switchLabel(), button -> {
			ZkGuiTheme.toggle();
			button.setMessage(ZkGuiTheme.switchLabel());
		}));
		int x = this.leftPos;
		int y = this.topPos;
		if (this.menu.kind() == WorkerKind.QUARRY) {
			QuarryBounds.Box box = this.menu.data().bounds();
			this.minX = this.boundField(x + 8, y + 24, this.value(kept, 0, box.minX()), "gui.zk-workers.quarry.min_x");
			this.maxX = this.boundField(x + 48, y + 24, this.value(kept, 1, box.maxX()), "gui.zk-workers.quarry.max_x");
			this.minZ = this.boundField(x + 92, y + 24, this.value(kept, 2, box.minZ()), "gui.zk-workers.quarry.min_z");
			this.maxZ = this.boundField(x + 132, y + 24, this.value(kept, 3, box.maxZ()), "gui.zk-workers.quarry.max_z");
			this.minY = this.boundField(x + 8, y + 48, this.value(kept, 4, box.minY()), "gui.zk-workers.quarry.min_y");
			this.maxY = this.boundField(x + 48, y + 48, this.value(kept, 5, box.maxY()), "gui.zk-workers.quarry.max_y");
			this.addRenderableWidget(new GuiButton(x + 92, y + 48, 76, 12, Component.translatable("gui.zk-workers.quarry.apply"), button -> this.applyBounds()));
		} else if (this.menu.kind().editableRadius()) {
			String value = radiusText != null ? radiusText : Integer.toString(this.menu.data().radius());
			this.radiusBox = this.boundField(x + 8, y + 22, value, "gui.zk-workers.radius");
			this.radiusBox.setMaxLength(2);
			this.radiusBox.setFilter(WorkerScreen::radiusText);
			if (this.menu.kind() == WorkerKind.LUMBERJACK) {
				this.radiusBox.setTooltip(Tooltip.create(Component.translatable("gui.zk-workers.radius.lumber")));
			}
			if (this.menu.kind().isPen()) {
				String limitValue = limitText != null ? limitText : Integer.toString(this.menu.data().adultLimit());
				this.limitBox = this.boundField(x + 128, y + 22, limitValue, "gui.zk-workers.pen.limit");
				this.limitBox.setMaxLength(2);
				this.limitBox.setFilter(WorkerScreen::radiusText);
				this.limitBox.setTooltip(Tooltip.create(Component.translatable("gui.zk-workers.pen.limit.tooltip")));
			}
			this.addRenderableWidget(new GuiButton(x + 48, y + 22, 76, 12, Component.translatable("gui.zk-workers.quarry.apply"), button -> this.applyRadius()));
		} else if (this.menu.kind() == WorkerKind.BUILDER) {
			this.addRenderableWidget(new GuiButton(x + 8, y + 18, 120, 14, Component.translatable("gui.zk-workers.builder.capture"), button -> LitematicaCapture.capture(this.menu.pos())));
		}
		this.keptRadius = radiusText;
		this.keptLimit = limitText;
		this.syncPreview();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.syncPreview();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.syncPreview();
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public void removed() {
		QuarryOutline.clear();
		super.removed();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		GuiStyle.drawPaperPanel(graphics, x, y, this.imageWidth, this.imageHeight);
		for (Slot slot : this.menu.slots) {
			GuiStyle.drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, GuiStyle.TITLE, false);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, GuiStyle.INK_MUTED, false);
		if (this.menu.kind() == WorkerKind.SMELTER) {
			this.drawBoundLabel(graphics, "gui.zk-workers.smelter.input", 8, WorkerMenu.SMELTER_INPUT_LABEL_Y);
			this.drawBoundLabel(graphics, "gui.zk-workers.smelter.fuel", 8, WorkerMenu.SMELTER_FUEL_LABEL_Y);
			this.drawBoundLabel(graphics, "gui.zk-workers.smelter.output", 8, WorkerMenu.SMELTER_OUTPUT_LABEL_Y);
			return;
		}
		if (this.menu.kind() == WorkerKind.CRAFTER) {
			this.drawBoundLabel(graphics, "gui.zk-workers.crafter.pattern", 8, WorkerMenu.CRAFTER_PATTERN_LABEL_Y);
			this.drawBoundLabel(graphics, "gui.zk-workers.crafter.resources", 8, WorkerMenu.CRAFTER_RESOURCE_LABEL_Y);
			this.drawBoundLabel(graphics, "gui.zk-workers.crafter.output", 8, WorkerMenu.CRAFTER_OUTPUT_LABEL_Y);
			return;
		}
		if (this.menu.kind() == WorkerKind.FARMER) {
			this.drawBoundLabel(graphics, "gui.zk-workers.farmer.seeds", 8, 9);
			Component radius = Component.translatable("gui.zk-workers.radius.value", WorkArea.DEFAULT_RADIUS);
			graphics.drawString(this.font, radius, this.imageWidth - 8 - this.font.width(radius), this.titleLabelY, GuiStyle.INK_MUTED, false);
			return;
		}
		if (this.menu.kind().editableRadius()) {
			this.drawBoundLabel(graphics, "gui.zk-workers.radius", 8, 12);
			if (this.menu.kind().isPen()) {
				this.drawBoundLabel(graphics, "gui.zk-workers.pen.limit", 128, 12);
			}
			return;
		}
		if (this.menu.kind() != WorkerKind.QUARRY) {
			return;
		}
		this.drawBoundLabel(graphics, "gui.zk-workers.quarry.min_x", 8, 15);
		this.drawBoundLabel(graphics, "gui.zk-workers.quarry.max_x", 48, 15);
		this.drawBoundLabel(graphics, "gui.zk-workers.quarry.min_z", 92, 15);
		this.drawBoundLabel(graphics, "gui.zk-workers.quarry.max_z", 132, 15);
		this.drawBoundLabel(graphics, "gui.zk-workers.quarry.min_y", 8, 39);
		this.drawBoundLabel(graphics, "gui.zk-workers.quarry.max_y", 48, 39);
	}

	private void applyBounds() {
		QuarryBounds.Box box = this.currentBounds();
		this.minX.setValue(Integer.toString(box.minX()));
		this.maxX.setValue(Integer.toString(box.maxX()));
		this.minY.setValue(Integer.toString(box.minY()));
		this.maxY.setValue(Integer.toString(box.maxY()));
		this.minZ.setValue(Integer.toString(box.minZ()));
		this.maxZ.setValue(Integer.toString(box.maxZ()));
		ClientPlayNetworking.send(new QuarryBoundsPayload(
				this.menu.pos(),
				box.minX(),
				box.maxX(),
				box.minY(),
				box.maxY(),
				box.minZ(),
				box.maxZ()
		));
		this.syncPreview();
	}

	private void applyRadius() {
		int radius = this.currentRadius();
		this.radiusBox.setValue(Integer.toString(radius));
		this.keptRadius = this.radiusBox.getValue();
		ClientPlayNetworking.send(new WorkRadiusPayload(this.menu.pos(), radius));
		if (this.menu.kind().isPen() && this.limitBox != null) {
			int limit = this.currentLimit();
			this.limitBox.setValue(Integer.toString(limit));
			this.keptLimit = this.limitBox.getValue();
			ClientPlayNetworking.send(new PenLimitPayload(this.menu.pos(), limit));
		}
		this.syncPreview();
	}

	private void syncPreview() {
		WorkerKind kind = this.menu.kind();
		if (kind == WorkerKind.QUARRY) {
			if (this.minX == null) {
				return;
			}
			QuarryOutline.show(this.menu.pos(), this.currentBounds());
			return;
		}
		if (kind == WorkerKind.FARMER) {
			QuarryOutline.show(this.menu.pos(), WorkArea.box(kind, WorkArea.DEFAULT_RADIUS));
			return;
		}
		if (kind.editableRadius()) {
			QuarryOutline.show(this.menu.pos(), WorkArea.box(kind, this.currentRadius()));
		}
	}

	private int currentRadius() {
		return WorkArea.clampRadius(this.menu.kind(), this.field(this.radiusBox, this.menu.data().radius()));
	}

	private int currentLimit() {
		return PenWork.clampAdults(this.field(this.limitBox, this.menu.data().adultLimit()));
	}

	private QuarryBounds.Box currentBounds() {
		QuarryBounds.Box fallback = this.menu.data().bounds();
		return QuarryBounds.clamp(
				this.field(this.minX, fallback.minX()),
				this.field(this.maxX, fallback.maxX()),
				this.field(this.minY, fallback.minY()),
				this.field(this.maxY, fallback.maxY()),
				this.field(this.minZ, fallback.minZ()),
				this.field(this.maxZ, fallback.maxZ())
		);
	}

	private int field(EditBox box, int fallback) {
		if (box == null) {
			return fallback;
		}
		String text = box.getValue().trim();
		if (text.isEmpty() || text.equals("-")) {
			return fallback;
		}
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private EditBox boundField(int x, int y, String value, String translation) {
		EditBox box = new GuiEditBox(this.font, x, y, 36, 12, Component.translatable(translation));
		box.setMaxLength(6);
		box.setFilter(WorkerScreen::integerText);
		box.setValue(value);
		box.setTooltip(Tooltip.create(Component.translatable(translation)));
		this.addRenderableWidget(box);
		return box;
	}

	private void drawBoundLabel(GuiGraphics graphics, String key, int x, int y) {
		graphics.drawString(this.font, Component.translatable(key), x, y, GuiStyle.INK_MUTED, false);
	}

	private String value(String[] kept, int index, int fallback) {
		if (kept != null && index < kept.length && kept[index] != null) {
			return kept[index];
		}
		return Integer.toString(fallback);
	}

	private String[] draft() {
		if (this.minX == null) {
			return null;
		}
		return new String[] {
				this.minX.getValue(),
				this.maxX.getValue(),
				this.minZ.getValue(),
				this.maxZ.getValue(),
				this.minY.getValue(),
				this.maxY.getValue()
		};
	}

	private static boolean integerText(String text) {
		return text.isEmpty() || text.equals("-") || text.matches("-?\\d{0,5}");
	}

	private static boolean radiusText(String text) {
		return text.isEmpty() || text.matches("\\d{0,2}");
	}
}
