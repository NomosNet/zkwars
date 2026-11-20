package com.nomosiki.client.rts;

import com.nomosiki.client.match.MatchClient;
import com.nomosiki.match.entity.BuildingHeartEntity;
import com.nomosiki.match.entity.CaptureBeaconEntity;
import com.nomosiki.network.squad.payload.s2c.SquadArmyStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.AABB;

/** Local map around the tactical focus. A click recenters the camera. */
public final class TacticalMinimap {
	private static final double RADIUS = 96.0;

	private TacticalMinimap() {
	}

	public static void render(GuiGraphics graphics, Minecraft mc) {
		if (mc == null || mc.level == null) {
			return;
		}
		int left = TacticalChrome.mapLeft();
		int top = TacticalChrome.mapTop(mc.getWindow().getGuiScaledHeight());
		int size = TacticalChrome.MAP;
		graphics.fill(left, top, left + size, top + size, 0xCC12100E);
		graphics.renderOutline(left, top, size, size, 0xFFD4A574);
		double focusX = TacticalCameraController.cameraX();
		double focusZ = TacticalCameraController.cameraZ();
		byte yourSide = MatchClient.last().yourSide();
		for (SquadArmyStatePayload.SquadRow row : RtsClientState.armySquads()) {
			if (row.alive() <= 0) {
				continue;
			}
			dot(graphics, left, top, size, focusX, focusZ, row.centerX(), row.centerZ(), 0xFF8BC34A);
		}
		AABB box = new AABB(focusX - RADIUS, -64, focusZ - RADIUS, focusX + RADIUS, 320, focusZ + RADIUS);
		for (BuildingHeartEntity heart : mc.level.getEntitiesOfClass(BuildingHeartEntity.class, box, BuildingHeartEntity::isAlive)) {
			int color = heart.matchSide() == yourSide ? 0xFF4FC3F7 : 0xFFE57373;
			dot(graphics, left, top, size, focusX, focusZ, heart.getX(), heart.getZ(), color);
		}
		for (CaptureBeaconEntity beacon : mc.level.getEntitiesOfClass(CaptureBeaconEntity.class, box, beacon -> beacon.isAlive())) {
			int color = beacon.ownerSide() == yourSide ? 0xFFFFE082
				: beacon.ownerSide() == CaptureBeaconEntity.SIDE_NONE ? 0xFFB0BEC5 : 0xFFFF8A80;
			dot(graphics, left, top, size, focusX, focusZ, beacon.getX(), beacon.getZ(), color);
		}
		int cx = left + size / 2;
		int cy = top + size / 2;
		graphics.fill(cx - 1, cy - 3, cx + 1, cy + 3, 0xFFFFFFFF);
		graphics.fill(cx - 3, cy - 1, cx + 3, cy + 1, 0xFFFFFFFF);
	}

	public static boolean click(int screenHeight, double mouseX, double mouseY) {
		if (!TacticalChrome.hitMap(screenHeight, mouseX, mouseY)) {
			return false;
		}
		int left = TacticalChrome.mapLeft();
		int top = TacticalChrome.mapTop(screenHeight);
		int size = TacticalChrome.MAP;
		double alongRight = (mouseX - (left + size / 2.0)) / size * RADIUS * 2.0;
		double alongForward = ((top + size / 2.0) - mouseY) / size * RADIUS * 2.0;
		double yawRad = Math.toRadians(TacticalCameraController.cameraYaw());
		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);
		double x = TacticalCameraController.cameraX() + alongRight * cos + alongForward * (-sin);
		double z = TacticalCameraController.cameraZ() + alongRight * sin + alongForward * cos;
		TacticalCameraController.focusAt(x, z);
		return true;
	}

	private static void dot(
		GuiGraphics graphics,
		int left,
		int top,
		int size,
		double focusX,
		double focusZ,
		double worldX,
		double worldZ,
		int color
	) {
		double yawRad = Math.toRadians(TacticalCameraController.cameraYaw());
		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);
		double dx = worldX - focusX;
		double dz = worldZ - focusZ;
		double alongRight = dx * cos + dz * sin;
		double alongForward = dx * (-sin) + dz * cos;
		int px = left + size / 2 + (int) Math.round(alongRight / (RADIUS * 2.0) * size);
		int py = top + size / 2 - (int) Math.round(alongForward / (RADIUS * 2.0) * size);
		if (px < left + 2 || py < top + 2 || px >= left + size - 2 || py >= top + size - 2) {
			return;
		}
		graphics.fill(px - 1, py - 1, px + 2, py + 2, color);
	}
}
