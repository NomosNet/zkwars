package com.nomosiki.work.gun;

import com.nomosiki.work.NomosikiWork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Locale;
import java.util.Scanner;

public final class Config {
	public static final int VERSION = 4;

	public static float bulletMaxDistance;
	public static final float BULLET_MAX_DISTANCE = 256.0f;

	public static int loadingStages;
	public static final int LOADING_STAGES = 3;
	public static float loadingStageDuration;
	public static final float LOADING_STAGE_DURATION = 0.5f;

	public static float pvpDamageMultiplier;
	public static final float PVP_DAMAGE_MULTIPLIER = 1.0f;
	public static float mobDamageMultiplier;
	public static final float MOB_DAMAGE_MULTIPLIER = 0.5f;

	public static float pistolPillagerChance;
	public static final float PISTOL_PILLAGER_CHANCE = 0.2f;
	public static float musketSkeletonChance;
	public static final float MUSKET_SKELETON_CHANCE = 0.05f;

	public static boolean alwaysAim;
	public static final boolean ALWAYS_AIM = false;

	public static float damagePerPowerLevel;
	public static final float DAMAGE_PER_POWER_LEVEL = 0.5f;

	public static float musketBulletStdDev;
	public static final float MUSKET_BULLET_STD_DEV = 1.0f;
	public static float musketBulletSpeed;
	public static final float MUSKET_BULLET_SPEED = 180.0f;
	public static float musketDamage;
	public static final float MUSKET_DAMAGE = 16.0f;
	public static float headshotDamageMultiplier;
	public static final float HEADSHOT_DAMAGE_MULTIPLIER = 1.3f;
	public static int bayonetDamage;
	public static final int BAYONET_DAMAGE = 5;
	public static float bayonetSpeed;
	public static final float BAYONET_SPEED = 2.0f;
	public static int musketDurability;
	public static final int MUSKET_DURABILITY = 250;

	public static float scopedMusketBulletStdDev;
	public static final float SCOPED_MUSKET_BULLET_STD_DEV = 0.2f;
	public static float scopeZoom;
	public static final float SCOPE_ZOOM = 3.0f;
	public static float bulletGravityMultiplier;
	public static final float BULLET_GRAVITY_MULTIPLIER = 0.5f;
	public static int scopedMusketDurability;
	public static final int SCOPED_MUSKET_DURABILITY = 150;

	public static float blunderbussBulletStdDev;
	public static final float BLUNDERBUSS_BULLET_STD_DEV = 2.5f;
	public static float blunderbussBulletSpeed;
	public static final float BLUNDERBUSS_BULLET_SPEED = 160.0f;
	public static float blunderbussDamage;
	public static final float BLUNDERBUSS_DAMAGE = 21.0f;
	public static int blunderbussPelletCount;
	public static final int BLUNDERBUSS_PELLET_COUNT = 9;
	public static int blunderbussDurability;
	public static final int BLUNDERBUSS_DURABILITY = 200;

	public static float pistolBulletStdDev;
	public static final float PISTOL_BULLET_STD_DEV = 1.5f;
	public static float pistolBulletSpeed;
	public static final float PISTOL_BULLET_SPEED = 140.0f;
	public static float pistolDamage;
	public static final float PISTOL_DAMAGE = 12.0f;
	public static float reductionPerQuickChargeLevel;
	public static final float REDUCTION_PER_QUICK_CHARGE_LEVEL = 0.15f;
	public static int pistolDurability;
	public static final int PISTOL_DURABILITY = 200;

	public static float dispenserBulletStdDev;
	public static final float DISPENSER_BULLET_STD_DEV = 2.0f;
	public static float dispenserBulletSpeed;
	public static final float DISPENSER_BULLET_SPEED = 120.0f;
	public static float dispenserDamage;
	public static final float DISPENSER_DAMAGE = 10.0f;

	private Config() {
	}

	public static void load() {
		bulletMaxDistance = BULLET_MAX_DISTANCE;
		loadingStages = LOADING_STAGES;
		loadingStageDuration = LOADING_STAGE_DURATION;
		pvpDamageMultiplier = PVP_DAMAGE_MULTIPLIER;
		mobDamageMultiplier = MOB_DAMAGE_MULTIPLIER;
		pistolPillagerChance = PISTOL_PILLAGER_CHANCE;
		musketSkeletonChance = MUSKET_SKELETON_CHANCE;
		alwaysAim = ALWAYS_AIM;
		damagePerPowerLevel = DAMAGE_PER_POWER_LEVEL;
		musketBulletStdDev = MUSKET_BULLET_STD_DEV;
		musketBulletSpeed = MUSKET_BULLET_SPEED;
		musketDamage = MUSKET_DAMAGE;
		headshotDamageMultiplier = HEADSHOT_DAMAGE_MULTIPLIER;
		bayonetDamage = BAYONET_DAMAGE;
		bayonetSpeed = BAYONET_SPEED;
		musketDurability = MUSKET_DURABILITY;
		scopedMusketBulletStdDev = SCOPED_MUSKET_BULLET_STD_DEV;
		scopeZoom = SCOPE_ZOOM;
		bulletGravityMultiplier = BULLET_GRAVITY_MULTIPLIER;
		scopedMusketDurability = SCOPED_MUSKET_DURABILITY;
		blunderbussBulletStdDev = BLUNDERBUSS_BULLET_STD_DEV;
		blunderbussBulletSpeed = BLUNDERBUSS_BULLET_SPEED;
		blunderbussDamage = BLUNDERBUSS_DAMAGE;
		blunderbussPelletCount = BLUNDERBUSS_PELLET_COUNT;
		blunderbussDurability = BLUNDERBUSS_DURABILITY;
		pistolBulletStdDev = PISTOL_BULLET_STD_DEV;
		pistolBulletSpeed = PISTOL_BULLET_SPEED;
		pistolDamage = PISTOL_DAMAGE;
		reductionPerQuickChargeLevel = REDUCTION_PER_QUICK_CHARGE_LEVEL;
		pistolDurability = PISTOL_DURABILITY;
		dispenserBulletStdDev = DISPENSER_BULLET_STD_DEV;
		dispenserBulletSpeed = DISPENSER_BULLET_SPEED;
		dispenserDamage = DISPENSER_DAMAGE;

		int version = 0;
		try (BufferedReader reader = Files.newBufferedReader(Guns.CONFIG_PATH)) {
			String line;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				int commentStart = line.indexOf('#');
				if (commentStart != -1) {
					line = line.substring(0, commentStart);
				}
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				String errorPrefix = Guns.CONFIG_PATH + ": line " + lineNumber + ": ";
				try (Scanner scanner = new Scanner(line)) {
					scanner.useLocale(Locale.US);
					scanner.useDelimiter("\\s*=\\s*");
					if (!scanner.hasNext()) {
						NomosikiWork.LOGGER.warn("{}parameter name is missing", errorPrefix);
						continue;
					}
					String key = scanner.next().trim();
					if (!scanner.hasNextFloat()) {
						NomosikiWork.LOGGER.warn("{}value is missing/wrong/not a number", errorPrefix);
						continue;
					}
					float value = scanner.nextFloat();
					switch (key) {
						case "version" -> version = (int) value;
						case "bulletMaxDistance" -> bulletMaxDistance = value;
						case "loadingStages" -> loadingStages = (int) value;
						case "loadingStageDuration" -> loadingStageDuration = value;
						case "pvpDamageMultiplier" -> pvpDamageMultiplier = value;
						case "mobDamageMultiplier" -> mobDamageMultiplier = value;
						case "pistolPillagerChance" -> pistolPillagerChance = value;
						case "musketSkeletonChance" -> musketSkeletonChance = value;
						case "damagePerPowerLevel" -> damagePerPowerLevel = value;
						case "bulletStdDev", "musketBulletStdDev" -> musketBulletStdDev = value;
						case "bulletSpeed", "musketBulletSpeed" -> musketBulletSpeed = value;
						case "damageMin", "musketDamage" -> musketDamage = value;
						case "damageMax" -> {
						}
						case "headshotDamageMultiplier" -> headshotDamageMultiplier = value;
						case "bayonetDamage" -> bayonetDamage = (int) value;
						case "bayonetSpeed" -> bayonetSpeed = value;
						case "durability", "musketDurability" -> musketDurability = (int) value;
						case "scopedMusketBulletStdDev" -> scopedMusketBulletStdDev = value;
						case "scopeZoom" -> scopeZoom = value;
						case "bulletGravityMultiplier" -> bulletGravityMultiplier = value;
						case "scopedMusketDurability" -> scopedMusketDurability = (int) value;
						case "blunderbussBulletStdDev" -> blunderbussBulletStdDev = value;
						case "blunderbussBulletSpeed" -> blunderbussBulletSpeed = value;
						case "blunderbussDamage" -> blunderbussDamage = value;
						case "blunderbussPelletCount" -> blunderbussPelletCount = (int) value;
						case "blunderbussDurability" -> blunderbussDurability = (int) value;
						case "pistolBulletStdDev" -> pistolBulletStdDev = value;
						case "pistolBulletSpeed" -> pistolBulletSpeed = value;
						case "pistolDamageMin", "pistolDamage" -> pistolDamage = value;
						case "pistolDamageMax" -> {
						}
						case "reductionPerQuickChargeLevel" -> reductionPerQuickChargeLevel = value;
						case "pistolDurability" -> pistolDurability = (int) value;
						case "dispenserBulletStdDev" -> dispenserBulletStdDev = value;
						case "dispenserBulletSpeed" -> dispenserBulletSpeed = value;
						case "dispenserDamage" -> dispenserDamage = value;
						default -> NomosikiWork.LOGGER.warn("{}unrecognized parameter name: {}", errorPrefix, key);
					}
				}
			}
		} catch (NoSuchFileException e) {
			save();
			NomosikiWork.LOGGER.info("Gun configuration file not found, default created");
		} catch (IOException e) {
			NomosikiWork.LOGGER.warn("Could not read gun configuration file", e);
		}
		if (version < VERSION) {
			NomosikiWork.LOGGER.info("Gun configuration file belongs to older version, updating");
			if (version < 4) {
				if (musketDamage == 20.5f) {
					musketDamage = MUSKET_DAMAGE;
				}
				if (bayonetDamage == 4) {
					bayonetDamage = BAYONET_DAMAGE;
				}
				if (pistolDurability == 150) {
					pistolDurability = PISTOL_DURABILITY;
				}
			}
			save();
		}
		NomosikiWork.LOGGER.info("Gun configuration has been loaded");
	}

	public static void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(Guns.CONFIG_PATH)) {
			writer.write("version = " + VERSION + "\n\n");
			writer.write("# Maximum bullet travel distance (in blocks)\n");
			writer.write("bulletMaxDistance = " + bulletMaxDistance + "\n\n");
			writer.write("# Number of loading stages\n");
			writer.write("loadingStages = " + loadingStages + "\n");
			writer.write("# Loading stage duration (in seconds)\n");
			writer.write("loadingStageDuration = " + loadingStageDuration + "\n\n");
			writer.write("# Damage multiplier for pvp\n");
			writer.write("pvpDamageMultiplier = " + pvpDamageMultiplier + "\n");
			writer.write("# Damage multiplier for monsters\n");
			writer.write("mobDamageMultiplier = " + mobDamageMultiplier + "\n\n");
			writer.write("# Probability of Pillager having a pistol\n");
			writer.write("pistolPillagerChance = " + pistolPillagerChance + "\n");
			writer.write("# Probability of Skeleton having a musket\n");
			writer.write("musketSkeletonChance = " + musketSkeletonChance + "\n\n");
			writer.write("# Extra damage per Power enchantment level\n");
			writer.write("damagePerPowerLevel = " + damagePerPowerLevel + "\n\n");
			writer.write("# Musket\n\n");
			writer.write("# Standard deviation of bullet spread (in degrees)\n");
			writer.write("musketBulletStdDev = " + musketBulletStdDev + "\n");
			writer.write("# Muzzle velocity of bullet (in blocks per second)\n");
			writer.write("musketBulletSpeed = " + musketBulletSpeed + "\n");
			writer.write("# Damage at point-blank range\n");
			writer.write("musketDamage = " + musketDamage + "\n");
			writer.write("# Headshot damage multiplier\n");
			writer.write("headshotDamageMultiplier = " + headshotDamageMultiplier + "\n");
			writer.write("# Durability (applied on restart)\n");
			writer.write("musketDurability = " + musketDurability + "\n\n");
			writer.write("# Musket with bayonet\n\n");
			writer.write("# Bayonet damage (applied on restart)\n");
			writer.write("bayonetDamage = " + bayonetDamage + "\n\n");
			writer.write("# Bayonet attack speed (applied on restart)\n");
			writer.write("bayonetSpeed = " + bayonetSpeed + "\n\n");
			writer.write("# Musket with scope\n\n");
			writer.write("# Standard deviation of bullet spread (in degrees)\n");
			writer.write("scopedMusketBulletStdDev = " + scopedMusketBulletStdDev + "\n");
			writer.write("# Scope zoom factor\n");
			writer.write("scopeZoom = " + scopeZoom + "\n");
			writer.write("# Bullet gravity multiplier\n");
			writer.write("bulletGravityMultiplier = " + bulletGravityMultiplier + "\n");
			writer.write("# Durability (applied on restart)\n");
			writer.write("scopedMusketDurability = " + scopedMusketDurability + "\n\n");
			writer.write("# Blunderbuss\n\n");
			writer.write("# Standard deviation of bullet spread (in degrees)\n");
			writer.write("blunderbussBulletStdDev = " + blunderbussBulletStdDev + "\n");
			writer.write("# Muzzle velocity of bullet (in blocks per second)\n");
			writer.write("blunderbussBulletSpeed = " + blunderbussBulletSpeed + "\n");
			writer.write("# Damage at point-blank range\n");
			writer.write("blunderbussDamage = " + blunderbussDamage + "\n");
			writer.write("# Pellet count\n");
			writer.write("blunderbussPelletCount = " + blunderbussPelletCount + "\n");
			writer.write("# Durability (applied on restart)\n");
			writer.write("blunderbussDurability = " + blunderbussDurability + "\n\n");
			writer.write("# Pistol\n\n");
			writer.write("# Standard deviation of bullet spread (in degrees)\n");
			writer.write("pistolBulletStdDev = " + pistolBulletStdDev + "\n");
			writer.write("# Muzzle velocity of bullet (in blocks per second)\n");
			writer.write("pistolBulletSpeed = " + pistolBulletSpeed + "\n");
			writer.write("# Damage at point-blank range\n");
			writer.write("pistolDamage = " + pistolDamage + "\n");
			writer.write("# Loading time reduction per Quick Charge level (in seconds)\n");
			writer.write("reductionPerQuickChargeLevel = " + reductionPerQuickChargeLevel + "\n");
			writer.write("# Durability (applied on restart)\n");
			writer.write("pistolDurability = " + pistolDurability + "\n\n");
			writer.write("# Dispenser\n\n");
			writer.write("# Standard deviation of bullet spread (in degrees)\n");
			writer.write("dispenserBulletStdDev = " + dispenserBulletStdDev + "\n");
			writer.write("# Muzzle velocity of bullet (in blocks per second)\n");
			writer.write("dispenserBulletSpeed = " + dispenserBulletSpeed + "\n");
			writer.write("# Damage at point-blank range\n");
			writer.write("dispenserDamage = " + dispenserDamage + "\n");
		} catch (IOException e) {
			NomosikiWork.LOGGER.warn("Could not save gun configuration file", e);
		}
	}
}
