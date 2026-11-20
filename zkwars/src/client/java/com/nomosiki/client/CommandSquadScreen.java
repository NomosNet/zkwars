package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraSlider;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.client.rts.BattleHudOverlay;
import com.nomosiki.client.rts.RtsClientState;
import com.nomosiki.client.rts.SquadCardRenderer;
import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikRangedFireMode;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.network.squad.payload.c2s.command.SquadFormationShiftPayload;
import com.nomosiki.network.squad.payload.c2s.sync.SquadStateRequestPayload;
import com.nomosiki.squad.SquadType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandSquadScreen extends Screen {
	private static final int ZONE_MIN = 4;
	private static final int ZONE_MAX = 24;
	private static final int ARMY_REFRESH_INTERVAL = 10;
	private static final int PAGE_ORDERS = 0;
	private static final int PAGE_STANCE = 1;
	private static final int PAGE_MORE = 2;
	private static final int PAGE_COUNT = 3;
	private static final int PANEL_WIDTH = 300;

	private int cardScroll;
	private int listLeft;
	private int listTop;
	private int listWidth;
	private int listHeight;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;
	private int doneButtonY;
	private int navLabelY;
	private int armyRefreshCooldown;
	private int currentPage;

	private int zoneRadius;
	private SakuraSlider radiusSlider;
	private Button emoteButton;
	private Button mountButton;
	private Button dismountButton;
	private Button bombardAimButton;
	private Button clearBombardButton;
	private int bombardStatusY = -1;
	private Button prevPageButton;
	private Button nextPageButton;
	private boolean keepOpenAfterOrder;
	private final List<AbstractWidget> pageOrders = new ArrayList<>();
	private final List<AbstractWidget> pageStance = new ArrayList<>();
	private final List<AbstractWidget> pageMore = new ArrayList<>();

	public CommandSquadScreen() {
		super(Component.translatable("screen.nomosiki.squad_commands"));
		this.zoneRadius = Mth.clamp(NomosikiConfig.INSTANCE.defaultAttackZoneRadius, ZONE_MIN, ZONE_MAX);
		this.keepOpenAfterOrder = !NomosikiConfig.INSTANCE.squadScreenCloseAfterOrder;
	}

	public void onArmyStateUpdated() {
		SquadCardRenderer.StripLayout layout = this.cardLayout();
		this.cardScroll = layout.scroll();
	}

	@Override
	protected void init() {
		ClientPlayNetworking.send(new SquadStateRequestPayload());
		SquadCommandClient.requestArmyState();
		this.pageOrders.clear();
		this.pageStance.clear();
		this.pageMore.clear();

		int centerX = this.width / 2;
		this.listWidth = PANEL_WIDTH;
		this.listLeft = centerX - this.listWidth / 2;
		this.listTop = 36;
		this.listHeight = SquadCardRenderer.CARD_H;

		int doneHeight = 20;
		int hotkeyReserve = 14;
		int navHeight = 18;
		int buttonHeight = 18;
		int gap = 2;
		int gapAfterList = 6;
		int selectGap = 4;
		if (this.height < 260) {
			buttonHeight = 16;
			gap = 1;
		}

		int doneY = this.height - doneHeight - 4;
		this.doneButtonY = doneY;
		int navY = doneY - hotkeyReserve - navHeight;
		this.navLabelY = navY + (navHeight - 8) / 2;

		this.panelLeft = this.listLeft - 10;
		this.panelTop = 6;
		this.panelWidth = this.listWidth + 20;
		this.panelHeight = doneY + doneHeight + 4 - this.panelTop;

		int rowY = this.listTop + this.listHeight + gapAfterList;
		int buttonWidth = this.listWidth;
		int half = buttonWidth / 2 - 2;
		this.addRenderableWidget(new SakuraButton(this.listLeft, rowY, half, buttonHeight,
			fitLabel("screen.nomosiki.squad_commands.select_all", half - 4), b -> RtsClientState.setAllSelected(true)));
		this.addRenderableWidget(new SakuraButton(this.listLeft + half + 4, rowY, half, buttonHeight,
			fitLabel("screen.nomosiki.squad_commands.select_none", half - 4), b -> RtsClientState.setAllSelected(false)));

		int pageTop = rowY + buttonHeight + selectGap;
		this.initOrdersPage(pageTop, buttonHeight, gap, buttonWidth, half);
		this.initStancePage(pageTop, buttonHeight, gap, half);
		this.initMorePage(pageTop, buttonHeight, gap, buttonWidth, half);

		int arrowW = 20;
		this.prevPageButton = this.addRenderableWidget(new SakuraButton(this.listLeft, navY, arrowW, navHeight,
			Component.literal("<"), b -> this.setPage(this.currentPage - 1)));
		this.nextPageButton = this.addRenderableWidget(new SakuraButton(this.listLeft + this.listWidth - arrowW, navY, arrowW, navHeight,
			Component.literal(">"), b -> this.setPage(this.currentPage + 1)));

		this.addRenderableWidget(new SakuraButton(centerX - 50, doneY, 100, doneHeight,
			Component.translatable("gui.done"), b -> this.onClose()));
		this.setPage(this.currentPage);
	}

	private void initOrdersPage(int pageTop, int buttonHeight, int gap, int buttonWidth, int half) {
		int y = pageTop;
		this.radiusSlider = new SakuraSlider(this.listLeft, y, this.listWidth - 72, buttonHeight, radiusNormalized(this.zoneRadius), value -> {
			this.zoneRadius = radiusFromNormalized(value);
			return Component.translatable("screen.nomosiki.squad_commands.zone_slider", this.zoneRadius);
		});
		this.radiusSlider.setTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_zone_radius")));
		this.addToPage(this.pageOrders, this.radiusSlider);

		int presetX = this.listLeft + this.listWidth - 66;
		this.addToPage(this.pageOrders, new SakuraButton(presetX, y, 20, buttonHeight, Component.literal("4"), b -> this.setZoneRadius(4)));
		this.addToPage(this.pageOrders, new SakuraButton(presetX + 22, y, 20, buttonHeight, Component.literal("8"), b -> this.setZoneRadius(8)));
		this.addToPage(this.pageOrders, new SakuraButton(presetX + 44, y, 20, buttonHeight, Component.literal("16"), b -> this.setZoneRadius(16)));

		y += buttonHeight + gap;
		this.addToPage(this.pageOrders, new SakuraButton(this.listLeft, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad_follow", half - 4), b -> this.issueOrder(SquadOrder.FOLLOW, null))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_follow"))));
		this.addToPage(this.pageOrders, new SakuraButton(this.listLeft + half + 4, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad_hold", half - 4), b -> this.issueOrder(SquadOrder.HOLD, this.anchorOrWarn()))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_hold"))));

		y += buttonHeight + gap;
		this.addToPage(this.pageOrders, new SakuraButton(this.listLeft, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad_move", half - 4), b -> this.issueOrder(SquadOrder.MOVE_TO, this.anchorOrWarn()))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_move"))));
		this.addToPage(this.pageOrders, new SakuraButton(this.listLeft + half + 4, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad_attack_zone", half - 4), b -> this.issueOrder(SquadOrder.ATTACK_ZONE, this.anchorOrWarn()))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_attack_zone"))));

		y += buttonHeight + gap;
		this.bombardStatusY = y;
		y += 10;
		this.bombardAimButton = new SakuraButton(this.listLeft, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad.bombard_aim", half - 4), b -> this.beginBombardAim())
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad.bombard_aim")));
		this.addToPage(this.pageOrders, this.bombardAimButton);
		this.clearBombardButton = new SakuraButton(this.listLeft + half + 4, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad.bombard_clear", half - 4), b -> this.clearBombard())
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad.bombard_clear")));
		this.addToPage(this.pageOrders, this.clearBombardButton);

		y += buttonHeight + gap;
		this.addToPage(this.pageOrders, new SakuraButton(this.listLeft, y, buttonWidth, buttonHeight, this.keepOpenLabel(), b -> {
			this.keepOpenAfterOrder = !this.keepOpenAfterOrder;
			b.setMessage(this.keepOpenLabel());
		}));
	}

	private void initStancePage(int pageTop, int buttonHeight, int gap, int half) {
		int y = pageTop;
		int cmW = (this.listWidth - 12) / 4;
		this.addToPage(this.pageStance, new SakuraButton(this.listLeft, y, cmW, buttonHeight,
			fitLabel("button.nomosiki.combat_mode.passive", cmW - 4), b -> this.issueCombatMode(NomosikCombatMode.PASSIVE))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.combat_mode.passive"))));
		this.addToPage(this.pageStance, new SakuraButton(this.listLeft + cmW + 4, y, cmW, buttonHeight,
			fitLabel("button.nomosiki.combat_mode.neutral", cmW - 4), b -> this.issueCombatMode(NomosikCombatMode.NEUTRAL))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.combat_mode.neutral"))));
		this.addToPage(this.pageStance, new SakuraButton(this.listLeft + 2 * (cmW + 4), y, cmW, buttonHeight,
			fitLabel("button.nomosiki.combat_mode.aggressive", cmW - 4), b -> this.issueCombatMode(NomosikCombatMode.AGGRESSIVE))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.combat_mode.aggressive"))));
		this.addToPage(this.pageStance, new SakuraButton(this.listLeft + 3 * (cmW + 4), y, cmW, buttonHeight,
			fitLabel("button.nomosiki.combat_mode.raid", cmW - 4), b -> this.issueCombatMode(NomosikCombatMode.RAID))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.combat_mode.raid"))));

		y += buttonHeight + gap;
		this.addToPage(this.pageStance, new SakuraButton(this.listLeft, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad.shield_up", half - 4), b -> this.issueShieldStance(true))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad.shield_up"))));
		this.addToPage(this.pageStance, new SakuraButton(this.listLeft + half + 4, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad.shield_down", half - 4), b -> this.issueShieldStance(false))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad.shield_down"))));

		y += buttonHeight + gap;
		this.dismountButton = new SakuraButton(this.listLeft, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad.dismount", half - 4), b -> this.issueMountCommand(false))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad.dismount")));
		this.addToPage(this.pageStance, this.dismountButton);
		this.mountButton = new SakuraButton(this.listLeft + half + 4, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad.mount", half - 4), b -> this.issueMountCommand(true))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad.mount")));
		this.addToPage(this.pageStance, this.mountButton);
	}

	private void initMorePage(int pageTop, int buttonHeight, int gap, int buttonWidth, int half) {
		int y = pageTop;
		this.addToPage(this.pageMore, new SakuraButton(this.listLeft, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad_formation_forward", half - 4), b -> this.issueFormationShift(true))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_formation_forward"))));
		this.addToPage(this.pageMore, new SakuraButton(this.listLeft + half + 4, y, half, buttonHeight,
			fitLabel("button.nomosiki.squad_formation_backward", half - 4), b -> this.issueFormationShift(false))
			.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_formation_backward"))));

		y += buttonHeight + gap;
		this.addToPage(this.pageMore, new SakuraButton(this.listLeft, y, buttonWidth, buttonHeight,
			fitLabel("screen.nomosiki.squad_formations.open", buttonWidth - 6), b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new SquadFormationScreen(this));
				}
			}).withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_formations.open"))));

		y += buttonHeight + gap;
		this.addToPage(this.pageMore, new SakuraButton(this.listLeft, y, buttonWidth, buttonHeight,
			fitLabel("screen.nomosiki.squad_list.open", buttonWidth - 6), b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new SquadListScreen(this));
				}
			}).withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_list.open"))));

		y += buttonHeight + gap;
		boolean emoteReady = com.nomosiki.compat.emote.NomosikEmoteCompat.isClientRuntimeReady();
		boolean hasSelection = !RtsClientState.orderTargetSquads().isEmpty();
		this.emoteButton = new SakuraButton(this.listLeft, y, buttonWidth, buttonHeight,
			fitLabel("screen.nomosiki.squad_commands.emotes", buttonWidth - 6), b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new com.nomosiki.client.compat.emote.ui.NomosikEmoteWheelScreen(
						this, RtsClientState.orderTargetSquads()));
				}
			}).withTooltip(Tooltip.create(Component.translatable(
			!emoteReady ? "tooltip.nomosiki.emote.unavailable"
				: (!hasSelection ? "tooltip.nomosiki.emote.no_selection" : "tooltip.nomosiki.emote.open")
		)));
		this.emoteButton.active = emoteReady && hasSelection;
		this.addToPage(this.pageMore, this.emoteButton);

		y += buttonHeight + gap;
		this.addToPage(this.pageMore, new SakuraButton(this.listLeft, y, buttonWidth, buttonHeight,
			fitLabel("screen.nomosiki.duel.open", buttonWidth - 6), b -> {
				if (this.minecraft != null) {
					DuelClient.open(this.minecraft, this);
				}
			}).withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.duel.open"))));

		if (CityChunkClaim.available()) {
			y += buttonHeight + gap;
			this.addToPage(this.pageMore, new SakuraButton(this.listLeft, y, buttonWidth, buttonHeight,
				fitLabel("screen.nomosiki.chunk_claim.open", buttonWidth - 6), b -> {
					if (this.minecraft != null) {
						this.minecraft.setScreen(new ChunkClaimScreen(this));
					}
				}));
		}
	}

	private <T extends AbstractWidget> T addToPage(List<AbstractWidget> page, T widget) {
		page.add(widget);
		return this.addRenderableWidget(widget);
	}

	private void setPage(int page) {
		this.currentPage = Mth.clamp(page, 0, PAGE_COUNT - 1);
		boolean orders = this.currentPage == PAGE_ORDERS;
		boolean stance = this.currentPage == PAGE_STANCE;
		boolean more = this.currentPage == PAGE_MORE;
		for (AbstractWidget widget : this.pageOrders) {
			widget.visible = orders;
		}
		for (AbstractWidget widget : this.pageStance) {
			widget.visible = stance;
		}
		for (AbstractWidget widget : this.pageMore) {
			widget.visible = more;
		}
		if (this.prevPageButton != null) {
			this.prevPageButton.active = this.currentPage > 0;
		}
		if (this.nextPageButton != null) {
			this.nextPageButton.active = this.currentPage < PAGE_COUNT - 1;
		}
		this.updateBombardButtons();
	}

	private Component pageName() {
		return Component.translatable(switch (this.currentPage) {
			case PAGE_STANCE -> "screen.nomosiki.squad_commands.page.stance";
			case PAGE_MORE -> "screen.nomosiki.squad_commands.page.more";
			default -> "screen.nomosiki.squad_commands.page.orders";
		});
	}

	@Override
	public void tick() {
		if (--this.armyRefreshCooldown <= 0) {
			this.armyRefreshCooldown = ARMY_REFRESH_INTERVAL;
			SquadCommandClient.requestArmyState();
		}
		if (this.mountButton != null && this.dismountButton != null) {
			boolean artillery = this.hasArtilleristSelection();
			int half = Math.max(40, (this.listWidth - 12) / 2);
			this.mountButton.setMessage(this.fitLabel(
				artillery ? "button.nomosiki.squad.siege_mount" : "button.nomosiki.squad.mount", half - 4));
			this.dismountButton.setMessage(this.fitLabel(
				artillery ? "button.nomosiki.squad.siege_dismount" : "button.nomosiki.squad.dismount", half - 4));
			this.mountButton.setTooltip(Tooltip.create(Component.translatable(
				artillery ? "tooltip.nomosiki.squad.siege_mount" : "tooltip.nomosiki.squad.mount")));
			this.dismountButton.setTooltip(Tooltip.create(Component.translatable(
				artillery ? "tooltip.nomosiki.squad.siege_dismount" : "tooltip.nomosiki.squad.dismount")));
		}
		this.updateBombardButtons();
		if (this.emoteButton != null) {
			boolean emoteReady = com.nomosiki.compat.emote.NomosikEmoteCompat.isClientRuntimeReady();
			boolean hasSelection = !RtsClientState.orderTargetSquads().isEmpty();
			this.emoteButton.active = emoteReady && hasSelection;
			this.emoteButton.setTooltip(Tooltip.create(Component.translatable(
				!emoteReady ? "tooltip.nomosiki.emote.unavailable"
					: (!hasSelection ? "tooltip.nomosiki.emote.no_selection" : "tooltip.nomosiki.emote.open")
			)));
		}
	}

	private void updateBombardButtons() {
		if (this.bombardAimButton == null) {
			return;
		}
		boolean show = this.currentPage == PAGE_ORDERS && this.hasRangedSelection();
		boolean zone = RtsClientState.activeBombardRadius().isPresent();
		this.bombardAimButton.visible = show;
		if (this.clearBombardButton != null) {
			this.clearBombardButton.visible = show && zone;
			this.clearBombardButton.active = zone;
		}
	}

	private boolean hasRangedSelection() {
		for (UUID squadId : RtsClientState.orderTargetSquads()) {
			if (RtsClientState.squadTypeFor(squadId)
				.map(type -> BattleHudOverlay.isRangedBranch(type.id()))
				.orElse(false)) {
				return true;
			}
		}
		return false;
	}

	private void beginBombardAim() {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		if (!this.hasRangedSelection()) {
			this.minecraft.player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		RtsClientState.setBombardRadius(this.zoneRadius);
		RtsClientState.setBombardDesignate(true);
		this.minecraft.player.displayClientMessage(
			Component.translatable("overlay.nomosiki.rts.bombard_hint", RtsClientState.bombardRadius()),
			true
		);
		this.minecraft.setScreen(null);
	}

	private void clearBombard() {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		List<UUID> squads = this.resolveOrderSquads();
		if (squads.isEmpty()) {
			this.minecraft.player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		SquadCommandClient.sendRangedFire(squads, NomosikRangedFireMode.FIRE_AT_WILL);
		this.minecraft.player.displayClientMessage(Component.translatable(
			"message.nomosiki.rts.fire_mode_set",
			Component.translatable("nomosiki.rts.fire_mode.fire_at_will")
		), true);
	}

	private boolean hasArtilleristSelection() {
		for (UUID squadId : RtsClientState.orderTargetSquads()) {
			if (RtsClientState.squadTypeFor(squadId).map(SquadType::isArtillerist).orElse(false)) {
				return true;
			}
		}
		return false;
	}

	private Component fitLabel(String key, int maxWidth) {
		String full = Component.translatable(key).getString();
		return Component.literal(this.font.plainSubstrByWidth(full, Math.max(10, maxWidth)));
	}

	private void setZoneRadius(int radius) {
		this.zoneRadius = Mth.clamp(radius, ZONE_MIN, ZONE_MAX);
		if (this.radiusSlider != null) {
			this.radiusSlider.setNormalized(radiusNormalized(this.zoneRadius));
		}
	}

	private static double radiusNormalized(int radius) {
		return (Mth.clamp(radius, ZONE_MIN, ZONE_MAX) - ZONE_MIN) / (double) (ZONE_MAX - ZONE_MIN);
	}

	private static int radiusFromNormalized(double value) {
		return Mth.clamp(ZONE_MIN + (int) Math.round(value * (ZONE_MAX - ZONE_MIN)), ZONE_MIN, ZONE_MAX);
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
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, 10, SakuraStyle.TITLE);
		this.renderSquadHeader(graphics);
		this.renderSquadCards(graphics, mouseX, mouseY);
		SakuraStyle.drawCentered(graphics, this.font, Component.translatable(
			"screen.nomosiki.squad_commands.page_label",
			this.currentPage + 1,
			PAGE_COUNT,
			this.pageName()
		), this.width / 2, this.navLabelY, SakuraStyle.INK);
		if (this.currentPage == PAGE_ORDERS && this.hasRangedSelection() && this.bombardStatusY > 0) {
			RtsClientState.activeBombardRadius().ifPresent(radius -> SakuraStyle.drawCentered(
				graphics,
				this.font,
				Component.translatable("screen.nomosiki.squad_commands.bombard_status", radius),
				this.listLeft + this.listWidth / 2,
				this.bombardStatusY,
				SakuraStyle.INK_MUTED
			));
		}
		if (this.doneButtonY > 14) {
			SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.squad_commands.hotkeys_v2"),
				this.width / 2, this.doneButtonY - 12, SakuraStyle.INK_MUTED);
		}
	}

	@Override
	public void onClose() {
		CityChunkClaim.clear();
		super.onClose();
	}

	private void renderSquadCards(GuiGraphics graphics, int mouseX, int mouseY) {
		List<SquadArmyStatePayload.SquadRow> rows = this.shownRows();
		SquadCardRenderer.StripLayout layout = this.cardLayout(rows);
		this.cardScroll = layout.scroll();
		if (rows.isEmpty()) {
			SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.squad_commands.no_squads"),
				this.listLeft + this.listWidth / 2, this.listTop + this.listHeight / 2 - 4, SakuraStyle.INK_MUTED);
			return;
		}
		int hovered = SquadCardRenderer.renderStrip(
			graphics, this.font, Minecraft.getInstance(), layout, rows, mouseX, mouseY, SquadClientState.activeSquadId());
		if (hovered >= 0) {
			graphics.renderComponentTooltip(this.font, SquadCardRenderer.cardTooltip(rows.get(hovered)), mouseX, mouseY);
		}
	}

	private List<SquadArmyStatePayload.SquadRow> shownRows() {
		return SquadCardRenderer.aliveRows(RtsClientState.armySquads());
	}

	private SquadCardRenderer.StripLayout cardLayout() {
		return this.cardLayout(this.shownRows());
	}

	private SquadCardRenderer.StripLayout cardLayout(List<SquadArmyStatePayload.SquadRow> rows) {
		return SquadCardRenderer.layoutStrip(
			this.listLeft,
			this.listTop,
			this.listWidth,
			rows,
			SquadClientState.activeSquadId(),
			this.cardScroll
		);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			List<SquadArmyStatePayload.SquadRow> rows = this.shownRows();
			SquadCardRenderer.StripLayout layout = this.cardLayout(rows);
			if (SquadCardRenderer.hitLeftChevron(layout, mouseX, mouseY)) {
				this.cardScroll = Math.max(0, layout.scroll() - 1);
				return true;
			}
			if (SquadCardRenderer.hitRightChevron(layout, mouseX, mouseY)) {
				this.cardScroll = Math.min(layout.maxScroll(), layout.scroll() + 1);
				return true;
			}
			int cardIndex = SquadCardRenderer.cardIndexAt(layout, mouseX, mouseY);
			if (cardIndex >= 0 && cardIndex < rows.size()) {
				UUID squadId = rows.get(cardIndex).squadId();
				RtsClientState.toggleSelected(squadId);
				RtsClientState.markRangeAnchor(cardIndex);
				SquadCommandClient.setActiveSquad(squadId);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		SquadCardRenderer.StripLayout layout = this.cardLayout();
		if (SquadCardRenderer.hitStrip(layout, mouseX, mouseY) && layout.maxScroll() > 0) {
			this.cardScroll = Mth.clamp(layout.scroll() - (int) Math.round(scrollY), 0, layout.maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (Screen.hasControlDown()) {
			int squadIndex = ctrlNumberIndex(keyCode);
			if (squadIndex >= 0) {
				this.activateSquadByIndex(squadIndex);
				return true;
			}
		}
		if (keyCode == GLFW.GLFW_KEY_LEFT) {
			this.setPage(this.currentPage - 1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_RIGHT) {
			this.setPage(this.currentPage + 1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_1) {
			this.issueOrder(SquadOrder.FOLLOW, null);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_2) {
			this.issueOrder(SquadOrder.HOLD, this.anchorOrWarn());
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_3) {
			this.issueOrder(SquadOrder.MOVE_TO, this.anchorOrWarn());
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_4) {
			this.issueOrder(SquadOrder.ATTACK_ZONE, this.anchorOrWarn());
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_5) {
			this.issueCombatMode(NomosikCombatMode.PASSIVE);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_6) {
			this.issueCombatMode(NomosikCombatMode.NEUTRAL);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_7) {
			this.issueCombatMode(NomosikCombatMode.AGGRESSIVE);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_8) {
			this.issueCombatMode(NomosikCombatMode.RAID);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private static int ctrlNumberIndex(int keyCode) {
		if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
			return keyCode - GLFW.GLFW_KEY_1;
		}
		return -1;
	}

	private void activateSquadByIndex(int index) {
		List<SquadArmyStatePayload.SquadRow> rows = RtsClientState.armySquads();
		if (index < 0 || index >= rows.size()) {
			return;
		}
		UUID squadId = rows.get(index).squadId();
		SquadCommandClient.setActiveSquad(squadId);
		if (this.minecraft != null && this.minecraft.player != null) {
			String name = rows.get(index).name();
			if (name == null || name.isEmpty()) {
				name = SquadType.fromId(rows.get(index).typeId()).name();
			} else {
				name = SquadLabels.text(name);
			}
			this.minecraft.player.displayClientMessage(
				Component.translatable("message.nomosiki.rts.active_squad_set", name),
				true
			);
		}
	}

	private void issueCombatMode(NomosikCombatMode mode) {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		Player player = this.minecraft.player;
		List<UUID> squads = this.resolveOrderSquads();
		if (squads.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		SquadCommandClient.sendCombatModeToSquads(mode, squads);
		int recipientCount = squads.stream().mapToInt(id -> RtsClientState.memberCountFor(id).orElse(0)).sum();
		Component modeLabel = Component.translatable(switch (mode) {
			case PASSIVE -> "button.nomosiki.combat_mode.passive";
			case NEUTRAL -> "button.nomosiki.combat_mode.neutral";
			case AGGRESSIVE -> "button.nomosiki.combat_mode.aggressive";
			case RAID -> "button.nomosiki.combat_mode.raid";
		});
		player.displayClientMessage(Component.translatable("message.nomosiki.squad_combat_mode_sent", Math.max(1, recipientCount), modeLabel), true);
		if (!this.keepOpenAfterOrder) {
			this.onClose();
		}
	}

	private void issueShieldStance(boolean up) {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		Player player = this.minecraft.player;
		List<UUID> squads = this.resolveOrderSquads();
		if (squads.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		SquadCommandClient.sendShieldStance(squads, up);
		player.displayClientMessage(Component.translatable(
			up ? "message.nomosiki.squad.shield_stance_on" : "message.nomosiki.squad.shield_stance_off"
		), true);
		if (!this.keepOpenAfterOrder) {
			this.onClose();
		}
	}

	private void issueMountCommand(boolean mount) {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		Player player = this.minecraft.player;
		List<UUID> squads = this.resolveOrderSquads();
		if (squads.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		boolean artillery = this.hasArtilleristSelection();
		SquadCommandClient.sendMountCommand(squads, mount);
		player.displayClientMessage(Component.translatable(
			artillery
				? (mount ? "message.nomosiki.squad.siege_mount" : "message.nomosiki.squad.siege_dismount")
				: (mount ? "message.nomosiki.squad.mount" : "message.nomosiki.squad.dismount")
		), true);
		if (!this.keepOpenAfterOrder) {
			this.onClose();
		}
	}

	private void issueOrder(SquadOrder order, @Nullable BlockPos anchor) {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		Player player = this.minecraft.player;
		List<UUID> squads = this.resolveOrderSquads();
		if (squads.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.nomosiki.rts.no_squads_selected"), true);
			return;
		}
		if (order != SquadOrder.FOLLOW && anchor == null) {
			return;
		}
		BlockPos packetAnchor = anchor == null ? BlockPos.ZERO : anchor;
		SquadCommandClient.sendToSquads(order, packetAnchor, this.zoneRadius, squads);
		this.playAnchorFeedback(anchor);
		int recipientCount = squads.stream().mapToInt(id -> RtsClientState.memberCountFor(id).orElse(0)).sum();
		Component orderLabel = Component.translatable(switch (order) {
			case FOLLOW -> "button.nomosiki.squad_follow";
			case HOLD -> "button.nomosiki.squad_hold";
			case MOVE_TO -> "button.nomosiki.squad_move";
			case ATTACK_ZONE -> "button.nomosiki.squad_attack_zone";
			case ATTACK_TARGET -> "button.nomosiki.squad_attack_target";
		});
		player.displayClientMessage(Component.translatable("message.nomosiki.squad_order_sent", Math.max(1, recipientCount), orderLabel), true);
		if (!this.keepOpenAfterOrder) {
			this.onClose();
		}
	}

	private void issueFormationShift(boolean forward) {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		List<UUID> squads = this.resolveOrderSquads();
		if (squads.isEmpty()) {
			this.minecraft.player.displayClientMessage(
				Component.translatable("message.nomosiki.rts.no_squads_selected"),
				true
			);
			return;
		}
		byte direction = forward
			? SquadFormationShiftPayload.STEP_FORWARD
			: SquadFormationShiftPayload.STEP_BACKWARD;
		SquadCommandClient.sendFormationShift(direction, squads);
		Component msg = forward
			? Component.translatable("message.nomosiki.rts.formation_shift_forward_local")
			: Component.translatable("message.nomosiki.rts.formation_shift_backward_local");
		this.minecraft.player.displayClientMessage(msg, true);
	}

	private List<UUID> resolveOrderSquads() {
		return RtsClientState.orderTargetSquads();
	}

	private void playAnchorFeedback(@Nullable BlockPos anchor) {
		if (anchor == null || this.minecraft == null || this.minecraft.level == null) {
			return;
		}
		Vec3 center = Vec3.atCenterOf(anchor);
		var random = RandomSource.create();
		for (int i = 0; i < 28; i++) {
			double ox = (random.nextDouble() - 0.5) * 0.9;
			double oy = random.nextDouble() * 0.9;
			double oz = (random.nextDouble() - 0.5) * 0.9;
			this.minecraft.level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
				center.x + ox, center.y + oy, center.z + oz, 0.0, 0.04, 0.0);
		}
	}

	@Nullable
	private BlockPos anchorOrWarn() {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
			return null;
		}
		Player player = this.minecraft.player;
		Vec3 eye = player.getEyePosition(1f);
		Vec3 reach = eye.add(player.getViewVector(1f).scale(64.0));
		BlockHitResult hit = this.minecraft.level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) {
			player.displayClientMessage(Component.translatable("message.nomosiki.squad_need_block"), true);
			return null;
		}
		return hit.getBlockPos();
	}

	private Component keepOpenLabel() {
		return Component.translatable(this.keepOpenAfterOrder
			? "screen.nomosiki.squad_commands.keep_open_on"
			: "screen.nomosiki.squad_commands.keep_open_off");
	}

	private void renderSquadHeader(GuiGraphics graphics) {
		int headerY = this.listTop - 12;
		Component header = Component.translatable("screen.nomosiki.squad_commands.squads_header");
		graphics.drawString(this.font, header, this.listLeft, headerY, SakuraStyle.INK_MUTED, false);
		int selected = RtsClientState.multiSelectedSquads().size();
		int total = RtsClientState.armySquads().size();
		Component count = Component.translatable("screen.nomosiki.squad_commands.selected_squads", selected, total);
		int countX = this.listLeft + this.listWidth - this.font.width(count);
		int headerRight = this.listLeft + this.font.width(header) + 8;
		if (countX < headerRight) {
			count = Component.literal(selected + " / " + total);
			countX = Math.max(headerRight, this.listLeft + this.listWidth - this.font.width(count));
		}
		graphics.drawString(this.font, count, countX, headerY, total == 0 ? SakuraStyle.INK_MUTED : SakuraStyle.ACCENT, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

}
