package com.nomosiki.entity;

import com.nomosiki.combat.StrategicFireController;
import com.nomosiki.compat.gun.GunsCompat;
import com.nomosiki.squad.SquadFormationLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

final class NomosikSquadLogic {
	static final int FORMATION_FACING_ALIGN_TICKS = 20;

	private NomosikSquadLogic() {
	}

	static void tickFormationFacing(NomosikEntity entity) {
		if (entity.squadFormationRotateTicks <= 0) {
			return;
		}
		if (StrategicFireController.isActive(entity)) {
			return;
		}
		if (!entity.getNavigation().isDone()) {
			return;
		}
		applyFormationFacingYaw(entity);
		entity.squadFormationRotateTicks--;
	}

	private static void applyFormationFacingYaw(NomosikEntity entity) {
		float yaw = entity.squadFormationFacingYaw;
		entity.setYRot(yaw);
		entity.yRotO = yaw;
		entity.yBodyRot = yaw;
		if (entity.getTarget() == null) {
			entity.yHeadRot = yaw;
		}
	}

	static boolean isAtFormationSlot(NomosikEntity entity) {
		Vec3 slot = entity.squadFormationTarget;
		return slot != null && entity.distanceToSqr(slot) <= NomosikEntity.SQUAD_FORMATION_SLOT_TOL_SQR;
	}

	static boolean shouldLockFormationBodyFacing(NomosikEntity entity) {
		return entity.squadInFormation && isAtFormationSlot(entity) && entity.getNavigation().isDone();
	}

	static void beginFormationFacing(NomosikEntity entity, int ticks) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadFormationRotateTicks = Math.max(entity.squadFormationRotateTicks, ticks);
	}

	static Optional<UUID> getSquadId(NomosikEntity entity) {
		return Optional.ofNullable(entity.squadId);
	}

	static Optional<UUID> getSquadIdForGui(NomosikEntity entity) {
		return entity.getEntityData().get(NomosikEntity.DATA_SQUAD_ID);
	}

	static void joinSquadServer(NomosikEntity entity, UUID squadUuid) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadId = squadUuid;
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_ID, Optional.of(squadUuid));
	}

	static void clearSquadIdServer(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadId = null;
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_ID, Optional.empty());
		clearSquadFormationAssignmentServer(entity);
	}

	static boolean isSquadInFormation(NomosikEntity entity) {
		return entity.squadInFormation;
	}

	static boolean isSquadHoldFormation(NomosikEntity entity) {
		return entity.squadHoldFormation;
	}

	static float getSquadFormationFacingYaw(NomosikEntity entity) {
		return entity.squadFormationFacingYaw;
	}

	static void setSquadFormationFlagsServer(NomosikEntity entity, boolean inFormation, boolean holdFormation) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadInFormation = inFormation;
		entity.squadHoldFormation = holdFormation;
	}

	static void setSquadFormationFacingServer(NomosikEntity entity, float yawDegrees) {
		if (entity.level().isClientSide()) {
			return;
		}
		float previous = entity.squadFormationFacingYaw;
		entity.squadFormationFacingYaw = yawDegrees;
		if (entity.squadInFormation && isAtFormationSlot(entity)
			&& Math.abs(Mth.wrapDegrees(yawDegrees - previous)) > 8.0F) {
			beginFormationFacing(entity, FORMATION_FACING_ALIGN_TICKS);
		}
	}

	static void clearSquadFormationAssignmentServer(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadFormationTarget = null;
		entity.squadFormationSlot = -1;
		entity.squadFormationRotateTicks = 0;
		entity.clearFormationRouteState();
		setSquadFormationFlagsServer(entity, false, false);
	}

	static @Nullable Vec3 getSquadFormationTarget(NomosikEntity entity) {
		return entity.squadFormationTarget;
	}

	static void setSquadFormationTargetServer(NomosikEntity entity, @Nullable Vec3 target) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadFormationTarget = target;
		if (target == null) {
			setSquadFormationFlagsServer(entity, false, false);
		}
	}

	static SquadFormation getSquadFormation(NomosikEntity entity) {
		return entity.squadFormation;
	}

	static SquadFormation getSquadFormationForGui(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return SquadFormation.fromId(entity.getEntityData().get(NomosikEntity.DATA_SQUAD_FORMATION));
		}
		return entity.squadFormation;
	}

	static void setSquadFormationServer(NomosikEntity entity, SquadFormation formation) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadFormation = formation;
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_FORMATION, formation.id());
		if (formation == SquadFormation.NONE) {
			clearSquadFormationAssignmentServer(entity);
		}
	}

	static double getSquadFormationSpacing(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return entity.getEntityData().get(NomosikEntity.DATA_SQUAD_FORMATION_SPACING);
		}
		return entity.squadFormationSpacing;
	}

	static void setSquadFormationSpacingServer(NomosikEntity entity, double spacing) {
		if (entity.level().isClientSide()) {
			return;
		}
		double clamped = clampSquadFormationSpacing(spacing);
		entity.squadFormationSpacing = clamped;
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_FORMATION_SPACING, (float) clamped);
	}

	static double clampSquadFormationSpacing(double spacing) {
		return Mth.clamp(spacing, NomosikEntity.SQUAD_FORMATION_SPACING_MIN, NomosikEntity.SQUAD_FORMATION_SPACING_MAX);
	}

	static int getSquadFormationMaxInRow(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return entity.getEntityData().get(NomosikEntity.DATA_SQUAD_FORMATION_MAX_IN_ROW);
		}
		return entity.squadFormationMaxInRow;
	}

	static void setSquadFormationMaxInRowServer(NomosikEntity entity, int maxInRow) {
		if (entity.level().isClientSide()) {
			return;
		}
		int clamped = clampSquadFormationMaxInRow(maxInRow);
		entity.squadFormationMaxInRow = clamped;
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_FORMATION_MAX_IN_ROW, clamped);
	}

	static int clampSquadFormationMaxInRow(int maxInRow) {
		return Mth.clamp(maxInRow, NomosikEntity.SQUAD_FORMATION_MAX_IN_ROW_MIN, NomosikEntity.SQUAD_FORMATION_MAX_IN_ROW_MAX);
	}

	static int getSquadFormationSlot(NomosikEntity entity) {
		return entity.squadFormationSlot;
	}

	static void setSquadFormationSlotServer(NomosikEntity entity, int slot) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadFormationSlot = slot;
	}

	static SquadOrder getSquadOrder(NomosikEntity entity) {
		return entity.squadOrder;
	}

	static SquadOrder getSquadOrderForGui(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return SquadOrder.fromId(entity.getEntityData().get(NomosikEntity.DATA_SQUAD_ORDER_SYNC));
		}
		return entity.squadOrder;
	}

	static int getSquadZoneRadiusForGui(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return entity.getEntityData().get(NomosikEntity.DATA_SQUAD_ZONE_RADIUS_SYNC);
		}
		return entity.squadZoneRadius;
	}

	static NomosikCombatMode getCombatMode(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return NomosikCombatMode.fromId(entity.getEntityData().get(NomosikEntity.DATA_COMBAT_MODE));
		}
		return entity.combatMode;
	}

	static void setCombatMode(NomosikEntity entity, NomosikCombatMode mode) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.combatMode = mode;
		entity.getEntityData().set(NomosikEntity.DATA_COMBAT_MODE, mode.id());
		if (mode == NomosikCombatMode.PASSIVE || mode == NomosikCombatMode.NEUTRAL) {
			entity.setTarget(null);
		}
		if (mode == NomosikCombatMode.RAID) {
			entity.squadHoldFormation = false;
		}
		GunsCompat.applyStanceHands(entity);
	}

	static NomosikRangedFireMode getRangedFireMode(NomosikEntity entity) {
		if (!entity.canUseRangedFireOrders()) {
			return NomosikRangedFireMode.FIRE_AT_WILL;
		}
		if (entity.level().isClientSide()) {
			return NomosikRangedFireMode.fromId(entity.getEntityData().get(NomosikEntity.DATA_RANGED_FIRE_MODE));
		}
		return entity.rangedFireMode;
	}

	static void setRangedFireMode(NomosikEntity entity, NomosikRangedFireMode mode) {
		if (entity.level().isClientSide() || !entity.canUseRangedFireOrders()) {
			return;
		}
		entity.rangedFireMode = mode;
		entity.getEntityData().set(NomosikEntity.DATA_RANGED_FIRE_MODE, mode.id());
		if (mode != NomosikRangedFireMode.STRATEGIC_FIRE) {
			entity.rangedFireZone = null;
			entity.rangedFireAnchor = null;
			entity.rangedFireRadius = 0;
		}
		if (mode == NomosikRangedFireMode.STRATEGIC_FIRE) {
			entity.setTarget(null);
		}
		if (mode == NomosikRangedFireMode.HOLD_FIRE) {
			entity.stopUsingItem();
		}
	}

	static void setRangedFireZone(NomosikEntity entity, BlockPos anchor, int zoneRadius) {
		if (entity.level().isClientSide() || !entity.canUseRangedFireOrders() || anchor == null) {
			return;
		}
		int radius = Math.max(2, Math.min(32, zoneRadius));
		entity.rangedFireAnchor = anchor;
		entity.rangedFireRadius = radius;
		entity.rangedFireZone = squadBoxAroundBlock(anchor, radius);
		entity.rangedFireMode = NomosikRangedFireMode.STRATEGIC_FIRE;
		entity.getEntityData().set(NomosikEntity.DATA_RANGED_FIRE_MODE, entity.rangedFireMode.id());
		entity.setTarget(null);
	}

	static @Nullable AABB getRangedFireZone(NomosikEntity entity) {
		return entity.rangedFireZone;
	}

	static void restoreRangedFireZone(NomosikEntity entity) {
		if (entity.rangedFireMode == NomosikRangedFireMode.STRATEGIC_FIRE && entity.rangedFireAnchor != null) {
			int radius = Math.max(2, Math.min(32, entity.rangedFireRadius));
			entity.rangedFireRadius = radius;
			entity.rangedFireZone = squadBoxAroundBlock(entity.rangedFireAnchor, radius);
			return;
		}
		entity.rangedFireZone = null;
		if (entity.rangedFireMode != NomosikRangedFireMode.STRATEGIC_FIRE) {
			entity.rangedFireAnchor = null;
			entity.rangedFireRadius = 0;
		}
	}

	static @Nullable AABB getEffectiveRangedFireZone(NomosikEntity entity) {
		if (entity.rangedFireZone != null) {
			return entity.rangedFireZone;
		}
		if (entity.rangedFireMode == NomosikRangedFireMode.STRATEGIC_FIRE && entity.squadAttackZone != null) {
			return entity.squadAttackZone;
		}
		return null;
	}

	static boolean isTargetInRangedFireZone(NomosikEntity entity, LivingEntity target) {
		AABB zone = getEffectiveRangedFireZone(entity);
		if (zone == null) {
			return true;
		}
		var p = target.position();
		return zone.contains(p.x, p.y, p.z);
	}

	static void syncSquadStateToData(NomosikEntity entity) {
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_ORDER_SYNC, entity.squadOrder.id());
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_ZONE_RADIUS_SYNC, entity.squadZoneRadius);
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_FORMATION, entity.squadFormation.id());
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_FORMATION_SPACING, (float) entity.squadFormationSpacing);
		entity.getEntityData().set(NomosikEntity.DATA_SQUAD_FORMATION_MAX_IN_ROW, entity.squadFormationMaxInRow);
	}

	static @Nullable BlockPos getSquadAnchorPos(NomosikEntity entity) {
		return entity.squadAnchorPos;
	}

	static @Nullable AABB getSquadAttackZone(NomosikEntity entity) {
		return entity.squadAttackZone;
	}

	static void applySquadOrder(NomosikEntity entity, SquadOrder order, @Nullable BlockPos anchor, int zoneRadius) {
		if (!entity.level().isClientSide) {
			com.nomosiki.compat.emote.server.NomosikSquadEmoteService.interruptIfConfiguredOnOrder(entity);
		}
		int radius = Math.max(2, Math.min(32, zoneRadius));
		if (order != SquadOrder.ATTACK_TARGET) {
			entity.squadAttackTargetId = null;
			entity.squadAttackTargetIsPlayer = false;
		}
		entity.squadOrder = order;
		switch (order) {
			case FOLLOW -> {
				entity.squadAnchorPos = null;
				entity.squadAttackZone = null;
				if (entity.squadFormation == SquadFormation.NONE) {
					clearSquadFormationAssignmentServer(entity);
				}
				entity.setShouldFollowOwner(true);
			}
			case HOLD -> {
				if (anchor == null) {
					applySquadOrderFallbackFollow(entity);
					return;
				}
				entity.squadAnchorPos = anchor.immutable();
				entity.squadAttackZone = null;
				entity.squadHoldFormation = true;
				entity.setShouldFollowOwner(false);
				entity.getNavigation().stop();
			}
			case MOVE_TO -> {
				if (anchor == null) {
					applySquadOrderFallbackFollow(entity);
					return;
				}
				entity.squadAnchorPos = anchor.immutable();
				entity.squadAttackZone = null;
				entity.squadHoldFormation = false;
				entity.setShouldFollowOwner(false);
			}
			case ATTACK_ZONE, ATTACK_TARGET -> {
				if (anchor == null) {
					applySquadOrderFallbackFollow(entity);
					return;
				}
				entity.squadAnchorPos = anchor.immutable();
				entity.squadZoneRadius = radius;
				entity.squadAttackZone = squadBoxAroundBlock(anchor, radius);
				entity.squadHoldFormation = order == SquadOrder.ATTACK_ZONE;
				entity.setShouldFollowOwner(false);
			}
		}
		syncSquadStateToData(entity);
	}

	static void applyAttackTargetOrder(NomosikEntity entity, UUID targetId, BlockPos anchor, int zoneRadius) {
		applyAttackTargetOrder(entity, targetId, anchor, zoneRadius, false);
	}

	static void applyAttackTargetOrder(
		NomosikEntity entity,
		UUID targetId,
		BlockPos anchor,
		int zoneRadius,
		boolean targetIsPlayer
	) {
		if (entity.level().isClientSide() || targetId == null || anchor == null) {
			return;
		}
		entity.squadAttackTargetId = targetId;
		entity.squadAttackTargetIsPlayer = targetIsPlayer;
		applySquadOrder(entity, SquadOrder.ATTACK_TARGET, anchor, zoneRadius);
	}

	static void refreshAttackTargetZone(NomosikEntity entity, BlockPos anchor) {
		if (entity.level().isClientSide() || anchor == null) {
			return;
		}
		if (entity.squadOrder != SquadOrder.ATTACK_TARGET) {
			return;
		}
		int radius = Math.max(2, Math.min(32, entity.squadZoneRadius));
		entity.squadAnchorPos = anchor.immutable();
		entity.squadZoneRadius = radius;
		entity.squadAttackZone = squadBoxAroundBlock(anchor, radius);
		entity.setShouldFollowOwner(false);
		syncSquadStateToData(entity);
	}

	static void convertAttackTargetToZone(NomosikEntity entity) {
		if (entity.level().isClientSide()) {
			return;
		}
		entity.squadAttackTargetId = null;
		entity.squadAttackTargetIsPlayer = false;
		if (entity.squadAnchorPos == null) {
			applySquadOrderFallbackFollow(entity);
			return;
		}
		entity.squadOrder = SquadOrder.ATTACK_ZONE;
		int radius = Math.max(2, Math.min(32, entity.squadZoneRadius));
		entity.squadAttackZone = squadBoxAroundBlock(entity.squadAnchorPos, radius);
		entity.squadHoldFormation = true;
		entity.setShouldFollowOwner(false);
		syncSquadStateToData(entity);
	}

	static AABB squadBoxAroundBlock(BlockPos pos, double inflateBlocks) {
		return new AABB(
			pos.getX() - inflateBlocks,
			pos.getY() - inflateBlocks,
			pos.getZ() - inflateBlocks,
			pos.getX() + 1.0 + inflateBlocks,
			pos.getY() + 1.0 + inflateBlocks,
			pos.getZ() + 1.0 + inflateBlocks
		);
	}

	static void applySquadOrderFallbackFollow(NomosikEntity entity) {
		entity.squadOrder = SquadOrder.FOLLOW;
		entity.squadAnchorPos = null;
		entity.squadAttackZone = null;
		entity.squadAttackTargetId = null;
		entity.squadAttackTargetIsPlayer = false;
		clearSquadFormationAssignmentServer(entity);
		entity.setShouldFollowOwner(true);
		syncSquadStateToData(entity);
	}

	static void rebuildSquadAttackZoneFromLoadedState(NomosikEntity entity) {
		if (entity.squadOrder.usesAttackVolume() && entity.squadAnchorPos != null) {
			int r = Math.max(2, Math.min(32, entity.squadZoneRadius));
			entity.squadAttackZone = squadBoxAroundBlock(entity.squadAnchorPos, r);
			if (entity.squadOrder == SquadOrder.ATTACK_TARGET && entity.squadAttackTargetId == null) {
				entity.squadOrder = SquadOrder.ATTACK_ZONE;
			}
			return;
		}
		entity.squadAttackZone = null;
		if (entity.squadOrder == SquadOrder.ATTACK_TARGET) {
			entity.squadAttackTargetId = null;
			entity.squadAttackTargetIsPlayer = false;
			entity.squadOrder = SquadOrder.FOLLOW;
		}
		if (entity.squadOrder != SquadOrder.FOLLOW && entity.squadAnchorPos == null) {
			entity.squadOrder = SquadOrder.FOLLOW;
		}
	}
}
