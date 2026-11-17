package com.example.block.work;

public final class QuarryBounds {
	public static final int MAX_HORIZONTAL = 16;
	public static final int MAX_VERTICAL = 64;
	public static final int HORIZONTAL_MIN = -15;
	public static final int HORIZONTAL_MAX = 15;
	public static final int VERTICAL_MIN = -64;
	public static final int VERTICAL_MAX = 63;

	public static final int DEFAULT_MIN_X = -1;
	public static final int DEFAULT_MAX_X = 1;
	public static final int DEFAULT_MIN_Y = -64;
	public static final int DEFAULT_MAX_Y = -1;
	public static final int DEFAULT_MIN_Z = -1;
	public static final int DEFAULT_MAX_Z = 1;

	private QuarryBounds() {
	}

	public static Box defaults() {
		return new Box(DEFAULT_MIN_X, DEFAULT_MAX_X, DEFAULT_MIN_Y, DEFAULT_MAX_Y, DEFAULT_MIN_Z, DEFAULT_MAX_Z);
	}

	public static Box clamp(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		int[] x = clampAxis(minX, maxX, HORIZONTAL_MIN, HORIZONTAL_MAX, MAX_HORIZONTAL);
		int[] y = clampAxis(minY, maxY, VERTICAL_MIN, VERTICAL_MAX, MAX_VERTICAL);
		int[] z = clampAxis(minZ, maxZ, HORIZONTAL_MIN, HORIZONTAL_MAX, MAX_HORIZONTAL);
		return new Box(x[0], x[1], y[0], y[1], z[0], z[1]);
	}

	private static int[] clampAxis(int min, int max, int low, int high, int span) {
		int start = Math.max(low, Math.min(high, min));
		int end = Math.max(low, Math.min(high, max));
		if (start > end) {
			int swap = start;
			start = end;
			end = swap;
		}
		if (end - start + 1 > span) {
			end = start + span - 1;
			if (end > high) {
				end = high;
				start = end - span + 1;
			}
		}
		return new int[] {start, end};
	}

	public record Box(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		public int width() {
			return this.maxX - this.minX + 1;
		}

		public int height() {
			return this.maxY - this.minY + 1;
		}

		public int length() {
			return this.maxZ - this.minZ + 1;
		}

		public int layerSize() {
			return this.width() * this.length();
		}
	}
}
