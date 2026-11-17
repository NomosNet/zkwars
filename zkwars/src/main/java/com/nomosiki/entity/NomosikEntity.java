package com.nomosiki.entity;

import com.nomosiki.NomosikMainThread;
import com.nomosiki.combat.StrategicFireController;
import com.nomosiki.advancement.NomosikiActions;
import com.nomosiki.advancement.NomosikiAdvancements;
import com.nomosiki.compat.items.FactionArmorCompat;
import com.nomosiki.compat.melodies.NomosikImmersiveMelodiesCompat;
import com.nomosiki.compat.ship.ZkShipCompat;
import com.nomosiki.entity.ai.NomosikFollowOwnerGoal;
import com.nomosiki.entity.ai.NomosikGuardGoal;
import com.nomosiki.entity.ai.NomosikHurtByTargetGoal;
import com.nomosiki.entity.ai.NomosikMeleeAttackGoal;
import com.nomosiki.entity.ai.NomosikMoveToBlockGoal;
import com.nomosiki.entity.ai.NomosikSeekAttackZoneGoal;
import com.nomosiki.entity.ai.NomosikSeekFormationGoal;
import com.nomosiki.entity.ai.NomosikShieldBlockGoal;
import com.nomosiki.entity.ai.NomosikSquadIdleStrollGoal;
import com.nomosiki.registry.ModScreenHandlers;
import com.nomosiki.screen.NomosikHireMenuProvider;
import com.nomosiki.screen.NomosikMenuData;
import com.nomosiki.screen.NomosikScreenHandler;
import com.nomosiki.faction.FactionColor;
import com.nomosiki.squad.SquadFormationLayout;
import com.nomosiki.squad.SquadService;
import com.nomosiki.squad.SquadType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class NomosikEntity extends PathfinderMob implements ExtendedScreenHandlerFactory<NomosikMenuData> {
	public static final int SLOT_COUNT = 6;
	public static final double SQUAD_FORMATION_SPACING_MIN = 0.75;
	public static final double SQUAD_FORMATION_SPACING_MAX = 4.0;
	public static final int SQUAD_FORMATION_MAX_IN_ROW_MIN = 1;
	public static final int SQUAD_FORMATION_MAX_IN_ROW_MAX = 128;
	public static final String NBT_RACE = "NomosikRace";
	public static final String NBT_HUMAN_SKIN = "NomosikHumanSkin";
	public static final String NBT_FACTION_COLOR = "NomosikFactionColor";
	public static final int HUMAN_SKIN_VARIANT_COUNT = 1;
	public static final double SQUAD_FORMATION_SLOT_TOL_SQR = 6.25;
	public static final double DEFAULT_MOVEMENT_SPEED = 0.28;
	public static final double DEFAULT_STEP_HEIGHT = 0.6;
	/** Revenge and owner-damage alert radius, in blocks. */
	public static final double REVENGE_RANGE = 64.0;
	public static final double REVENGE_RANGE_SQR = REVENGE_RANGE * REVENGE_RANGE;
	/** Ticks a hurt attacker stays sticky against formation hold and squad retarget. */
	public static final int RECENT_ATTACKER_TICKS = 200;

	static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	static final EntityDataAccessor<Boolean> DATA_SHOULD_FOLLOW =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BOOLEAN);
	static final EntityDataAccessor<Byte> DATA_SQUAD_ORDER_SYNC =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BYTE);
	static final EntityDataAccessor<Integer> DATA_SQUAD_ZONE_RADIUS_SYNC =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.INT);
	static final EntityDataAccessor<Byte> DATA_COMBAT_MODE =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BYTE);
	static final EntityDataAccessor<Byte> DATA_RANGED_FIRE_MODE =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BYTE);
	static final EntityDataAccessor<Byte> DATA_SQUAD_FORMATION =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BYTE);
	static final EntityDataAccessor<Float> DATA_SQUAD_FORMATION_SPACING =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.FLOAT);
	static final EntityDataAccessor<Integer> DATA_SQUAD_FORMATION_MAX_IN_ROW =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.INT);
	static final EntityDataAccessor<Optional<UUID>> DATA_SQUAD_ID =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	static final EntityDataAccessor<Byte> DATA_NOMOSIK_RACE =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BYTE);
	static final EntityDataAccessor<Integer> DATA_HUMAN_SKIN_VARIANT =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.INT);
	static final EntityDataAccessor<Byte> DATA_ATTACK_SWING_SEQ =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BYTE);
	static final EntityDataAccessor<Boolean> DATA_SHIELD_STANCE =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.BOOLEAN);
	static final EntityDataAccessor<String> DATA_FACTION_COLOR_ID =
		SynchedEntityData.defineId(NomosikEntity.class, EntityDataSerializers.STRING);

	final SimpleContainer equipmentInventory = new SimpleContainer(SLOT_COUNT);
	boolean syncingVanillaEquipment;

	NomosikCombatMode combatMode = NomosikCombatMode.NEUTRAL;
	NomosikRangedFireMode rangedFireMode = NomosikRangedFireMode.FIRE_AT_WILL;
	boolean shieldStance;
	int shieldBlockCooldown;
	int rangedShotCooldown;
	@Nullable
	AABB rangedFireZone;
	@Nullable
	BlockPos rangedFireAnchor;
	int rangedFireRadius;

	SquadOrder squadOrder = SquadOrder.FOLLOW;
	@Nullable
	BlockPos squadAnchorPos;
	int squadZoneRadius = 8;
	@Nullable
	AABB squadAttackZone;
	@Nullable
	UUID squadAttackTargetId;
	boolean squadAttackTargetIsPlayer;

	@Nullable
	UUID squadId;
	@Nullable
	Vec3 squadFormationTarget;

	SquadFormation squadFormation = SquadFormation.LINE;
	double squadFormationSpacing = SquadFormationLayout.DEFAULT_SPACING;
	int squadFormationMaxInRow = SquadFormationLayout.DEFAULT_MAX_IN_ROW;
	int squadFormationSlot = -1;
	boolean squadInFormation;
	boolean squadHoldFormation;
	float squadFormationFacingYaw;
	int squadFormationRotateTicks;
	final Set<Integer> rejectedFormationSlots = new HashSet<>();
	long formationRejectPauseUntil;
	boolean formationSlotRepick;
	boolean formationProbeValid;
	@Nullable
	BlockPos formationProbeAnchor;
	int formationProbeSlot = -1;
	@Nullable
	Vec3 formationProbeGeometric;
	@Nullable
	Vec3 formationProbeReachable;

	volatile boolean autoTargetSearchInFlight;
	int autoTargetGeneration;
	boolean guiPreview;
	@Nullable
	FactionColor guiPreviewFactionColor;
	boolean wasPassenger;
	/** Applies a squad transfer that happened while this fighter's chunk was unloaded. */
	private boolean ownerReconciled;

	public NomosikEntity(EntityType<? extends NomosikEntity> type, Level level) {
		super(type, level);
		allowWoodDoors(super.getNavigation());
		NomosikEquipmentLogic.registerEquipmentListener(this);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
			.add(Attributes.STEP_HEIGHT, DEFAULT_STEP_HEIGHT)
			.add(Attributes.ATTACK_DAMAGE, 1.0)
			.add(Attributes.ATTACK_SPEED, 4.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER_UUID, Optional.empty());
		builder.define(DATA_SHOULD_FOLLOW, true);
		builder.define(DATA_SQUAD_ORDER_SYNC, SquadOrder.FOLLOW.id());
		builder.define(DATA_SQUAD_ZONE_RADIUS_SYNC, 8);
		builder.define(DATA_COMBAT_MODE, NomosikCombatMode.NEUTRAL.id());
		builder.define(DATA_RANGED_FIRE_MODE, NomosikRangedFireMode.FIRE_AT_WILL.id());
		builder.define(DATA_SQUAD_FORMATION, SquadFormation.LINE.id());
		builder.define(DATA_SQUAD_FORMATION_SPACING, (float) SquadFormationLayout.DEFAULT_SPACING);
		builder.define(DATA_SQUAD_FORMATION_MAX_IN_ROW, SquadFormationLayout.DEFAULT_MAX_IN_ROW);
		builder.define(DATA_SQUAD_ID, Optional.empty());
		builder.define(DATA_NOMOSIK_RACE, NomosikRace.HUMAN.id());
		builder.define(DATA_HUMAN_SKIN_VARIANT, -1);
		builder.define(DATA_ATTACK_SWING_SEQ, (byte) 0);
		builder.define(DATA_SHIELD_STANCE, false);
		builder.define(DATA_FACTION_COLOR_ID, "");
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(this.standingHeightScale());
	}

	public float standingHeightScale() {
		return this.getNomosikRace().standingHeightScale();
	}

	public boolean isCavalryMounted() {
		return this.getVehicle() instanceof net.minecraft.world.entity.animal.horse.AbstractHorse;
	}

	/** Sitting on a cushion or, for an artillerist, crewing a gun. Horse riders still move their mount. */
	public boolean isRidingHeldSeat() {
		return ZkShipCompat.isSeated(this) || (this.isArtillerist() && this.isPassenger());
	}

	@Override
	public PathNavigation getNavigation() {
		Entity vehicle = this.getVehicle();
		if (vehicle instanceof Mob mount && NomosikiHorseMount.isNomosikiMountEntity(mount)) {
			PathNavigation navigation = mount.getNavigation();
			allowWoodDoors(navigation);
			return navigation;
		}
		return super.getNavigation();
	}

	private static void allowWoodDoors(PathNavigation navigation) {
		NodeEvaluator nodes = navigation.getNodeEvaluator();
		nodes.setCanOpenDoors(true);
		nodes.setCanPassDoors(true);
	}

	/**
	 * Ladders, scaffolding, and trapdoors used as ladders still climb.
	 * Other {@code #climbable} blocks (vines) do not, so a swamp trunk is not a ladder.
	 */
	@Override
	public boolean onClimbable() {
		if (this.isSpectator()) {
			return false;
		}
		BlockState state = this.getInBlockState();
		if (state.is(BlockTags.CLIMBABLE) && !state.is(Blocks.LADDER) && !state.is(Blocks.SCAFFOLDING)) {
			return false;
		}
		return super.onClimbable();
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (!this.level().isClientSide()) {
			return;
		}
		if (DATA_NOMOSIK_RACE.equals(key)) {
			this.refreshDimensions();
		}
		if (DATA_ATTACK_SWING_SEQ.equals(key)) {
			this.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSwingTime();
		boolean passengerNow = this.isPassenger();
		if (passengerNow != this.wasPassenger) {
			this.wasPassenger = passengerNow;
			this.refreshDimensions();
		}
		if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!this.ownerReconciled) {
			this.ownerReconciled = true;
			SquadService.get(serverLevel.getServer()).reconcileLoadedOwner(this);
		}
		NomosikAppearanceLogic.tickServer(this);
		NomosikiHorseMount.ensureMountMatchesRole(this, serverLevel);
		if (this.getOwnerUUID() == null) {
			return;
		}
		LivingEntity current = this.getTarget();
		if (current != null && NomosikAutoTarget.shouldClearTarget(current)) {
			this.setTarget(null);
		}
		NomosikAutoTarget.tickSearch(this, serverLevel);
		NomosikSquadLogic.tickFormationFacing(this);
		NomosikShieldLogic.tickCooldown(this);
		if (this.rangedShotCooldown > 0) {
			this.rangedShotCooldown--;
		}
	}

	@Override
	protected float tickHeadTurn(float yRot, float animStep) {
		if (StrategicFireController.isActive(this)) {
			return 0.0F;
		}
		if (NomosikSquadLogic.shouldLockFormationBodyFacing(this)) {
			this.yBodyRot = this.squadFormationFacingYaw;
			if (this.squadFormationRotateTicks > 0 || this.getTarget() == null) {
				this.yHeadRot = this.squadFormationFacingYaw;
			}
			if (this.squadFormationRotateTicks > 0) {
				this.setYRot(this.squadFormationFacingYaw);
				return 0.0F;
			}
		}
		return super.tickHeadTurn(yRot, animStep);
	}

	@Override
	public void swing(InteractionHand hand, boolean updateSelf) {
		if (this.level() instanceof ServerLevel serverLevel) {
			NomosikMainThread.run(serverLevel, () -> this.swingOnServer(hand, updateSelf));
			return;
		}
		this.swingOnServer(hand, updateSelf);
	}

	@Override
	public void startUsingItem(InteractionHand hand) {
		if (this.level() instanceof ServerLevel serverLevel) {
			NomosikMainThread.run(serverLevel, () -> super.startUsingItem(hand));
			return;
		}
		super.startUsingItem(hand);
	}

	@Override
	public void stopUsingItem() {
		if (this.level() instanceof ServerLevel serverLevel) {
			NomosikMainThread.run(serverLevel, () -> super.stopUsingItem());
			return;
		}
		super.stopUsingItem();
	}

	private void swingOnServer(InteractionHand hand, boolean updateSelf) {
		super.swing(hand, updateSelf);
		if (!this.level().isClientSide()) {
			byte seq = this.entityData.get(DATA_ATTACK_SWING_SEQ);
			this.entityData.set(DATA_ATTACK_SWING_SEQ, (byte) (seq + 1));
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new NomosikMeleeAttackGoal(this, 1.05, true));
		this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
		this.goalSelector.addGoal(2, new NomosikShieldBlockGoal(this));
		this.goalSelector.addGoal(3, new NomosikSeekFormationGoal(this));
		this.goalSelector.addGoal(4, new NomosikMoveToBlockGoal(this));
		this.goalSelector.addGoal(5, new NomosikSeekAttackZoneGoal(this));
		this.goalSelector.addGoal(6, new NomosikGuardGoal(this));
		this.goalSelector.addGoal(7, new NomosikFollowOwnerGoal(this, 1.05F, 10.0F, 2.5F));
		this.goalSelector.addGoal(8, new NomosikSquadIdleStrollGoal(this, 0.9));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NomosikHurtByTargetGoal(this));
	}

	@Nullable
	public UUID getOwnerUUID() {
		return this.entityData.get(DATA_OWNER_UUID).orElse(null);
	}

	public void setOwnerUUID(@Nullable UUID uuid) {
		if (Objects.equals(this.getOwnerUUID(), uuid)) {
			return;
		}
		this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
		if (!this.level().isClientSide()) {
			FactionArmorCompat.refreshWorn(this);
		}
	}

	public boolean shouldFollowOwner() {
		return this.entityData.get(DATA_SHOULD_FOLLOW) && this.getOwnerUUID() != null;
	}

	public void setShouldFollowOwner(boolean value) {
		this.entityData.set(DATA_SHOULD_FOLLOW, value);
	}

	@Nullable
	public Player getOwner() {
		UUID id = getOwnerUUID();
		if (id == null) {
			return null;
		}
		if (this.level().getPlayerByUUID(id) instanceof Player p) {
			return p;
		}
		return null;
	}

	public boolean isOwnedBy(Player player) {
		return player.getUUID().equals(getOwnerUUID());
	}

	public SquadType getSquadBranchType() {
		return SquadType.INFANTRY;
	}

	public NomosikRace getNomosikRace() { return NomosikAppearanceLogic.getNomosikRace(this); }
	public int getHumanSkinVariant() { return NomosikAppearanceLogic.getHumanSkinVariant(this); }
	public void setHumanSkinVariant(int index) { NomosikAppearanceLogic.setHumanSkinVariant(this, index); }
	public void configureGuiPreview(NomosikRace race, int humanSkinVariant) { NomosikAppearanceLogic.configureGuiPreview(this, race, humanSkinVariant, null); }
	public void configureGuiPreview(NomosikRace race, int humanSkinVariant, @Nullable FactionColor factionColor) {
		NomosikAppearanceLogic.configureGuiPreview(this, race, humanSkinVariant, factionColor);
	}
	@Nullable
	public FactionColor getGuiPreviewFactionColor() {
		return this.guiPreviewFactionColor;
	}
	public void setGuiPreviewFactionColor(@Nullable FactionColor factionColor) {
		this.guiPreviewFactionColor = factionColor;
	}
	@Nullable
	public FactionColor getFactionColorOverride() {
		String id = this.entityData.get(DATA_FACTION_COLOR_ID);
		if (id == null || id.isBlank()) {
			return null;
		}
		return FactionColor.fromId(id);
	}
	public void setFactionColorOverride(@Nullable FactionColor factionColor) {
		this.entityData.set(DATA_FACTION_COLOR_ID, factionColor == null ? "" : factionColor.id());
	}
	public void setNomosikRace(NomosikRace race) { NomosikAppearanceLogic.setNomosikRace(this, race); }

	public boolean isAtFormationSlot() { return NomosikSquadLogic.isAtFormationSlot(this); }
	public void beginFormationFacing(int ticks) { NomosikSquadLogic.beginFormationFacing(this, ticks); }
	public Optional<UUID> getSquadId() { return NomosikSquadLogic.getSquadId(this); }
	public Optional<UUID> getSquadIdForGui() { return NomosikSquadLogic.getSquadIdForGui(this); }
	public void joinSquadServer(UUID squadUuid) { NomosikSquadLogic.joinSquadServer(this, squadUuid); }
	public void clearSquadIdServer() { NomosikSquadLogic.clearSquadIdServer(this); }
	public boolean isSquadInFormation() { return NomosikSquadLogic.isSquadInFormation(this); }
	public boolean isSquadHoldFormation() { return NomosikSquadLogic.isSquadHoldFormation(this); }
	public float getSquadFormationFacingYaw() { return NomosikSquadLogic.getSquadFormationFacingYaw(this); }
	public void setSquadFormationFlagsServer(boolean inFormation, boolean holdFormation) { NomosikSquadLogic.setSquadFormationFlagsServer(this, inFormation, holdFormation); }
	public void setSquadFormationFacingServer(float yawDegrees) { NomosikSquadLogic.setSquadFormationFacingServer(this, yawDegrees); }
	public void clearSquadFormationAssignmentServer() { NomosikSquadLogic.clearSquadFormationAssignmentServer(this); }
	public @Nullable Vec3 getSquadFormationTarget() { return NomosikSquadLogic.getSquadFormationTarget(this); }
	public void setSquadFormationTargetServer(@Nullable Vec3 target) { NomosikSquadLogic.setSquadFormationTargetServer(this, target); }
	public SquadFormation getSquadFormation() { return NomosikSquadLogic.getSquadFormation(this); }
	public SquadFormation getSquadFormationForGui() { return NomosikSquadLogic.getSquadFormationForGui(this); }
	public void setSquadFormationServer(SquadFormation formation) { NomosikSquadLogic.setSquadFormationServer(this, formation); }
	public double getSquadFormationSpacing() { return NomosikSquadLogic.getSquadFormationSpacing(this); }
	public double getSquadFormationSpacingForGui() { return this.getSquadFormationSpacing(); }
	public void setSquadFormationSpacingServer(double spacing) { NomosikSquadLogic.setSquadFormationSpacingServer(this, spacing); }
	public int getSquadFormationMaxInRow() { return NomosikSquadLogic.getSquadFormationMaxInRow(this); }
	public void setSquadFormationMaxInRowServer(int maxInRow) { NomosikSquadLogic.setSquadFormationMaxInRowServer(this, maxInRow); }
	public int getSquadFormationSlot() { return NomosikSquadLogic.getSquadFormationSlot(this); }
	public void setSquadFormationSlotServer(int slot) { NomosikSquadLogic.setSquadFormationSlotServer(this, slot); }

	public void rejectCurrentFormationSlot() {
		if (this.level().isClientSide() || this.level().getGameTime() < this.formationRejectPauseUntil) {
			return;
		}
		if (this.squadFormationSlot < 0) {
			return;
		}
		if (this.rejectedFormationSlots.add(this.squadFormationSlot)) {
			this.formationSlotRepick = true;
			this.formationProbeValid = false;
		}
	}

	public boolean isFormationSlotRejected(int slot) {
		return this.rejectedFormationSlots.contains(slot);
	}

	public int rejectedFormationSlotCount() {
		return this.rejectedFormationSlots.size();
	}

	public void clearFormationRouteState() {
		this.rejectedFormationSlots.clear();
		this.formationRejectPauseUntil = 0L;
		this.formationSlotRepick = false;
		this.formationProbeValid = false;
		this.formationProbeAnchor = null;
		this.formationProbeSlot = -1;
		this.formationProbeGeometric = null;
		this.formationProbeReachable = null;
	}

	public boolean isFormationRejectPaused(long gameTime) {
		return gameTime < this.formationRejectPauseUntil;
	}

	public void pauseFormationRejects(long until) {
		this.formationRejectPauseUntil = until;
		this.rejectedFormationSlots.clear();
		this.formationSlotRepick = true;
		this.formationProbeValid = false;
	}

	public boolean consumeFormationSlotRepick() {
		boolean repick = this.formationSlotRepick;
		this.formationSlotRepick = false;
		return repick;
	}

	/** @return true when the layout anchor changed and route memory was cleared */
	public boolean noteFormationAnchor(BlockPos anchor) {
		if (anchor.equals(this.formationProbeAnchor)) {
			return false;
		}
		this.clearFormationRouteState();
		this.formationProbeAnchor = anchor.immutable();
		return true;
	}

	public @Nullable Vec3 cachedReachableSlot(BlockPos anchor, int slot, Vec3 geometric) {
		if (!this.formationProbeValid || this.formationProbeReachable == null || this.formationProbeGeometric == null) {
			return null;
		}
		if (slot != this.formationProbeSlot || !anchor.equals(this.formationProbeAnchor)) {
			return null;
		}
		if (this.formationProbeGeometric.distanceToSqr(geometric) > 0.01) {
			return null;
		}
		return this.formationProbeReachable;
	}

	public void cacheReachableSlot(BlockPos anchor, int slot, Vec3 geometric, Vec3 reachable) {
		this.formationProbeAnchor = anchor.immutable();
		this.formationProbeSlot = slot;
		this.formationProbeGeometric = geometric;
		this.formationProbeReachable = reachable;
		this.formationProbeValid = true;
	}

	public SquadOrder getSquadOrder() { return NomosikSquadLogic.getSquadOrder(this); }
	public SquadOrder getSquadOrderForGui() { return NomosikSquadLogic.getSquadOrderForGui(this); }
	public int getSquadZoneRadiusForGui() { return NomosikSquadLogic.getSquadZoneRadiusForGui(this); }
	public NomosikCombatMode getCombatMode() { return NomosikSquadLogic.getCombatMode(this); }
	public void setCombatMode(NomosikCombatMode mode) { NomosikSquadLogic.setCombatMode(this, mode); }

	public boolean isRangedUnit() { return this instanceof ArcherEntity; }
	public boolean isArtillerist() { return false; }
	public boolean isCaptain() { return false; }
	public boolean wantsHelm() { return false; }
	public void setWantsHelm(boolean wants) {}
	public boolean canUseRangedFireOrders() { return this.isRangedUnit() || this.isArtillerist(); }
	public SimpleContainer getAmmoInventory() { return null; }
	public boolean isShieldStance() { return NomosikShieldLogic.isShieldStance(this); }
	public void setShieldStance(boolean up) { NomosikShieldLogic.setShieldStance(this, up); }
	public boolean canUseShieldStance() { return NomosikShieldLogic.canUseShieldStance(this); }
	public boolean shouldBlockWithShieldNow() { return NomosikShieldLogic.shouldBlockWithShieldNow(this); }
	public int getShieldBlockCooldown() { return NomosikShieldLogic.getShieldBlockCooldown(this); }
	public void setShieldBlockCooldown(int ticks) { NomosikShieldLogic.setShieldBlockCooldown(this, ticks); }
	public void disableShieldStance(int ticks) { NomosikShieldLogic.disableShieldStance(this, ticks); }
	public int getRangedShotCooldown() { return this.rangedShotCooldown; }
	public void setRangedShotCooldown(int ticks) { this.rangedShotCooldown = Math.max(0, ticks); }

	@Override
	public void hurtCurrentlyUsedShield(float amount) { NomosikShieldLogic.hurtCurrentlyUsedShield(this, amount); }

	@Override
	protected void blockUsingShield(LivingEntity attacker) {
		super.blockUsingShield(attacker);
		NomosikShieldLogic.blockUsingShield(this, attacker);
	}

	public NomosikRangedFireMode getRangedFireMode() { return NomosikSquadLogic.getRangedFireMode(this); }
	public NomosikRangedFireMode getRangedFireModeForGui() { return this.getRangedFireMode(); }
	public void setRangedFireMode(NomosikRangedFireMode mode) { NomosikSquadLogic.setRangedFireMode(this, mode); }
	public void setRangedFireZone(BlockPos anchor, int zoneRadius) { NomosikSquadLogic.setRangedFireZone(this, anchor, zoneRadius); }
	@Nullable
	public BlockPos getRangedFireAnchor() { return this.rangedFireAnchor; }
	public int getRangedFireRadius() { return this.rangedFireRadius; }
	@Nullable
	public AABB getRangedFireZone() { return NomosikSquadLogic.getRangedFireZone(this); }
	@Nullable
	public AABB getEffectiveRangedFireZone() { return NomosikSquadLogic.getEffectiveRangedFireZone(this); }
	public boolean isTargetInRangedFireZone(LivingEntity target) { return NomosikSquadLogic.isTargetInRangedFireZone(this, target); }

	public static void alertNomosikiOfOwnerDamage(ServerLevel level, ServerPlayer owner, LivingEntity attacker) {
		NomosikAutoTarget.alertNomosikiOfOwnerDamage(level, owner, attacker);
	}

	public boolean isAllyOrSelf(LivingEntity other) { return NomosikCombatLogic.isAllyOrSelf(this, other); }

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (NomosikCombatLogic.isAllyDamage(this, source)) {
			return false;
		}
		boolean hurt = super.hurt(source, amount);
		if (hurt && !this.level().isClientSide) {
			com.nomosiki.compat.emote.server.NomosikSquadEmoteService.interruptIfConfiguredOnDamage(this);
		}
		return hurt;
	}

	public boolean canApplyAutoTarget(LivingEntity candidate) { return NomosikAutoTarget.canApplyAutoTarget(this, candidate); }

	/** Attacker who damaged this unit within {@link #RECENT_ATTACKER_TICKS}. */
	public boolean isRecentAttacker(@Nullable LivingEntity candidate) {
		if (candidate == null || candidate != this.getLastHurtByMob() || !candidate.isAlive() || candidate.isRemoved()) {
			return false;
		}
		return this.tickCount - this.getLastHurtByMobTimestamp() <= RECENT_ATTACKER_TICKS;
	}
	@Nullable
	public BlockPos getSquadAnchorPos() { return NomosikSquadLogic.getSquadAnchorPos(this); }
	@Nullable
	public AABB getSquadAttackZone() { return NomosikSquadLogic.getSquadAttackZone(this); }
	public void applySquadOrder(SquadOrder order, @Nullable BlockPos anchor, int zoneRadius) {
		NomosikSquadLogic.applySquadOrder(this, order, anchor, zoneRadius);
	}

	public Optional<UUID> getSquadAttackTargetId() {
		return Optional.ofNullable(this.squadAttackTargetId);
	}

	public boolean isSquadAttackTargetPlayer() {
		return this.squadAttackTargetIsPlayer;
	}

	public void applySquadAttackTarget(UUID targetSquadId, BlockPos anchor, int zoneRadius) {
		this.applySquadAttackTarget(targetSquadId, anchor, zoneRadius, false);
	}

	public void applySquadAttackTarget(UUID targetId, BlockPos anchor, int zoneRadius, boolean targetIsPlayer) {
		NomosikSquadLogic.applyAttackTargetOrder(this, targetId, anchor, zoneRadius, targetIsPlayer);
	}

	public void refreshAttackTargetZone(BlockPos anchor) {
		NomosikSquadLogic.refreshAttackTargetZone(this, anchor);
	}

	public void convertAttackTargetToZone() {
		NomosikSquadLogic.convertAttackTargetToZone(this);
	}

	@Override
	public ItemStack getItemBySlot(EquipmentSlot slot) { return NomosikEquipmentLogic.getItemBySlot(this, slot); }

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) { NomosikEquipmentLogic.setItemSlot(this, slot, stack); }

	protected void validateAndSetSlot(EquipmentSlot slot, ItemStack stack) { NomosikEquipmentLogic.validateAndSetSlot(this, slot, stack); }

	public SimpleContainer getEquipmentInventory() { return this.equipmentInventory; }

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		NomosikEntityNbt.write(this, tag);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		NomosikEntityNbt.read(this, tag);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource damageSource, boolean recentlyHit) {
		NomosikEquipmentLogic.dropCustomDeathLoot(this, serverLevel, damageSource, recentlyHit);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if (!this.level().isClientSide() && reason.shouldDestroy()) {
			NomosikiHorseMount.discardMountOf(this);
		}
		super.remove(reason);
	}

	/**
	 * {@code Mob.interact} is final in 1.21.1, so Immersive Melodies' drop-on-click mixin
	 * cannot be skipped by an override. {@link com.nomosiki.mixin.NomosikInstrumentInteractMixin}
	 * routes here instead of {@code super.interact}.
	 */
	public InteractionResult interactKeepingInstrument(Player player, InteractionHand hand) {
		return this.mobInteract(player, hand);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && isOwnedBy(player)) {
			if (this.level().isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer sp) {
				sp.openMenu(this);
			}
			return InteractionResult.CONSUME;
		}
		if (held.isEmpty() && !player.isShiftKeyDown()) {
			if (this.level().isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (getOwnerUUID() == null) {
				if (player instanceof ServerPlayer sp) {
					sp.openMenu(new NomosikHireMenuProvider(this));
				}
				return InteractionResult.CONSUME;
			}
			if (isOwnedBy(player)) {
				setShouldFollowOwner(!this.entityData.get(DATA_SHOULD_FOLLOW));
				if (player instanceof ServerPlayer owner) {
					NomosikiAdvancements.grant(owner, NomosikiActions.FOLLOW);
				}
				boolean on = this.entityData.get(DATA_SHOULD_FOLLOW);
				player.displayClientMessage(
					on ? Component.translatable("message.nomosiki.follow_on") : Component.translatable("message.nomosiki.follow_off"),
					true);
				return InteractionResult.CONSUME;
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public Component getDisplayName() {
		Component custom = this.getCustomName();
		if (custom != null) {
			return custom;
		}
		return Component.translatable(this.getType().getDescriptionId());
	}

	@Override
	public NomosikMenuData getScreenOpeningData(ServerPlayer player) {
		return new NomosikMenuData(this.getId());
	}

	@Override
	public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
		return new NomosikScreenHandler(ModScreenHandlers.NOMOSIK, syncId, inventory, this);
	}

	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
		this.setPersistenceRequired();
		this.setCanPickUpLoot(true);
		if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		}
		if (this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
			this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
		}
		this.entityData.set(DATA_HUMAN_SKIN_VARIANT, this.getRandom().nextInt(HUMAN_SKIN_VARIANT_COUNT));
		return data;
	}

	@Override
	public boolean wantsToPickUp(ItemStack stack) {
		if (NomosikImmersiveMelodiesCompat.isInstrument(stack)
			&& (!this.getMainHandItem().isEmpty() || !this.getOffhandItem().isEmpty())) {
			return false;
		}
		return super.wantsToPickUp(stack);
	}

	@Override
	protected boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing) {
		if (NomosikImmersiveMelodiesCompat.isInstrument(candidate) && !existing.isEmpty()) {
			return false;
		}
		return super.canReplaceCurrentItem(candidate, existing);
	}

	@Override
	protected void pickUpItem(ItemEntity itemEntity) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			this.setDropChance(slot, 2.0F);
		}
		try {
			super.pickUpItem(itemEntity);
		} finally {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				this.setDropChance(slot, 0.0F);
			}
		}
	}

	ItemStack superGetItemBySlot(EquipmentSlot slot) {
		return super.getItemBySlot(slot);
	}

	void superSetItemSlot(EquipmentSlot slot, ItemStack stack) {
		super.setItemSlot(slot, stack);
	}

	void superDropCustomDeathLoot(ServerLevel serverLevel, DamageSource damageSource, boolean recentlyHit) {
		super.dropCustomDeathLoot(serverLevel, damageSource, recentlyHit);
	}

	/** Package helper: {@link LivingEntity#useItem} is protected. */
	void clearUseItemField() {
		this.useItem = ItemStack.EMPTY;
	}
}
