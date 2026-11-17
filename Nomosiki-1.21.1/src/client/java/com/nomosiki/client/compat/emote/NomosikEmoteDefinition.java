package com.nomosiki.client.compat.emote;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Parsed EmoteCraft-subset keyframe animation. */
public final class NomosikEmoteDefinition {
	public enum Part {
		HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
	}

	public record PartPose(float x, float y, float z, float pitch, float yaw, float roll) {
		public static final PartPose ZERO = new PartPose(0, 0, 0, 0, 0, 0);
	}

	public record Keyframe(int tick, Map<Part, PartPose> parts) {
	}

	private final UUID id;
	private final ResourceLocation key;
	private final String name;
	private final int beginTick;
	private final int endTick;
	private final int stopTick;
	private final boolean degrees;
	private final boolean loop;
	private final int returnTick;
	private final List<Keyframe> keyframes;

	public NomosikEmoteDefinition(
		UUID id,
		ResourceLocation key,
		String name,
		int beginTick,
		int endTick,
		int stopTick,
		boolean degrees,
		boolean loop,
		int returnTick,
		List<Keyframe> keyframes
	) {
		this.id = id;
		this.key = key;
		this.name = name;
		this.beginTick = beginTick;
		this.endTick = Math.max(beginTick + 1, endTick);
		this.stopTick = Math.max(this.endTick, stopTick);
		this.degrees = degrees;
		this.loop = loop;
		this.returnTick = Math.max(0, Math.min(returnTick, this.endTick - 1));
		this.keyframes = List.copyOf(keyframes);
	}

	public UUID id() {
		return id;
	}

	public ResourceLocation key() {
		return key;
	}

	public String name() {
		return name;
	}

	public int beginTick() {
		return beginTick;
	}

	public int endTick() {
		return endTick;
	}

	public int stopTick() {
		return stopTick;
	}

	public boolean degrees() {
		return degrees;
	}

	public boolean loop() {
		return loop;
	}

	public int returnTick() {
		return returnTick;
	}

	public List<Keyframe> keyframes() {
		return keyframes;
	}

	public Map<Part, PartPose> sample(float tick) {
		if (keyframes.isEmpty()) {
			return Map.of();
		}
		float t = tick;
		if (loop && t > endTick) {
			float span = endTick - returnTick;
			if (span > 0.01f) {
				t = returnTick + ((t - returnTick) % span);
			} else {
				t = endTick;
			}
		}
		if (t <= keyframes.getFirst().tick()) {
			return keyframes.getFirst().parts();
		}
		Keyframe prev = keyframes.getFirst();
		for (int i = 1; i < keyframes.size(); i++) {
			Keyframe next = keyframes.get(i);
			if (t <= next.tick()) {
				float span = next.tick() - prev.tick();
				float alpha = span <= 0.001f ? 1f : (t - prev.tick()) / span;
				return lerp(prev.parts(), next.parts(), alpha);
			}
			prev = next;
		}
		return keyframes.getLast().parts();
	}

	private static Map<Part, PartPose> lerp(Map<Part, PartPose> a, Map<Part, PartPose> b, float alpha) {
		Map<Part, PartPose> out = new HashMap<>();
		for (Part part : Part.values()) {
			PartPose pa = a.getOrDefault(part, PartPose.ZERO);
			PartPose pb = b.getOrDefault(part, PartPose.ZERO);
			if (pa == PartPose.ZERO && pb == PartPose.ZERO && !a.containsKey(part) && !b.containsKey(part)) {
				continue;
			}
			out.put(part, new PartPose(
				lerp(pa.x(), pb.x(), alpha),
				lerp(pa.y(), pb.y(), alpha),
				lerp(pa.z(), pb.z(), alpha),
				lerp(pa.pitch(), pb.pitch(), alpha),
				lerp(pa.yaw(), pb.yaw(), alpha),
				lerp(pa.roll(), pb.roll(), alpha)
			));
		}
		return Collections.unmodifiableMap(out);
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}
}
