package com.example.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Shared client choice for the three mods. The same file is read by nomosiki, nomos-city and zkwars-workers.
 */
public final class ZkGuiTheme {
	public static final String SAKURA = "sakura";
	public static final String LACQUER = "lacquer";

	public record Palette(
		int ink,
		int inkMuted,
		int inkDisabled,
		int paper,
		int paperInner,
		int button,
		int buttonHover,
		int buttonDisabled,
		int frameInk,
		int frameMid,
		int frameHi,
		int frameGold,
		int frameGoldHi,
		int frameInner,
		int frameDisabled,
		int frameDisabledHi,
		int handle,
		int handleHover,
		int title,
		int accent,
		int dim,
		int portrait,
		int slotFill,
		int slotInner,
		int editFill,
		int row,
		int rowHover,
		int rowSelected,
		int scrollTrack
	) {
	}

	public static final Palette SAKURA_PALETTE = new Palette(
		0xFF5A2434, 0xFF8A5A68, 0xFFA88890,
		0x55F4D6E0, 0x33FADFE8,
		0xFFF2C4D0, 0xFFFFD0DC, 0xFFD4C0C6,
		0xFF5A2434, 0xFFE89BB0, 0xFFFFE4EE,
		0xFFD4A574, 0xFFFFE0A8, 0x665A2434,
		0xFFB898A0, 0xFFD0C0C4,
		0xFFC45C7A, 0xFFE07090,
		0xFF5A2434, 0xFFC45C7A, 0x66101018,
		0xEEFFF8EE, 0xDDF8E8EE, 0xEEFFF5F8, 0xCCFFF5F8,
		0x33FADFE8, 0x66FFD0DC, 0xAAFFD0DC,
		0x66000000
	);

	public static final Palette LACQUER_PALETTE = new Palette(
		0xFFE6E0D4, 0xFFB7AA96, 0xFF8C7E6C,
		0xF016120C, 0xF0231A12,
		0xFF3A2C1E, 0xFF4A3B2A, 0xFF241C16,
		0xFFC4A574, 0xFF6E5434, 0xFFE7D3A1,
		0xFFC4A574, 0xFFFFE1A8, 0xFF6E5434,
		0xFF6E5434, 0xFF8C7E6C,
		0xFF8A6A32, 0xFFC4A574,
		0xFFE7D3A1, 0xFFE7D3A1, 0x88000000,
		0xFF2A2118, 0xFF120E0A, 0xFF1A140E, 0xFF2A2118,
		0xFF241C16, 0xFF3A2C1E, 0xFF4A3B2A,
		0xFF1A140E
	);

	private static String id = SAKURA;
	private static long seenMillis = Long.MIN_VALUE;
	private static Consumer<Palette> applier = palette -> {
	};

	private ZkGuiTheme() {
	}

	public static void bind(Consumer<Palette> applier) {
		ZkGuiTheme.applier = applier;
		readAndApply(true);
	}

	public static void reloadIfChanged() {
		if (modified() != seenMillis) {
			readAndApply(false);
		}
	}

	public static void toggle() {
		id = LACQUER.equals(id) ? SAKURA : LACQUER;
		write();
		seenMillis = modified();
		apply();
	}

	public static boolean lacquer() {
		return LACQUER.equals(id);
	}

	public static Component switchLabel() {
		return Component.translatable(lacquer() ? "gui.zk.theme.sakura" : "gui.zk.theme.lacquer");
	}

	public static Palette current() {
		return lacquer() ? LACQUER_PALETTE : SAKURA_PALETTE;
	}

	private static void readAndApply(boolean force) {
		String next = readId();
		seenMillis = modified();
		if (force || !next.equals(id)) {
			id = next;
			apply();
		}
	}

	private static void apply() {
		applier.accept(current());
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("zk-gui.json");
	}

	private static long modified() {
		try {
			Path path = path();
			return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : 0L;
		} catch (IOException exception) {
			return 0L;
		}
	}

	private static String readId() {
		Path path = path();
		if (!Files.exists(path)) {
			return SAKURA;
		}
		try {
			String text = Files.readString(path);
			if (text.contains("\"lacquer\"")) {
				return LACQUER;
			}
		} catch (IOException ignored) {
		}
		return SAKURA;
	}

	private static void write() {
		try {
			Files.writeString(path(), "{\"theme\":\"" + id + "\"}\n", StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}
}
