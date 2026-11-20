package com.nomosiki.client.rts;

import com.nomosiki.client.match.MatchClient;
import com.nomosiki.match.MatchPhase;

/**
 * Shared rectangles for the tactical command chrome. Clicks inside these stay on the panels.
 */
public final class TacticalChrome {
	public static final int MARGIN = 6;
	public static final int TOP_H = 16;
	public static final int MAP = 92;
	public static final int GRID_CELL = 28;
	public static final int GRID_PAD = 4;
	public static final int GRID_COLS = 2;

	private TacticalChrome() {
	}

	public static int cardLeft() {
		return MARGIN + MAP + 4;
	}

	public static int cardTop(int screenHeight) {
		return screenHeight - SquadCardRenderer.CARD_H - MARGIN;
	}

	public static int cardWidth(int screenWidth) {
		return Math.max(80, screenWidth - cardLeft() - gridWidth() - MARGIN * 2);
	}

	public static int mapLeft() {
		return MARGIN;
	}

	public static int mapTop(int screenHeight) {
		return screenHeight - MAP - MARGIN;
	}

	public static int gridWidth() {
		return GRID_PAD * 2 + GRID_COLS * GRID_CELL;
	}

	public static int gridHeight(int cells) {
		int rows = Math.max(1, (Math.max(1, cells) + GRID_COLS - 1) / GRID_COLS);
		return GRID_PAD * 2 + rows * GRID_CELL;
	}

	public static int gridLeft(int screenWidth) {
		return screenWidth - gridWidth() - MARGIN;
	}

	public static int gridTop(int screenHeight, int cells) {
		int top = screenHeight - MARGIN - gridHeight(cells);
		return Math.max(TOP_H + 2, top);
	}

	public static boolean matchRunning() {
		return MatchClient.last().phase() == MatchPhase.RUNNING.id();
	}

	public static boolean hitTop(int screenWidth, double mouseX, double mouseY) {
		return matchRunning()
			&& mouseX >= 0 && mouseX < screenWidth
			&& mouseY >= 0 && mouseY < TOP_H;
	}

	public static boolean hitMap(int screenHeight, double mouseX, double mouseY) {
		int x = mapLeft();
		int y = mapTop(screenHeight);
		return mouseX >= x && mouseX < x + MAP && mouseY >= y && mouseY < y + MAP;
	}
}
