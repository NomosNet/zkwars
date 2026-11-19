package com.nomosiki.match;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Default local layout relative to the commanding player's position when
 * {@code /match solo here} runs. Absolute coords from {@code rtsmap.json} can be used later.
 */
public final class MatchMapLayout {
	public record Start(Vec3 pos) {
	}

	public record CaptureSpot(String id, Vec3 pos) {
	}

	private MatchMapLayout() {
	}

	public static List<Start> startsAround(Vec3 origin) {
		return List.of(
			new Start(origin.add(-40.0, 0.0, -40.0)),
			new Start(origin.add(40.0, 0.0, 40.0))
		);
	}

	public static List<CaptureSpot> beaconsAround(Vec3 origin) {
		return List.of(
			new CaptureSpot("mid", origin.add(0.0, 0.0, 0.0)),
			new CaptureSpot("north", origin.add(0.0, 0.0, -55.0)),
			new CaptureSpot("south", origin.add(0.0, 0.0, 55.0)),
			new CaptureSpot("west", origin.add(-55.0, 0.0, 0.0)),
			new CaptureSpot("east", origin.add(55.0, 0.0, 0.0))
		);
	}

	public static BlockPos blockPos(Vec3 vec) {
		return BlockPos.containing(vec.x, vec.y, vec.z);
	}
}
