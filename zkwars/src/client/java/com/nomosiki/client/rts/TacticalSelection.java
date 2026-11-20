package com.nomosiki.client.rts;

import com.nomosiki.match.entity.BuildingHeartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Building picked in the tactical view. Selecting a squad clears it, and selecting a building clears squads.
 */
public final class TacticalSelection {
	private static int buildingId = -1;

	private TacticalSelection() {
	}

	public static void selectBuilding(BuildingHeartEntity building) {
		buildingId = building == null ? -1 : building.getId();
		RtsClientState.clearSelection();
	}

	public static void clearBuilding() {
		buildingId = -1;
	}

	@Nullable
	public static BuildingHeartEntity building(Minecraft mc) {
		if (buildingId < 0 || mc == null || mc.level == null) {
			return null;
		}
		Entity entity = mc.level.getEntity(buildingId);
		if (!(entity instanceof BuildingHeartEntity heart) || !heart.isAlive()) {
			buildingId = -1;
			return null;
		}
		return heart;
	}
}
