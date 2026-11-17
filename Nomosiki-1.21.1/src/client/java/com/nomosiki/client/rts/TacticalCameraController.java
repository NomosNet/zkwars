package com.nomosiki.client.rts;

import com.nomosiki.client.rts.TacticalHudScreen;
import com.nomosiki.config.NomosikiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class TacticalCameraController {
	private static final double YAW_STEP = 2.5;
	private static final float SAVED_FOV_UNSET = -1f;

	private static boolean active;
	private static double focusX;
	private static double focusY;
	private static double focusZ;
	private static float yaw;
	private static float height;
	private static float savedFov = SAVED_FOV_UNSET;
	private static double pendingScroll;

	private TacticalCameraController() {
	}

	public static boolean isActive() {
		return active;
	}

	public static boolean toggle(Minecraft mc) {
		if (active) {
			deactivate(mc);
			return false;
		}
		return activate(mc);
	}

	public static void deactivate(Minecraft mc) {
		if (!active) {
			return;
		}
		active = false;
		pendingScroll = 0.0;
		RtsClientState.setBombardDesignate(false);
		restoreFov(mc);
		RtsCommandModeClient.syncActive(false);
		FormationGhostRenderer.clear();
		RtsClientState.ensureSelectionAfterTacticalExit();
		if (mc.screen instanceof TacticalHudScreen) {
			mc.setScreen(null);
		}
	}

	private static boolean activate(Minecraft mc) {
		NomosikiConfig cfg = NomosikiConfig.INSTANCE;
		if (!cfg.rtsEnabled || !cfg.rtsTacticalCameraEnabled || mc.player == null || mc.level == null) {
			return false;
		}
		Vec3 focus = resolveInitialFocus(mc);
		focusX = focus.x;
		focusY = focus.y;
		focusZ = focus.z;
		yaw = mc.player.getYRot();
		height = cfg.rtsTacticalDefaultHeight;
		active = true;
		if (mc.gameMode != null) {
			mc.gameMode.stopDestroyBlock();
		}
		applyFovBoost(mc);
		RtsCommandModeClient.syncActive(true);
		if (!(mc.screen instanceof TacticalHudScreen)) {
			mc.setScreen(new TacticalHudScreen());
		}
		return true;
	}

	public static void tick(Minecraft mc) {
		if (!active || mc.player == null || mc.level == null) {
			return;
		}
		if (!mc.player.isAlive()) {
			deactivate(mc);
			return;
		}
		if (!NomosikiConfig.INSTANCE.rtsEnabled) {
			deactivate(mc);
			return;
		}
		if (mc.screen == null) {
			mc.setScreen(new TacticalHudScreen());
			return;
		}
		if (!allowsCameraPan(mc)) {
			return;
		}

		long window = mc.getWindow().getWindow();
		NomosikiConfig cfg = NomosikiConfig.INSTANCE;
		float pan = cfg.rtsTacticalPanSpeed;
		float forward = 0f;
		float strafe = 0f;
		if (isKeyDown(window, GLFW.GLFW_KEY_W)) {
			forward += 1f;
		}
		if (isKeyDown(window, GLFW.GLFW_KEY_S)) {
			forward -= 1f;
		}
		if (isKeyDown(window, GLFW.GLFW_KEY_A)) {
			strafe += 1f;
		}
		if (isKeyDown(window, GLFW.GLFW_KEY_D)) {
			strafe -= 1f;
		}
		if (forward != 0f || strafe != 0f) {
			double rad = Math.toRadians(yaw);
			double sin = Math.sin(rad);
			double cos = Math.cos(rad);
			focusX += (-forward * sin + strafe * cos) * pan;
			focusZ += (forward * cos + strafe * sin) * pan;
		}
		if (isKeyDown(window, GLFW.GLFW_KEY_Q)) {
			yaw -= YAW_STEP;
		}
		if (isKeyDown(window, GLFW.GLFW_KEY_E)) {
			yaw += YAW_STEP;
		}

		if (pendingScroll != 0.0) {
			height = Mth.clamp(
				height - (float) pendingScroll * cfg.rtsTacticalZoomStep,
				cfg.rtsTacticalMinHeight,
				cfg.rtsTacticalMaxHeight
			);
			pendingScroll = 0.0;
		}
	}

	public static boolean allowsCameraPan(Minecraft mc) {
		if (!active) {
			return false;
		}
		if (mc.screen == null) {
			return true;
		}
		return mc.screen instanceof TacticalHudScreen
			|| mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen;
	}

	public static void onMouseScroll(double vertical) {
		if (active && vertical != 0.0) {
			pendingScroll += vertical;
		}
	}

	public static double cameraX() {
		return focusX;
	}

	public static double cameraY() {
		return focusY + height;
	}

	public static double cameraZ() {
		return focusZ;
	}

	public static float cameraYaw() {
		return yaw;
	}

	public static void freezePlayerMovement(LocalPlayer player) {
		if (!active) {
			return;
		}
		if (player.input != null) {
			player.input.forwardImpulse = 0f;
			player.input.leftImpulse = 0f;
			player.input.jumping = false;
			player.input.up = false;
			player.input.down = false;
			player.input.left = false;
			player.input.right = false;
		}
		player.xxa = 0f;
		player.zza = 0f;
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(0.0, player.isPassenger() ? 0.0 : motion.y, 0.0);
		Entity vehicle = player.getVehicle();
		if (vehicle != null) {
			vehicle.setDeltaMovement(Vec3.ZERO);
			vehicle.fallDistance = 0.0f;
			if (vehicle instanceof LivingEntity living) {
				living.xxa = 0f;
				living.zza = 0f;
			}
		}
	}

	@Nullable
	public static BlockPos raycastAnchor(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return null;
		}
		BlockHitResult hit = raycast(mc);
		if (hit == null || hit.getType() == HitResult.Type.MISS) {
			return null;
		}
		return hit.getBlockPos();
	}

	@Nullable
	public static BlockHitResult raycast(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return null;
		}
		Vec3 eye = getCameraEye();
		Vec3 look = getCameraLookVector();
		double reach = active
			? NomosikiConfig.INSTANCE.rtsTacticalRaycastRange
			: 64.0;
		Vec3 end = eye.add(look.scale(reach));
		return mc.level.clip(new ClipContext(
			eye,
			end,
			ClipContext.Block.OUTLINE,
			ClipContext.Fluid.NONE,
			mc.player
		));
	}

	public static BlockPos focusBlockPos() {
		return BlockPos.containing(focusX, focusY, focusZ);
	}

	public static Vec3 getCameraEye() {
		if (!active) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				return mc.player.getEyePosition(1f);
			}
			return Vec3.ZERO;
		}
		return new Vec3(focusX, focusY + height, focusZ);
	}

	public static Vec3 getCameraLookVector() {
		if (!active) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				return mc.player.getViewVector(1f);
			}
			return new Vec3(0, -1, 0);
		}
		float pitchRad = (float) Math.toRadians(NomosikiConfig.INSTANCE.rtsTacticalPitch);
		float yawRad = (float) Math.toRadians(yaw);
		float cosPitch = Mth.cos(pitchRad);
		return new Vec3(
			-Mth.sin(yawRad) * cosPitch,
			-Mth.sin(pitchRad),
			Mth.cos(yawRad) * cosPitch
		);
	}

	private static Vec3 resolveInitialFocus(Minecraft mc) {
		Player player = mc.player;
		if (player == null) {
			return Vec3.ZERO;
		}
		return player.position();
	}

	private static void applyFovBoost(Minecraft mc) {
		if (mc.options == null) {
			return;
		}
		float boost = NomosikiConfig.INSTANCE.rtsTacticalFovBoost;
		if (boost <= 0f) {
			return;
		}
		if (savedFov < 0f) {
			savedFov = mc.options.fov().get().floatValue();
		}
		mc.options.fov().set((int) Mth.clamp(savedFov + boost, 30, 130));
	}

	private static void restoreFov(Minecraft mc) {
		if (mc.options == null || savedFov < 0f) {
			savedFov = SAVED_FOV_UNSET;
			return;
		}
		mc.options.fov().set((int) savedFov);
		savedFov = SAVED_FOV_UNSET;
	}

	private static boolean isKeyDown(long window, int key) {
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}
}
