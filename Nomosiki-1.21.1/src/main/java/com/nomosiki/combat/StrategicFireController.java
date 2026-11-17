package com.nomosiki.combat;

import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRangedFireMode;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Strategic fire: pick aim points inside a zone rather than soft-locking on one entity.
 */
public final class StrategicFireController {
	private StrategicFireController() {
	}

	public static boolean isActive(NomosikEntity nomosik) {
		return nomosik.isRangedUnit()
			&& nomosik.getRangedFireMode() == NomosikRangedFireMode.STRATEGIC_FIRE
			&& nomosik.getEffectiveRangedFireZone() != null;
	}

	@Nullable
	public static AABB activeZone(NomosikEntity nomosik) {
		if (!isActive(nomosik)) {
			return null;
		}
		return nomosik.getEffectiveRangedFireZone();
	}

	public static Vec3 pickAimPoint(NomosikEntity nomosik) {
		AABB zone = activeZone(nomosik);
		if (zone == null) {
			return nomosik.position();
		}
		return NomosikProjectileAim.randomPointInZone(nomosik.level(), zone, nomosik.getRandom());
	}

	/** Body and head yaw toward the bombardment point. The formation lock stays off while this order is active. */
	public static void faceZone(NomosikEntity nomosik, Vec3 aim) {
		double dx = aim.x - nomosik.getX();
		double dz = aim.z - nomosik.getZ();
		if (dx * dx + dz * dz < 1.0E-4) {
			return;
		}
		float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		nomosik.setYRot(yaw);
		nomosik.yRotO = yaw;
		nomosik.yBodyRot = yaw;
		nomosik.yHeadRot = yaw;
	}

	/** Bow bombardment: the lob itself is the sight line. Bodies do not block it; blocks along the arc do. */
	public static boolean bombardmentArcClear(NomosikEntity nomosik, Vec3 targetPoint) {
		float speed = NomosikiServerConfig.INSTANCE.combat.bow.velocity;
		return NomosikProjectileAim.bombardmentArc(nomosik, targetPoint, speed).isPresent();
	}

	public static boolean hasLineOfSightTo(NomosikEntity nomosik, Vec3 targetPoint) {
		Vec3 eye = nomosik.getEyePosition();
		HitResult hit = nomosik.level().clip(new ClipContext(
			eye,
			targetPoint,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			nomosik
		));
		if (hit.getType() == HitResult.Type.MISS) {
			return true;
		}
		return eye.distanceToSqr(hit.getLocation()) >= eye.distanceToSqr(targetPoint) - 1.0;
	}

	public static boolean withinBowRange(NomosikEntity nomosik, AABB zone, double maxRange) {
		Vec3 center = zone.getCenter();
		return nomosik.distanceToSqr(center) <= maxRange * maxRange;
	}
}
