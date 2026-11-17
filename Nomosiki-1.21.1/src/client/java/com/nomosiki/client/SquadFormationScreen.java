package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraSlider;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.network.squad.payload.c2s.sync.SquadStateRequestPayload;
import com.nomosiki.squad.SquadFormationLayout;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Chooses how selected recruits (or the active squad) arrange while holding / moving / zoning.
 */
public class SquadFormationScreen extends Screen {
	private static final double SCAN_RANGE = 48.0;
	private static final int PREVIEW_SLOT = SakuraStyle.ACCENT;
	private static final int PREVIEW_ANCHOR = SakuraStyle.FRAME_GOLD;
	private static final int PREVIEW_LOOSE = SakuraStyle.INK_DISABLED;
	private static final SquadFormation[] GRID_ORDER = {
		SquadFormation.NONE, SquadFormation.LINE, SquadFormation.SQUARE,
		SquadFormation.TRIANGLE, SquadFormation.HOLLOW_CIRCLE, SquadFormation.HOLLOW_SQUARE,
		SquadFormation.V, SquadFormation.CIRCLE, SquadFormation.MOVEMENT
	};

	private final @Nullable Screen returnTo;
	private SquadFormation selected = SquadFormation.LINE;
	private boolean orderViaActiveSquad = true;
	/** Value shown / sent with Apply; kept in sync with {@link #spacingSlider}. */
	private float spacingSendValue = (float) SquadFormationLayout.DEFAULT_SPACING;
	private @Nullable SakuraSlider spacingSlider;
	/** Same order as {@link #GRID_ORDER}; used to draw selection frame around the active formation button. */
	private @Nullable Button[] formationButtons;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public SquadFormationScreen(@Nullable Screen returnTo) {
		super(Component.translatable("screen.nomosiki.squad_formations.title"));
		this.returnTo = returnTo;
	}

	@Override
	protected void init() {
		ClientPlayNetworking.send(new SquadStateRequestPayload());
		this.syncSelectionFromWorld();

		int centerX = this.width / 2;
		int panelW = Math.min(340, this.width - 24);
		int left = centerX - panelW / 2;
		int cols = 3;
		int gap = 4;
		int cellW = (panelW - gap * (cols - 1)) / cols;
		int y = 56;

		this.addRenderableWidget(new SakuraButton(left, y, panelW, 18, this.toggleSquadLabel(), b -> {
			this.orderViaActiveSquad = !this.orderViaActiveSquad;
			b.setMessage(this.toggleSquadLabel());
		}));
		y += 22;

		this.formationButtons = new Button[GRID_ORDER.length];
		for (int i = 0; i < GRID_ORDER.length; i++) {
			SquadFormation f = GRID_ORDER[i];
			int col = i % cols;
			int row = i / cols;
			int bx = left + col * (cellW + gap);
			int by = y + row * 22;
			String tipKey = "tooltip.nomosiki.squad_formations." + f.formationsScreenSuffix();
			this.formationButtons[i] = this.addRenderableWidget(new SakuraButton(bx, by, cellW, 20, f.formationsScreenLabel(), b -> this.selected = f)
				.withTooltip(Tooltip.create(Component.translatable(tipKey))));
		}
		y += ((GRID_ORDER.length + cols - 1) / cols) * 22 + 6;

		float initialSpacing = this.initialSpacingFromWorld();
		this.spacingSendValue = initialSpacing;
		this.spacingSlider = new SakuraSlider(left, y, panelW, 20, spacingNormalized(initialSpacing), value -> {
			double span = NomosikEntity.SQUAD_FORMATION_SPACING_MAX - NomosikEntity.SQUAD_FORMATION_SPACING_MIN;
			double blocks = NomosikEntity.SQUAD_FORMATION_SPACING_MIN + value * span;
			SquadFormationScreen.this.spacingSendValue = (float) blocks;
			return Component.translatable(
				"screen.nomosiki.squad_formations.spacing_slider",
				String.format(Locale.ROOT, "%.2f", blocks)
			);
		});
		this.spacingSlider.setTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_formations.spacing")));
		this.addRenderableWidget(this.spacingSlider);
		y += 24;

		this.addRenderableWidget(new SakuraButton(left, y, panelW, 20,
			Component.translatable("screen.nomosiki.squad_formations.apply"), b -> this.applyFormation()));
		y += 26;

		this.addRenderableWidget(new SakuraButton(centerX - 50, y, 100, 20,
			Component.translatable("gui.done"), b -> this.onClose()));

		this.panelLeft = left - 10;
		this.panelTop = 8;
		this.panelWidth = panelW + 20;
		this.panelHeight = this.height - 16;
	}

	private Component toggleSquadLabel() {
		return Component.translatable(this.orderViaActiveSquad
			? "screen.nomosiki.squad_commands.order_via_squad_on"
			: "screen.nomosiki.squad_commands.order_via_squad_off");
	}

	private void syncSelectionFromWorld() {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
			return;
		}
		Player player = this.minecraft.player;
		AABB box = player.getBoundingBox().inflate(SCAN_RANGE, SCAN_RANGE, SCAN_RANGE);
		List<NomosikEntity> found = this.minecraft.level.getEntitiesOfClass(NomosikEntity.class, box,
			w -> player.getUUID().equals(w.getOwnerUUID()));
		found.sort(Comparator.comparingDouble(player::distanceTo));
		if (!found.isEmpty()) {
			this.selected = found.get(0).getSquadFormationForGui();
		}
	}

	private void applyFormation() {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		Player player = this.minecraft.player;
		float spacing = this.spacingSendValue;
		Optional<UUID> squadOpt = this.orderViaActiveSquad ? SquadClientState.activeSquadId() : Optional.empty();
		if (squadOpt.isPresent()) {
			SquadCommandClient.sendFormation(this.selected, spacing, List.of(), squadOpt);
			int n = Math.max(1, SquadClientState.memberCountForActiveSquad());
			player.displayClientMessage(Component.translatable("message.nomosiki.squad_formation_set", n, this.selected.formationsScreenLabel()), true);
			return;
		}
		SquadCommandClient.sendFormation(this.selected, spacing, List.of(), Optional.empty());
		player.displayClientMessage(Component.translatable("message.nomosiki.squad_formation_set_all", this.selected.formationsScreenLabel()), true);
	}

	/** Nearest owned nomosik spacing (client synced), clamped to allowed range, else default. */
	private float initialSpacingFromWorld() {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
			return (float) SquadFormationLayout.DEFAULT_SPACING;
		}
		Player player = this.minecraft.player;
		AABB box = player.getBoundingBox().inflate(SCAN_RANGE, SCAN_RANGE, SCAN_RANGE);
		List<NomosikEntity> found = this.minecraft.level.getEntitiesOfClass(NomosikEntity.class, box,
			w -> player.getUUID().equals(w.getOwnerUUID()));
		found.sort(Comparator.comparingDouble(player::distanceTo));
		if (found.isEmpty()) {
			return (float) SquadFormationLayout.DEFAULT_SPACING;
		}
		return (float) Mth.clamp(
			found.get(0).getSquadFormationSpacingForGui(),
			NomosikEntity.SQUAD_FORMATION_SPACING_MIN,
			NomosikEntity.SQUAD_FORMATION_SPACING_MAX
		);
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
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, 18, SakuraStyle.TITLE);
		int cx = this.width / 2;
		Component cur = Component.translatable("screen.nomosiki.squad_formations.current",
			this.selected.formationsScreenLabel());
		SakuraStyle.drawCentered(graphics, this.font, cur, cx, 34, SakuraStyle.INK_MUTED);
		this.renderSquadHint(graphics);
		if (this.formationButtons != null) {
			for (int i = 0; i < GRID_ORDER.length && i < this.formationButtons.length; i++) {
				Button b = this.formationButtons[i];
				if (b != null && GRID_ORDER[i] == this.selected) {
					this.renderFormationSelectionFrame(graphics, b);
					break;
				}
			}
		}
		int previewY = Math.max(200, this.height - 118);
		this.renderPreview(graphics, cx - 40, previewY);
		SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.squad_formations.preview_caption"),
			cx, this.height - 28, SakuraStyle.INK_MUTED);
	}

	private void renderSquadHint(GuiGraphics graphics) {
		Optional<UUID> id = SquadClientState.activeSquadId();
		if (this.orderViaActiveSquad) {
			if (id.isEmpty()) {
				SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.squad_commands.no_active_squad"),
					this.width / 2, 44, SakuraStyle.INK_MUTED);
				return;
			}
			String shortId = id.get().toString().substring(0, 8);
			int n = SquadClientState.memberCountForActiveSquad();
			Component branch = SquadClientState.squadTypeFor(id.get())
				.map(t -> Component.translatable(t.translationKey()))
				.orElseGet(() -> Component.translatable("nomosiki.squad.branch.unknown"));
			SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.squad_commands.active_squad", shortId, branch, n),
				this.width / 2, 44, SakuraStyle.ACCENT);
			return;
		}
		SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.squad_formations.hint_list"),
			this.width / 2, 44, SakuraStyle.INK_MUTED);
	}

	private void renderFormationSelectionFrame(GuiGraphics graphics, Button b) {
		SakuraStyle.drawGoldSelection(graphics, b.getX() - 2, b.getY() - 2, b.getWidth() + 4, b.getHeight() + 4);
	}

	private static void previewSlot(GuiGraphics g, int cx, int cy, int argb) {
		g.fill(cx - 2, cy - 2, cx + 3, cy + 3, argb);
	}

	private static void previewAnchor(GuiGraphics g, int x, int y) {
		g.fill(x, y, x + 4, y + 4, PREVIEW_ANCHOR);
	}

	/**
	 * Schematic top-down preview (anchor = sand square toward “front” of the panel; green = slot).
	 * Matches the convention used for LINE: ranks sit above the anchor row.
	 */
	private void renderPreview(GuiGraphics graphics, int x, int y) {
		SakuraStyle.drawPortraitFrame(graphics, x - 4, y - 4, 88, 48);
		int ax = x + 38;
		int ay = y + 32;
		switch (this.selected) {
			case NONE -> {
				for (int i = 0; i < 5; i++) {
					int px = x + 12 + (i % 3) * 18;
					int py = y + 12 + (i / 3) * 16;
					previewSlot(graphics, px, py, PREVIEW_LOOSE);
				}
			}
			case LINE -> {
				for (int i = 0; i < 5; i++) {
					int px = x + 8 + i * 14;
					graphics.fill(px, y + 18, px + 8, y + 26, PREVIEW_SLOT);
				}
				previewAnchor(graphics, ax, ay);
			}
			case SQUARE -> {
				int midX = x + 40;
				int baseY = y + 10;
				for (int row = 0; row < 3; row++) {
					for (int col = 0; col < 3; col++) {
						previewSlot(graphics, midX + (col - 1) * 9, baseY + row * 9, PREVIEW_SLOT);
					}
				}
				previewAnchor(graphics, ax, ay);
			}
			case TRIANGLE -> {
				int midX = x + 40;
				previewSlot(graphics, midX, y + 8, PREVIEW_SLOT);
				previewSlot(graphics, midX - 7, y + 16, PREVIEW_SLOT);
				previewSlot(graphics, midX + 7, y + 16, PREVIEW_SLOT);
				previewSlot(graphics, midX - 14, y + 24, PREVIEW_SLOT);
				previewSlot(graphics, midX, y + 24, PREVIEW_SLOT);
				previewSlot(graphics, midX + 14, y + 24, PREVIEW_SLOT);
				previewAnchor(graphics, ax, ay);
			}
			case HOLLOW_CIRCLE -> {
				int cx = x + 40;
				int cy = y + 16;
				int n = 12;
				for (int i = 0; i < n; i++) {
					double a = (i + 0.5) * 2.0 * Math.PI / n;
					int px = cx + (int) (Math.cos(a) * 15.0);
					int py = cy + (int) (Math.sin(a) * 9.0);
					previewSlot(graphics, px, py, PREVIEW_SLOT);
				}
				previewAnchor(graphics, ax, ay);
			}
			case HOLLOW_SQUARE -> {
				int minX = x + 22;
				int maxX = x + 58;
				int minY = y + 8;
				int maxY = y + 26;
				int midX = (minX + maxX) / 2;
				int midY = (minY + maxY) / 2;
				previewSlot(graphics, minX, minY, PREVIEW_SLOT);
				previewSlot(graphics, midX, minY, PREVIEW_SLOT);
				previewSlot(graphics, maxX, minY, PREVIEW_SLOT);
				previewSlot(graphics, maxX, midY, PREVIEW_SLOT);
				previewSlot(graphics, maxX, maxY, PREVIEW_SLOT);
				previewSlot(graphics, midX, maxY, PREVIEW_SLOT);
				previewSlot(graphics, minX, maxY, PREVIEW_SLOT);
				previewSlot(graphics, minX, midY, PREVIEW_SLOT);
				previewAnchor(graphics, ax, ay);
			}
			case V -> {
				for (int i = 0; i < 5; i++) {
					double t = i / 4.0;
					int lx = (int) (ax + 2 - t * 17.0);
					int ly = (int) (ay - 3 - t * 22.0);
					previewSlot(graphics, lx, ly, PREVIEW_SLOT);
					int rx = (int) (ax + 2 + t * 17.0);
					previewSlot(graphics, rx, ly, PREVIEW_SLOT);
				}
				previewAnchor(graphics, ax, ay);
			}
			case CIRCLE -> {
				int cx = x + 40;
				int cy = y + 16;
				for (int i = 0; i < 6; i++) {
					double a = i * 2.0 * Math.PI / 6.0;
					previewSlot(graphics, cx + (int) (Math.cos(a) * 6.0), cy + (int) (Math.sin(a) * 4.0), PREVIEW_SLOT);
				}
				for (int i = 0; i < 8; i++) {
					double a = (i + 0.25) * 2.0 * Math.PI / 8.0;
					previewSlot(graphics, cx + (int) (Math.cos(a) * 12.0), cy + (int) (Math.sin(a) * 7.0), PREVIEW_SLOT);
				}
				for (int i = 0; i < 10; i++) {
					double a = i * 2.0 * Math.PI / 10.0;
					previewSlot(graphics, cx + (int) (Math.cos(a) * 18.0), cy + (int) (Math.sin(a) * 10.0), PREVIEW_SLOT);
				}
				previewAnchor(graphics, ax, ay);
			}
			case MOVEMENT -> {
				for (int row = 0; row < 5; row++) {
					previewSlot(graphics, x + 34, y + 8 + row * 5, PREVIEW_SLOT);
					previewSlot(graphics, x + 46, y + 8 + row * 5, PREVIEW_SLOT);
				}
				previewAnchor(graphics, ax, ay);
			}
		}
	}

	@Override
	public void onClose() {
		if (this.minecraft != null && this.returnTo != null) {
			this.minecraft.setScreen(this.returnTo);
			return;
		}
		super.onClose();
	}

	private static double spacingNormalized(float blocks) {
		double clamped = Mth.clamp(
			blocks,
			NomosikEntity.SQUAD_FORMATION_SPACING_MIN,
			NomosikEntity.SQUAD_FORMATION_SPACING_MAX
		);
		double span = NomosikEntity.SQUAD_FORMATION_SPACING_MAX - NomosikEntity.SQUAD_FORMATION_SPACING_MIN;
		return (clamped - NomosikEntity.SQUAD_FORMATION_SPACING_MIN) / span;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
