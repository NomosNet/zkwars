package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.network.squad.payload.s2c.SquadStatePayload;
import com.nomosiki.network.squad.payload.c2s.sync.SquadStateRequestPayload;
import com.nomosiki.squad.SquadIcons;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Compact squad list for assigning one nomosik from {@link NomosikScreen}: icon, name, member count;
 * top: leave / none; row click assigns then returns to parent.
 */
public class SquadPickerScreen extends Screen {
	private static final int ROW = 18;

	private final Screen parent;
	private final int entityId;
	private final Optional<UUID> currentSquad;
	private List<SquadStatePayload.Brief> squads = List.of();
	private int listScroll;
	private int listLeft;
	private int listTop;
	private int listWidth;
	private int listHeight;
	private int visibleRows = 8;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public SquadPickerScreen(Screen parent, int entityId, Optional<UUID> currentSquad) {
		super(Component.translatable("screen.nomosiki.squad_picker.title"));
		this.parent = parent;
		this.entityId = entityId;
		this.currentSquad = currentSquad;
	}

	@Override
	protected void init() {
		ClientPlayNetworking.send(new SquadStateRequestPayload());
		int cx = this.width / 2;
		this.listLeft = cx - 118;
		this.listWidth = 236;
		int noneY = 42;
		this.addRenderableWidget(new SakuraButton(this.listLeft, noneY, this.listWidth, 20,
			Component.translatable("screen.nomosiki.squad_picker.none_option"), b -> {
				SquadCommandClient.joinSquad(this.entityId, null);
				this.onClose();
			}).withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_picker.none"))));
		this.listTop = noneY + 20 + 6;
		int backY = this.height - 28;
		this.addRenderableWidget(new SakuraButton(cx - 50, backY, 100, 20,
			Component.translatable("screen.nomosiki.squad_list.back"), b -> this.onClose()));
		int maxList = backY - 8 - this.listTop - 14;
		this.visibleRows = Mth.clamp(maxList / ROW, 3, 14);
		this.listHeight = this.visibleRows * ROW;
		this.panelLeft = this.listLeft - 10;
		this.panelTop = 8;
		this.panelWidth = this.listWidth + 20;
		this.panelHeight = backY + 24 - this.panelTop;
	}

	@Override
	public void tick() {
		this.squads = new ArrayList<>(SquadClientState.squads());
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
		SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.squad_picker.hint"),
			this.width / 2, 30, SakuraStyle.INK_MUTED);
		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_picker.column_name"),
			this.listLeft + 22, this.listTop - 12, SakuraStyle.INK_MUTED);
		Component membersHeader = Component.translatable("screen.nomosiki.squad_picker.column_members");
		SakuraStyle.drawLabel(graphics, this.font, membersHeader,
			this.listLeft + this.listWidth - 6 - this.font.width(membersHeader), this.listTop - 12, SakuraStyle.INK_MUTED);

		List<SquadStatePayload.Brief> rows = this.squads;
		int maxScroll = Math.max(0, rows.size() - this.visibleRows);
		this.listScroll = Mth.clamp(this.listScroll, 0, maxScroll);
		for (int i = 0; i < this.visibleRows; i++) {
			int idx = i + this.listScroll;
			if (idx >= rows.size()) {
				break;
			}
			SquadStatePayload.Brief b = rows.get(idx);
			int y = this.listTop + i * ROW;
			boolean hovering = mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth && mouseY >= y && mouseY < y + ROW;
			boolean current = this.currentSquad.filter(id -> id.equals(b.squadId())).isPresent();
			SakuraStyle.drawListRow(graphics, this.listLeft, y, this.listWidth, ROW, current, hovering);
			graphics.blit(SquadIcons.byId(b.iconId()), this.listLeft + 2, y + 1, 0, 0, 16, 16, 16, 16);
			String dispName = b.name().isEmpty()
				? b.squadId().toString().substring(0, 8) + "…"
				: this.font.plainSubstrByWidth(SquadLabels.text(b.name()), this.listWidth - 22 - 52);
			graphics.drawString(this.font, dispName, this.listLeft + 22, y + 5, SakuraStyle.INK, false);
			String members = "(" + b.memberCount() + ")";
			graphics.drawString(this.font, members,
				this.listLeft + this.listWidth - 6 - this.font.width(members), y + 5, SakuraStyle.INK_MUTED, false);
		}
		if (rows.isEmpty()) {
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_list.empty"),
				this.listLeft + 8, this.listTop + 8, SakuraStyle.INK_MUTED);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseY >= this.listTop && mouseY < this.listTop + this.listHeight
			&& mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth) {
			int row = (int) ((mouseY - this.listTop) / ROW);
			int idx = row + this.listScroll;
			if (idx >= 0 && idx < this.squads.size()) {
				SquadStatePayload.Brief b = this.squads.get(idx);
				SquadCommandClient.joinSquad(this.entityId, b.squadId());
				this.onClose();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth
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
