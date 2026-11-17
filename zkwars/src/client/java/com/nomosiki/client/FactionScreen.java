package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraEditBox;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.network.faction.payload.c2s.FactionInvitePayload;
import com.nomosiki.network.faction.payload.c2s.FactionLeavePayload;
import com.nomosiki.network.faction.payload.c2s.FactionOpenScreenPayload;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class FactionScreen extends Screen {
	private static final int CONTENT_W = 240;
	private static final int MEMBER_ROW = 12;
	private static final int HEADER = 12;

	private static int lastMemberScroll;

	private EditBox inviteBox;
	private int memberScroll;
	private int contentLeft;
	private int membersTop;
	private int membersHeight;
	private int membersVisible = 1;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public FactionScreen() {
		super(Component.translatable("screen.nomosiki.faction.manage_title"));
		this.memberScroll = lastMemberScroll;
	}

	@Override
	protected void init() {
		FactionScreenDataPayload data = FactionClientState.lastScreenData();
		this.contentLeft = this.width / 2 - 120;
		this.panelLeft = this.contentLeft - 10;
		this.panelTop = 6;
		this.panelWidth = 260;
		this.panelHeight = this.height - 12;

		this.addRenderableWidget(new SakuraButton(this.contentLeft, 32, 60, 20,
			Component.translatable("button.nomosiki.faction.back"), b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new FactionListScreen());
				}
			}));

		int inviteY = 56;
		this.inviteBox = new SakuraEditBox(this.font, this.contentLeft, inviteY, 154, 20,
			Component.translatable("screen.nomosiki.faction.invite_name"));
		this.inviteBox.setMaxLength(32);
		this.addRenderableWidget(this.inviteBox);
		this.addRenderableWidget(new SakuraButton(this.contentLeft + 158, inviteY, 82, 20,
			Component.translatable("button.nomosiki.faction.invite"), b -> {
				ClientPlayNetworking.send(new FactionInvitePayload(this.inviteBox.getValue()));
				this.inviteBox.setValue("");
				ClientPlayNetworking.send(new FactionOpenScreenPayload());
			}));

		int leaveY = this.panelTop + this.panelHeight - 28;
		this.addRenderableWidget(new SakuraButton(this.contentLeft, leaveY, CONTENT_W, 20,
			Component.translatable("button.nomosiki.faction.leave"), b -> this.confirmLeave()));

		int listsTop = inviteY + 24;
		int listsBottom = leaveY - 6;
		this.membersTop = listsTop + HEADER;
		this.membersVisible = Math.max(1, (listsBottom - this.membersTop) / MEMBER_ROW);
		this.membersHeight = this.membersVisible * MEMBER_ROW;

		this.memberScroll = Mth.clamp(this.memberScroll, 0, Math.max(0, data.members().size() - this.membersVisible));
		lastMemberScroll = this.memberScroll;
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
					this.minecraft.setScreen(new FactionScreen());
				}
			},
			Component.translatable("message.nomosiki.faction.leave_confirm", faction),
			Component.empty()
		));
	}

	private boolean inList(double mouseX, double mouseY, int top, int height) {
		return mouseX >= this.contentLeft && mouseX < this.contentLeft + CONTENT_W
			&& mouseY >= top && mouseY < top + height;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		FactionScreenDataPayload data = FactionClientState.lastScreenData();
		int delta = (int) Math.signum(scrollY);
		if (this.inList(mouseX, mouseY, this.membersTop, this.membersHeight) && !data.members().isEmpty()) {
			int maxScroll = Math.max(0, data.members().size() - this.membersVisible);
			this.memberScroll = Mth.clamp(this.memberScroll - delta, 0, maxScroll);
			lastMemberScroll = this.memberScroll;
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
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, 16, SakuraStyle.TITLE);

		Component current = data.factionName().isBlank()
			? Component.translatable("screen.nomosiki.faction.no_faction")
			: Component.translatable("screen.nomosiki.faction.current", data.factionName());
		int currentX = this.contentLeft + 68;
		int maxCurrentW = this.contentLeft + CONTENT_W - currentX;
		graphics.drawString(this.font, this.font.plainSubstrByWidth(current.getString(), maxCurrentW),
			currentX, 38, data.factionName().isBlank() ? SakuraStyle.INK_MUTED : SakuraStyle.INK, false);

		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.faction.members"),
			this.contentLeft, this.membersTop - HEADER, SakuraStyle.TITLE);
		this.memberScroll = Mth.clamp(this.memberScroll, 0, Math.max(0, data.members().size() - this.membersVisible));
		graphics.enableScissor(this.contentLeft, this.membersTop, this.contentLeft + CONTENT_W, this.membersTop + this.membersHeight);
		if (data.members().isEmpty()) {
			SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.faction.members_none"),
				this.contentLeft, this.membersTop + 2, SakuraStyle.INK_MUTED);
		} else {
			for (int i = 0; i < this.membersVisible; i++) {
				int idx = i + this.memberScroll;
				if (idx >= data.members().size()) {
					break;
				}
				graphics.drawString(this.font, "- " + data.members().get(idx),
					this.contentLeft, this.membersTop + i * MEMBER_ROW + 2, SakuraStyle.INK, false);
			}
		}
		graphics.disableScissor();
		SakuraStyle.drawScrollbar(graphics, this.contentLeft + CONTENT_W - 4, this.membersTop, this.membersHeight,
			this.memberScroll, this.membersVisible, data.members().size());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
