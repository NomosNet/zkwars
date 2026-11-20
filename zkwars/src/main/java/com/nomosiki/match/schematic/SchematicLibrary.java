package com.nomosiki.match.schematic;

import com.nomosiki.NomosikiMod;
import com.nomosiki.entity.NomosikRace;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code config/zkwars/schematics/<race>/<kind>.litematic}. Parsed files are cached until the file changes.
 */
public final class SchematicLibrary {
	public enum Status {
		OK,
		MISSING,
		BAD,
		TOO_BIG
	}

	public record Result(Status status, String fileName, @Nullable LitematicaSchematic schematic) {
		public boolean ok() {
			return this.status == Status.OK && this.schematic != null;
		}

		public Component message() {
			String key = switch (this.status) {
				case MISSING -> "message.nomosiki.match.schematic_missing";
				case TOO_BIG -> "message.nomosiki.match.schematic_too_big";
				default -> "message.nomosiki.match.schematic_bad";
			};
			return Component.translatable(key, this.fileName);
		}
	}

	private record CacheKey(Path path, long modified, long length) {
	}

	private static final Map<Path, CacheKey> CACHE_META = new ConcurrentHashMap<>();
	private static final Map<Path, Result> CACHE = new ConcurrentHashMap<>();
	private static volatile boolean foldersReady;

	private SchematicLibrary() {
	}

	public static Path root() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("zkwars").resolve("schematics");
	}

	public static Result get(ServerLevel level, NomosikRace race, String kind) {
		return get(level, race.name(), kind);
	}

	public static Result get(ServerLevel level, String race, String kind) {
		ensureFolders();
		String raceName = race == null ? "" : race.toLowerCase(Locale.ROOT);
		String kindName = kind == null ? "" : kind.toLowerCase(Locale.ROOT);
		String fileName = raceName + "/" + kindName + ".litematic";
		Path path = root().resolve(raceName).resolve(kindName + ".litematic");
		if (!Files.isRegularFile(path)) {
			CACHE.remove(path);
			CACHE_META.remove(path);
			return new Result(Status.MISSING, fileName, null);
		}
		long modified;
		long length;
		try {
			modified = Files.getLastModifiedTime(path).toMillis();
			length = Files.size(path);
		} catch (IOException exception) {
			NomosikiMod.LOGGER.warn("Could not stat schematic {}", path, exception);
			return new Result(Status.BAD, fileName, null);
		}
		CacheKey cached = CACHE_META.get(path);
		if (cached != null && cached.modified == modified && cached.length == length) {
			Result hit = CACHE.get(path);
			if (hit != null) {
				return hit;
			}
		}
		Result loaded = read(level, path, fileName);
		CACHE_META.put(path, new CacheKey(path, modified, length));
		CACHE.put(path, loaded);
		return loaded;
	}

	@Nullable
	public static Component missingHq(ServerLevel level, NomosikRace race0, NomosikRace race1) {
		Result first = get(level, race0, "hq");
		if (!first.ok()) {
			return first.message();
		}
		if (race1 != race0) {
			Result second = get(level, race1, "hq");
			if (!second.ok()) {
				return second.message();
			}
		}
		return null;
	}

	private static Result read(ServerLevel level, Path path, String fileName) {
		try (InputStream input = Files.newInputStream(path)) {
			CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
			LitematicaSchematic schematic = LitematicaSchematic.parse(root, level.registryAccess());
			return new Result(Status.OK, fileName, schematic);
		} catch (LitematicaSchematic.SchematicException exception) {
			return new Result(exception.tooBig() ? Status.TOO_BIG : Status.BAD, fileName, null);
		} catch (IOException | RuntimeException exception) {
			NomosikiMod.LOGGER.warn("Could not read schematic {}", path, exception);
			return new Result(Status.BAD, fileName, null);
		}
	}

	private static void ensureFolders() {
		if (foldersReady) {
			return;
		}
		try {
			for (NomosikRace race : NomosikRace.playableValues()) {
				Files.createDirectories(root().resolve(race.name().toLowerCase(Locale.ROOT)));
			}
			foldersReady = true;
		} catch (IOException exception) {
			NomosikiMod.LOGGER.warn("Could not create schematic folders under {}", root(), exception);
		}
	}
}
