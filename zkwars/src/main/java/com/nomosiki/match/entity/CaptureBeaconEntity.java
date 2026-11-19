package com.nomosiki.match.entity;

import com.nomosiki.match.MatchService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Capture point beacon. Friendly units fill progress; a planted sword speeds capture.
 * States: neutral / contesting / hold / owned (encoded in progress + owner side).
 */
public class CaptureBeaconEntity extends Mob {
	public static final String NBT_POINT_ID = "PointId";
	public static final String NBT_OWNER_SIDE = "OwnerSide";
	public static final String NBT_PROGRESS = "Progress";
	public static final String NBT_HOLD = "HoldTicks";
	public static final String NBT_BLADE = "BladeOwner";
	public static final String NBT_CAPTURING_SIDE = "CapturingSide";

	/** -1 = unowned. */
	public static final byte SIDE_NONE = -1;

	private static final EntityDataAccessor<Byte> DATA_OWNER_SIDE =
		SynchedEntityData.defineId(CaptureBeaconEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_CAPTURING_SIDE =
		SynchedEntityData.defineId(CaptureBeaconEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> DATA_PROGRESS =
		SynchedEntityData.defineId(CaptureBeaconEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> DATA_HAS_BLADE =
		SynchedEntityData.defineId(CaptureBeaconEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_HOLD_TICKS =
		SynchedEntityData.defineId(CaptureBeaconEntity.class, EntityDataSerializers.INT);

	private String pointId = "point";
	@Nullable
	private UUID bladeOwnerId;

	public CaptureBeaconEntity(EntityType<? extends CaptureBeaconEntity> type, Level level) {
		super(type, level);
		this.setNoAi(true);
		this.setPersistenceRequired();
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER_SIDE, SIDE_NONE);
		builder.define(DATA_CAPTURING_SIDE, SIDE_NONE);
		builder.define(DATA_PROGRESS, 0.0f);
		builder.define(DATA_HAS_BLADE, false);
		builder.define(DATA_HOLD_TICKS, 0);
	}

	public String pointId() {
		return this.pointId;
	}

	public void setPointId(String pointId) {
		this.pointId = pointId == null || pointId.isBlank() ? "point" : pointId;
	}

	public byte ownerSide() {
		return this.entityData.get(DATA_OWNER_SIDE);
	}

	public void setOwnerSide(byte side) {
		this.entityData.set(DATA_OWNER_SIDE, side);
	}

	public byte capturingSide() {
		return this.entityData.get(DATA_CAPTURING_SIDE);
	}

	public void setCapturingSide(byte side) {
		this.entityData.set(DATA_CAPTURING_SIDE, side);
	}

	public float progress() {
		return this.entityData.get(DATA_PROGRESS);
	}

	public void setProgress(float progress) {
		this.entityData.set(DATA_PROGRESS, Math.max(0.0f, Math.min(100.0f, progress)));
	}

	public boolean hasBlade() {
		return this.entityData.get(DATA_HAS_BLADE);
	}

	public int holdTicksRemaining() {
		return this.entityData.get(DATA_HOLD_TICKS);
	}

	public void setHoldTicksRemaining(int holdTicksRemaining) {
		this.entityData.set(DATA_HOLD_TICKS, Math.max(0, holdTicksRemaining));
	}

	@Nullable
	public UUID bladeOwnerId() {
		return this.bladeOwnerId;
	}

	public void clearBlade() {
		this.bladeOwnerId = null;
		this.entityData.set(DATA_HAS_BLADE, false);
	}

	public void plantBlade(UUID ownerId) {
		this.bladeOwnerId = ownerId;
		this.entityData.set(DATA_HAS_BLADE, true);
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(0.0, 0.0, 0.0);
		if (!this.level().isClientSide && this.tickCount % 20 == 0 && this.level().getServer() != null) {
			MatchService.get(this.level().getServer()).tickCaptureBeacon(this);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (this.level().isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		if (!player.getItemInHand(hand).is(ItemTags.SWORDS)) {
			serverPlayer.displayClientMessage(Component.translatable("message.nomosiki.match.beacon_need_sword"), true);
			return InteractionResult.CONSUME;
		}
		MatchService service = MatchService.get(serverPlayer.server);
		Byte side = service.sideOfPlayer(serverPlayer.getUUID());
		if (side == null) {
			serverPlayer.displayClientMessage(Component.translatable("message.nomosiki.match.not_in_match"), true);
			return InteractionResult.CONSUME;
		}
		if (this.hasBlade() && this.bladeOwnerId != null && this.bladeOwnerId.equals(serverPlayer.getUUID())) {
			this.clearBlade();
			serverPlayer.displayClientMessage(Component.translatable("message.nomosiki.match.blade_removed"), true);
			return InteractionResult.CONSUME;
		}
		this.plantBlade(serverPlayer.getUUID());
		player.getItemInHand(hand).hurtAndBreak(1, player, player.getSlotForHand(hand));
		serverPlayer.displayClientMessage(Component.translatable("message.nomosiki.match.blade_planted"), true);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString(NBT_POINT_ID, this.pointId);
		tag.putByte(NBT_OWNER_SIDE, this.ownerSide());
		tag.putByte(NBT_CAPTURING_SIDE, this.capturingSide());
		tag.putFloat(NBT_PROGRESS, this.progress());
		tag.putInt(NBT_HOLD, this.holdTicksRemaining());
		if (this.bladeOwnerId != null) {
			tag.putUUID(NBT_BLADE, this.bladeOwnerId);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.setPointId(tag.getString(NBT_POINT_ID));
		this.setOwnerSide(tag.getByte(NBT_OWNER_SIDE));
		this.setCapturingSide(tag.getByte(NBT_CAPTURING_SIDE));
		this.setProgress(tag.getFloat(NBT_PROGRESS));
		this.setHoldTicksRemaining(tag.getInt(NBT_HOLD));
		if (tag.hasUUID(NBT_BLADE)) {
			this.plantBlade(tag.getUUID(NBT_BLADE));
		} else {
			this.clearBlade();
		}
	}

	@Override
	public Component getName() {
		return Component.translatable("entity.zkwars.capture_beacon");
	}
}
