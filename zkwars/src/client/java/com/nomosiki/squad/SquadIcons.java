package com.nomosiki.squad;

import com.nomosiki.NomosikiMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Squad list / editor icons (16×16 PNGs under {@code assets/nomosiki/textures/gui/squad/}).
 * Indices match persisted {@code iconId} on {@link NomosikSquad}.
 */
public final class SquadIcons {
	/** Squad icon textures; indices match persisted {@code iconId} on {@link NomosikSquad}. Append only. */
	public static final List<ResourceLocation> IMAGES = List.of(
		NomosikiMod.id("textures/gui/squad/symbol_01.png"),
		NomosikiMod.id("textures/gui/squad/symbol_02.png"),
		NomosikiMod.id("textures/gui/squad/symbol_03.png"),
		NomosikiMod.id("textures/gui/squad/symbol_04.png"),
		NomosikiMod.id("textures/gui/squad/symbol_05.png"),
		NomosikiMod.id("textures/gui/squad/symbol_06.png"),
		NomosikiMod.id("textures/gui/squad/symbol_07.png"),
		NomosikiMod.id("textures/gui/squad/symbol_08.png"),
		NomosikiMod.id("textures/gui/squad/symbol_09.png"),
		NomosikiMod.id("textures/gui/squad/symbol_10.png"),
		NomosikiMod.id("textures/gui/squad/symbol_11.png"),
		NomosikiMod.id("textures/gui/squad/symbol_12.png"),
		NomosikiMod.id("textures/gui/squad/symbol_13.png"),
		NomosikiMod.id("textures/gui/squad/symbol_14.png"),
		NomosikiMod.id("textures/gui/squad/symbol_15.png"),
		NomosikiMod.id("textures/gui/squad/symbol_16.png")
	);

	private SquadIcons() {
	}

	/** Clamps {@code raw} to a valid index in {@link #IMAGES}. */
	public static byte clampIcon(byte raw) {
		int max = IMAGES.size() - 1;
		int v = raw;
		if (v < 0) {
			return 0;
		}
		if (v > max) {
			return (byte) max;
		}
		return raw;
	}

	public static ResourceLocation byId(byte id) {
		return IMAGES.get(clampIcon(id));
	}
}
