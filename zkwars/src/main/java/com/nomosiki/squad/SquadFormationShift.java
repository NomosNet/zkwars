package com.nomosiki.squad;

import com.nomosiki.entity.NomosikEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Incremental anchor movement for tactical formations (Recruits forward / backward commands).
 * Shifts along the squad's saved formation axis, not the commander's camera yaw.
 */
public final class SquadFormationShift {
	private SquadFormationShift() {
	}

	/**
	 * @param signedStepBlocks positive = forward along formation front, negative = backward
	 */
	public static Optional<BlockPos> shiftAnchor(
		ServerLevel level,
		BlockPos currentAnchor,
		UUID squadId,
		NomosikEntity facingSource,
		int signedStepBlocks
	) {
		if (signedStepBlocks == 0) {
			return Optional.empty();
		}
		Vec3 forward = SquadBrain.formationForwardUnit(squadId, currentAnchor, facingSource);
		Vec3 anchorCenter = Vec3.atBottomCenterOf(currentAnchor);
		Vec3 shifted = anchorCenter.add(forward.scale(signedStepBlocks));
		BlockPos snapped = SquadFormationLayout.surfaceAt(
			level,
			BlockPos.containing(shifted.x, shifted.y, shifted.z)
		);
		return Optional.of(snapped);
	}
}
