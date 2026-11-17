package com.nomosiki.combat;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bow/crossbow aim helpers for nomosiki ranged units.
 */
public final class NomosikProjectileAim {
	/** Arrow gravity / drag used by vanilla AbstractArrow. */
	private static final double ARROW_GRAVITY = 0.05;
	private static final double ARROW_DRAG = 0.99;

	private NomosikProjectileAim() {
	}

	public record AimVector(double dx, double dy, double dz) {
	}

	/**
	 * Aim at upper chest / eyes so shots do not land at the target's feet.
	 */
	public static Vec3 entityAimPoint(LivingEntity target, boolean leadTarget) {
		double x = target.getX();
		double y = target.getEyeY() - target.getBbHeight() * 0.1;
		double z = target.getZ();
		if (leadTarget) {
			Vec3 motion = target.getDeltaMovement();
			double lead = 0.35;
			x += motion.x * lead * 20.0;
			y += motion.y * lead * 20.0;
			z += motion.z * lead * 20.0;
		}
		return new Vec3(x, y, z);
	}

	/**
	 * Random ground/surface point inside a strategic fire zone.
	 */
	public static Vec3 randomPointInZone(Level level, AABB zone, RandomSource random) {
		double x = Mth.lerp(random.nextDouble(), zone.minX, zone.maxX);
		double z = Mth.lerp(random.nextDouble(), zone.minZ, zone.maxZ);
		int blockX = Mth.floor(x);
		int blockZ = Mth.floor(z);
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
		double y = Mth.clamp(surfaceY + 0.5, zone.minY + 0.5, zone.maxY - 0.5);
		return new Vec3(x, y, z);
	}

	public static AimVector directionFrom(LivingEntity shooter, Vec3 targetPoint, double projectileOriginY, float velocity) {
		double dx = targetPoint.x - shooter.getX();
		double dy = targetPoint.y - projectileOriginY;
		double dz = targetPoint.z - shooter.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		dy += estimateGravityDrop(horizontal, Math.max(0.1F, velocity));
		return new AimVector(dx, dy, dz);
	}

	/**
	 * Compensates arrow drop over distance using the same drag/gravity model as vanilla arrows.
	 */
	public static double estimateGravityDrop(double horizontalDist, float speed) {
		if (horizontalDist < 0.001) {
			return 0.0;
		}
		double flightTime = calcFlightTime(horizontalDist, speed, ARROW_DRAG);
		return calcGravityDrop(flightTime, ARROW_GRAVITY, ARROW_DRAG);
	}

	private static double calcFlightTime(double horizontalDist, double speed, double drag) {
		double a = 1.0 - drag;
		double factor = 1.0 - horizontalDist * a / speed;
		if (factor <= 0.01) {
			factor = 0.01;
		}
		return Math.log(factor) / Math.log(drag);
	}

	private static double calcGravityDrop(double flightTime, double gravity, double drag) {
		double a = 1.0 - drag;
		double dragPowT = Math.pow(drag, flightTime);
		return (gravity / a) * (flightTime - (1.0 - dragPowT) / a);
	}

	public static void shootArrow(AbstractArrow arrow, AimVector direction, float velocity, float inaccuracy) {
		arrow.shoot(direction.dx(), direction.dy(), direction.dz(), velocity, inaccuracy);
	}

	/**
	 * Steep bombardment arc. Clears the rank in front, and falls on the zone instead of a flat shot into their backs.
	 * Empty when no arc both reaches the point and stays clear of blocks.
	 */
	public static java.util.Optional<AimVector> bombardmentArc(LivingEntity shooter, Vec3 target, float velocity) {
		Vec3 origin = new Vec3(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
		double dx = target.x - origin.x;
		double dz = target.z - origin.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal < 0.35) {
			return java.util.Optional.empty();
		}
		double inv = 1.0 / horizontal;
		double dirX = dx * inv;
		double dirZ = dz * inv;
		float speed = Math.max(0.1F, velocity);
		java.util.List<Flight> reaching = new java.util.ArrayList<>();
		for (double degrees : LOB_PITCH_DEGREES) {
			Flight flight = simulateLob(origin, dirX, dirZ, Math.toRadians(degrees), speed, target, shooter.getY(), horizontal);
			if (flight.impactMiss <= LOB_LAND_TOLERANCE) {
				reaching.add(flight);
			}
		}
		for (Flight flight : reaching) {
			if (flight.clearsHeads && !lobHitsBlock(shooter, origin, dirX, dirZ, flight.pitch, speed, target)) {
				return java.util.Optional.of(flight.direction);
			}
		}
		for (Flight flight : reaching) {
			if (!lobHitsBlock(shooter, origin, dirX, dirZ, flight.pitch, speed, target)) {
				return java.util.Optional.of(flight.direction);
			}
		}
		return java.util.Optional.empty();
	}

	private static final double LOB_HEAD_CLEARANCE = 2.4;
	private static final double LOB_CLEARANCE_START = 1.0;
	private static final double LOB_CLEARANCE_END = 4.0;
	private static final double LOB_LAND_TOLERANCE = 2.0;
	private static final int LOB_MAX_TICKS = 120;
	/** Steepest first. The high branch lands close; lower pitches are only a fallback when the lob cannot reach. */
	private static final double[] LOB_PITCH_DEGREES = {
		86, 84, 82, 80, 78, 76, 74, 72, 70, 68, 66, 64, 62, 60, 58, 56, 54, 52, 50, 48, 46, 44, 42, 40, 36, 32, 28, 24, 20, 16
	};

	private record Flight(AimVector direction, double pitch, double impactMiss, boolean clearsHeads) {
	}

	private static Flight simulateLob(
		Vec3 origin,
		double dirX,
		double dirZ,
		double pitch,
		float speed,
		Vec3 target,
		double feetY,
		double targetHorizontal
	) {
		double cos = Math.cos(pitch);
		double sin = Math.sin(pitch);
		AimVector direction = new AimVector(dirX * cos, sin, dirZ * cos);
		double vx = direction.dx() * speed;
		double vy = direction.dy() * speed;
		double vz = direction.dz() * speed;
		double x = origin.x;
		double y = origin.y;
		double z = origin.z;
		double impactMiss = Double.MAX_VALUE;
		boolean clears = true;
		double windowEnd = Math.min(LOB_CLEARANCE_END, targetHorizontal - 0.75);
		for (int tick = 0; tick < LOB_MAX_TICKS; tick++) {
			double nx = x + vx;
			double ny = y + vy;
			double nz = z + vz;
			if (windowEnd > LOB_CLEARANCE_START) {
				clears = clears && segmentClearsHeads(origin, feetY, x, y, z, nx, ny, nz, windowEnd);
			}
			if (vy < 0.0 && y >= target.y && ny <= target.y) {
				double drop = y - ny;
				double t = drop < 1.0E-6 ? 0.0 : (y - target.y) / drop;
				double landX = x + (nx - x) * t;
				double landZ = z + (nz - z) * t;
				impactMiss = Math.abs(horizontalFrom(origin, landX, landZ) - targetHorizontal);
				break;
			}
			x = nx;
			y = ny;
			z = nz;
			vx *= ARROW_DRAG;
			vz *= ARROW_DRAG;
			vy = vy * ARROW_DRAG - ARROW_GRAVITY;
		}
		return new Flight(direction, pitch, impactMiss, clears);
	}

	private static boolean segmentClearsHeads(
		Vec3 origin,
		double feetY,
		double x,
		double y,
		double z,
		double nx,
		double ny,
		double nz,
		double windowEnd
	) {
		double prev = horizontalFrom(origin, x, z);
		double next = horizontalFrom(origin, nx, nz);
		if (next <= LOB_CLEARANCE_START || prev >= windowEnd || next <= prev) {
			return true;
		}
		double sample = Math.max(prev, LOB_CLEARANCE_START);
		double t = (sample - prev) / (next - prev);
		double sampleY = Mth.lerp(t, y, ny);
		return sampleY >= feetY + LOB_HEAD_CLEARANCE;
	}

	private static boolean lobHitsBlock(
		LivingEntity shooter,
		Vec3 origin,
		double dirX,
		double dirZ,
		double pitch,
		float speed,
		Vec3 target
	) {
		double cos = Math.cos(pitch);
		double sin = Math.sin(pitch);
		double vx = dirX * cos * speed;
		double vy = sin * speed;
		double vz = dirZ * cos * speed;
		double x = origin.x;
		double y = origin.y;
		double z = origin.z;
		for (int tick = 0; tick < LOB_MAX_TICKS; tick++) {
			double nx = x + vx;
			double ny = y + vy;
			double nz = z + vz;
			var hit = shooter.level().clip(new net.minecraft.world.level.ClipContext(
				new Vec3(x, y, z),
				new Vec3(nx, ny, nz),
				net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE,
				shooter
			));
			if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS
				&& hit.getLocation().distanceToSqr(target) > LOB_LAND_TOLERANCE * LOB_LAND_TOLERANCE) {
				return true;
			}
			if (vy < 0.0 && y >= target.y && ny <= target.y) {
				return false;
			}
			x = nx;
			y = ny;
			z = nz;
			vx *= ARROW_DRAG;
			vz *= ARROW_DRAG;
			vy = vy * ARROW_DRAG - ARROW_GRAVITY;
		}
		return false;
	}

	private static double horizontalFrom(Vec3 origin, double x, double z) {
		double dx = x - origin.x;
		double dz = z - origin.z;
		return Math.sqrt(dx * dx + dz * dz);
	}
}
