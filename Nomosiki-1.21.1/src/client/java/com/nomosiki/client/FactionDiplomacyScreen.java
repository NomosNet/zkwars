package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.FactionColor;
import com.nomosiki.faction.FactionRelationStatus;
import com.nomosiki.network.faction.FactionDiplomacyRow;
import com.nomosiki.network.faction.payload.c2s.FactionAllianceActionPayload;
import com.nomosiki.network.faction.payload.c2s.FactionDeclareWarPayload;
import com.nomosiki.network.faction.payload.c2s.FactionOpenScreenPayload;
import com.nomosiki.network.faction.payload.s2c.FactionScreenDataPayload;
import com.nomosiki.squad.SamuraiGearLoadout;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FactionDiplomacyScreen extends Screen {
	private static final int PANEL_W = FactionDiplomacyUi.PANEL_WIDTH;
	private static final int PANEL_H = FactionDiplomacyUi.PANEL_HEIGHT;
	private static final int COL_W = FactionDiplomacyUi.COL_WIDTH;
	private static final int CENTER_W = FactionDiplomacyUi.CENTER_WIDTH;
	private static final int ACTION_ROW_W = 130;
	private static final int ACTION_COL_W = CENTER_W - 24;
	private static final int ACTION_BTN_H = 16;
	private static final int ACTION_GAP = 2;
	private static final int PORTRAIT_H = 100;
	private static final int PORTRAIT_SCALE = 42;
	private static final int STATUS_ICON_Y = 112;
	private static final int STATUS_TEXT_Y = 128;
	private static final int ACTION_Y = 144;

	private final String targetFaction;
	private int leftPos;
	private int topPos;
	@Nullable
	private NomosikEntity portrait;
	private final FactionBannerRenderer selfBannerRenderer = new FactionBannerRenderer();
	private final FactionBannerRenderer targetBannerRenderer = new FactionBannerRenderer();
	private final FactionBannerRenderer flagRenderer = new FactionBannerRenderer();
	private final List<FlagHit> flagHits = new ArrayList<>();

	public FactionDiplomacyScreen(String targetFaction) {
		super(Component.translatable("screen.nomosiki.faction.diplomacy_title", targetFaction));
		this.targetFaction = targetFaction;
	}

	public String targetFaction() {
		return this.targetFaction;
	}

	@Override
	protected void init() {
		this.leftPos = (this.width - PANEL_W) / 2;
		this.topPos = (this.height - PANEL_H) / 2;
		this.portrait = null;
		this.ensurePortrait();

		ClientPlayNetworking.send(new FactionOpenScreenPayload());

		this.addRenderableWidget(new SakuraButton(this.leftPos + 8, this.topPos + PANEL_H - 22, 56, 16,
			Component.translatable("button.nomosiki.faction.back"), b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new FactionListScreen());
				}
			}));

		FactionScreenDataPayload data = FactionClientState.lastScreenData();
		FactionDiplomacyRow row = FactionClientState.diplomacyRowFor(this.targetFaction).orElse(null);
		boolean canAct = !data.factionName().isBlank() && data.isOwner() && row != null;
		if (canAct && row != null) {
			this.addDiplomacyActions(row);
		}
	}

	private void ensurePortrait() {
		if (this.minecraft == null) {
			return;
		}
		FactionDiplomacyRow row = FactionClientState.diplomacyRowFor(this.targetFaction).orElse(null);
		String colorId = row == null ? null : row.colorId();
		if (this.portrait == null) {
			this.portrait = FactionPreviewEntities.createSamuraiPortrait(
				this.minecraft, this.targetFaction, colorId);
		} else if (row != null) {
			FactionColor color = FactionPreviewEntities.colorFromId(row.colorId());
			if (this.portrait.getGuiPreviewFactionColor() != color) {
				this.portrait.setGuiPreviewFactionColor(color);
				SamuraiGearLoadout.dyeEquippedArmor(this.portrait, color);
			}
		}
	}

	private void addDiplomacyActions(FactionDiplomacyRow row) {
		String faction = row.factionName();
		int centerX = this.leftPos + FactionDiplomacyUi.CENTER_X;
		int rowY = this.topPos + ACTION_Y;
		int left = centerX + (CENTER_W - ACTION_ROW_W) / 2;
		if (row.warSideChoicePendingIncoming()) {
			this.addRenderableWidget(new SakuraButton(left, rowY, 42, 16,
				Component.translatable("button.nomosiki.faction.war_side_aggressor", faction),
				b -> this.sendAlliance(FactionAllianceActionPayload.WAR_SIDE_SUPPORT_AGGRESSOR, faction)));
			this.addRenderableWidget(new SakuraButton(left + 44, rowY, 42, 16,
				Component.translatable("button.nomosiki.faction.war_side_defender", row.warSideDefenderName()),
				b -> this.sendAlliance(FactionAllianceActionPayload.WAR_SIDE_SUPPORT_DEFENDER, faction)));
			this.addRenderableWidget(new SakuraButton(left + 88, rowY, 42, 16,
				Component.translatable("button.nomosiki.faction.war_side_neutral"),
				b -> this.sendAlliance(FactionAllianceActionPayload.WAR_SIDE_STAY_NEUTRAL, faction)));
			return;
		}
		if (row.warJoinPendingIncoming()) {
			this.addRenderableWidget(new SakuraButton(left, rowY, 64, 16,
				Component.translatable("button.nomosiki.faction.join_war"),
				b -> this.sendAlliance(FactionAllianceActionPayload.JOIN_WAR, faction)));
			this.addRenderableWidget(new SakuraButton(left + 66, rowY, 64, 16,
				Component.translatable("button.nomosiki.faction.ignore_war"),
				b -> this.sendAlliance(FactionAllianceActionPayload.DECLINE_WAR_JOIN, faction)));
			return;
		}
		if (row.alliancePendingIncoming()) {
			this.addRenderableWidget(new SakuraButton(left, rowY, 64, 16,
				Component.translatable("button.nomosiki.faction.accept_alliance"),
				b -> this.sendAlliance(FactionAllianceActionPayload.ACCEPT, faction)));
			this.addRenderableWidget(new SakuraButton(left + 66, rowY, 64, 16,
				Component.translatable("button.nomosiki.faction.decline_alliance"),
				b -> this.sendAlliance(FactionAllianceActionPayload.DECLINE, faction)));
			return;
		}
		if (row.alliancePendingOutgoing()) {
			this.addRenderableWidget(new SakuraButton(left + 17, rowY, 96, 16,
				Component.translatable("button.nomosiki.faction.cancel_alliance"),
				b -> this.sendAlliance(FactionAllianceActionPayload.CANCEL, faction)));
			return;
		}
		switch (row.relationStatus()) {
			case ALLY -> {
				this.addRenderableWidget(new SakuraButton(left, rowY, 64, 16,
					Component.translatable("button.nomosiki.faction.break_alliance"),
					b -> this.sendAlliance(FactionAllianceActionPayload.BREAK, faction)));
				this.addRenderableWidget(new SakuraButton(left + 66, rowY, 64, 16,
					Component.translatable("button.nomosiki.faction.upgrade_military"),
					b -> this.sendAlliance(FactionAllianceActionPayload.UPGRADE_TO_MILITARY, faction)));
			}
			case MILITARY_ALLY -> {
				this.addRenderableWidget(new SakuraButton(left, rowY, 64, 16,
					Component.translatable("button.nomosiki.faction.break_alliance"),
					b -> this.sendAlliance(FactionAllianceActionPayload.BREAK, faction)));
				this.addRenderableWidget(new SakuraButton(left + 66, rowY, 64, 16,
					Component.translatable("button.nomosiki.faction.downgrade_regular"),
					b -> this.sendAlliance(FactionAllianceActionPayload.DOWNGRADE_TO_REGULAR, faction)));
			}
			case AT_WAR -> {
				if (row.peacePendingIncoming()) {
					this.addRenderableWidget(new SakuraButton(left, rowY, 64, 16,
						Component.translatable("button.nomosiki.faction.accept_peace"),
						b -> this.sendAlliance(FactionAllianceActionPayload.ACCEPT_PEACE, faction)));
					this.addRenderableWidget(new SakuraButton(left + 66, rowY, 64, 16,
						Component.translatable("button.nomosiki.faction.decline_peace"),
						b -> this.sendAlliance(FactionAllianceActionPayload.DECLINE_PEACE, faction)));
				} else if (row.peacePendingOutgoing()) {
					this.addRenderableWidget(new SakuraButton(left + 17, rowY, 96, 16,
						Component.translatable("button.nomosiki.faction.cancel_peace"),
						b -> this.sendAlliance(FactionAllianceActionPayload.CANCEL_PEACE, faction)));
				} else {
					this.addRenderableWidget(new SakuraButton(left + 17, rowY, 96, 16,
						Component.translatable("button.nomosiki.faction.offer_peace"),
						b -> this.sendAlliance(FactionAllianceActionPayload.REQUEST_PEACE, faction)));
				}
			}
			case NEUTRAL -> {
				int colLeft = centerX + (CENTER_W - ACTION_COL_W) / 2;
				int step = ACTION_BTN_H + ACTION_GAP;
				this.addRenderableWidget(new SakuraButton(colLeft, rowY, ACTION_COL_W, ACTION_BTN_H,
					Component.translatable("button.nomosiki.faction.request_alliance"),
					b -> this.sendAlliance(FactionAllianceActionPayload.REQUEST, faction)));
				this.addRenderableWidget(new SakuraButton(colLeft, rowY + step, ACTION_COL_W, ACTION_BTN_H,
					Component.translatable("button.nomosiki.faction.request_military"),
					b -> this.sendAlliance(FactionAllianceActionPayload.REQUEST_MILITARY, faction)));
				this.addRenderableWidget(new SakuraButton(colLeft, rowY + step * 2, ACTION_COL_W, ACTION_BTN_H,
					Component.translatable("button.nomosiki.faction.declare_war"),
					b -> this.confirmWar(faction)));
			}
		}
	}

	private void sendAlliance(byte action, String faction) {
		ClientPlayNetworking.send(new FactionAllianceActionPayload(action, faction));
		ClientPlayNetworking.send(new FactionOpenScreenPayload());
	}

	private void confirmWar(String faction) {
		if (this.minecraft == null) {
			return;
		}
		this.minecraft.setScreen(new ConfirmScreen(
			confirmed -> {
				if (confirmed) {
					ClientPlayNetworking.send(new FactionDeclareWarPayload(faction));
					ClientPlayNetworking.send(new FactionOpenScreenPayload());
				}
				if (this.minecraft != null) {
					this.minecraft.setScreen(new FactionDiplomacyScreen(faction));
				}
			},
			Component.translatable("message.nomosiki.faction.war_confirm", faction),
			Component.empty()
		));
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawDim(graphics, this.width, this.height);
		SakuraStyle.drawPaperPanel(graphics, this.leftPos, this.topPos, PANEL_W, PANEL_H);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.ensurePortrait();
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		this.flagHits.clear();

		FactionScreenDataPayload data = FactionClientState.lastScreenData();
		FactionDiplomacyRow row = FactionClientState.diplomacyRowFor(this.targetFaction).orElse(null);
		int leftX = this.leftPos + FactionDiplomacyUi.PAD;
		int centerX = this.leftPos + FactionDiplomacyUi.CENTER_X;
		int rightX = this.leftPos + FactionDiplomacyUi.RIGHT_X;
		int cx = centerX + CENTER_W / 2;

		this.renderFactionColumn(
			graphics, data, leftX, true, data.factionName(), data.myBanner(), data.colorId(),
			data.members().size()
		);
		this.renderFactionColumn(
			graphics, data, rightX, false, this.targetFaction,
			row == null ? ItemStack.EMPTY : row.banner(),
			row == null ? "" : row.colorId(),
			row == null ? 0 : row.memberCount()
		);

		int frameX = centerX + 2;
		int frameY = this.topPos + 8;
		int frameW = CENTER_W - 4;
		SakuraStyle.drawPortraitFrame(graphics, frameX, frameY, frameW, PORTRAIT_H);
		if (this.portrait != null) {
			graphics.enableScissor(frameX + 1, frameY + 1, frameX + frameW - 1, frameY + PORTRAIT_H - 1);
			try {
				InventoryScreen.renderEntityInInventoryFollowsMouse(
					graphics,
					frameX, frameY,
					frameX + frameW, frameY + PORTRAIT_H,
					PORTRAIT_SCALE, 0.12F, (float) mouseX, (float) mouseY, this.portrait
				);
			} finally {
				graphics.disableScissor();
			}
		}

		if (row != null) {
			graphics.blit(FactionDiplomacyUi.statusIcon(row), cx - 8, this.topPos + STATUS_ICON_Y, 0, 0, 16, 16, 16, 16);
			Component status = FactionDiplomacyUi.listStatusLine(row);
			String clipped = this.font.plainSubstrByWidth(status.getString(), CENTER_W - 8);
			SakuraStyle.drawCentered(graphics, this.font, Component.literal(clipped),
				cx, this.topPos + STATUS_TEXT_Y, FactionDiplomacyUi.diplomacyColor(row));
		}

		if (data.factionName().isBlank()) {
			SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.faction.diplomacy_need_faction"),
				cx, this.topPos + ACTION_Y, SakuraStyle.ACCENT);
		} else if (!data.isOwner()) {
			SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.faction.diplomacy_owner_only"),
				cx, this.topPos + ACTION_Y, SakuraStyle.ACCENT);
		}

		for (var child : this.children()) {
			if (child instanceof Renderable renderable) {
				renderable.render(graphics, mouseX, mouseY, partialTick);
			}
		}

		for (FlagHit hit : this.flagHits) {
			if (hit.contains(mouseX, mouseY)) {
				graphics.renderTooltip(this.font, Component.literal(hit.name()), mouseX, mouseY);
				break;
			}
		}
	}

	private void renderFactionColumn(
		GuiGraphics graphics,
		FactionScreenDataPayload data,
		int colX,
		boolean self,
		String factionName,
		ItemStack banner,
		String colorId,
		int memberCount
	) {
		int mid = colX + COL_W / 2;
		if (factionName == null || factionName.isBlank()) {
			SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.faction.no_faction"),
				mid, this.topPos + 64, SakuraStyle.INK_MUTED);
			return;
		}
		FactionBannerRenderer crest = self ? this.selfBannerRenderer : this.targetBannerRenderer;
		crest.setBannerStack(banner, colorId);
		if (crest.hasBanner()) {
			crest.render(graphics, colX + COL_W / 2 - 16, this.topPos + 32, FactionDiplomacyUi.CREST_SCALE);
		}

		String clipped = this.font.plainSubstrByWidth(factionName, COL_W - 4);
		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 300.0F);
		SakuraStyle.drawCentered(graphics, this.font, Component.literal(clipped),
			mid, this.topPos + 8, SakuraStyle.TITLE);
		graphics.pose().popPose();

		SakuraStyle.drawCentered(graphics, this.font,
			Component.translatable("screen.nomosiki.faction.diplomacy_members", memberCount),
			mid, this.topPos + 78, SakuraStyle.INK_MUTED);

		this.renderFlagRow(graphics, data, factionName, FactionRelationStatus.ALLY, colX, this.topPos + 94);
		this.renderFlagRow(graphics, data, factionName, FactionRelationStatus.MILITARY_ALLY,
			colX, this.topPos + 114);
		this.renderFlagRow(graphics, data, factionName, FactionRelationStatus.AT_WAR, colX, this.topPos + 134);
	}

	private void renderFlagRow(
		GuiGraphics graphics,
		FactionScreenDataPayload data,
		String factionName,
		FactionRelationStatus status,
		int colX,
		int y
	) {
		SakuraStyle.drawLabel(graphics, this.font, FactionDiplomacyUi.rowLabel(status),
			colX, y + 4, SakuraStyle.INK_MUTED);
		List<FactionDiplomacyUi.FlagLook> flags = FactionDiplomacyUi.partners(data, factionName, status);
		int flagX = colX + 36;
		if (flags.isEmpty()) {
			SakuraStyle.drawLabel(graphics, this.font,
				Component.translatable("screen.nomosiki.faction.diplomacy_flags_empty"),
				flagX, y + 4, SakuraStyle.INK_DISABLED);
			return;
		}
		int shown = Math.min(FactionDiplomacyUi.FLAG_MAX, flags.size());
		for (int i = 0; i < shown; i++) {
			FactionDiplomacyUi.FlagLook look = flags.get(i);
			int x = flagX + i * FactionDiplomacyUi.FLAG_SLOT;
			this.flagRenderer.setBannerStack(look.banner(), look.colorId());
			if (this.flagRenderer.hasBanner()) {
				this.flagRenderer.render(graphics, x, y - 6, FactionDiplomacyUi.FLAG_SCALE);
			}
			this.flagHits.add(new FlagHit(x, y, FactionDiplomacyUi.FLAG_SLOT, 18, look.name()));
		}
		if (flags.size() > FactionDiplomacyUi.FLAG_MAX) {
			SakuraStyle.drawLabel(graphics, this.font,
				Component.translatable("screen.nomosiki.faction.diplomacy_flags_more", flags.size() - FactionDiplomacyUi.FLAG_MAX),
				flagX + shown * FactionDiplomacyUi.FLAG_SLOT, y + 4, SakuraStyle.INK_MUTED);
		}
	}

	@Override
	public void onClose() {
		this.portrait = null;
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private record FlagHit(int x, int y, int w, int h, String name) {
		boolean contains(int mx, int my) {
			return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
		}
	}
}
