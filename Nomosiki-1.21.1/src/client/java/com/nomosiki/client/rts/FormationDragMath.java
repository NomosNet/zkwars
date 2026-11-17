package com.nomosiki.client.rts;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.SquadFormation;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import com.nomosiki.squad.SquadFormationLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Total War-style front-line geometry: A→B is the formation front, perpendicular is facing.
 */
public final class FormationDragMath {
	public static final int CLICK_PIXEL_THRESHOLD = 8;
	public static final double CLICK_WORLD_THRESHOLD = 2.0;

	private FormationDragMath() {
	}

	public record Placement(
		UUID squadId,
		Vec3 start,
		Vec3 end,
		Vec3 center,
		Vec3 facing,
		float facingYaw,
		float spacing,
		int maxInRow,
		SquadFormation formation,
		int unitCount
	) {
		public BlockPos anchorBlock() {
			return BlockPos.containing(this.center.x, this.center.y, this.center.z);
		}

		public Vec3 behindPoint() {
			return this.center.subtract(this.facing);
		}
	}

	public static boolean isClick(double pixelDist, Vec3 a, Vec3 b) {
		if (pixelDist < CLICK_PIXEL_THRESHOLD) {
			return true;
		}
		if (a == null || b == null) {
			return true;
		}
		return flatten(a).distanceTo(flatten(b)) < CLICK_WORLD_THRESHOLD;
	}

	public static Vec3 facingFromFront(Vec3 a, Vec3 b, Vec3 cameraLook) {
		Vec3 along = flatten(b.subtract(a));
		if (along.lengthSqr() < 1.0e-6) {
			Vec3 cam = flatten(cameraLook);
			if (cam.lengthSqr() < 1.0e-6) {
				return new Vec3(0.0, 0.0, 1.0);
			}
			return cam.normalize();
		}
		Vec3 n1 = new Vec3(-along.z, 0.0, along.x).normalize();
		Vec3 n2 = n1.scale(-1.0);
		Vec3 cam = flatten(cameraLook);
		if (cam.lengthSqr() < 1.0e-6) {
			return n1;
		}
		cam = cam.normalize();
		return n1.dot(cam) >= n2.dot(cam) ? n1 : n2;
	}

	public static List<Placement> placementsAlongFront(
		Vec3 a,
		Vec3 b,
		List<SquadArmyStatePayload.SquadRow> squads,
		Vec3 cameraLook
	) {
		if (squads.isEmpty() || a == null || b == null) {
			return List.of();
		}
		Vec3 start = flatten(a);
		Vec3 end = flatten(b);
		start = new Vec3(start.x, a.y, start.z);
		end = new Vec3(end.x, b.y, end.z);
		Vec3 facing = facingFromFront(start, end, cameraLook);
		Vec3 delta = end.subtract(start);
		int n = squads.size();
		List<Placement> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			Vec3 segA = start.add(delta.scale((double) i / n));
			Vec3 segB = start.add(delta.scale((double) (i + 1) / n));
			out.add(placementForSegment(squads.get(i), segA, segB, facing));
		}
		return List.copyOf(out);
	}

	public static Placement placementForSegment(
		SquadArmyStatePayload.SquadRow row,
		Vec3 start,
		Vec3 end,
		Vec3 facing
	) {
		Vec3 center = start.add(end).scale(0.5);
		double width = flatten(start).distanceTo(flatten(end));
		int n = Math.max(1, row.alive());
		SquadFormation formation = SquadFormation.fromId(row.formationId());
		if (formation == SquadFormation.NONE) {
			formation = SquadFormation.LINE;
		}
		float spacing = (float) Mth.clamp(
			row.formationSpacing(),
			NomosikEntity.SQUAD_FORMATION_SPACING_MIN,
			NomosikEntity.SQUAD_FORMATION_SPACING_MAX
		);
		int maxInRow = Math.max(1, row.formationMaxInRow());
		if (formation == SquadFormation.LINE || formation == SquadFormation.MOVEMENT) {
			maxInRow = Mth.clamp((int) Math.round(width / Math.max(0.25, spacing)) + 1, 1, n);
			if (maxInRow >= n && n > 1) {
				double stretched = width / (n - 1);
				if (stretched > spacing) {
					spacing = (float) Mth.clamp(
						stretched,
						NomosikEntity.SQUAD_FORMATION_SPACING_MIN,
						NomosikEntity.SQUAD_FORMATION_SPACING_MAX
					);
					maxInRow = n;
				}
			}
		} else {
			int side = Math.max(2, (int) Math.ceil(Math.sqrt(n)));
			spacing = (float) Mth.clamp(
				width / Math.max(1, side - 1),
				NomosikEntity.SQUAD_FORMATION_SPACING_MIN,
				NomosikEntity.SQUAD_FORMATION_SPACING_MAX
			);
		}
		float yaw = yawDegrees(facing);
		return new Placement(row.squadId(), start, end, center, facing, yaw, spacing, maxInRow, formation, n);
	}

	public static List<Vec3> previewSlots(net.minecraft.world.level.Level level, Placement placement) {
		if (placement.unitCount() <= 0) {
			return List.of();
		}
		Vec3 anchorCenter = placement.center();
		Vec3 squadCenter = placement.behindPoint();
		SquadFormation formation = placement.formation() == SquadFormation.NONE
			? SquadFormation.LINE
			: placement.formation();
		return SquadFormationLayout.slotsForFormation(
			formation,
			level,
			anchorCenter,
			squadCenter,
			placement.unitCount(),
			placement.spacing(),
			placement.maxInRow()
		);
	}

	public static float yawDegrees(Vec3 facing) {
		Vec3 flat = flatten(facing);
		if (flat.lengthSqr() < 1.0e-8) {
			return 0f;
		}
		flat = flat.normalize();
		return (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
	}

	private static Vec3 flatten(Vec3 v) {
		return new Vec3(v.x, 0.0, v.z);
	}
}
