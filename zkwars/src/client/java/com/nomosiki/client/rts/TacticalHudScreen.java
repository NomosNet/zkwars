package com.nomosiki.client.rts;

import com.nomosiki.client.NomosikiModClient;
import com.nomosiki.client.SquadClientState;
import com.nomosiki.client.SquadCommandClient;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.client.match.MatchClient;
import com.nomosiki.client.match.MatchClientState;
import com.nomosiki.client.match.MatchHqScreen;
import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.match.MatchPhase;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.network.match.payload.c2s.MatchBuildPlacePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadDeployFormationPayload;
import com.nomosiki.network.squad.payload.s2c.ClanArmyRosterPayload;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transparent command overlay opened with the tactical camera so vanilla releases the mouse.
 */
public class TacticalHudScreen extends Screen {
	private static final int STRIP_MARGIN = 8;
	private static final int ARMY_REFRESH_INTERVAL = 10;
	private static final long DOUBLE_CLICK_MS = 280L;

	private int cardScroll;
	private int watchScroll;
	private int rosterScroll;
	private @Nullable UUID pendingGiveSquad;
	private int armyRefreshCooldown;
	private int lastCardIndex = -1;
	private long lastCardClickMs;

	private int pressButton = -1;
	private double pressX;
	private double pressY;
	private double dragX;
	private double dragY;
	private Vec3 pressWorld;
	private boolean dragging;
	private boolean boxSelect;
	private @Nullable UUID pressEnemySquadId;
	private boolean pressEnemyIsPlayer;
	private boolean pressFriendlyBanner;

	public TacticalHudScreen() {
		super(Component.translatable("overlay.nomosiki.rts.tactical_active"));
	}

	public static boolean isOpen(Minecraft mc) {
		return mc.screen instanceof TacticalHudScreen;
	}

	public static boolean shouldConsumeScroll(Minecraft mc) {
		if (!(mc.screen instanceof TacticalHudScreen hud)) {
			return false;
		}
		double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getWidth());
		double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getHeight());
		return SquadCardRenderer.hitStrip(hud.cardLayout(), mx, my);
	}

	@Override
	protected void init() {
		SquadCommandClient.requestArmyState();
		SquadCommandClient.requestClanRoster();
		this.armyRefreshCooldown = ARMY_REFRESH_INTERVAL;
		this.releaseGameCursor();
	}

	@Override
	public void tick() {
		if (!TacticalCameraController.isActive()) {
			this.onClose();
			return;
		}
		this.armyRefreshCooldown--;
		if (this.armyRefreshCooldown <= 0) {
			this.armyRefreshCooldown = ARMY_REFRESH_INTERVAL;
			SquadCommandClient.requestArmyState();
		}
		this.releaseGameCursor();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderHint(graphics);
		this.renderSquadBanners(graphics, mouseX, mouseY);
		this.renderSquadCards(graphics, mouseX, mouseY);
		this.renderRoster(graphics, mouseX, mouseY);
		this.renderGivePicker(graphics, mouseX, mouseY);
		this.renderCommandBar(graphics, mouseX, mouseY);
		this.renderSelectionBox(graphics);
		this.updateGhost(mouseX, mouseY);
		this.renderCursor(graphics, mouseX, mouseY);
	}

	private void renderHint(GuiGraphics graphics) {
		int selected = RtsClientState.multiSelectedSquads().size();
		Component title = Component.translatable("overlay.nomosiki.rts.tactical_active");
		Component hint;
		if (MatchClientState.buildPlaceMode()) {
			hint = Component.translatable("screen.nomosiki.match.build.place_hint");
		} else if (RtsClientState.bombardDesignate()) {
			hint = Component.translatable("overlay.nomosiki.rts.bombard_hint", RtsClientState.bombardRadius());
		} else if (selected == 0) {
			hint = Component.translatable("overlay.nomosiki.rts.tactical_no_selection");
		} else {
			hint = Component.translatable("overlay.nomosiki.rts.hint_tactical_mouse");
		}
		SakuraStyle.drawCentered(graphics, this.font, title, this.width / 2, 8, SakuraStyle.TITLE);
		SakuraStyle.drawCentered(graphics, this.font, hint, this.width / 2, 20, SakuraStyle.INK_MUTED);
	}

	private void renderSquadBanners(GuiGraphics graphics, int mouseX, int mouseY) {
		Minecraft mc = this.minecraft;
		if (mc == null) {
			return;
		}
		List<SquadBannerRenderer.Banner> banners = SquadBannerRenderer.collect(mc);
		SquadBannerRenderer.render(graphics, this.font, banners, mouseX, mouseY);
		if (TacticalCommandBar.layout(this.width, this.height).hitBar(mouseX, mouseY)
			|| (RtsClientState.hudVisible() && SquadCardRenderer.hitStrip(this.cardLayout(), mouseX, mouseY))) {
			return;
		}
		SquadBannerRenderer.hit(banners, mouseX, mouseY).ifPresent(banner ->
			graphics.renderComponentTooltip(this.font, SquadBannerRenderer.tooltip(banner), mouseX, mouseY));
	}

	private void renderCommandBar(GuiGraphics graphics, int mouseX, int mouseY) {
		if (this.minecraft == null) {
			return;
		}
		TacticalCommandBar.Layout layout = TacticalCommandBar.layout(this.width, this.height);
		int hovered = TacticalCommandBar.render(graphics, this.font, this.minecraft, layout, mouseX, mouseY);
		if (hovered >= 0) {
			List<Component> tooltip = TacticalCommandBar.tooltip(hovered, this.minecraft);
			if (!tooltip.isEmpty()) {
				graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
			}
		}
	}

	private void renderSquadCards(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!RtsClientState.hudVisible()) {
			return;
		}
		List<SquadArmyStatePayload.SquadRow> rows = this.shownRows();
		SquadCardRenderer.StripLayout layout = this.cardLayout();
		this.storeScroll(layout.scroll());
		if (rows.isEmpty()) {
			return;
		}
		boolean badge = ClanArmyClientState.watching() ? ClanArmyClientState.admin() : ClanArmyClientState.canTransfer();
		java.util.Optional<UUID> active = ClanArmyClientState.watching()
			? java.util.Optional.empty()
			: SquadClientState.activeSquadId();
		int hovered = SquadCardRenderer.renderStrip(
			graphics, this.font, Minecraft.getInstance(), layout, rows, mouseX, mouseY, active, badge);
		if (hovered >= 0) {
			graphics.renderComponentTooltip(this.font, SquadCardRenderer.cardTooltip(rows.get(hovered)), mouseX, mouseY);
		}
	}

	private void renderSelectionBox(GuiGraphics graphics) {
		if (!this.boxSelect || this.pressButton != 0 || !this.dragging) {
			return;
		}
		int x0 = (int) Math.min(this.pressX, this.dragX);
		int y0 = (int) Math.min(this.pressY, this.dragY);
		int x1 = (int) Math.max(this.pressX, this.dragX);
		int y1 = (int) Math.max(this.pressY, this.dragY);
		graphics.fill(x0, y0, x1, y1, 0x4433AAFF);
		graphics.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFF66CCFF);
	}

	private void updateGhost(int mouseX, int mouseY) {
		if (RtsClientState.bombardDesignate()) {
			this.trackBombardDrag(mouseX, mouseY);
			FormationGhostRenderer.clear();
			return;
		}
		if (this.pressButton != 1 || this.pressWorld == null || !this.dragging) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 now = TacticalCursorPicking.hitLocation(mc, mouseX, mouseY);
		if (now == null || FormationDragMath.isClick(pixelDrag(), this.pressWorld, now)) {
			FormationGhostRenderer.clear();
			return;
		}
		List<SquadArmyStatePayload.SquadRow> selected = selectedRowsInCardOrder();
		if (selected.isEmpty()) {
			FormationGhostRenderer.clear();
			return;
		}
		List<FormationDragMath.Placement> placements = FormationDragMath.placementsAlongFront(
			this.pressWorld, now, selected, TacticalCameraController.getCameraLookVector());
		List<Vec3> slots = new ArrayList<>();
		if (mc.level != null) {
			for (FormationDragMath.Placement placement : placements) {
				slots.addAll(FormationDragMath.previewSlots(mc.level, placement));
			}
		}
		FormationGhostRenderer.setPreview(slots, this.pressWorld, now);
	}

	private void trackBombardDrag(int mouseX, int mouseY) {
		Minecraft mc = this.minecraft;
		if (mc == null) {
			return;
		}
		Vec3 now = TacticalCursorPicking.hitLocation(mc, mouseX, mouseY);
		if (now != null) {
			RtsClientState.setBombardHover(BlockPos.containing(now));
		}
		if (this.pressButton != 1 || this.pressWorld == null || now == null) {
			return;
		}
		RtsOrderInputHandler.stretchBombardRadius(BlockPos.containing(now));
	}

	private SquadCardRenderer.StripLayout cardLayout() {
		int width = Math.max(80, this.width - STRIP_MARGIN * 2);
		int top = this.height - SquadCardRenderer.CARD_H - STRIP_MARGIN;
		return SquadCardRenderer.layoutStrip(
			STRIP_MARGIN, top, width, this.shownRows().size(), this.shownScroll());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.clickGivePicker(mouseX, mouseY)) {
			return true;
		}
		if (button == 0 && this.clickRoster(mouseX, mouseY)) {
			return true;
		}
		TacticalCommandBar.Layout commandLayout = TacticalCommandBar.layout(this.width, this.height);
		if (commandLayout.hitBar(mouseX, mouseY)) {
			if (button == 0) {
				TacticalCommandBar.click(commandLayout, mouseX, mouseY);
			}
			return true;
		}
		SquadCardRenderer.StripLayout layout = this.cardLayout();
		if (button == 0 && RtsClientState.hudVisible()) {
			if (SquadCardRenderer.hitLeftChevron(layout, mouseX, mouseY)) {
				this.storeScroll(Math.max(0, layout.scroll() - 1));
				return true;
			}
			if (SquadCardRenderer.hitRightChevron(layout, mouseX, mouseY)) {
				this.storeScroll(Math.min(layout.maxScroll(), layout.scroll() + 1));
				return true;
			}
			List<SquadArmyStatePayload.SquadRow> rows = this.shownRows();
			boolean badge = ClanArmyClientState.watching() ? ClanArmyClientState.admin() : ClanArmyClientState.canTransfer();
			int badgeIndex = badge ? SquadCardRenderer.transferBadgeAt(layout, mouseX, mouseY) : -1;
			if (badgeIndex >= 0 && badgeIndex < rows.size()) {
				this.pendingGiveSquad = rows.get(badgeIndex).squadId();
				return true;
			}
			int cardIndex = SquadCardRenderer.cardIndexAt(layout, mouseX, mouseY);
			if (cardIndex >= 0 && cardIndex < rows.size()) {
				if (!ClanArmyClientState.watching()) {
					this.handleCardClick(rows, cardIndex);
				}
				return true;
			}
		}
		if (button == 1 && RtsClientState.hudVisible() && SquadCardRenderer.hitStrip(layout, mouseX, mouseY)) {
			return true;
		}
		this.pressButton = button;
		this.pressX = mouseX;
		this.pressY = mouseY;
		this.dragX = mouseX;
		this.dragY = mouseY;
		this.pressWorld = TacticalCursorPicking.hitLocation(this.minecraft, mouseX, mouseY);
		if (button == 1 && RtsClientState.bombardDesignate() && this.pressWorld != null && this.minecraft != null) {
			long time = this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
			RtsClientState.beginBombardDrag(BlockPos.containing(this.pressWorld), time);
		}
		this.dragging = false;
		Optional<SquadBannerRenderer.Banner> banner = this.minecraft == null
			? Optional.empty()
			: SquadBannerRenderer.hitAt(this.minecraft, mouseX, mouseY);
		this.pressEnemySquadId = banner.filter(SquadBannerRenderer.Banner::allowsAttack)
			.map(SquadBannerRenderer.Banner::squadId)
			.orElse(null);
		this.pressEnemyIsPlayer = banner.filter(SquadBannerRenderer.Banner::allowsAttack)
			.map(SquadBannerRenderer.Banner::isPlayerMark)
			.orElse(false);
		this.pressFriendlyBanner = banner.filter(b -> b.stance().isFriendly()).isPresent();
		this.boxSelect = button == 0 && banner.isEmpty() && !RtsClientState.bombardDesignate();
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button != this.pressButton) {
			return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		this.dragX = mouseX;
		this.dragY = mouseY;
		if (pixelDrag() >= FormationDragMath.CLICK_PIXEL_THRESHOLD) {
			this.dragging = true;
		}
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button != this.pressButton) {
			return super.mouseReleased(mouseX, mouseY, button);
		}
		this.dragX = mouseX;
		this.dragY = mouseY;
		boolean wasDrag = this.dragging && pixelDrag() >= FormationDragMath.CLICK_PIXEL_THRESHOLD;
		if (RtsClientState.bombardDesignate() && button == 1) {
			this.releaseBombardDrag();
		} else if (button == 0) {
			this.handleLeftRelease(mouseX, mouseY, wasDrag);
		} else if (button == 1) {
			this.handleRightRelease(mouseX, mouseY, wasDrag);
		}
		this.resetPointer();
		FormationGhostRenderer.clear();
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		SquadCardRenderer.StripLayout layout = this.cardLayout();
		if (SquadCardRenderer.hitStrip(layout, mouseX, mouseY) && layout.maxScroll() > 0) {
			this.storeScroll(Mth.clamp(layout.scroll() - (int) Math.round(scrollY), 0, layout.maxScroll()));
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			TacticalCameraController.deactivate(this.minecraft);
			return true;
		}
		if (this.minecraft != null && NomosikiModClient.handleTacticalHudKey(this.minecraft, keyCode, scanCode)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		if (TacticalCameraController.isActive() && this.minecraft != null
			&& this.minecraft.screen == this) {
			this.minecraft.setScreen(null);
		}
	}

	@Override
	public void removed() {
		FormationGhostRenderer.clear();
		this.pendingGiveSquad = null;
		ClanArmyClientState.clearWatch();
		super.removed();
	}

	private List<SquadArmyStatePayload.SquadRow> shownRows() {
		return ClanArmyClientState.watching() ? ClanArmyClientState.watchedSquads() : RtsClientState.armySquads();
	}

	private int shownScroll() {
		return ClanArmyClientState.watching() ? this.watchScroll : this.cardScroll;
	}

	private void storeScroll(int value) {
		if (ClanArmyClientState.watching()) {
			this.watchScroll = value;
		} else {
			this.cardScroll = value;
		}
	}

	private void renderRoster(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!ClanArmyClientState.showsRoster()) {
			return;
		}
		for (NameButton button : this.rosterButtons()) {
			ClanArmyRosterPayload.Member member = ClanArmyClientState.members().get(button.index);
			boolean selected = ClanArmyClientState.watchedId().map(id -> id.equals(member.id())).orElse(false);
			if (!ClanArmyClientState.watching() && this.minecraft != null && this.minecraft.player != null
				&& member.id().equals(this.minecraft.player.getUUID())) {
				selected = true;
			}
			boolean hovered = button.hit(mouseX, mouseY);
			int fill = selected ? 0xFF8B1E3F : hovered ? 0xFF5A3040 : 0xCC2A1608;
			graphics.fill(button.x, button.y, button.x + button.w, button.y + button.h, fill);
			int color = member.online() ? 0xFFFFF3E4 : 0xFFAA9080;
			graphics.drawString(this.font, this.rosterLabel(member), button.x + 3, button.y + 3, color, false);
		}
	}

	private void renderGivePicker(GuiGraphics graphics, int mouseX, int mouseY) {
		if (this.pendingGiveSquad == null) {
			return;
		}
		List<ClanArmyRosterPayload.Member> targets = this.giveTargets();
		int rowH = 12;
		int rows = Math.max(1, targets.size());
		int panelW = 140;
		int panelH = rows * rowH + 4;
		int x = STRIP_MARGIN;
		int y = Math.max(28, TacticalCommandBar.layout(this.width, this.height).top() - panelH - 4);
		graphics.fill(x, y, x + panelW, y + panelH, 0xEE2A1608);
		graphics.renderOutline(x, y, panelW, panelH, 0xFFC47A1A);
		if (targets.isEmpty()) {
			graphics.drawString(this.font, Component.translatable("overlay.nomosiki.rts.transfer_empty"), x + 4, y + 4, 0xFFFFE0C0, false);
			return;
		}
		for (int i = 0; i < targets.size(); i++) {
			int rowY = y + 2 + i * rowH;
			boolean hovered = mouseX >= x && mouseX < x + panelW && mouseY >= rowY && mouseY < rowY + rowH;
			if (hovered) {
				graphics.fill(x + 1, rowY, x + panelW - 1, rowY + rowH, 0x66FFC14D);
			}
			graphics.drawString(this.font, targets.get(i).name(), x + 4, rowY + 2, 0xFFFFF3E4, false);
		}
	}

	private boolean clickGivePicker(double mouseX, double mouseY) {
		if (this.pendingGiveSquad == null) {
			return false;
		}
		List<ClanArmyRosterPayload.Member> targets = this.giveTargets();
		int rowH = 12;
		int rows = Math.max(1, targets.size());
		int panelW = 140;
		int panelH = rows * rowH + 4;
		int x = STRIP_MARGIN;
		int y = Math.max(28, TacticalCommandBar.layout(this.width, this.height).top() - panelH - 4);
		if (mouseX < x || mouseX >= x + panelW || mouseY < y || mouseY >= y + panelH) {
			this.pendingGiveSquad = null;
			return true;
		}
		if (targets.isEmpty()) {
			this.pendingGiveSquad = null;
			return true;
		}
		int row = (int) ((mouseY - y - 2) / rowH);
		if (row >= 0 && row < targets.size()) {
			SquadCommandClient.giveSquad(this.pendingGiveSquad, targets.get(row).id());
		}
		this.pendingGiveSquad = null;
		return true;
	}

	private boolean clickRoster(double mouseX, double mouseY) {
		if (!ClanArmyClientState.showsRoster() || this.minecraft == null || this.minecraft.player == null) {
			return false;
		}
		int top = this.height - SquadCardRenderer.CARD_H - STRIP_MARGIN - ClanArmyClientState.ROSTER_H;
		if (mouseY < top || mouseY >= top + ClanArmyClientState.ROSTER_H) {
			return false;
		}
		if (this.rosterOverflow()) {
			if (mouseX >= STRIP_MARGIN && mouseX < STRIP_MARGIN + 12 && this.rosterScroll > 0) {
				this.rosterScroll--;
				return true;
			}
			if (mouseX >= this.width - STRIP_MARGIN - 12 && mouseX < this.width - STRIP_MARGIN) {
				this.rosterScroll++;
				return true;
			}
		}
		for (NameButton button : this.rosterButtons()) {
			if (!button.hit(mouseX, mouseY)) {
				continue;
			}
			ClanArmyRosterPayload.Member member = ClanArmyClientState.members().get(button.index);
			this.pendingGiveSquad = null;
			this.watchScroll = 0;
			if (member.id().equals(this.minecraft.player.getUUID())) {
				ClanArmyClientState.clearWatch();
				SquadCommandClient.watchClan(Optional.empty());
			} else {
				SquadCommandClient.watchClan(Optional.of(member.id()));
			}
			return true;
		}
		return mouseY >= top && mouseY < top + ClanArmyClientState.ROSTER_H
			&& mouseX >= STRIP_MARGIN && mouseX < this.width - STRIP_MARGIN;
	}

	private List<ClanArmyRosterPayload.Member> giveTargets() {
		if (this.minecraft == null || this.minecraft.player == null) {
			return List.of();
		}
		UUID commander = ClanArmyClientState.watchedId().orElse(this.minecraft.player.getUUID());
		return ClanArmyClientState.onlineExcept(commander);
	}

	private String rosterLabel(ClanArmyRosterPayload.Member member) {
		return member.online() ? member.name() : member.name() + " ·";
	}

	private boolean rosterOverflow() {
		int total = 0;
		for (ClanArmyRosterPayload.Member member : ClanArmyClientState.members()) {
			total += this.font.width(this.rosterLabel(member)) + 10;
		}
		return total > this.width - STRIP_MARGIN * 2;
	}

	private List<NameButton> rosterButtons() {
		List<ClanArmyRosterPayload.Member> members = ClanArmyClientState.members();
		List<NameButton> buttons = new ArrayList<>();
		if (members.isEmpty()) {
			return buttons;
		}
		int top = this.height - SquadCardRenderer.CARD_H - STRIP_MARGIN - ClanArmyClientState.ROSTER_H;
		boolean overflow = this.rosterOverflow();
		int x = STRIP_MARGIN + (overflow ? 14 : 0);
		int limit = this.width - STRIP_MARGIN - (overflow ? 14 : 0);
		int start = Mth.clamp(this.rosterScroll, 0, Math.max(0, members.size() - 1));
		this.rosterScroll = start;
		for (int i = start; i < members.size(); i++) {
			int w = this.font.width(this.rosterLabel(members.get(i))) + 8;
			if (x + w > limit) {
				break;
			}
			buttons.add(new NameButton(i, x, top, w, ClanArmyClientState.ROSTER_H - 1));
			x += w + 2;
		}
		return buttons;
	}

	private record NameButton(int index, int x, int y, int w, int h) {
		boolean hit(double mouseX, double mouseY) {
			return mouseX >= this.x && mouseX < this.x + this.w && mouseY >= this.y && mouseY < this.y + this.h;
		}
	}

	private void handleCardClick(List<SquadArmyStatePayload.SquadRow> rows, int cardIndex) {
		UUID squadId = rows.get(cardIndex).squadId();
		long now = System.currentTimeMillis();
		boolean doubleClick = cardIndex == this.lastCardIndex && now - this.lastCardClickMs <= DOUBLE_CLICK_MS;
		this.lastCardIndex = cardIndex;
		this.lastCardClickMs = now;
		if (doubleClick) {
			RtsClientState.selectAllOfType(rows.get(cardIndex).typeId());
			SquadCommandClient.setActiveSquad(squadId);
			return;
		}
		if (Screen.hasShiftDown() && this.lastExclusiveIndex() >= 0) {
			RtsClientState.selectRange(this.lastExclusiveIndex(), cardIndex);
			SquadCommandClient.setActiveSquad(squadId);
			return;
		}
		if (Screen.hasControlDown()) {
			RtsClientState.toggleSelected(squadId);
			if (RtsClientState.isSelected(squadId)) {
				SquadCommandClient.setActiveSquad(squadId);
			}
			return;
		}
		RtsClientState.selectExclusive(squadId);
		RtsClientState.markRangeAnchor(cardIndex);
		SquadCommandClient.setActiveSquad(squadId);
	}

	private int lastExclusiveIndex() {
		return RtsClientState.rangeAnchorIndex();
	}

	private void handleLeftRelease(double mouseX, double mouseY, boolean wasDrag) {
		if (RtsClientState.bombardDesignate()) {
			return;
		}
		if (MatchClientState.buildPlaceMode() && this.minecraft != null) {
			this.tryPlaceBuilding(mouseX, mouseY);
			return;
		}
		if (wasDrag && this.boxSelect) {
			this.selectSquadsInBox(mouseX, mouseY);
			return;
		}
		Optional<SquadBannerRenderer.Banner> banner = this.minecraft == null
			? Optional.empty()
			: SquadBannerRenderer.hitAt(this.minecraft, mouseX, mouseY);
		if (banner.isPresent()) {
			if (banner.get().owned()) {
				UUID squadId = banner.get().squadId();
				if (Screen.hasControlDown()) {
					RtsClientState.toggleSelected(squadId);
				} else {
					RtsClientState.selectExclusive(squadId);
				}
				SquadCommandClient.setActiveSquad(squadId);
			}
			return;
		}
		Optional<UUID> squad = TacticalCursorPicking.ownedSquadAt(this.minecraft, mouseX, mouseY);
		if (squad.isPresent()) {
			if (Screen.hasControlDown()) {
				RtsClientState.toggleSelected(squad.get());
			} else {
				RtsClientState.selectExclusive(squad.get());
			}
			SquadCommandClient.setActiveSquad(squad.get());
			return;
		}
		BuildingHeartEntity building = this.minecraft == null
			? null
			: TacticalCursorPicking.ownedBuildingAt(this.minecraft, mouseX, mouseY);
		if (building != null
			&& MatchClient.last().phase() == MatchPhase.RUNNING.id()
			&& this.minecraft != null) {
			this.minecraft.setScreen(new MatchHqScreen());
			return;
		}
		RtsClientState.clearSelection();
	}

	private void tryPlaceBuilding(double mouseX, double mouseY) {
		Minecraft mc = this.minecraft;
		String buildingId = MatchClientState.pendingBuildingId();
		if (mc == null || buildingId == null) {
			MatchClientState.clearBuildPlace();
			return;
		}
		Vec3 hit = TacticalCursorPicking.hitLocation(mc, mouseX, mouseY);
		if (hit == null) {
			return;
		}
		BlockPos pos = BlockPos.containing(hit);
		ClientPlayNetworking.send(new MatchBuildPlacePayload(buildingId, pos));
		MatchClientState.clearBuildPlace();
	}

	private void releaseBombardDrag() {
		Minecraft mc = this.minecraft;
		if (mc == null) {
			return;
		}
		BlockPos center = RtsClientState.bombardDragCenter();
		if (center == null && this.pressWorld != null) {
			center = BlockPos.containing(this.pressWorld);
		}
		if (center == null) {
			RtsClientState.clearBombardDrag();
			return;
		}
		RtsOrderInputHandler.confirmBombard(mc, center);
	}

	private void selectSquadsInBox(double mouseX, double mouseY) {
		Minecraft mc = this.minecraft;
		if (mc == null) {
			return;
		}
		double x0 = Math.min(this.pressX, mouseX);
		double y0 = Math.min(this.pressY, mouseY);
		double x1 = Math.max(this.pressX, mouseX);
		double y1 = Math.max(this.pressY, mouseY);
		List<UUID> hit = new ArrayList<>();
		for (SquadArmyStatePayload.SquadRow row : RtsClientState.armySquads()) {
			if (row.alive() <= 0) {
				continue;
			}
			Vec2 screen = TacticalCursorPicking.worldToScreen(mc, new Vec3(row.centerX(), row.centerY(), row.centerZ()));
			if (screen == null) {
				continue;
			}
			if (screen.x >= x0 && screen.x <= x1 && screen.y >= y0 && screen.y <= y1) {
				hit.add(row.squadId());
			}
		}
		if (Screen.hasControlDown() || Screen.hasShiftDown()) {
			for (UUID id : hit) {
				if (!RtsClientState.isSelected(id)) {
					RtsClientState.toggleSelected(id);
				}
			}
		} else {
			RtsClientState.replaceSelection(hit);
		}
		if (!hit.isEmpty()) {
			SquadCommandClient.setActiveSquad(hit.get(0));
		}
	}

	private void handleRightRelease(double mouseX, double mouseY, boolean wasDrag) {
		Minecraft mc = this.minecraft;
		if (mc == null || mc.player == null) {
			return;
		}
		if (this.pressFriendlyBanner) {
			mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.cannot_attack_allies"), true);
			return;
		}
		if (this.pressEnemySquadId != null) {
			this.issueAttackTarget(mc, this.pressEnemySquadId, this.pressEnemyIsPlayer);
			return;
		}
		Vec3 now = TacticalCursorPicking.hitLocation(mc, mouseX, mouseY);
		if (now == null) {
			return;
		}
		List<UUID> targets = RtsClientState.orderTargetSquads();
		if (targets.isEmpty() || RtsClientState.totalAliveInSquads(targets) <= 0) {
			mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		if (!wasDrag || this.pressWorld == null || FormationDragMath.isClick(pixelDrag(), this.pressWorld, now)) {
			BlockPos anchor = BlockPos.containing(now.x, now.y, now.z);
			RtsOrderInputHandler.issueCursorOrder(mc, anchor);
			return;
		}
		List<SquadArmyStatePayload.SquadRow> selected = selectedRowsInCardOrder();
		List<FormationDragMath.Placement> placements = FormationDragMath.placementsAlongFront(
			this.pressWorld, now, selected, TacticalCameraController.getCameraLookVector());
		if (placements.isEmpty()) {
			return;
		}
		SquadOrder order = Screen.hasControlDown() ? SquadOrder.ATTACK_ZONE : SquadOrder.MOVE_TO;
		int zoneRadius = Mth.clamp(NomosikiConfig.INSTANCE.defaultAttackZoneRadius, 4, 24);
		List<SquadDeployFormationPayload.Entry> entries = new ArrayList<>();
		for (FormationDragMath.Placement placement : placements) {
			entries.add(new SquadDeployFormationPayload.Entry(
				placement.squadId(),
				placement.anchorBlock(),
				placement.facingYaw(),
				placement.spacing(),
				placement.maxInRow(),
				order.id(),
				zoneRadius
			));
		}
		SquadCommandClient.sendDeployFormation(entries);
		int members = Math.max(1, RtsClientState.totalAliveInSquads(targets));
		RtsClientState.onLocalOrderIssued(order, placements.get(0).anchorBlock(), zoneRadius, members);
		OrderMarkerRenderer.spawnParticles(mc, placements.get(0).anchorBlock());
		mc.player.displayClientMessage(
			Component.translatable("message.nomosiki.rts.formation_deployed", placements.size()),
			true
		);
	}

	private void issueAttackTarget(Minecraft mc, UUID targetId, boolean targetIsPlayer) {
		if (mc.player == null) {
			return;
		}
		List<UUID> attackers = RtsClientState.orderTargetSquads();
		if (attackers.isEmpty() || RtsClientState.totalAliveInSquads(attackers) <= 0) {
			mc.player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		int zoneRadius = Mth.clamp(NomosikiConfig.INSTANCE.defaultAttackZoneRadius, 4, 24);
		int members = Math.max(1, RtsClientState.totalAliveInSquads(attackers));
		SquadCommandClient.sendAttackTarget(attackers, targetId, zoneRadius, targetIsPlayer);
		RtsClientState.onLocalOrderIssued(SquadOrder.ATTACK_TARGET, BlockPos.ZERO, zoneRadius, members);
		mc.player.displayClientMessage(
			Component.translatable(targetIsPlayer
				? "message.nomosiki.rts.attack_player_sent"
				: "message.nomosiki.rts.attack_target_sent"),
			true
		);
	}

	private List<SquadArmyStatePayload.SquadRow> selectedRowsInCardOrder() {
		List<SquadArmyStatePayload.SquadRow> out = new ArrayList<>();
		for (UUID id : RtsClientState.orderTargetSquads()) {
			RtsClientState.squadRow(id).ifPresent(out::add);
		}
		return out;
	}

	private double pixelDrag() {
		double dx = this.dragX - this.pressX;
		double dy = this.dragY - this.pressY;
		return Math.sqrt(dx * dx + dy * dy);
	}

	private void resetPointer() {
		this.pressButton = -1;
		this.pressWorld = null;
		this.dragging = false;
		this.boxSelect = false;
		this.pressEnemySquadId = null;
		this.pressEnemyIsPlayer = false;
		this.pressFriendlyBanner = false;
	}

	private void releaseGameCursor() {
		if (this.minecraft == null) {
			return;
		}
		this.minecraft.mouseHandler.releaseMouse();
		long window = this.minecraft.getWindow().getWindow();
		GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
	}

	private void renderCursor(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 400.0F);
		int s = 2;
		int[][] outline = {
			{0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6}, {0, 7}, {0, 8}, {0, 9}, {0, 10},
			{1, 10}, {2, 9}, {3, 8}, {4, 9}, {5, 10}, {6, 11}, {4, 7}, {5, 7}, {6, 7}, {7, 7},
			{1, 1}, {2, 2}, {3, 3}, {4, 4}, {5, 5}, {3, 6}
		};
		int[][] fill = {
			{1, 2}, {1, 3}, {1, 4}, {1, 5}, {1, 6}, {1, 7}, {1, 8}, {1, 9},
			{2, 3}, {2, 4}, {2, 5}, {2, 6}, {2, 7}, {2, 8},
			{3, 4}, {3, 5}, {3, 7},
			{4, 5}, {4, 6},
			{5, 6}
		};
		for (int[] p : outline) {
			int x = mouseX + p[0] * s;
			int y = mouseY + p[1] * s;
			graphics.fill(x, y, x + s, y + s, SakuraStyle.FRAME_INK);
		}
		for (int[] p : fill) {
			int x = mouseX + p[0] * s;
			int y = mouseY + p[1] * s;
			graphics.fill(x, y, x + s, y + s, SakuraStyle.FRAME_GOLD_HI);
		}
		graphics.pose().popPose();
	}
}
