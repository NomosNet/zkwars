package com.nomosiki.client.rts;

import com.nomosiki.NomosikiMod;
import com.nomosiki.client.FactionClientState;
import com.nomosiki.client.SquadLabels;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.NinjaEntity;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.faction.SquadBannerStance;
import com.nomosiki.network.squad.payload.s2c.NinjaMarkedPlayersPayload;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.squad.SquadIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Screen-space Total War-style banners above squads in the tactical camera.
 */
public final class SquadBannerRenderer {
	public static final int BANNER_W = 22;
	public static final int BANNER_H = 34;
	private static final double WORLD_Y_OFFSET = 3.2;
	private static final int ICON = 16;

	private static final int OWN_FILL = 0xE0C45C7A;
	private static final int OWN_FILL_SELECTED = 0xF0E07090;
	private static final int FACTION_FILL = 0xE0E89BB0;
	private static final int ALLY_FILL = 0xE04FC3F7;
	private static final int MILITARY_FILL = 0xE01565C0;
	private static final int WAR_FILL = 0xE0C62828;
	private static final int NEUTRAL_FILL = 0xE0F9A825;
	private static final int PLAYER_MARK_FILL = 0xE07B1FA2;

	private static final ResourceLocation ICON_MARK = NomosikiMod.id("textures/gui/rts/cmd_mark.png");

	private static final int ALLY_BORDER = 0xFF29B6F6;
	private static final int ALLY_BORDER_HI = 0xFF81D4FA;
	private static final int MILITARY_BORDER = 0xFF0D47A1;
	private static final int MILITARY_BORDER_HI = 0xFF42A5F5;
	private static final int WAR_BORDER = 0xFFE64A19;
	private static final int WAR_BORDER_HI = 0xFFFF8A50;
	private static final int NEUTRAL_BORDER = 0xFFF57F17;
	private static final int NEUTRAL_BORDER_HI = 0xFFFFD54F;

	private static final int COUNT_LIGHT = 0xFFFFE4EE;
	private static final int COUNT_DARK = 0xFF3A2A20;

	private SquadBannerRenderer() {
	}

	public record Banner(
		UUID squadId,
		Kind kind,
		SquadBannerStance stance,
		byte iconId,
		String name,
		int alive,
		int remainingTicks,
		int left,
		int top,
		double camDistSqr
	) {
		public int right() {
			return this.left + BANNER_W;
		}

		public int bottom() {
			return this.top + BANNER_H;
		}

		public boolean contains(double mx, double my) {
			return mx >= this.left && mx < this.right() && my >= this.top && my < this.bottom();
		}

		public boolean owned() {
			return this.stance == SquadBannerStance.OWN;
		}

		public boolean allowsAttack() {
			return this.stance.allowsAttack();
		}

		public boolean showsStrength() {
			return this.kind == Kind.SQUAD && this.stance != SquadBannerStance.WAR;
		}

		public boolean isPlayerMark() {
			return this.kind == Kind.PLAYER;
		}
	}

	public enum Kind {
		SQUAD,
		PLAYER
	}

	public static List<Banner> collect(Minecraft mc) {
		if (mc.player == null || mc.level == null || !TacticalCameraController.isActive()) {
			return List.of();
		}
		List<Banner> banners = new ArrayList<>();
		collectOwned(mc, banners);
		collectForeign(mc, banners);
		collectMarkedPlayers(mc, banners);
		banners.sort(Comparator.comparingDouble(Banner::camDistSqr).reversed());
		return banners;
	}

	public static Optional<Banner> hit(List<Banner> banners, double mouseX, double mouseY) {
		for (int i = banners.size() - 1; i >= 0; i--) {
			Banner banner = banners.get(i);
			if (banner.contains(mouseX, mouseY)) {
				return Optional.of(banner);
			}
		}
		return Optional.empty();
	}

	public static Optional<Banner> hitAt(Minecraft mc, double mouseX, double mouseY) {
		return hit(collect(mc), mouseX, mouseY);
	}

	public static void render(GuiGraphics graphics, Font font, List<Banner> banners, int mouseX, int mouseY) {
		Banner hovered = hit(banners, mouseX, mouseY).orElse(null);
		for (Banner banner : banners) {
			drawBanner(graphics, font, banner, banner == hovered);
		}
	}

	public static List<Component> tooltip(Banner banner) {
		List<Component> lines = new ArrayList<>(3);
		if (banner.owned()) {
			if (banner.name() != null && !banner.name().isBlank()) {
				lines.add(SquadLabels.component(banner.name()));
			}
		} else if (banner.isPlayerMark()) {
			if (banner.name() != null && !banner.name().isBlank()) {
				lines.add(Component.literal(banner.name()));
			}
			lines.add(Component.translatable("overlay.nomosiki.rts.banner.marked_player"));
			int seconds = Math.max(1, (banner.remainingTicks() + 19) / 20);
			lines.add(Component.translatable("overlay.nomosiki.rts.banner.mark_remaining", seconds));
		} else {
			lines.add(Component.translatable(stanceTooltipKey(banner.stance())));
		}
		if (banner.showsStrength()) {
			lines.add(Component.translatable("overlay.nomosiki.rts.banner.strength", banner.alive()));
		}
		if (banner.allowsAttack()) {
			lines.add(Component.translatable(banner.isPlayerMark()
				? "overlay.nomosiki.rts.banner.attack_player_hint"
				: "overlay.nomosiki.rts.banner.attack_hint"));
		}
		return lines;
	}

	private static String stanceTooltipKey(SquadBannerStance stance) {
		return switch (stance) {
			case FACTION -> "overlay.nomosiki.rts.banner.faction";
			case ALLY -> "overlay.nomosiki.rts.banner.ally";
			case MILITARY_ALLY -> "overlay.nomosiki.rts.banner.military_ally";
			case WAR -> "overlay.nomosiki.rts.banner.war";
			default -> "overlay.nomosiki.rts.banner.neutral";
		};
	}

	private static void collectOwned(Minecraft mc, List<Banner> out) {
		Vec3 cam = TacticalCameraController.getCameraEye();
		for (SquadArmyStatePayload.SquadRow row : RtsClientState.armySquads()) {
			if (row.alive() <= 0) {
				continue;
			}
			Vec3 world = new Vec3(row.centerX(), row.centerY() + WORLD_Y_OFFSET, row.centerZ());
			Banner banner = project(mc, cam, row.squadId(), Kind.SQUAD, SquadBannerStance.OWN, row.iconId(), row.name(), row.alive(), 0, world);
			if (banner != null) {
				out.add(banner);
			}
		}
	}

	private static void collectForeign(Minecraft mc, List<Banner> out) {
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return;
		}
		Vec3 cam = TacticalCameraController.getCameraEye();
		double reach = NomosikiConfig.INSTANCE.rtsTacticalRaycastRange;
		AABB box = new AABB(cam, cam).inflate(reach);
		List<NomosikEntity> found = mc.level.getEntitiesOfClass(
			NomosikEntity.class,
			box,
			n -> n.isAlive() && n.getSquadIdForGui().isPresent() && !player.getUUID().equals(n.getOwnerUUID())
		);
		if (found.isEmpty()) {
			return;
		}
		Map<UUID, Cluster> clusters = new HashMap<>();
		for (NomosikEntity nomosik : found) {
			UUID squadId = nomosik.getSquadIdForGui().orElse(null);
			if (squadId == null) {
				continue;
			}
			if (nomosik instanceof NinjaEntity ninja) {
				SquadBannerStance stance = FactionClientState.stanceToward(ninja.getOwnerUUID());
				if (!stance.isFriendly() && !ninja.isMapRevealed()) {
					continue;
				}
			}
			clusters.computeIfAbsent(squadId, id -> new Cluster()).add(nomosik);
		}
		for (Map.Entry<UUID, Cluster> entry : clusters.entrySet()) {
			Cluster cluster = entry.getValue();
			if (cluster.count <= 0) {
				continue;
			}
			Vec3 world = new Vec3(
				cluster.x / cluster.count,
				cluster.y / cluster.count + WORLD_Y_OFFSET,
				cluster.z / cluster.count
			);
			SquadBannerStance stance = FactionClientState.stanceToward(cluster.ownerId);
			Banner banner = project(mc, cam, entry.getKey(), Kind.SQUAD, stance, (byte) 0, "", cluster.count, 0, world);
			if (banner != null) {
				out.add(banner);
			}
		}
	}

	private static void collectMarkedPlayers(Minecraft mc, List<Banner> out) {
		Vec3 cam = TacticalCameraController.getCameraEye();
		for (NinjaMarkedPlayersPayload.MarkedPlayerRow row : RtsClientState.markedPlayers()) {
			if (row.remainingTicks() <= 0) {
				continue;
			}
			Vec3 world = new Vec3(row.x(), row.y() + WORLD_Y_OFFSET, row.z());
			SquadBannerStance stance = SquadBannerStance.fromId(row.stanceId());
			Banner banner = project(
				mc,
				cam,
				row.playerId(),
				Kind.PLAYER,
				stance,
				(byte) 0,
				row.name(),
				0,
				row.remainingTicks(),
				world
			);
			if (banner != null) {
				out.add(banner);
			}
		}
	}

	@Nullable
	private static Banner project(
		Minecraft mc,
		Vec3 cam,
		UUID squadId,
		Kind kind,
		SquadBannerStance stance,
		byte iconId,
		String name,
		int alive,
		int remainingTicks,
		Vec3 world
	) {
		Vec2 screen = TacticalCursorPicking.worldToScreen(mc, world);
		if (screen == null) {
			return null;
		}
		int left = Math.round(screen.x) - BANNER_W / 2;
		int top = Math.round(screen.y) - BANNER_H;
		return new Banner(
			squadId,
			kind,
			stance,
			iconId,
			name == null ? "" : name,
			alive,
			remainingTicks,
			left,
			top,
			cam.distanceToSqr(world)
		);
	}

	private static void drawBanner(GuiGraphics graphics, Font font, Banner banner, boolean hovered) {
		int x = banner.left();
		int y = banner.top();
		boolean selected = banner.owned() && RtsClientState.isSelected(banner.squadId());
		int fill = banner.isPlayerMark() ? PLAYER_MARK_FILL : fillColor(banner.stance(), selected, hovered);
		if (banner.isPlayerMark() && hovered) {
			fill = lightenArgb(fill);
		}
		graphics.fill(x, y, x + BANNER_W, y + BANNER_H, fill);
		drawFrame(graphics, x, y, banner.stance(), selected, hovered);
		int iconX = x + (BANNER_W - ICON) / 2;
		int iconY = y + 3;
		if (banner.isPlayerMark()) {
			graphics.blit(ICON_MARK, iconX, iconY, 0, 0, ICON, ICON, 32, 32);
		} else {
			graphics.blit(SquadIcons.byId(banner.iconId()), iconX, iconY, 0, 0, ICON, ICON, ICON, ICON);
		}
		if (banner.showsStrength()) {
			String count = String.valueOf(banner.alive());
			int countX = x + (BANNER_W - font.width(count)) / 2;
			graphics.drawString(font, count, countX, y + BANNER_H - 10, countColor(banner.stance()), false);
		}
	}

	private static int fillColor(SquadBannerStance stance, boolean selected, boolean hovered) {
		int fill = switch (stance) {
			case OWN -> selected ? OWN_FILL_SELECTED : OWN_FILL;
			case FACTION -> FACTION_FILL;
			case ALLY -> ALLY_FILL;
			case MILITARY_ALLY -> MILITARY_FILL;
			case WAR -> WAR_FILL;
			case NEUTRAL -> NEUTRAL_FILL;
		};
		return hovered ? lightenArgb(fill) : fill;
	}

	private static int countColor(SquadBannerStance stance) {
		return switch (stance) {
			case OWN, MILITARY_ALLY, WAR -> COUNT_LIGHT;
			case FACTION, ALLY, NEUTRAL -> COUNT_DARK;
		};
	}

	private static void drawFrame(GuiGraphics graphics, int x, int y, SquadBannerStance stance, boolean selected, boolean hovered) {
		int ink = SakuraStyle.FRAME_INK;
		switch (stance) {
			case OWN -> {
				int border = selected || hovered ? SakuraStyle.FRAME_GOLD : SakuraStyle.FRAME_SAKURA;
				int hi = selected || hovered ? SakuraStyle.FRAME_GOLD_HI : SakuraStyle.FRAME_HI;
				SakuraStyle.drawWaFrame(graphics, x, y, BANNER_W, BANNER_H, ink, border, hi);
			}
			case FACTION -> {
				int border = hovered ? SakuraStyle.FRAME_HI : 0xFFF0B8C8;
				SakuraStyle.drawWaFrame(graphics, x, y, BANNER_W, BANNER_H, ink, border, SakuraStyle.FRAME_HI);
			}
			case ALLY -> {
				int border = hovered ? ALLY_BORDER_HI : ALLY_BORDER;
				SakuraStyle.drawWaFrame(graphics, x, y, BANNER_W, BANNER_H, ink, border, ALLY_BORDER_HI);
			}
			case MILITARY_ALLY -> {
				int border = hovered ? MILITARY_BORDER_HI : MILITARY_BORDER;
				SakuraStyle.drawWaFrame(graphics, x, y, BANNER_W, BANNER_H, ink, border, MILITARY_BORDER_HI);
			}
			case WAR -> {
				int border = hovered ? WAR_BORDER_HI : WAR_BORDER;
				SakuraStyle.drawWaFrame(graphics, x, y, BANNER_W, BANNER_H, ink, border, WAR_BORDER_HI);
			}
			case NEUTRAL -> {
				int border = hovered ? NEUTRAL_BORDER_HI : NEUTRAL_BORDER;
				SakuraStyle.drawWaFrame(graphics, x, y, BANNER_W, BANNER_H, ink, border, NEUTRAL_BORDER_HI);
			}
		}
	}

	private static int lightenArgb(int argb) {
		int a = (argb >>> 24) & 0xFF;
		int r = Math.min(255, ((argb >>> 16) & 0xFF) + 28);
		int g = Math.min(255, ((argb >>> 8) & 0xFF) + 28);
		int b = Math.min(255, (argb & 0xFF) + 28);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static final class Cluster {
		private double x;
		private double y;
		private double z;
		private int count;
		private @Nullable UUID ownerId;

		private void add(NomosikEntity nomosik) {
			if (this.ownerId == null) {
				this.ownerId = nomosik.getOwnerUUID();
			}
			this.x += nomosik.getX();
			this.y += nomosik.getY();
			this.z += nomosik.getZ();
			this.count++;
		}
	}
}
