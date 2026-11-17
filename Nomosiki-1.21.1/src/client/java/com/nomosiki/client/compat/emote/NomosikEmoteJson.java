package com.nomosiki.client.compat.emote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nomosiki.NomosikiMod;
import com.nomosiki.compat.emote.NomosikEmoteIds;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Parses a subset of EmoteCraft JSON emote format. */
public final class NomosikEmoteJson {
	private NomosikEmoteJson() {
	}

	public static NomosikEmoteDefinition parse(ResourceLocation key, String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		String name = root.has("name") ? root.get("name").getAsString() : key.getPath();
		JsonObject emote = root.getAsJsonObject("emote");
		int beginTick = getInt(emote, "beginTick", 0);
		int endTick = getInt(emote, "endTick", 40);
		int stopTick = getInt(emote, "stopTick", endTick + 1);
		boolean degrees = !emote.has("degrees") || emote.get("degrees").getAsBoolean();
		boolean loop = emote.has("isLoop") && (
			emote.get("isLoop").isJsonPrimitive() && emote.get("isLoop").getAsBoolean()
				|| "true".equalsIgnoreCase(emote.get("isLoop").getAsString()));
		int returnTick = getInt(emote, "returnTick", beginTick);
		UUID id = NomosikEmoteIds.uuidOf(key);

		List<NomosikEmoteDefinition.Keyframe> frames = new ArrayList<>();
		if (emote.has("moves") && emote.get("moves").isJsonArray()) {
			JsonArray moves = emote.getAsJsonArray("moves");
			for (JsonElement el : moves) {
				if (!el.isJsonObject()) {
					continue;
				}
				JsonObject move = el.getAsJsonObject();
				int tick = getInt(move, "tick", 0);
				Map<NomosikEmoteDefinition.Part, NomosikEmoteDefinition.PartPose> parts = new EnumMap<>(NomosikEmoteDefinition.Part.class);
				readPart(move, "head", NomosikEmoteDefinition.Part.HEAD, parts);
				readPart(move, "torso", NomosikEmoteDefinition.Part.TORSO, parts);
				readPart(move, "body", NomosikEmoteDefinition.Part.TORSO, parts);
				readPart(move, "leftArm", NomosikEmoteDefinition.Part.LEFT_ARM, parts);
				readPart(move, "rightArm", NomosikEmoteDefinition.Part.RIGHT_ARM, parts);
				readPart(move, "leftLeg", NomosikEmoteDefinition.Part.LEFT_LEG, parts);
				readPart(move, "rightLeg", NomosikEmoteDefinition.Part.RIGHT_LEG, parts);
				frames.add(new NomosikEmoteDefinition.Keyframe(tick, Map.copyOf(parts)));
			}
		}
		frames.sort((a, b) -> Integer.compare(a.tick(), b.tick()));
		return new NomosikEmoteDefinition(id, key, name, beginTick, endTick, stopTick, degrees, loop, returnTick, frames);
	}

	public static NomosikEmoteDefinition loadBuiltin(ResourceLocation key) {
		String path = "assets/" + key.getNamespace() + "/emotes/" + key.getPath().replace("emote/", "") + ".json";
		try (var in = NomosikEmoteJson.class.getClassLoader().getResourceAsStream(path)) {
			if (in == null) {
				NomosikiMod.LOGGER.warn("nomosiki emote missing: {}", path);
				return empty(key);
			}
			String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return parse(key, json);
		} catch (Exception ex) {
			NomosikiMod.LOGGER.warn("nomosiki emote failed to load {}", key, ex);
			return empty(key);
		}
	}

	private static NomosikEmoteDefinition empty(ResourceLocation key) {
		return new NomosikEmoteDefinition(
			NomosikEmoteIds.uuidOf(key), key, key.getPath(), 0, 1, 2, true, false, 0, List.of());
	}

	private static void readPart(
		JsonObject move,
		String field,
		NomosikEmoteDefinition.Part part,
		Map<NomosikEmoteDefinition.Part, NomosikEmoteDefinition.PartPose> out
	) {
		if (!move.has(field) || !move.get(field).isJsonObject()) {
			return;
		}
		JsonObject o = move.getAsJsonObject(field);
		out.put(part, new NomosikEmoteDefinition.PartPose(
			getFloat(o, "x", 0),
			getFloat(o, "y", 0),
			getFloat(o, "z", 0),
			getFloat(o, "pitch", 0),
			getFloat(o, "yaw", 0),
			getFloat(o, "roll", 0)
		));
	}

	private static int getInt(JsonObject o, String key, int def) {
		return o.has(key) ? o.get(key).getAsInt() : def;
	}

	private static float getFloat(JsonObject o, String key, float def) {
		return o.has(key) ? o.get(key).getAsFloat() : def;
	}
}
