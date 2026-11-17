package com.nomosiki.client.rts;

import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

/**
 * Mouse-to-world picking for the tactical camera: rays follow the cursor, not the screen center.
 */
public final class TacticalCursorPicking {
	private TacticalCursorPicking() {
	}

	private record ViewAxes(Vec3 origin, Vec3 forward, Vec3 right, Vec3 up) {
	}

	public static Vec3 worldDirectionFromCursor(Minecraft mc, double mouseX, double mouseY) {
		int guiW = Math.max(1, mc.getWindow().getGuiScaledWidth());
		int guiH = Math.max(1, mc.getWindow().getGuiScaledHeight());
		double ndcX = (mouseX / guiW) * 2.0 - 1.0;
		double ndcY = 1.0 - (mouseY / guiH) * 2.0;
		double tanHalf = tanHalfFov(mc);
		double aspect = framebufferAspect(mc);
		ViewAxes view = viewAxes(mc);
		return view.right.scale(ndcX * tanHalf * aspect)
			.add(view.up.scale(ndcY * tanHalf))
			.add(view.forward)
			.normalize();
	}

	public static HitResult pick(Minecraft mc, double mouseX, double mouseY) {
		if (mc.player == null || mc.level == null) {
			return BlockHitResult.miss(Vec3.ZERO, net.minecraft.core.Direction.UP, net.minecraft.core.BlockPos.ZERO);
		}
		Vec3 origin = viewAxes(mc).origin;
		Vec3 dir = worldDirectionFromCursor(mc, mouseX, mouseY);
		double reach = NomosikiConfig.INSTANCE.rtsTacticalRaycastRange;
		Vec3 end = origin.add(dir.scale(reach));
		BlockHitResult block = mc.level.clip(new ClipContext(
			origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
		double blockDist = block.getType() == HitResult.Type.MISS ? reach : origin.distanceTo(block.getLocation());
		AABB sweep = new AABB(origin, origin.add(dir.scale(blockDist))).inflate(0.75);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
			mc.player,
			origin,
			origin.add(dir.scale(blockDist)),
			sweep,
			entity -> entity.isPickable() && entity.isAlive(),
			blockDist * blockDist
		);
		if (entityHit != null) {
			return entityHit;
		}
		return block;
	}

	@Nullable
	public static Vec3 hitLocation(Minecraft mc, double mouseX, double mouseY) {
		HitResult hit = pick(mc, mouseX, mouseY);
		if (hit.getType() == HitResult.Type.MISS) {
			return groundFallback(mc, mouseX, mouseY);
		}
		return hit.getLocation();
	}

	@Nullable
	public static NomosikEntity ownedNomosikAt(Minecraft mc, double mouseX, double mouseY) {
		if (mc.player == null) {
			return null;
		}
		HitResult hit = pick(mc, mouseX, mouseY);
		if (!(hit instanceof EntityHitResult entityHit)) {
			return null;
		}
		Entity entity = entityHit.getEntity();
		if (!(entity instanceof NomosikEntity nomosik) || !nomosik.isAlive()) {
			return null;
		}
		if (!mc.player.getUUID().equals(nomosik.getOwnerUUID())) {
			return null;
		}
		return nomosik;
	}

	public static Optional<UUID> ownedSquadAt(Minecraft mc, double mouseX, double mouseY) {
		NomosikEntity nomosik = ownedNomosikAt(mc, mouseX, mouseY);
		if (nomosik == null) {
			return Optional.empty();
		}
		return nomosik.getSquadIdForGui();
	}

	@Nullable
	public static Vec2 worldToScreen(Minecraft mc, Vec3 world) {
		ViewAxes view = viewAxes(mc);
		Vec3 rel = world.subtract(view.origin);
		double z = rel.dot(view.forward);
		if (z < 0.4) {
			return null;
		}
		double tanHalf = tanHalfFov(mc);
		double aspect = framebufferAspect(mc);
		double ndcX = rel.dot(view.right) / (z * tanHalf * aspect);
		double ndcY = rel.dot(view.up) / (z * tanHalf);
		float sx = (float) ((ndcX + 1.0) * 0.5 * mc.getWindow().getGuiScaledWidth());
		float sy = (float) ((1.0 - ndcY) * 0.5 * mc.getWindow().getGuiScaledHeight());
		if (!Float.isFinite(sx) || !Float.isFinite(sy)) {
			return null;
		}
		return new Vec2(sx, sy);
	}

	@Nullable
	private static Vec3 groundFallback(Minecraft mc, double mouseX, double mouseY) {
		if (mc.level == null) {
			return null;
		}
		Vec3 origin = viewAxes(mc).origin;
		Vec3 dir = worldDirectionFromCursor(mc, mouseX, mouseY);
		if (Math.abs(dir.y) < 1.0e-4) {
			return null;
		}
		double t = (TacticalCameraController.focusBlockPos().getY() + 0.5 - origin.y) / dir.y;
		if (t < 1.0 || t > NomosikiConfig.INSTANCE.rtsTacticalRaycastRange) {
			return null;
		}
		return origin.add(dir.scale(t));
	}

	/**
	 * Camera axes matching the rendered view. Minecraft's right is {@code forward × worldUp};
	 * using {@code worldUp × forward} mirrors X/Y and makes overlays fly opposite the pan.
	 */
	private static ViewAxes viewAxes(Minecraft mc) {
		Camera camera = mc.gameRenderer.getMainCamera();
		Vector3f look = camera.getLookVector();
		Vec3 liveForward = new Vec3(look.x(), look.y(), look.z());
		if (TacticalCameraController.isActive() && liveForward.lengthSqr() > 1.0e-8) {
			Vector3f upv = camera.getUpVector();
			Vector3f left = camera.getLeftVector();
			Vec3 origin = camera.getPosition();
			Vec3 forward = liveForward.normalize();
			Vec3 up = new Vec3(upv.x(), upv.y(), upv.z());
			if (up.lengthSqr() < 1.0e-8) {
				up = cameraRight(forward).cross(forward);
			}
			up = up.normalize();
			Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
			if (right.lengthSqr() < 1.0e-8) {
				right = cameraRight(forward);
			} else {
				right = right.normalize();
			}
			return new ViewAxes(origin, forward, right, up);
		}
		Vec3 origin = TacticalCameraController.getCameraEye();
		Vec3 forward = TacticalCameraController.getCameraLookVector();
		Vec3 right = cameraRight(forward);
		Vec3 up = right.cross(forward).normalize();
		return new ViewAxes(origin, forward, right, up);
	}

	private static Vec3 cameraRight(Vec3 forward) {
		Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
		if (right.lengthSqr() < 1.0e-8) {
			return new Vec3(1.0, 0.0, 0.0);
		}
		return right.normalize();
	}

	private static double tanHalfFov(Minecraft mc) {
		return Math.tan(Math.toRadians(mc.options.fov().get()) * 0.5);
	}

	private static double framebufferAspect(Minecraft mc) {
		return (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
	}
}
