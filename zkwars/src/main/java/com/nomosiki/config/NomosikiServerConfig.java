package com.nomosiki.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nomosiki.NomosikiMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared server settings; file lives in {@code config/nomosiki/common.json}.
 * This config is read once during mod init; changes require a restart.
 */
public final class NomosikiServerConfig {
	public static final NomosikiServerConfig INSTANCE = new NomosikiServerConfig();

	public final Map<String, Weapon> weapons = new LinkedHashMap<>();
	public final Map<String, ArmorSet> armor = new LinkedHashMap<>();

	/** Max distance (blocks) from player to squad member for RTS / squad orders. */
	public int rtsCommandRange = 128;
	/** Max queued waypoints per squad (RTS phase 3). */
	public int rtsMaxWaypoints = 5;
	/** When a waypoint route finishes, use FOLLOW instead of HOLD. */
	public boolean rtsWaypointIdleFollow = false;
	/** Blocks per forward/backward formation step (Recruits default ~10). */
	public int rtsFormationStepBlocks = 10;

	/** Squad humanoid emotes (R GUI wheel). */
	public boolean emotesEnabled = true;
	public boolean emoteInterruptOnDamage = true;
	public boolean emoteInterruptOnOrder = true;
	/** Safety cap; looping emotes are stopped after this many ticks. */
	public int emoteMaxDurationTicks = 200;

	/** Discord Incoming Webhook notifications for diplomacy events. */
	public final Discord discord = new Discord();

	/** Duel refusal webhook. Create it in a channel other than politics. */
	public final DuelDiscord duelDiscord = new DuelDiscord();

	/** Ranged/melee combat tuning for nomosiki units. */
	public final Combat combat = new Combat();

	/** Ninja movement and tactical mark ability. */
	public final NinjaSettings ninja = new NinjaSettings();

	/** Dawn-of-War style match mode. */
	public final MatchSettings match = new MatchSettings();

	private NomosikiServerConfig() {
		weapons.put("katana", new Weapon(6.0, -2.4, 500));
	}

	public Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("zkwars").resolve("common.json");
	}

	private static Path legacyPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("nomosiki-server.json");
	}

	/**
	 * Loads common.json. Call once from mod init.
	 */
	public void loadAll() {
		migrateLegacyIfNeeded();
		load();
		saveIfAbsent();
	}

	public void load() {
		Path path = configPath();
		if (!Files.isRegularFile(path)) {
			return;
		}
		try (var reader = Files.newBufferedReader(path)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			loadFromRoot(root);
		} catch (Exception ex) {
			NomosikiMod.LOGGER.warn("zkwars: failed to load common config {}", path, ex);
		}
	}

	/** Package-private for legacy migration of role fields from old nomosiki-server.json. */
	void loadFromRoot(JsonObject root) {
		if (root.has("weapons") && root.get("weapons").isJsonObject()) {
			JsonObject w = root.getAsJsonObject("weapons");
			for (Map.Entry<String, JsonElement> e : w.entrySet()) {
				if (!e.getValue().isJsonObject()) continue;
				Weapon current = weapons.getOrDefault(e.getKey(), new Weapon(6.0, -2.4, 500));
				JsonObject o = e.getValue().getAsJsonObject();
				current.attackDamage = getDouble(o, "attackDamage", current.attackDamage);
				current.attackSpeed = getDouble(o, "attackSpeed", current.attackSpeed);
				current.durability = Math.max(1, getInt(o, "durability", current.durability));
				weapons.put(e.getKey(), current);
			}
		}

		if (root.has("armor") && root.get("armor").isJsonObject()) {
			JsonObject a = root.getAsJsonObject("armor");
			for (Map.Entry<String, JsonElement> e : a.entrySet()) {
				if (!e.getValue().isJsonObject()) continue;
				ArmorSet current = armor.getOrDefault(e.getKey(), defaultArmor());
				JsonObject o = e.getValue().getAsJsonObject();
				if (o.has("defense") && o.get("defense").isJsonObject()) {
					JsonObject d = o.getAsJsonObject("defense");
					current.defense.head = Math.max(0, getInt(d, "head", current.defense.head));
					current.defense.chest = Math.max(0, getInt(d, "chest", current.defense.chest));
					current.defense.legs = Math.max(0, getInt(d, "legs", current.defense.legs));
					current.defense.feet = Math.max(0, getInt(d, "feet", current.defense.feet));
				}
				current.enchantability = Math.max(0, getInt(o, "enchantability", current.enchantability));
				current.toughness = (float) getDouble(o, "toughness", current.toughness);
				current.knockbackResistance = (float) getDouble(o, "knockbackResistance", current.knockbackResistance);
				current.durability = Math.max(1, getInt(o, "durability", current.durability));
				current.equipSound = getString(o, "equipSound", current.equipSound);
				armor.put(e.getKey(), current);
			}
		}

		if (root.has("rtsCommandRange")) {
			this.rtsCommandRange = Math.max(16, Math.min(512, root.get("rtsCommandRange").getAsInt()));
		}
		if (root.has("rtsMaxWaypoints")) {
			this.rtsMaxWaypoints = Math.max(1, Math.min(8, root.get("rtsMaxWaypoints").getAsInt()));
		}
		if (root.has("rtsWaypointIdleFollow")) {
			this.rtsWaypointIdleFollow = root.get("rtsWaypointIdleFollow").getAsBoolean();
		}
		if (root.has("rtsFormationStepBlocks")) {
			this.rtsFormationStepBlocks = Math.max(2, Math.min(32, root.get("rtsFormationStepBlocks").getAsInt()));
		}
		if (root.has("emotesEnabled")) {
			this.emotesEnabled = root.get("emotesEnabled").getAsBoolean();
		}
		if (root.has("emoteInterruptOnDamage")) {
			this.emoteInterruptOnDamage = root.get("emoteInterruptOnDamage").getAsBoolean();
		}
		if (root.has("emoteInterruptOnOrder")) {
			this.emoteInterruptOnOrder = root.get("emoteInterruptOnOrder").getAsBoolean();
		}
		if (root.has("emoteMaxDurationTicks")) {
			this.emoteMaxDurationTicks = Math.max(20, Math.min(1200, root.get("emoteMaxDurationTicks").getAsInt()));
		}

		if (root.has("discord") && root.get("discord").isJsonObject()) {
			loadDiscord(root.getAsJsonObject("discord"));
		}
		if (root.has("duelDiscord") && root.get("duelDiscord").isJsonObject()) {
			loadDuelDiscord(root.getAsJsonObject("duelDiscord"));
		}

		if (root.has("combat") && root.get("combat").isJsonObject()) {
			loadCombat(root.getAsJsonObject("combat"));
		}

		if (root.has("ninja") && root.get("ninja").isJsonObject()) {
			loadNinja(root.getAsJsonObject("ninja"));
		}
		if (root.has("match") && root.get("match").isJsonObject()) {
			loadMatch(root.getAsJsonObject("match"));
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
			NomosikiMod.LOGGER.info("zkwars: migrated server config {} → {}", legacy, path);
		} catch (Exception e) {
			NomosikiMod.LOGGER.warn("zkwars: failed to migrate server config from {}", legacy, e);
		}
	}

	private void loadCombat(JsonObject root) {
		if (root.has("bow") && root.get("bow").isJsonObject()) {
			JsonObject o = root.getAsJsonObject("bow");
			BowSettings bow = this.combat.bow;
			bow.arrowDamage = getDouble(o, "arrowDamage", bow.arrowDamage);
			bow.velocity = (float) getDouble(o, "velocity", bow.velocity);
			bow.inaccuracy = (float) getDouble(o, "inaccuracy", bow.inaccuracy);
			bow.drawTicksMin = Math.max(5, getInt(o, "drawTicksMin", bow.drawTicksMin));
			bow.maxRange = getDouble(o, "maxRange", bow.maxRange);
			bow.mountedInaccuracyBonus = (float) getDouble(o, "mountedInaccuracyBonus", bow.mountedInaccuracyBonus);
		}
		if (root.has("cavalry") && root.get("cavalry").isJsonObject()) {
			JsonObject o = root.getAsJsonObject("cavalry");
			CavalrySettings cavalry = this.combat.cavalry;
			cavalry.horseMovementSpeed = clampHorseSpeed(getDouble(o, "horseMovementSpeed", cavalry.horseMovementSpeed));
			cavalry.horseStepHeight = Math.max(0.6, Math.min(2.0, getDouble(o, "horseStepHeight", cavalry.horseStepHeight)));
		}
		this.combat.musketeerReloadTicks = clampReloadTicks(
			getInt(root, "musketeerReloadTicks", this.combat.musketeerReloadTicks));
		this.combat.siegeReloadTicks = clampReloadTicks(
			getInt(root, "siegeReloadTicks", this.combat.siegeReloadTicks));
	}

	private void loadNinja(JsonObject o) {
		this.ninja.movementSpeed = clampNinjaSpeed(getDouble(o, "movementSpeed", this.ninja.movementSpeed));
		this.ninja.markRange = Math.max(8.0, Math.min(128.0, getDouble(o, "markRange", this.ninja.markRange)));
		this.ninja.markDurationTicks = Math.max(20, Math.min(6000, getInt(o, "markDurationTicks", this.ninja.markDurationTicks)));
		this.ninja.markCooldownTicks = Math.max(0, Math.min(6000, getInt(o, "markCooldownTicks", this.ninja.markCooldownTicks)));
	}

	private void loadMatch(JsonObject o) {
		this.match.durationSeconds = Math.max(60, Math.min(7200, getInt(o, "durationSeconds", this.match.durationSeconds)));
		this.match.testDurationSeconds = Math.max(30, Math.min(3600, getInt(o, "testDurationSeconds", this.match.testDurationSeconds)));
		this.match.holdSeconds = Math.max(5, Math.min(120, getInt(o, "holdSeconds", this.match.holdSeconds)));
		this.match.captureRadius = Math.max(4.0, Math.min(32.0, getDouble(o, "captureRadius", this.match.captureRadius)));
		this.match.bladeSpeedMultiplier = (float) Math.max(1.0, Math.min(5.0, getDouble(o, "bladeSpeedMultiplier", this.match.bladeSpeedMultiplier)));
		this.match.soloBotEnabled = getBoolean(o, "soloBotEnabled", this.match.soloBotEnabled);
		this.match.debugCommands = getBoolean(o, "debugCommands", this.match.debugCommands);
		this.match.siegeDamageMultiplier = (float) Math.max(1.0, Math.min(10.0, getDouble(o, "siegeDamageMultiplier", this.match.siegeDamageMultiplier)));
	}

	private static double clampHorseSpeed(double speed) {
		return Math.max(0.1, Math.min(1.0, speed));
	}

	private static double clampNinjaSpeed(double speed) {
		return Math.max(0.1, Math.min(1.0, speed));
	}

	private static int clampReloadTicks(int ticks) {
		return Math.max(20, Math.min(12000, ticks));
	}

	private void loadDiscord(JsonObject o) {
		this.discord.enabled = getBoolean(o, "enabled", this.discord.enabled);
		this.discord.webhookUrl = getString(o, "webhookUrl", this.discord.webhookUrl).trim();
		String language = getString(o, "language", this.discord.language).trim().toLowerCase();
		this.discord.language = "en".equals(language) ? "en" : "ru";
		this.discord.includeLeaderName = getBoolean(o, "includeLeaderName", this.discord.includeLeaderName);
		this.discord.includeServerTime = getBoolean(o, "includeServerTime", this.discord.includeServerTime);
		if (o.has("events") && o.get("events").isJsonObject()) {
			JsonObject events = o.getAsJsonObject("events");
			this.discord.events.warDeclared = getBoolean(events, "warDeclared", this.discord.events.warDeclared);
			this.discord.events.peaceDeclared = getBoolean(events, "peaceDeclared", this.discord.events.peaceDeclared);
			this.discord.events.peaceOffered = getBoolean(events, "peaceOffered", this.discord.events.peaceOffered);
			this.discord.events.peaceDeclined = getBoolean(events, "peaceDeclined", this.discord.events.peaceDeclined);
			this.discord.events.wartime = getBoolean(events, "wartime", this.discord.events.wartime);
			this.discord.events.capture = getBoolean(events, "capture", this.discord.events.capture);
			this.discord.events.allianceRequested = getBoolean(events, "allianceRequested", this.discord.events.allianceRequested);
			this.discord.events.allianceFormed = getBoolean(events, "allianceFormed", this.discord.events.allianceFormed);
			this.discord.events.allianceDeclined = getBoolean(events, "allianceDeclined", this.discord.events.allianceDeclined);
			this.discord.events.allianceBroken = getBoolean(events, "allianceBroken", this.discord.events.allianceBroken);
		}
	}

	private void loadDuelDiscord(JsonObject o) {
		this.duelDiscord.enabled = getBoolean(o, "enabled", this.duelDiscord.enabled);
		this.duelDiscord.webhookUrl = getString(o, "webhookUrl", this.duelDiscord.webhookUrl).trim();
	}

	public void saveIfAbsent() {
		Path path = configPath();
		if (Files.exists(path)) {
			return;
		}
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter w = Files.newBufferedWriter(path)) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonObject root = new JsonObject();

				JsonObject weaponsObj = new JsonObject();
				for (Map.Entry<String, Weapon> e : weapons.entrySet()) {
					Weapon weap = e.getValue();
					JsonObject o = new JsonObject();
					o.addProperty("attackDamage", weap.attackDamage);
					o.addProperty("attackSpeed", weap.attackSpeed);
					o.addProperty("durability", weap.durability);
					weaponsObj.add(e.getKey(), o);
				}
				root.add("weapons", weaponsObj);

				JsonObject armorObj = new JsonObject();
				for (Map.Entry<String, ArmorSet> e : armor.entrySet()) {
					ArmorSet set = e.getValue();
					JsonObject o = new JsonObject();
					JsonObject d = new JsonObject();
					d.addProperty("head", set.defense.head);
					d.addProperty("chest", set.defense.chest);
					d.addProperty("legs", set.defense.legs);
					d.addProperty("feet", set.defense.feet);
					o.add("defense", d);
					o.addProperty("enchantability", set.enchantability);
					o.addProperty("toughness", set.toughness);
					o.addProperty("knockbackResistance", set.knockbackResistance);
					o.addProperty("durability", set.durability);
					o.addProperty("equipSound", set.equipSound);
					armorObj.add(e.getKey(), o);
				}
				root.add("armor", armorObj);

				root.addProperty("rtsCommandRange", this.rtsCommandRange);
				root.addProperty("rtsMaxWaypoints", this.rtsMaxWaypoints);
				root.addProperty("rtsWaypointIdleFollow", this.rtsWaypointIdleFollow);
				root.addProperty("rtsFormationStepBlocks", this.rtsFormationStepBlocks);
				root.addProperty("emotesEnabled", this.emotesEnabled);
				root.addProperty("emoteInterruptOnDamage", this.emoteInterruptOnDamage);
				root.addProperty("emoteInterruptOnOrder", this.emoteInterruptOnOrder);
				root.addProperty("emoteMaxDurationTicks", this.emoteMaxDurationTicks);
				root.add("discord", this.discord.toJson());
				root.add("duelDiscord", this.duelDiscord.toJson());
				root.add("combat", this.combat.toJson());
				root.add("ninja", this.ninja.toJson());
				root.add("match", this.match.toJson());

				gson.toJson(root, w);
			}
		} catch (IOException ex) {
			NomosikiMod.LOGGER.warn("zkwars: failed to write default common config", ex);
		}
	}

	private static ArmorSet defaultArmor() {
		return new ArmorSet(new Defense(2, 5, 6, 2), 12, 0.0f, 0.0f, 165, "iron");
	}

	public static int getInt(JsonObject o, String key, int def) {
		return o.has(key) ? o.get(key).getAsInt() : def;
	}

	public static double getDouble(JsonObject o, String key, double def) {
		return o.has(key) ? o.get(key).getAsDouble() : def;
	}

	public static String getString(JsonObject o, String key, String def) {
		return o.has(key) ? o.get(key).getAsString() : def;
	}

	public static boolean getBoolean(JsonObject o, String key, boolean def) {
		return o.has(key) ? o.get(key).getAsBoolean() : def;
	}

	public static int clampPort(int port) {
		return Math.max(1, Math.min(65535, port));
	}

	public static final class Discord {
		public boolean enabled = false;
		/** Full Discord webhook URL; empty = do not send. Treat as a secret. */
		public String webhookUrl = "";
		public String language = "ru";
		public boolean includeLeaderName = true;
		public boolean includeServerTime = true;
		public final Events events = new Events();

		JsonObject toJson() {
			JsonObject discordObj = new JsonObject();
			discordObj.addProperty("enabled", this.enabled);
			discordObj.addProperty("webhookUrl", this.webhookUrl);
			discordObj.addProperty("language", this.language);
			discordObj.addProperty("includeLeaderName", this.includeLeaderName);
			discordObj.addProperty("includeServerTime", this.includeServerTime);
			JsonObject eventsObj = new JsonObject();
			eventsObj.addProperty("warDeclared", this.events.warDeclared);
			eventsObj.addProperty("peaceDeclared", this.events.peaceDeclared);
			eventsObj.addProperty("peaceOffered", this.events.peaceOffered);
			eventsObj.addProperty("peaceDeclined", this.events.peaceDeclined);
			eventsObj.addProperty("wartime", this.events.wartime);
			eventsObj.addProperty("capture", this.events.capture);
			eventsObj.addProperty("allianceRequested", this.events.allianceRequested);
			eventsObj.addProperty("allianceFormed", this.events.allianceFormed);
			eventsObj.addProperty("allianceDeclined", this.events.allianceDeclined);
			eventsObj.addProperty("allianceBroken", this.events.allianceBroken);
			discordObj.add("events", eventsObj);
			return discordObj;
		}

		public static final class Events {
			public boolean warDeclared = true;
			public boolean peaceDeclared = true;
			public boolean peaceOffered = true;
			public boolean peaceDeclined = true;
			public boolean wartime = true;
			public boolean capture = true;
			public boolean allianceRequested = false;
			public boolean allianceFormed = true;
			public boolean allianceDeclined = false;
			public boolean allianceBroken = true;
		}
	}

	public static final class Combat {
		public final BowSettings bow = new BowSettings();
		public final CavalrySettings cavalry = new CavalrySettings();
		/** Delay after a musketeer shot before the next shot (20 ticks = 1s). */
		public int musketeerReloadTicks = 1200;
		/** Delay after a siege machine shot before the next shot (20 ticks = 1s). */
		public int siegeReloadTicks = 3600;

		JsonObject toJson() {
			JsonObject combatObj = new JsonObject();
			JsonObject bowObj = new JsonObject();
			bowObj.addProperty("arrowDamage", this.bow.arrowDamage);
			bowObj.addProperty("velocity", this.bow.velocity);
			bowObj.addProperty("inaccuracy", this.bow.inaccuracy);
			bowObj.addProperty("drawTicksMin", this.bow.drawTicksMin);
			bowObj.addProperty("maxRange", this.bow.maxRange);
			bowObj.addProperty("mountedInaccuracyBonus", this.bow.mountedInaccuracyBonus);
			combatObj.add("bow", bowObj);
			JsonObject cavalryObj = new JsonObject();
			cavalryObj.addProperty("horseMovementSpeed", this.cavalry.horseMovementSpeed);
			cavalryObj.addProperty("horseStepHeight", this.cavalry.horseStepHeight);
			combatObj.add("cavalry", cavalryObj);
			combatObj.addProperty("musketeerReloadTicks", this.musketeerReloadTicks);
			combatObj.addProperty("siegeReloadTicks", this.siegeReloadTicks);
			return combatObj;
		}
	}

	public static final class BowSettings {
		public double arrowDamage = 4.0;
		public float velocity = 1.85F;
		public float inaccuracy = 2.5F;
		public int drawTicksMin = 20;
		public double maxRange = 44.0;
		public float mountedInaccuracyBonus = 1.0F;
	}

	public static final class NinjaSettings {
		/** Foot speed; default is faster than {@link com.nomosiki.entity.NomosikEntity#DEFAULT_MOVEMENT_SPEED}. */
		public double movementSpeed = 0.36;
		/** Blocks from a ninja member to an enemy player for the mark ability. */
		public double markRange = 48.0;
		/** How long marked players stay on the tactical map. */
		public int markDurationTicks = 1200;
		/** Per-squad cooldown after using mark. */
		public int markCooldownTicks = 400;

		JsonObject toJson() {
			JsonObject ninjaObj = new JsonObject();
			ninjaObj.addProperty("movementSpeed", this.movementSpeed);
			ninjaObj.addProperty("markRange", this.markRange);
			ninjaObj.addProperty("markDurationTicks", this.markDurationTicks);
			ninjaObj.addProperty("markCooldownTicks", this.markCooldownTicks);
			return ninjaObj;
		}
	}

	/**
	 * Mount tuning for knights / nomads.
	 * <p>
	 * Vanilla player-ridden average is attribute {@code 0.225}, but mob-AI steering of a horse
	 * does not use {@code travelRidden}, so the default is higher to match that gallop feel.
	 */
	public static final class CavalrySettings {
		/** Effective AI speed for horses (~player-ridden average). */
		public double horseMovementSpeed = 0.45;
		public double horseStepHeight = 1.0;
	}

	public static final class Weapon {
		public double attackDamage;
		public double attackSpeed;
		public int durability;

		public Weapon(double attackDamage, double attackSpeed, int durability) {
			this.attackDamage = attackDamage;
			this.attackSpeed = attackSpeed;
			this.durability = durability;
		}
	}

	public static final class Defense {
		public int head;
		public int chest;
		public int legs;
		public int feet;

		public Defense(int head, int chest, int legs, int feet) {
			this.head = head;
			this.chest = chest;
			this.legs = legs;
			this.feet = feet;
		}
	}

	public static final class ArmorSet {
		public Defense defense;
		public int enchantability;
		public float toughness;
		public float knockbackResistance;
		public int durability;
		public String equipSound;

		public ArmorSet(Defense defense, int enchantability, float toughness, float knockbackResistance, int durability, String equipSound) {
			this.defense = defense;
			this.enchantability = enchantability;
			this.toughness = toughness;
			this.knockbackResistance = knockbackResistance;
			this.durability = durability;
			this.equipSound = equipSound;
		}
	}

	public static final class DuelDiscord {
		public boolean enabled = false;
		/** Full Discord webhook URL for duel refusals. Empty = do not send. Treat as a secret. */
		public String webhookUrl = "";

		JsonObject toJson() {
			JsonObject obj = new JsonObject();
			obj.addProperty("enabled", this.enabled);
			obj.addProperty("webhookUrl", this.webhookUrl);
			return obj;
		}
	}

	public static final class MatchSettings {
		/** Production match length (45 minutes). */
		public int durationSeconds = 2700;
		/** Used when {@link #debugCommands} is true (local playtest). */
		public int testDurationSeconds = 900;
		public int holdSeconds = 15;
		public double captureRadius = 10.0;
		public float bladeSpeedMultiplier = 1.75f;
		public boolean soloBotEnabled = true;
		public boolean debugCommands = true;
		public float siegeDamageMultiplier = 2.5f;

		JsonObject toJson() {
			JsonObject obj = new JsonObject();
			obj.addProperty("durationSeconds", this.durationSeconds);
			obj.addProperty("testDurationSeconds", this.testDurationSeconds);
			obj.addProperty("holdSeconds", this.holdSeconds);
			obj.addProperty("captureRadius", this.captureRadius);
			obj.addProperty("bladeSpeedMultiplier", this.bladeSpeedMultiplier);
			obj.addProperty("soloBotEnabled", this.soloBotEnabled);
			obj.addProperty("debugCommands", this.debugCommands);
			obj.addProperty("siegeDamageMultiplier", this.siegeDamageMultiplier);
			return obj;
		}
	}
}
