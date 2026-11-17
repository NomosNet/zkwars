package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.network.faction.FactionDiplomacyRow;
import com.nomosiki.network.faction.payload.c2s.FactionInviteResponsePayload;
import com.nomosiki.network.faction.payload.c2s.FactionLeavePayload;
import com.nomosiki.network.faction.payload.c2s.FactionOpenCreateMenuPayload;
import com.nomosiki.network.faction.payload.c2s.FactionOpenScreenPayload;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public final class FactionListScreen extends Screen {
	private static final int ROW_HEIGHT = 34;
	private static final int BUTTON_HEIGHT = 20;
	private static final int STATUS_OFFSET = 22;
	private static final int INVITE_ROW = 22;
	private static final int INVITE_HEADER = 12;

	private static int lastScroll;
	private static int lastInviteScroll;

	private int scroll;
	private int inviteScroll;
	private int listLeft;
	private int listTop;
	private int listHeight;
	private int visibleRows = 1;
	private int invitesTop;
	private int invitesHeight;
	private int invitesVisible;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public FactionListScreen() {
		super(Component.translatable("screen.nomosiki.faction.list_title"));
		this.scroll = lastScroll;
		this.inviteScroll = lastInviteScroll;
	}

	@Override
	protected void init() {
		FactionScreenDataPayload data = FactionClientState.lastScreenData();
		this.listLeft = this.width / 2 - 130;
		this.panelLeft = this.listLeft - 10;
		this.panelTop = 6;
		this.panelWidth = 260;
		this.panelHeight = this.height - 12;

		int y = data.factionName().isBlank() ? 56 : 36;
		var invites = data.invites();
		if (invites.isEmpty()) {
			this.invitesTop = 0;
			this.invitesHeight = 0;
			this.invitesVisible = 0;
		} else {
			if (!data.factionName().isBlank()) {
				y = 48;
			}
			this.invitesVisible = Math.min(3, invites.size());
			int maxInviteScroll = Math.max(0, invites.size() - this.invitesVisible);
			this.inviteScroll = Mth.clamp(this.inviteScroll, 0, maxInviteScroll);
			lastInviteScroll = this.inviteScroll;
			this.invitesTop = y + INVITE_HEADER;
			this.invitesHeight = this.invitesVisible * INVITE_ROW;
			for (int i = 0; i < this.invitesVisible; i++) {
				int idx = i + this.inviteScroll;
				if (idx >= invites.size()) {
					break;
				}
				var invite = invites.get(idx);
				int rowY = this.invitesTop + i * INVITE_ROW;
				this.addRenderableWidget(new SakuraButton(this.listLeft + 112, rowY, 62, 20,
					Component.translatable("button.nomosiki.faction.join"), b -> {
						ClientPlayNetworking.send(new FactionInviteResponsePayload(invite.factionName(), true));
						ClientPlayNetworking.send(new FactionOpenScreenPayload());
					}));
				this.addRenderableWidget(new SakuraButton(this.listLeft + 176, rowY, 64, 20,
					Component.translatable("button.nomosiki.faction.decline_invite"), b -> {
						ClientPlayNetworking.send(new FactionInviteResponsePayload(invite.factionName(), false));
						ClientPlayNetworking.send(new FactionOpenScreenPayload());
					}));
			}
			y = this.invitesTop + this.invitesHeight + 6;
		}
		if (data.factionName().isBlank()) {
			this.addRenderableWidget(new SakuraButton(this.listLeft, y, 240, BUTTON_HEIGHT,
				Component.translatable("button.nomosiki.faction.create"), b ->
					ClientPlayNetworking.send(new FactionOpenCreateMenuPayload())));
			y += 28;
		} else if (data.isOwner()) {
			this.addRenderableWidget(new SakuraButton(this.listLeft + 150, y, 90, BUTTON_HEIGHT,
				Component.translatable("button.nomosiki.faction.manage"), b -> {
					if (this.minecraft != null) {
						this.minecraft.setScreen(new FactionScreen());
					}
				}));
			y += 24;
		} else {
			this.addRenderableWidget(new SakuraButton(this.listLeft + 150, y, 90, BUTTON_HEIGHT,
				Component.translatable("button.nomosiki.faction.leave_member"), b -> this.confirmLeave()));
			y += 24;
		}

		this.listTop = y + 10;
		int listBottom = this.panelTop + this.panelHeight - 8;
		this.visibleRows = Math.max(1, (listBottom - this.listTop) / ROW_HEIGHT);
		this.listHeight = this.visibleRows * ROW_HEIGHT;

		List<FactionDiplomacyRow> factions = data.diplomacy();
		int maxScroll = Math.max(0, factions.size() - this.visibleRows);
		this.scroll = Mth.clamp(this.scroll, 0, maxScroll);
		lastScroll = this.scroll;

		for (int i = 0; i < this.visibleRows; i++) {
			int idx = i + this.scroll;
			if (idx >= factions.size()) {
				break;
			}
			FactionDiplomacyRow row = factions.get(idx);
			int rowY = this.listTop + i * ROW_HEIGHT;
			this.addRenderableWidget(new SakuraButton(this.listLeft + 20, rowY, 220, BUTTON_HEIGHT,
				Component.literal(row.factionName()),
				b -> {
					ClientPlayNetworking.send(new FactionOpenScreenPayload());
					if (this.minecraft != null) {
						this.minecraft.setScreen(new FactionDiplomacyScreen(row.factionName()));
					}
				}));
		}
	}

	private void confirmLeave() {
		if (this.minecraft == null) {
			return;
		}
		String faction = FactionClientState.lastScreenData().factionName();
		this.minecraft.setScreen(new ConfirmScreen(
			confirmed -> {
				if (confirmed) {
					ClientPlayNetworking.send(new FactionLeavePayload());
					ClientPlayNetworking.send(new FactionOpenScreenPayload());
				}
				if (this.minecraft != null) {
					this.minecraft.setScreen(new FactionListScreen());
				}
			},
			Component.translatable("message.nomosiki.faction.leave_confirm", faction),
			Component.empty()
		));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		FactionScreenDataPayload data = FactionClientState.lastScreenData();
		List<FactionDiplomacyRow> factions = data.diplomacy();
		if (this.invitesVisible > 0
			&& mouseX >= this.panelLeft && mouseX < this.panelLeft + this.panelWidth
			&& mouseY >= this.invitesTop && mouseY < this.invitesTop + this.invitesHeight
			&& data.invites().size() > this.invitesVisible) {
			int maxInviteScroll = Math.max(0, data.invites().size() - this.invitesVisible);
			this.inviteScroll = Mth.clamp(this.inviteScroll - (int) Math.signum(scrollY), 0, maxInviteScroll);
			lastInviteScroll = this.inviteScroll;
			this.rebuildWidgets();
			return true;
		}
		if (mouseX >= this.panelLeft && mouseX < this.panelLeft + this.panelWidth
			&& mouseY >= this.listTop && mouseY < this.listTop + this.listHeight && !factions.isEmpty()) {
			int maxScroll = Math.max(0, factions.size() - this.visibleRows);
			this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, maxScroll);
			lastScroll = this.scroll;
			this.rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
		FactionScreenDataPayload data = FactionClientState.lastScreenData();
		int left = this.listLeft;
		int y = 14;

		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, y, SakuraStyle.TITLE);
		y += 16;
		if (data.factionName().isBlank()) {
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.faction.no_faction"),
				left, y, SakuraStyle.INK_MUTED);
			y += 12;
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.faction.list_create_hint"),
				left, y, SakuraStyle.INK_MUTED);
		} else {
			Component current = Component.translatable("screen.nomosiki.faction.current", data.factionName());
			graphics.drawString(this.font, this.font.plainSubstrByWidth(current.getString(), 146),
				left, y, SakuraStyle.INK, false);
		}

		if (this.invitesVisible > 0) {
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.faction.invites_incoming"),
				left, this.invitesTop - INVITE_HEADER, SakuraStyle.TITLE);
			graphics.enableScissor(this.panelLeft + 4, this.invitesTop, this.listLeft + 110, this.invitesTop + this.invitesHeight);
			for (int i = 0; i < this.invitesVisible; i++) {
				int idx = i + this.inviteScroll;
				if (idx >= data.invites().size()) {
					break;
				}
				var invite = data.invites().get(idx);
				Component line = Component.translatable("screen.nomosiki.faction.invite_from",
					invite.factionName(), invite.ownerName());
				graphics.drawString(this.font, this.font.plainSubstrByWidth(line.getString(), 108),
					left, this.invitesTop + i * INVITE_ROW + 6, SakuraStyle.INK, false);
			}
			graphics.disableScissor();
		}

		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.faction.list_header"),
			left, this.listTop - 12, SakuraStyle.TITLE);
		List<FactionDiplomacyRow> factions = data.diplomacy();
		if (factions.isEmpty()) {
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.faction.diplomacy_none"),
				left, this.listTop + 4, SakuraStyle.INK_MUTED);
			return;
		}
		graphics.enableScissor(this.panelLeft + 4, this.listTop, this.panelLeft + this.panelWidth - 4, this.listTop + this.listHeight);
		for (int i = 0; i < this.visibleRows; i++) {
			int idx = i + this.scroll;
			if (idx >= factions.size()) {
				break;
			}
			FactionDiplomacyRow row = factions.get(idx);
			int rowY = this.listTop + i * ROW_HEIGHT;
			graphics.blit(FactionDiplomacyUi.statusIcon(row), left + 2, rowY + 2, 0, 0, 16, 16, 16, 16);
			Component status = FactionDiplomacyUi.listStatusLine(row);
			graphics.drawString(this.font, status, left + 20, rowY + STATUS_OFFSET, FactionDiplomacyUi.diplomacyColor(row), false);
		}
		graphics.disableScissor();
		SakuraStyle.drawScrollbar(graphics, this.panelLeft + this.panelWidth - 10, this.listTop, this.listHeight,
			this.scroll, this.visibleRows, factions.size());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
