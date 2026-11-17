package com.nomosiki.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Mth;
import com.nomosiki.NomosikiMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Client-oriented defaults; file lives in {@code config/nomosiki/client.json}.
 */
public final class NomosikiConfig {
	public static final NomosikiConfig INSTANCE = new NomosikiConfig();

	/** Initial attack-zone radius when opening squad screen (also used by slider default). */
	public int defaultAttackZoneRadius = 8;
	/** If true, squad screen closes after issuing an order; if false, stays open for another order. */
	public boolean squadScreenCloseAfterOrder = true;

	/** Master switch for RTS mouse / hotkey orders in the world. */
	public boolean rtsEnabled = true;
	/** Show the battle HUD overlay when an active squad is set. */
	public boolean rtsShowBattleHud = true;
	/** HUD panel opacity 0.0–1.0. */
	public float rtsHudOpacity = 0.85f;
	/** Max queued waypoints per squad (client hint; server enforces via common.json). */
	public int rtsMaxWaypoints = 5;
	/** Tactical top-down camera (T key). */
	public boolean rtsTacticalCameraEnabled = true;
	public float rtsTacticalFovBoost = 12f;
	public int rtsTacticalRaycastRange = 256;
	public float rtsTacticalPanSpeed = 1.25f;
	public float rtsTacticalMinHeight = 16f;
	public float rtsTacticalMaxHeight = 96f;
	public float rtsTacticalDefaultHeight = 40f;
	public float rtsTacticalPitch = 45f;
	public float rtsTacticalZoomStep = 4f;

	private NomosikiConfig() {
	}

	public Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("nomosiki").resolve("client.json");
	}

	private static Path legacyPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("nomosiki.json");
	}

	public void load() {
		migrateLegacyIfNeeded();
		Path path = configPath();
		if (!Files.isRegularFile(path)) {
			return;
		}
		try (var reader = Files.newBufferedReader(path)) {
			JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
			if (o.has("defaultAttackZoneRadius")) {
				this.defaultAttackZoneRadius = Mth.clamp(o.get("defaultAttackZoneRadius").getAsInt(), 4, 24);
			}
			if (o.has("squadScreenCloseAfterOrder")) {
				this.squadScreenCloseAfterOrder = o.get("squadScreenCloseAfterOrder").getAsBoolean();
			}
			if (o.has("rtsEnabled")) {
				this.rtsEnabled = o.get("rtsEnabled").getAsBoolean();
			}
			if (o.has("rtsShowBattleHud")) {
				this.rtsShowBattleHud = o.get("rtsShowBattleHud").getAsBoolean();
			}
			if (o.has("rtsHudOpacity")) {
				this.rtsHudOpacity = Mth.clamp(o.get("rtsHudOpacity").getAsFloat(), 0.1f, 1.0f);
			}
			if (o.has("rtsMaxWaypoints")) {
				this.rtsMaxWaypoints = Mth.clamp(o.get("rtsMaxWaypoints").getAsInt(), 1, 8);
			}
			if (o.has("rtsTacticalCameraEnabled")) {
				this.rtsTacticalCameraEnabled = o.get("rtsTacticalCameraEnabled").getAsBoolean();
			}
			if (o.has("rtsTacticalFovBoost")) {
				this.rtsTacticalFovBoost = Mth.clamp(o.get("rtsTacticalFovBoost").getAsFloat(), 0f, 30f);
			}
			if (o.has("rtsTacticalRaycastRange")) {
				this.rtsTacticalRaycastRange = Mth.clamp(o.get("rtsTacticalRaycastRange").getAsInt(), 64, 512);
			}
			if (o.has("rtsTacticalPanSpeed")) {
				this.rtsTacticalPanSpeed = Mth.clamp(o.get("rtsTacticalPanSpeed").getAsFloat(), 0.25f, 8f);
			}
			if (o.has("rtsTacticalMinHeight")) {
				this.rtsTacticalMinHeight = Mth.clamp(o.get("rtsTacticalMinHeight").getAsFloat(), 8f, 128f);
			}
			if (o.has("rtsTacticalMaxHeight")) {
				this.rtsTacticalMaxHeight = Mth.clamp(o.get("rtsTacticalMaxHeight").getAsFloat(), 16f, 256f);
			}
			if (o.has("rtsTacticalDefaultHeight")) {
				this.rtsTacticalDefaultHeight = Mth.clamp(o.get("rtsTacticalDefaultHeight").getAsFloat(), 8f, 256f);
			}
			if (o.has("rtsTacticalPitch")) {
				this.rtsTacticalPitch = Mth.clamp(o.get("rtsTacticalPitch").getAsFloat(), 20f, 80f);
			}
			if (o.has("rtsTacticalZoomStep")) {
				this.rtsTacticalZoomStep = Mth.clamp(o.get("rtsTacticalZoomStep").getAsFloat(), 1f, 16f);
			}
		} catch (Exception e) {
			NomosikiMod.LOGGER.warn("nomosiki: failed to load config {}", path, e);
		}
	}

	private void migrateLegacyIfNeeded() {
		Path path = configPath();
		Path legacy = legacyPath();
		if (Files.isRegularFile(path) || !Files.isRegularFile(legacy)) {
			return;
		}
		try {
			Files.createDirectories(path.getParent());
			Files.copy(legacy, path, StandardCopyOption.REPLACE_EXISTING);
			NomosikiMod.LOGGER.info("nomosiki: migrated client config {} → {}", legacy, path);
		} catch (IOException e) {
			NomosikiMod.LOGGER.warn("nomosiki: failed to migrate client config from {}", legacy, e);
		}
	}

	public void saveIfAbsent() {
		Path path = configPath();
		if (Files.exists(path)) {
			return;
		}
		try {
			Files.createDirectories(path.getParent());
			JsonObject o = new JsonObject();
			o.addProperty("defaultAttackZoneRadius", this.defaultAttackZoneRadius);
			o.addProperty("squadScreenCloseAfterOrder", this.squadScreenCloseAfterOrder);
			o.addProperty("rtsEnabled", this.rtsEnabled);
			o.addProperty("rtsShowBattleHud", this.rtsShowBattleHud);
			o.addProperty("rtsHudOpacity", this.rtsHudOpacity);
			o.addProperty("rtsMaxWaypoints", this.rtsMaxWaypoints);
			o.addProperty("rtsTacticalCameraEnabled", this.rtsTacticalCameraEnabled);
			o.addProperty("rtsTacticalFovBoost", this.rtsTacticalFovBoost);
			o.addProperty("rtsTacticalRaycastRange", this.rtsTacticalRaycastRange);
			o.addProperty("rtsTacticalPanSpeed", this.rtsTacticalPanSpeed);
			o.addProperty("rtsTacticalMinHeight", this.rtsTacticalMinHeight);
			o.addProperty("rtsTacticalMaxHeight", this.rtsTacticalMaxHeight);
			o.addProperty("rtsTacticalDefaultHeight", this.rtsTacticalDefaultHeight);
			o.addProperty("rtsTacticalPitch", this.rtsTacticalPitch);
			o.addProperty("rtsTacticalZoomStep", this.rtsTacticalZoomStep);
			try (BufferedWriter w = Files.newBufferedWriter(path)) {
				new GsonBuilder().setPrettyPrinting().create().toJson(o, w);
			}
		} catch (IOException e) {
			NomosikiMod.LOGGER.warn("nomosiki: failed to write default config", e);
		}
	}
}
