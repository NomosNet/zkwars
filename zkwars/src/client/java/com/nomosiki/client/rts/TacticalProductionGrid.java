package com.nomosiki.client.rts;

import com.nomosiki.client.match.MatchClient;
import com.nomosiki.client.match.MatchClientState;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.match.BuildingHeartType;
import com.nomosiki.match.MatchBuildingCatalog;
import com.nomosiki.match.MatchRecruitment;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.network.match.payload.c2s.MatchRecruitPayload;
import com.nomosiki.network.match.payload.s2c.MatchHudPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-hand production grid for the selected match building. Stays on the tactical view.
 */
public final class TacticalProductionGrid {
	private TacticalProductionGrid() {
	}

	public record Cell(String action, ItemStack icon, boolean enabled, List<Component> tooltip) {
	}

	public record Layout(int left, int top, int width, int height, int cells) {
		public boolean hit(double mouseX, double mouseY) {
			return mouseX >= this.left && mouseX < this.left + this.width
				&& mouseY >= this.top && mouseY < this.top + this.height;
		}

		public int hitCell(double mouseX, double mouseY) {
			if (!this.hit(mouseX, mouseY)) {
				return -1;
			}
			int col = (int) ((mouseX - this.left - TacticalChrome.GRID_PAD) / TacticalChrome.GRID_CELL);
			int row = (int) ((mouseY - this.top - TacticalChrome.GRID_PAD) / TacticalChrome.GRID_CELL);
			if (col < 0 || col >= TacticalChrome.GRID_COLS || row < 0) {
				return -1;
			}
			int index = row * TacticalChrome.GRID_COLS + col;
			if (index < 0 || index >= this.cells) {
				return -1;
			}
			int localX = (int) (mouseX - this.left - TacticalChrome.GRID_PAD) % TacticalChrome.GRID_CELL;
			int localY = (int) (mouseY - this.top - TacticalChrome.GRID_PAD) % TacticalChrome.GRID_CELL;
			if (localX >= TacticalChrome.GRID_CELL - 2 || localY >= TacticalChrome.GRID_CELL - 2) {
				return -1;
			}
			return index;
		}
	}

	public static Layout layout(int screenWidth, int screenHeight, int cells) {
		int width = TacticalChrome.gridWidth();
		int height = TacticalChrome.gridHeight(cells);
		return new Layout(
			TacticalChrome.gridLeft(screenWidth),
			TacticalChrome.gridTop(screenHeight, cells),
			width,
			height,
			cells
		);
	}

	public static List<Cell> cells(@Nullable BuildingHeartEntity building) {
		List<Cell> out = new ArrayList<>();
		if (building == null || !TacticalChrome.matchRunning()) {
			return out;
		}
		MatchHudPayload hud = MatchClient.last();
		NomosikRace race = NomosikRace.fromId(hud.raceId());
		boolean hq = "hq".equals(building.buildKind()) || building.buildingType() == BuildingHeartType.HQ;
		if (hq) {
			for (MatchRecruitment.Role role : MatchRecruitment.Role.values()) {
				if (MatchBuildingCatalog.alwaysUnlocked(role)) {
					out.add(recruitCell(role, race, hud));
				}
			}
			out.addAll(buildCells(race, hud));
			return out;
		}
		MatchBuildingCatalog.Kind kind = MatchBuildingCatalog.Kind.fromId(building.buildKind());
		if (kind != null) {
			for (MatchRecruitment.Role role : kind.unlocks()) {
				out.add(recruitCell(role, race, hud));
			}
		}
		if (out.isEmpty()) {
			ItemStack icon = kind == null ? new ItemStack(Items.BRICKS) : iconFor(kind);
			Component name = kind == null
				? Component.literal(building.buildKind())
				: Component.translatable("screen.nomosiki.match.build." + kind.id(), kind.cost(race));
			out.add(new Cell("", icon, false, List.of(name)));
		}
		return out;
	}

	/** Build catalog shown when a worker squad is selected. */
	public static List<Cell> buildCells() {
		if (!TacticalChrome.matchRunning()) {
			return List.of();
		}
		MatchHudPayload hud = MatchClient.last();
		return buildCells(NomosikRace.fromId(hud.raceId()), hud);
	}

	private static List<Cell> buildCells(NomosikRace race, MatchHudPayload hud) {
		List<Cell> out = new ArrayList<>();
		for (MatchBuildingCatalog.Kind kind : MatchBuildingCatalog.Kind.all()) {
			out.add(buildCell(kind, race, hud));
		}
		return out;
	}

	public static int render(GuiGraphics graphics, Font font, Layout layout, List<Cell> cells, int mouseX, int mouseY) {
		graphics.fill(layout.left, layout.top, layout.left + layout.width, layout.top + layout.height, 0xCC1A100C);
		graphics.renderOutline(layout.left, layout.top, layout.width, layout.height, 0xFFD4A574);
		int hovered = layout.hitCell(mouseX, mouseY);
		for (int i = 0; i < cells.size(); i++) {
			Cell cell = cells.get(i);
			int col = i % TacticalChrome.GRID_COLS;
			int row = i / TacticalChrome.GRID_COLS;
			int x = layout.left + TacticalChrome.GRID_PAD + col * TacticalChrome.GRID_CELL;
			int y = layout.top + TacticalChrome.GRID_PAD + row * TacticalChrome.GRID_CELL;
			int fill = i == hovered ? 0xFF5A3040 : 0xFF2A1608;
			graphics.fill(x, y, x + 24, y + 24, fill);
			graphics.renderOutline(x, y, 24, 24, cell.enabled ? 0xFFD4A574 : 0xFF6A5048);
			if (!cell.enabled) {
				graphics.setColor(0.45f, 0.45f, 0.45f, 1.0f);
			}
			graphics.renderItem(cell.icon, x + 4, y + 4);
			graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
		return hovered;
	}

	public static boolean click(Layout layout, List<Cell> cells, double mouseX, double mouseY) {
		int index = layout.hitCell(mouseX, mouseY);
		if (index < 0 || index >= cells.size()) {
			return true;
		}
		Cell cell = cells.get(index);
		if (!cell.enabled || cell.action.isEmpty()) {
			return true;
		}
		if (cell.action.startsWith("recruit:")) {
			ClientPlayNetworking.send(new MatchRecruitPayload(cell.action.substring("recruit:".length())));
		} else if (cell.action.startsWith("build:")) {
			MatchClientState.beginBuildPlace(cell.action.substring("build:".length()));
		}
		return true;
	}

	private static Cell recruitCell(MatchRecruitment.Role role, NomosikRace race, MatchHudPayload hud) {
		int cost = role.cost(race);
		int popNeed = role.pop() * MatchRecruitment.squadSize(role);
		boolean raceOk = role.allowedFor(race);
		boolean popOk = hud.pop() + popNeed <= hud.popCap();
		boolean afford = hud.authority() >= cost;
		List<Component> tip = new ArrayList<>();
		tip.add(Component.translatable("screen.nomosiki.match.recruit." + role.id(), cost));
		if (!raceOk) {
			tip.add(Component.translatable("message.nomosiki.match.recruit_denied"));
		} else if (!popOk) {
			tip.add(Component.translatable("message.nomosiki.match.pop_full"));
		} else if (!afford) {
			tip.add(Component.translatable("message.nomosiki.match.need_authority", cost));
		}
		return new Cell("recruit:" + role.id(), iconFor(role), raceOk && popOk && afford, tip);
	}

	private static Cell buildCell(MatchBuildingCatalog.Kind kind, NomosikRace race, MatchHudPayload hud) {
		int cost = kind.cost(race);
		boolean raceOk = kind.allowedFor(race);
		boolean afford = hud.authority() >= cost;
		List<Component> tip = new ArrayList<>();
		tip.add(Component.translatable("screen.nomosiki.match.build." + kind.id(), cost));
		if (!raceOk) {
			tip.add(Component.translatable("message.nomosiki.match.recruit_denied"));
		} else if (!afford) {
			tip.add(Component.translatable("message.nomosiki.match.need_authority", cost));
		}
		return new Cell("build:" + kind.id(), iconFor(kind), raceOk && afford, tip);
	}

	private static ItemStack iconFor(MatchRecruitment.Role role) {
		return new ItemStack(switch (role) {
			case WORKER -> Items.IRON_PICKAXE;
			case INFANTRY -> Items.IRON_SWORD;
			case ANTI_ARMOR -> Items.IRON_AXE;
			case ARCHER -> Items.BOW;
			case CAVALRY -> Items.SADDLE;
			case SIEGE -> Items.TNT;
			case SUPPORT -> Items.GOLDEN_APPLE;
			case ELITE -> Items.DIAMOND_SWORD;
		});
	}

	private static ItemStack iconFor(MatchBuildingCatalog.Kind kind) {
		return new ItemStack(switch (kind) {
			case BARRACKS -> Items.BRICKS;
			case ARCHERY -> Items.FLETCHING_TABLE;
			case STABLE -> Items.HAY_BLOCK;
			case SIEGE_WORKS -> Items.DISPENSER;
			case OUTPOST -> Items.OAK_FENCE;
			case TOWER -> Items.COBBLESTONE_WALL;
		});
	}
}
