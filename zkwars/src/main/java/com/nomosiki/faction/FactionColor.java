package com.nomosiki.faction;

import com.nomosiki.NomosikiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum FactionColor {
	WHITE("white"),
	BLACK("black"),
	RED("red"),
	BLUE("blue"),
	BROWN("brown"),
	CYAN("cyan"),
	DARK_BLUE("dark_blue"),
	DARK_BROWN("dark_brown"),
	DARK_CYAN("dark_cyan"),
	DARK_GREEN("dark_green"),
	DARK_GREY("dark_grey"),
	DARK_RED("dark_red"),
	GOLD("gold"),
	GREEN("green"),
	GREY("grey"),
	LIGHT_BLUE("light_blue"),
	LIGHT_BROWN("light_brown"),
	LIGHT_CYAN("light_cyan"),
	LIGHT_GREEN("light_green"),
	LIGHT_GREY("light_grey"),
	LIGHT_RED("light_red"),
	MAGENTA("magenta"),
	ORANGE("orange"),
	YELLOW("yellow"),
	PURPLE("purple");

	private static final Set<FactionColor> DYE_MAPPED_COLORS = dyeMappedColors();

	private final String id;
	private final ResourceLocation texture;
	private final ResourceLocation ashigaruTexture;
	private final ResourceLocation samuraiTexture;
	private final ResourceLocation britishTexture;
	private final ResourceLocation spanishTexture;
	private final ResourceLocation ninjaTexture;
	private final ResourceLocation monkTexture;
	private final ResourceLocation captainTexture;

	FactionColor(String id) {
		this.id = id;
		this.texture = NomosikiMod.id("textures/entity/team/human_team_" + id + ".png");
		this.ashigaruTexture = NomosikiMod.id("textures/entity/asigaru/" + id + ".png");
		this.samuraiTexture = NomosikiMod.id("textures/entity/samurai/" + id + ".png");
		this.britishTexture = NomosikiMod.id("textures/entity/british/" + id + ".png");
		this.spanishTexture = NomosikiMod.id("textures/entity/spanish/" + id + ".png");
		this.ninjaTexture = NomosikiMod.id("textures/entity/ninja/" + id + ".png");
		this.monkTexture = NomosikiMod.id("textures/entity/monk/" + id + ".png");
		this.captainTexture = NomosikiMod.id("textures/entity/captain/" + id + ".png");
	}

	public String id() {
		return this.id;
	}

	public ResourceLocation texture() {
		return this.texture;
	}

	/** Full infantry skin for the 16 banner {@link DyeColor}s; {@code null} for extra/legacy ids. */
	@Nullable
	public ResourceLocation ashigaruTexture() {
		return DYE_MAPPED_COLORS.contains(this) ? this.ashigaruTexture : null;
	}

	/** Full samurai skin for the 16 banner {@link DyeColor}s; {@code null} for extra/legacy ids. */
	@Nullable
	public ResourceLocation samuraiTexture() {
		return DYE_MAPPED_COLORS.contains(this) ? this.samuraiTexture : null;
	}

	/** Full British skin for the 16 banner {@link DyeColor}s; {@code null} for extra/legacy ids. */
	@Nullable
	public ResourceLocation britishTexture() {
		return DYE_MAPPED_COLORS.contains(this) ? this.britishTexture : null;
	}

	/** Full Spanish skin for the 16 banner {@link DyeColor}s; {@code null} for extra/legacy ids. */
	@Nullable
	public ResourceLocation spanishTexture() {
		return DYE_MAPPED_COLORS.contains(this) ? this.spanishTexture : null;
	}

	/** Full ninja skin for the 16 banner {@link DyeColor}s; {@code null} for extra/legacy ids. */
	@Nullable
	public ResourceLocation ninjaTexture() {
		return DYE_MAPPED_COLORS.contains(this) ? this.ninjaTexture : null;
	}

	/** Full monk skin for the 16 banner {@link DyeColor}s; {@code null} for extra/legacy ids. */
	@Nullable
	public ResourceLocation monkTexture() {
		return DYE_MAPPED_COLORS.contains(this) ? this.monkTexture : null;
	}

	/** Full captain skin for the 16 banner {@link DyeColor}s; {@code null} for extra/legacy ids. */
	@Nullable
	public ResourceLocation captainTexture() {
		return DYE_MAPPED_COLORS.contains(this) ? this.captainTexture : null;
	}

	private static Set<FactionColor> dyeMappedColors() {
		return Set.copyOf(ashigaruColors());
	}

	/** The 16 banner {@link DyeColor}s, in vanilla dye order. */
	public static List<FactionColor> ashigaruColors() {
		List<FactionColor> colors = new ArrayList<>(DyeColor.values().length);
		for (DyeColor dye : DyeColor.values()) {
			colors.add(fromDye(dye));
		}
		return List.copyOf(colors);
	}

	public static FactionColor fromId(String id) {
		for (FactionColor color : values()) {
			if (color.id.equalsIgnoreCase(id)) {
				return color;
			}
		}
		return RED;
	}

	public static FactionColor fromDye(DyeColor dye) {
		return switch (dye) {
			case WHITE -> WHITE;
			case LIGHT_GRAY -> LIGHT_GREY;
			case GRAY -> GREY;
			case BLACK -> BLACK;
			case BROWN -> BROWN;
			case RED -> RED;
			case ORANGE -> ORANGE;
			case YELLOW -> YELLOW;
			case LIME -> LIGHT_GREEN;
			case GREEN -> GREEN;
			case CYAN -> CYAN;
			case LIGHT_BLUE -> LIGHT_BLUE;
			case BLUE -> BLUE;
			case PURPLE -> PURPLE;
			case MAGENTA -> MAGENTA;
			case PINK -> LIGHT_RED;
		};
	}

	/** Nametag and chat color for this banner dye. */
	public int nameRgb() {
		return this.toVanillaDye().getFireworkColor();
	}

	/** Vanilla dye for dyed items (steel samurai armor). Inverse of {@link #fromDye} for the 16 banner colors. */
	public DyeColor toVanillaDye() {
		for (DyeColor dye : DyeColor.values()) {
			if (fromDye(dye) == this) {
				return dye;
			}
		}
		return switch (this) {
			case DARK_GREY -> DyeColor.BLACK;
			case DARK_RED, LIGHT_RED -> DyeColor.RED;
			case DARK_BLUE -> DyeColor.BLUE;
			case DARK_BROWN, LIGHT_BROWN -> DyeColor.BROWN;
			case DARK_CYAN, LIGHT_CYAN -> DyeColor.CYAN;
			case DARK_GREEN -> DyeColor.GREEN;
			case GOLD -> DyeColor.YELLOW;
			default -> DyeColor.RED;
		};
	}
}
