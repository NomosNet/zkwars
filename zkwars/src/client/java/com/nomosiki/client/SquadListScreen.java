package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.network.squad.payload.s2c.SquadStatePayload;
import com.nomosiki.network.squad.payload.c2s.sync.SquadStateRequestPayload;
import com.nomosiki.squad.SquadIcons;
import com.nomosiki.squad.SquadType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SquadListScreen extends Screen {
	private static final int ROW = 18;

	private final @Nullable Screen parent;
	private List<SquadStatePayload.Brief> squads = List.of();
	private int listScroll;
	private int selectedIndex = -1;
	private int listLeft;
	private int listTop;
	private int listWidth;
	private int listHeight;
	private int visibleRows = 6;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public SquadListScreen(@Nullable Screen parent) {
		super(Component.translatable("screen.nomosiki.squad_list.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientPlayNetworking.send(new SquadStateRequestPayload());
		int cx = this.width / 2;
		this.listLeft = cx - 118;
		this.listWidth = 236;
		this.listTop = 44;
		this.panelLeft = this.listLeft - 10;
		this.panelTop = 8;
		this.panelWidth = this.listWidth + 20;
		int actionsTop = this.height - 100;
		int maxList = actionsTop - 8 - this.listTop;
		this.visibleRows = Math.max(1, maxList / ROW);
		this.listHeight = this.visibleRows * ROW;
		int third = (this.listWidth - 8) / 3;
		int btnY = actionsTop;
		this.addRenderableWidget(new SakuraButton(this.listLeft, btnY, third, 20,
			Component.translatable("screen.nomosiki.squad_list.add"), b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new SquadEditScreen(this, null));
				}
			}));
		this.addRenderableWidget(new SakuraButton(this.listLeft + third + 4, btnY, third, 20,
			Component.translatable("screen.nomosiki.squad_list.edit"), b -> {
				SquadStatePayload.Brief sel = this.selectedBrief();
				if (this.minecraft != null && sel != null) {
					this.minecraft.setScreen(new SquadEditScreen(this, sel));
				}
			}));
		this.addRenderableWidget(new SakuraButton(this.listLeft + 2 * (third + 4), btnY, third, 20,
			Component.translatable("screen.nomosiki.squad_list.remove"), b -> {
				SquadStatePayload.Brief sel = this.selectedBrief();
				if (sel != null) {
					SquadCommandClient.disbandSquad(sel.squadId());
					this.selectedIndex = -1;
				}
			}));
		btnY += 24;
		this.addRenderableWidget(new SakuraButton(this.listLeft, btnY, this.listWidth, 20,
			Component.translatable("screen.nomosiki.squad_list.set_active"), b -> {
				SquadStatePayload.Brief sel = this.selectedBrief();
				if (sel != null) {
					SquadCommandClient.setActiveSquad(sel.squadId());
				}
			}));
		btnY += 24;
		this.addRenderableWidget(new SakuraButton(cx - 50, btnY, 100, 20,
			Component.translatable("screen.nomosiki.squad_list.back"), b -> this.onClose()));
		this.panelHeight = btnY + 24 - this.panelTop;
	}

	@Override
	public void tick() {
		this.squads = new ArrayList<>(SquadClientState.squads());
		if (this.selectedIndex >= this.squads.size()) {
			this.selectedIndex = -1;
		}
	}

	private @Nullable SquadStatePayload.Brief selectedBrief() {
		if (this.selectedIndex < 0 || this.selectedIndex >= this.squads.size()) {
			return null;
		}
		return this.squads.get(this.selectedIndex);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawDim(graphics, this.width, this.height);
		if (this.panelWidth > 0 && this.panelHeight > 0) {
			SakuraStyle.drawPaperPanel(graphics, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, 16, SakuraStyle.TITLE);
		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_list.column_name"),
			this.listLeft + 22, this.listTop - 12, SakuraStyle.INK_MUTED);
		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_list.column_type"),
			this.listLeft + 120, this.listTop - 12, SakuraStyle.INK_MUTED);
		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_list.column_count"),
			this.listLeft + this.listWidth - 40, this.listTop - 12, SakuraStyle.INK_MUTED);

		List<SquadStatePayload.Brief> rows = this.squads;
		int maxScroll = Math.max(0, rows.size() - this.visibleRows);
		this.listScroll = Mth.clamp(this.listScroll, 0, maxScroll);
		Optional<UUID> active = SquadClientState.activeSquadId();
		graphics.enableScissor(this.listLeft, this.listTop, this.listLeft + this.listWidth, this.listTop + this.listHeight);
		for (int i = 0; i < this.visibleRows; i++) {
			int idx = i + this.listScroll;
			if (idx >= rows.size()) {
				break;
			}
			SquadStatePayload.Brief b = rows.get(idx);
			int y = this.listTop + i * ROW;
			boolean sel = idx == this.selectedIndex;
			boolean hovering = mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth && mouseY >= y && mouseY < y + ROW;
			SakuraStyle.drawListRow(graphics, this.listLeft, y, this.listWidth, ROW, sel, hovering);
			graphics.blit(SquadIcons.byId(b.iconId()), this.listLeft + 2, y + 1, 0, 0, 16, 16, 16, 16);
			String dispName = b.name().isEmpty()
				? b.squadId().toString().substring(0, 8) + "…"
				: this.font.plainSubstrByWidth(SquadLabels.text(b.name()), 70);
			graphics.drawString(this.font, dispName, this.listLeft + 22, y + 5, SakuraStyle.INK, false);
			Component type = Component.translatable(SquadType.fromId(b.typeId()).translationKey());
			graphics.drawString(this.font, this.font.plainSubstrByWidth(type.getString(), 80), this.listLeft + 120, y + 5, SakuraStyle.INK_MUTED, false);
			graphics.drawString(this.font, Integer.toString(b.memberCount()), this.listLeft + this.listWidth - 32, y + 5, SakuraStyle.INK, false);
			if (active.filter(id -> id.equals(b.squadId())).isPresent()) {
				graphics.drawString(this.font, "★", this.listLeft + this.listWidth - 12, y + 4, SakuraStyle.FRAME_GOLD_HI, false);
			}
		}
		if (rows.isEmpty()) {
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_list.empty"),
				this.listLeft + 8, this.listTop + 8, SakuraStyle.INK_MUTED);
		}
		graphics.disableScissor();
		SakuraStyle.drawScrollbar(graphics, this.listLeft + this.listWidth + 2, this.listTop, this.listHeight,
			this.listScroll, this.visibleRows, rows.size());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseY >= this.listTop && mouseY < this.listTop + this.listHeight
			&& mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth) {
			int row = (int) ((mouseY - this.listTop) / ROW);
			int idx = row + this.listScroll;
			if (idx >= 0 && idx < this.squads.size()) {
				this.selectedIndex = idx;
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth + 8
			&& mouseY >= this.listTop && mouseY < this.listTop + this.listHeight
			&& this.squads.size() > this.visibleRows) {
			int maxScroll = this.squads.size() - this.visibleRows;
			this.listScroll = Mth.clamp(this.listScroll - (int) Math.round(scrollY * 3), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
