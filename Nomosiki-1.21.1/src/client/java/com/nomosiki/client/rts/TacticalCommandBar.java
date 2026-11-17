package com.nomosiki.client.rts;

import com.nomosiki.NomosikiMod;
import com.nomosiki.client.SquadCommandClient;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.squad.SquadType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Total War-style circular command buttons above the tactical card strip.
 */
public final class TacticalCommandBar {
	public static final int BUTTON_SIZE = 24;
	public static final int GAP = 4;
	public static final int LABEL_H = 10;
	public static final int COUNT = 4;
	public static final int SPACING = 0;
	public static final int STANCE = 1;
	public static final int SHIELD = 2;
	public static final int FORMATION = 3;

	private static final int STRIP_MARGIN = 8;
	private static final int BAR_GAP = 6;
	private static final int ICON = 16;
	private static final int TEX = 32;
	private static final double SHIELD_SCAN_RANGE = 256.0;

	private static final ResourceLocation ICON_SPACING = NomosikiMod.id("textures/gui/rts/cmd_spacing.png");
	private static final ResourceLocation ICON_STANCE_PASSIVE = NomosikiMod.id("textures/gui/rts/cmd_stance_passive.png");
	private static final ResourceLocation ICON_STANCE_NEUTRAL = NomosikiMod.id("textures/gui/rts/cmd_stance_neutral.png");
	private static final ResourceLocation ICON_STANCE_AGGRESSIVE = NomosikiMod.id("textures/gui/rts/cmd_stance_aggressive.png");
	private static final ResourceLocation ICON_STANCE_RAID = NomosikiMod.id("textures/gui/rts/cmd_stance_raid.png");
	private static final ResourceLocation ICON_SHIELD = NomosikiMod.id("textures/gui/rts/cmd_shield.png");
	private static final ResourceLocation ICON_FORM_LOOSE = NomosikiMod.id("textures/gui/rts/form_loose.png");
	private static final ResourceLocation ICON_FORM_LINE = NomosikiMod.id("textures/gui/rts/form_line.png");
	private static final ResourceLocation ICON_FORM_SQUARE = NomosikiMod.id("textures/gui/rts/form_square.png");
	private static final ResourceLocation ICON_FORM_TRIANGLE = NomosikiMod.id("textures/gui/rts/form_triangle.png");
	private static final ResourceLocation ICON_FORM_HOLLOW_CIRCLE = NomosikiMod.id("textures/gui/rts/form_hollow_circle.png");
	private static final ResourceLocation ICON_FORM_HOLLOW_SQUARE = NomosikiMod.id("textures/gui/rts/form_hollow_square.png");
	private static final ResourceLocation ICON_FORM_V = NomosikiMod.id("textures/gui/rts/form_v.png");
	private static final ResourceLocation ICON_FORM_CIRCLE = NomosikiMod.id("textures/gui/rts/form_circle.png");
	private static final ResourceLocation ICON_FORM_MOVEMENT = NomosikiMod.id("textures/gui/rts/form_movement.png");
	private static final ResourceLocation ICON_BOMBARD = NomosikiMod.id("textures/gui/rts/cmd_bombard.png");
	private static final ResourceLocation ICON_MARK = NomosikiMod.id("textures/gui/rts/cmd_mark.png");
	private static final ResourceLocation ICON_SIEGE_MOUNT = NomosikiMod.id("textures/gui/rts/cmd_siege_mount.png");

	private TacticalCommandBar() {
	}

	public record Layout(int left, int top, int width, int height, int buttonCount) {
		public boolean hitBar(double mouseX, double mouseY) {
			return mouseX >= this.left && mouseX < this.left + this.width
				&& mouseY >= this.top && mouseY < this.top + this.height;
		}

		public int hitButton(double mouseX, double mouseY) {
			if (!this.hitBar(mouseX, mouseY)) {
				return -1;
			}
			for (int i = 0; i < this.buttonCount; i++) {
				int x0 = this.left + i * (BUTTON_SIZE + GAP);
				if (mouseX >= x0 && mouseX < x0 + BUTTON_SIZE) {
					return i;
				}
			}
			return -1;
		}

		public int buttonCenterX(int index) {
			return this.left + index * (BUTTON_SIZE + GAP) + BUTTON_SIZE / 2;
		}

		public int buttonCenterY() {
			return this.top + BUTTON_SIZE / 2;
		}
	}

	public static int visibleCount() {
		int count = COUNT;
		if (hasNinjaSelection()) {
			count++;
		}
		if (hasRangedBombardSelection()) {
			count++;
		}
		if (hasArtilleristSelection()) {
			count++;
		}
		return count;
	}

	public static int markButtonIndex() {
		return hasNinjaSelection() ? COUNT : -1;
	}

	public static int bombardButtonIndex() {
		if (!hasRangedBombardSelection()) {
			return -1;
		}
		return COUNT + (hasNinjaSelection() ? 1 : 0);
	}

	public static int siegeMountButtonIndex() {
		if (!hasArtilleristSelection()) {
			return -1;
		}
		int bombard = bombardButtonIndex();
		if (bombard >= 0) {
			return bombard + 1;
		}
		return COUNT + (hasNinjaSelection() ? 1 : 0);
	}

	public static Layout layout(int screenWidth, int screenHeight) {
		int count = visibleCount();
		int width = count * BUTTON_SIZE + (count - 1) * GAP;
		int height = BUTTON_SIZE + LABEL_H;
		int top = screenHeight - SquadCardRenderer.CARD_H - STRIP_MARGIN - ClanArmyClientState.rosterHeight() - BAR_GAP - height;
		return new Layout(STRIP_MARGIN, top, width, height, count);
	}

	public static boolean click(Layout layout, double mouseX, double mouseY) {
		int id = layout.hitButton(mouseX, mouseY);
		if (id < 0) {
			return true;
		}
		if (!hasSelectedSquads()) {
			return true;
		}
		if (id == SPACING) {
			RtsOrderInputHandler.cycleFormationSpacing();
		} else if (id == STANCE) {
			RtsOrderInputHandler.cycleCombatMode();
		} else if (id == SHIELD) {
			RtsOrderInputHandler.toggleShieldStance();
		} else if (id == FORMATION) {
			RtsOrderInputHandler.cycleFormation();
		} else if (id == markButtonIndex()) {
			SquadCommandClient.sendNinjaMark(RtsClientState.orderTargetSquads());
		} else if (id == bombardButtonIndex()) {
			RtsOrderInputHandler.onBombardButton();
		} else if (id == siegeMountButtonIndex()) {
			RtsOrderInputHandler.toggleSiegeMount(Minecraft.getInstance());
		}
		return true;
	}

	public static int render(GuiGraphics graphics, Font font, Minecraft mc, Layout layout, int mouseX, int mouseY) {
		boolean selected = hasSelectedSquads();
		boolean canShield = selected && hasShieldCapableSelection(mc);
		int hovered = layout.hitButton(mouseX, mouseY);
		int spacingIndex = currentSpacingPresetIndex();
		NomosikCombatMode stance = currentCombatMode();
		boolean shieldsUp = currentShieldsUp();
		SquadFormation formation = currentFormation();

		drawSpacingButton(graphics, font, layout, spacingIndex, hovered == SPACING, selected);
		drawStanceButton(graphics, font, layout, stance, hovered == STANCE, selected);
		drawShieldButton(graphics, font, layout, shieldsUp, hovered == SHIELD, canShield);
		drawFormationButton(graphics, font, layout, formation, hovered == FORMATION, selected);
		int markIndex = markButtonIndex();
		if (markIndex >= 0) {
			boolean ninja = selected && hasNinjaSelection();
			drawMarkButton(graphics, font, layout, markIndex, hovered == markIndex, ninja);
		}
		int bombardIndex = bombardButtonIndex();
		if (bombardIndex >= 0) {
			boolean aiming = RtsClientState.bombardDesignate();
			boolean ranged = selected && hasRangedBombardSelection();
			int zoneRadius = RtsClientState.activeBombardRadius().orElse(0);
			int shownRadius = aiming ? RtsClientState.bombardRadius() : zoneRadius;
			drawBombardButton(
				graphics, font, layout, bombardIndex, aiming, zoneRadius > 0, shownRadius, hovered == bombardIndex, ranged
			);
		}
		int siegeIndex = siegeMountButtonIndex();
		if (siegeIndex >= 0) {
			boolean artillery = selected && hasArtilleristSelection();
			boolean mounted = artillery && hasSiegeMountedSelection(mc);
			drawSiegeMountButton(graphics, font, layout, siegeIndex, mounted, hovered == siegeIndex, artillery);
		}
		return hovered;
	}

	public static List<Component> tooltip(int buttonId, Minecraft mc) {
		boolean selected = hasSelectedSquads();
		SquadFormation formation = currentFormation();
		if (buttonId == SPACING) {
			return List.of(
				Component.translatable("tooltip.nomosiki.rts.cmd.spacing"),
				Component.translatable(spacingLabelKey(currentSpacingPresetIndex()))
			);
		}
		if (buttonId == STANCE) {
			return List.of(
				Component.translatable("tooltip.nomosiki.rts.cmd.stance"),
				combatModeLabel(currentCombatMode())
			);
		}
		if (buttonId == SHIELD) {
			return List.of(
				Component.translatable(
					selected && hasShieldCapableSelection(mc)
						? "tooltip.nomosiki.rts.cmd.shield"
						: "tooltip.nomosiki.rts.cmd.shield_none"
				),
				Component.translatable(currentShieldsUp()
					? "overlay.nomosiki.rts.cmd.shield.up"
					: "overlay.nomosiki.rts.cmd.shield.down")
			);
		}
		if (buttonId == FORMATION) {
			return List.of(
				Component.translatable("tooltip.nomosiki.rts.cmd.formation"),
				formation.formationsScreenLabel(),
				Component.translatable("tooltip.nomosiki.squad_formations." + formation.formationsScreenSuffix())
			);
		}
		if (buttonId == markButtonIndex()) {
			return List.of(Component.translatable("tooltip.nomosiki.rts.cmd.mark"));
		}
		if (buttonId == bombardButtonIndex()) {
			if (RtsClientState.bombardDesignate()) {
				return List.of(
					Component.translatable("tooltip.nomosiki.rts.cmd.bombard"),
					Component.translatable("overlay.nomosiki.rts.bombard_hint", RtsClientState.bombardRadius())
				);
			}
			OptionalInt radius = RtsClientState.activeBombardRadius();
			if (radius.isPresent()) {
				return List.of(
					Component.translatable("tooltip.nomosiki.squad.bombard_clear"),
					Component.translatable("screen.nomosiki.squad_commands.bombard_status", radius.getAsInt())
				);
			}
			return List.of(Component.translatable("tooltip.nomosiki.rts.cmd.bombard"));
		}
		if (buttonId == siegeMountButtonIndex()) {
			return List.of(
				Component.translatable(
					hasSiegeMountedSelection(mc)
						? "tooltip.nomosiki.rts.cmd.siege_dismount"
						: "tooltip.nomosiki.rts.cmd.siege_mount"
				)
			);
		}
		return List.of();
	}

	public static boolean hasShieldCapableSelection(Minecraft mc) {
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return false;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return false;
		}
		var box = player.getBoundingBox().inflate(SHIELD_SCAN_RANGE, SHIELD_SCAN_RANGE, SHIELD_SCAN_RANGE);
		List<NomosikEntity> found = mc.level.getEntitiesOfClass(
			NomosikEntity.class,
			box,
			n -> n.isAlive()
				&& player.getUUID().equals(n.getOwnerUUID())
				&& n.getSquadIdForGui().filter(squadIds::contains).isPresent()
				&& n.canUseShieldStance()
		);
		return !found.isEmpty();
	}

	public static boolean hasNinjaSelection() {
		for (UUID squadId : RtsClientState.orderTargetSquads()) {
			if (RtsClientState.squadTypeFor(squadId).orElse(null) == SquadType.NINJA) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasRangedBombardSelection() {
		for (UUID squadId : RtsClientState.orderTargetSquads()) {
			if (RtsClientState.squadTypeFor(squadId)
				.map(type -> BattleHudOverlay.isRangedBranch(type.id()))
				.orElse(false)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasArtilleristSelection() {
		for (UUID squadId : RtsClientState.orderTargetSquads()) {
			if (RtsClientState.squadTypeFor(squadId).map(SquadType::isArtillerist).orElse(false)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasSiegeMountedSelection(Minecraft mc) {
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return false;
		}
		List<UUID> squadIds = RtsClientState.orderTargetSquads();
		if (squadIds.isEmpty()) {
			return false;
		}
		var box = player.getBoundingBox().inflate(SHIELD_SCAN_RANGE, SHIELD_SCAN_RANGE, SHIELD_SCAN_RANGE);
		List<NomosikEntity> found = mc.level.getEntitiesOfClass(
			NomosikEntity.class,
			box,
			n -> n.isAlive()
				&& n.isArtillerist()
				&& n.isPassenger()
				&& player.getUUID().equals(n.getOwnerUUID())
				&& n.getSquadIdForGui().filter(squadIds::contains).isPresent()
		);
		return !found.isEmpty();
	}

	private static void drawSpacingButton(GuiGraphics graphics, Font font, Layout layout, int presetIndex, boolean hovered, boolean active) {
		int cx = layout.buttonCenterX(SPACING);
		int cy = layout.buttonCenterY();
		SakuraStyle.drawCircleButton(graphics, cx, cy, BUTTON_SIZE / 2, hovered, active, false);
		drawIcon(graphics, ICON_SPACING, cx, cy, active);
		Component icon = Component.literal(String.valueOf(presetIndex + 1));
		drawCenteredIcon(graphics, font, icon, cx, cy, active);
		drawLabel(graphics, font, Component.translatable(spacingLabelKey(presetIndex)), cx, layout.top + BUTTON_SIZE, active);
	}

	private static void drawStanceButton(
		GuiGraphics graphics,
		Font font,
		Layout layout,
		NomosikCombatMode stance,
		boolean hovered,
		boolean active
	) {
		int cx = layout.buttonCenterX(STANCE);
		int cy = layout.buttonCenterY();
		SakuraStyle.drawCircleButton(graphics, cx, cy, BUTTON_SIZE / 2, hovered, active, false);
		if (active) {
			fillInnerDisc(graphics, cx, cy, 9, stanceTint(stance));
		}
		drawIcon(graphics, stanceIcon(stance), cx, cy, active);
		drawLabel(graphics, font, stanceShortLabel(stance), cx, layout.top + BUTTON_SIZE, active);
	}

	private static void drawShieldButton(
		GuiGraphics graphics,
		Font font,
		Layout layout,
		boolean up,
		boolean hovered,
		boolean active
	) {
		int cx = layout.buttonCenterX(SHIELD);
		int cy = layout.buttonCenterY();
		SakuraStyle.drawCircleButton(graphics, cx, cy, BUTTON_SIZE / 2, hovered, active, active && up);
		drawIcon(graphics, ICON_SHIELD, cx, cy, active);
		Component label = Component.translatable(up
			? "overlay.nomosiki.rts.cmd.shield.up"
			: "overlay.nomosiki.rts.cmd.shield.down");
		drawLabel(graphics, font, label, cx, layout.top + BUTTON_SIZE, active);
	}

	private static void drawFormationButton(
		GuiGraphics graphics,
		Font font,
		Layout layout,
		SquadFormation formation,
		boolean hovered,
		boolean active
	) {
		int cx = layout.buttonCenterX(FORMATION);
		int cy = layout.buttonCenterY();
		SakuraStyle.drawCircleButton(graphics, cx, cy, BUTTON_SIZE / 2, hovered, active, false);
		drawIcon(graphics, formationIcon(formation), cx, cy, active);
		drawLabel(graphics, font, formationShortLabel(formation), cx, layout.top + BUTTON_SIZE, active);
	}

	private static void drawMarkButton(
		GuiGraphics graphics,
		Font font,
		Layout layout,
		int index,
		boolean hovered,
		boolean active
	) {
		int cx = layout.buttonCenterX(index);
		int cy = layout.buttonCenterY();
		SakuraStyle.drawCircleButton(graphics, cx, cy, BUTTON_SIZE / 2, hovered, active, false);
		drawIcon(graphics, ICON_MARK, cx, cy, active);
		drawLabel(graphics, font, Component.translatable("overlay.nomosiki.rts.cmd.mark"), cx, layout.top + BUTTON_SIZE, active);
	}

	private static void drawBombardButton(
		GuiGraphics graphics,
		Font font,
		Layout layout,
		int index,
		boolean aiming,
		boolean zoneSet,
		int zoneRadius,
		boolean hovered,
		boolean active
	) {
		int cx = layout.buttonCenterX(index);
		int cy = layout.buttonCenterY();
		SakuraStyle.drawCircleButton(graphics, cx, cy, BUTTON_SIZE / 2, hovered, active, active && aiming);
		drawIcon(graphics, ICON_BOMBARD, cx, cy, active);
		Component label = aiming
			? Component.translatable("overlay.nomosiki.rts.cmd.bombard.radius", zoneRadius)
			: zoneSet
				? Component.translatable("overlay.nomosiki.rts.cmd.bombard.clear")
				: Component.translatable("overlay.nomosiki.rts.cmd.bombard.idle");
		drawLabel(graphics, font, label, cx, layout.top + BUTTON_SIZE, active);
	}

	private static void drawSiegeMountButton(
		GuiGraphics graphics,
		Font font,
		Layout layout,
		int index,
		boolean mounted,
		boolean hovered,
		boolean active
	) {
		int cx = layout.buttonCenterX(index);
		int cy = layout.buttonCenterY();
		SakuraStyle.drawCircleButton(graphics, cx, cy, BUTTON_SIZE / 2, hovered, active, active && mounted);
		drawIcon(graphics, ICON_SIEGE_MOUNT, cx, cy, active);
		Component label = Component.translatable(mounted
			? "overlay.nomosiki.rts.cmd.siege_dismount"
			: "overlay.nomosiki.rts.cmd.siege_mount");
		drawLabel(graphics, font, label, cx, layout.top + BUTTON_SIZE, active);
	}

	private static void drawIcon(GuiGraphics graphics, ResourceLocation texture, int cx, int cy, boolean active) {
		int x = cx - ICON / 2;
		int y = cy - ICON / 2;
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		float scale = ICON / (float) TEX;
		graphics.pose().scale(scale, scale, 1.0f);
		if (!active) {
			graphics.setColor(0.72f, 0.68f, 0.70f, 0.85f);
		}
		graphics.blit(texture, 0, 0, 0, 0, TEX, TEX, TEX, TEX);
		graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.pose().popPose();
	}

	private static void drawCenteredIcon(GuiGraphics graphics, Font font, Component text, int cx, int cy, boolean active) {
		int color = active ? SakuraStyle.INK : SakuraStyle.INK_DISABLED;
		int tw = font.width(text);
		graphics.drawString(font, text, cx - tw / 2, cy - 4, color, false);
	}

	private static void drawLabel(GuiGraphics graphics, Font font, Component text, int cx, int y, boolean active) {
		int color = active ? SakuraStyle.INK : SakuraStyle.INK_DISABLED;
		SakuraStyle.drawCentered(graphics, font, text, cx, y, color);
	}

	private static void fillInnerDisc(GuiGraphics graphics, int cx, int cy, int radius, int color) {
		int r2 = radius * radius;
		for (int dy = -radius; dy <= radius; dy++) {
			int remain = r2 - dy * dy;
			if (remain < 0) {
				continue;
			}
			int dx = (int) Math.floor(Math.sqrt(remain));
			graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	private static boolean hasSelectedSquads() {
		List<UUID> squads = RtsClientState.orderTargetSquads();
		return !squads.isEmpty() && RtsClientState.totalAliveInSquads(squads) > 0;
	}

	private static int currentSpacingPresetIndex() {
		float spacing = RtsClientState.activeRow()
			.map(SquadArmyStatePayload.SquadRow::formationSpacing)
			.orElse(RtsOrderInputHandler.FORMATION_SPACING_PRESETS[1]);
		return RtsOrderInputHandler.formationSpacingPresetIndex(spacing);
	}

	private static NomosikCombatMode currentCombatMode() {
		return RtsClientState.activeRow()
			.map(row -> NomosikCombatMode.fromId(row.combatModeId()))
			.orElse(NomosikCombatMode.NEUTRAL);
	}

	private static boolean currentShieldsUp() {
		return RtsClientState.activeRow()
			.map(SquadArmyStatePayload.SquadRow::shieldStance)
			.orElse(false);
	}

	private static SquadFormation currentFormation() {
		return RtsClientState.activeRow()
			.map(row -> SquadFormation.fromId(row.formationId()))
			.orElse(SquadFormation.LINE);
	}

	private static String spacingLabelKey(int presetIndex) {
		return switch (presetIndex) {
			case 0 -> "overlay.nomosiki.rts.cmd.spacing.1";
			case 2 -> "overlay.nomosiki.rts.cmd.spacing.3";
			default -> "overlay.nomosiki.rts.cmd.spacing.2";
		};
	}

	private static Component stanceShortLabel(NomosikCombatMode mode) {
		return Component.translatable(switch (mode) {
			case PASSIVE -> "overlay.nomosiki.rts.cmd.stance.passive";
			case NEUTRAL -> "overlay.nomosiki.rts.cmd.stance.neutral";
			case AGGRESSIVE -> "overlay.nomosiki.rts.cmd.stance.aggressive";
			case RAID -> "overlay.nomosiki.rts.cmd.stance.raid";
		});
	}

	private static Component formationShortLabel(SquadFormation formation) {
		return Component.translatable("overlay.nomosiki.rts.cmd.formation." + formation.formationsScreenSuffix());
	}

	static Component combatModeLabel(NomosikCombatMode mode) {
		return Component.translatable(switch (mode) {
			case PASSIVE -> "button.nomosiki.combat_mode.passive";
			case NEUTRAL -> "button.nomosiki.combat_mode.neutral";
			case AGGRESSIVE -> "button.nomosiki.combat_mode.aggressive";
			case RAID -> "button.nomosiki.combat_mode.raid";
		});
	}

	private static ResourceLocation stanceIcon(NomosikCombatMode mode) {
		return switch (mode) {
			case PASSIVE -> ICON_STANCE_PASSIVE;
			case NEUTRAL -> ICON_STANCE_NEUTRAL;
			case AGGRESSIVE -> ICON_STANCE_AGGRESSIVE;
			case RAID -> ICON_STANCE_RAID;
		};
	}

	private static ResourceLocation formationIcon(SquadFormation formation) {
		return switch (formation) {
			case NONE -> ICON_FORM_LOOSE;
			case LINE -> ICON_FORM_LINE;
			case SQUARE -> ICON_FORM_SQUARE;
			case TRIANGLE -> ICON_FORM_TRIANGLE;
			case HOLLOW_CIRCLE -> ICON_FORM_HOLLOW_CIRCLE;
			case HOLLOW_SQUARE -> ICON_FORM_HOLLOW_SQUARE;
			case V -> ICON_FORM_V;
			case CIRCLE -> ICON_FORM_CIRCLE;
			case MOVEMENT -> ICON_FORM_MOVEMENT;
		};
	}

	private static int stanceTint(NomosikCombatMode mode) {
		return switch (mode) {
			case PASSIVE -> 0xFF8A9A7A;
			case NEUTRAL -> 0xFFD4A574;
			case AGGRESSIVE -> 0xFFC45C7A;
			case RAID -> 0xFF8B2E3A;
		};
	}
}
