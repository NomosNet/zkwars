package com.example.entity;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.work.BuilderTasks;
import com.example.block.work.SmeltWork;
import com.example.block.WorkerBeds;
import com.example.block.WorkerBlock;
import com.example.block.WorkerKind;
import com.example.compat.WartimeGate;
import com.example.entity.ai.StandCells;
import com.example.entity.ai.RouteWalker;
import com.example.entity.ai.WorkerPassageGoal;
import com.example.entity.ai.WorkerRoutineGoal;
import com.example.item.CourierScrollItem;
import com.example.item.HammerItem;
import com.example.item.ScrollData;
import com.example.item.SmelterScrollItem;
import com.example.item.WorkerScrollItem;
import com.example.registry.ModEntities;
import com.example.registry.ModItems;
import com.example.block.entity.WorkerBlockEntity;
import com.example.inventory.WorkerInspectData;
import com.example.inventory.WorkerInspectMenu;
import com.example.inventory.WorkerSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HumanWorkerEntity extends PathfinderMob implements ExtendedScreenHandlerFactory<WorkerInspectData> {
	public static final int CARGO_LIMIT = CourierLoad.LIMIT;
	public static final int SMELT_CARRY_LIMIT = 2;
	private static final String SMELT_CARRY = "SmeltCarry";
	private static final String SMELT_KIND = "Kind";
	private static final String SMELT_ITEM = "Item";
	private static final EntityDataAccessor<String> DATA_FACTION_COLOR = SynchedEntityData.defineId(HumanWorkerEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> DATA_FEMALE = SynchedEntityData.defineId(HumanWorkerEntity.class, EntityDataSerializers.BOOLEAN);
	private static final double ARRIVE_DISTANCE_SQR = 2.25;
	private static final double BED_REACH_SQR = 6.25;

	private @Nullable BlockPos workBlockPos;
	private @Nullable BlockPos bedPos;
	private @Nullable BlockPos chestPos;
	private final List<BlockPos> route = new ArrayList<>();
	private final List<BlockPos> furnaces = new ArrayList<>();
	private final List<CarryPiece> smeltCarry = new ArrayList<>();
	private int furnaceIndex;
	private int smeltPhase;
	private @Nullable BlockPos smeltTarget;
	private final CourierLoad cargo = new CourierLoad();
	private boolean courierSupply;
	private boolean courierSort;
	private boolean returningCargo;
	private boolean cargoStuck;
	private int routeIndex;
	private int rejectMark = -1;
	private final RouteWalker routeWalker = new RouteWalker();
	private @Nullable BlockPos standTarget;
	private @Nullable BlockPos standCell;
	private @Nullable BlockPos rejectedStand;
	private boolean standDirty = true;
	private boolean cantReach;
	private double workLookX;
	private double workLookY;
	private double workLookZ;
	private int workLookUntil;
	private @Nullable UUID assignerId;

	public HumanWorkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		NodeEvaluator nodes = this.getNavigation().getNodeEvaluator();
		nodes.setCanOpenDoors(true);
		nodes.setCanPassDoors(true);
		if (type == ModEntities.HUMAN_SMELTER) {
			this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FACTION_COLOR, "");
		builder.define(DATA_FEMALE, false);
	}

	public String factionColorId() {
		String id = this.entityData.get(DATA_FACTION_COLOR);
		return id == null ? "" : id;
	}

	public void setFactionColorId(String colorId) {
		this.entityData.set(DATA_FACTION_COLOR, colorId == null ? "" : colorId);
	}

	public boolean isFemale() {
		return this.entityData.get(DATA_FEMALE);
	}

	public void setFemale(boolean female) {
		this.entityData.set(DATA_FEMALE, female);
	}

	public @Nullable UUID assignerId() {
		return this.assignerId;
	}

	public void rememberAssigner(Player player) {
		this.assignerId = player.getUUID();
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
		this.setFemale(this.getRandom().nextBoolean());
		return data;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new WorkerPassageGoal(this));
		this.goalSelector.addGoal(2, new WorkerRoutineGoal(this));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
			if (this.isSleeping()) {
				this.wakeFromBed();
			}
			if (this.isSmelter()) {
				this.pushFurnaces(this.workBlockPos, List.of(), true);
			}
		}
		super.remove(reason);
	}

	public boolean isCourier() {
		return this.getType() == ModEntities.HUMAN_COURIER;
	}

	public boolean isSmelter() {
		return this.getType() == ModEntities.HUMAN_SMELTER;
	}

	public @Nullable WorkerKind profession() {
		EntityType<?> type = this.getType();
		if (type == ModEntities.HUMAN_MINER) {
			return WorkerKind.QUARRY;
		}
		if (type == ModEntities.HUMAN_FARMER) {
			return WorkerKind.FARMER;
		}
		if (type == ModEntities.HUMAN_LUMBERJACK) {
			return WorkerKind.LUMBERJACK;
		}
		if (type == ModEntities.HUMAN_CHICKEN_BREEDER) {
			return WorkerKind.CHICKEN;
		}
		if (type == ModEntities.HUMAN_COW_BREEDER) {
			return WorkerKind.COW;
		}
		if (type == ModEntities.HUMAN_SHEEP_BREEDER) {
			return WorkerKind.SHEEP;
		}
		if (type == ModEntities.HUMAN_PIG_BREEDER) {
			return WorkerKind.PIG;
		}
		if (type == ModEntities.HUMAN_BUILDER) {
			return WorkerKind.BUILDER;
		}
		if (type == ModEntities.HUMAN_SMELTER) {
			return WorkerKind.SMELTER;
		}
		if (type == ModEntities.HUMAN_FISHER) {
			return WorkerKind.FISHER;
		}
		if (type == ModEntities.HUMAN_CRAFTER) {
			return WorkerKind.CRAFTER;
		}
		return null;
	}

	public @Nullable BlockPos getWorkBlockPos() {
		return this.workBlockPos;
	}

	public @Nullable BlockPos getBedPos() {
		return this.bedPos;
	}

	public static @org.jetbrains.annotations.Nullable HumanWorkerEntity sleepingAt(net.minecraft.world.level.Level level, BlockPos pos) {
		BlockPos head = WorkerBeds.head(level, pos);
		if (head == null) {
			head = pos;
		}
		BlockPos foot = WorkerBeds.otherHalf(level, head);
		AABB area = new AABB(head).inflate(1.5);
		if (foot != null) {
			area = area.minmax(new AABB(foot)).inflate(1.5);
		}
		for (HumanWorkerEntity worker : level.getEntitiesOfClass(HumanWorkerEntity.class, area)) {
			if (!worker.isSleeping()) {
				continue;
			}
			BlockPos sleeping = worker.getSleepingPos().orElse(worker.getBedPos());
			if (sleeping != null && (sleeping.equals(head) || sleeping.equals(pos) || (foot != null && sleeping.equals(foot)))) {
				return worker;
			}
		}
		return null;
	}

	public boolean hasAssignment() {
		if (this.isCourier()) {
			return this.chestPos != null || !this.route.isEmpty() || this.bedPos != null;
		}
		return this.workBlockPos != null || this.bedPos != null;
	}

	public boolean opensPassages() {
		return !this.isCourier() || (this.chestPos != null && !this.route.isEmpty());
	}

	public boolean followsSchedule() {
		if (this.isCourier()) {
			return this.chestPos != null && !this.route.isEmpty();
		}
		return this.hasAssignment();
	}

	public @Nullable BlockPos chestPos() {
		return this.chestPos;
	}

	public List<BlockPos> route() {
		return this.route;
	}

	public boolean courierSupply() {
		return this.courierSupply;
	}

	public boolean courierSort() {
		return this.courierSort;
	}

	public boolean returningCargo() {
		return this.returningCargo;
	}

	public void setReturningCargo(boolean returningCargo) {
		this.returningCargo = returningCargo;
	}

	public int routeIndex() {
		return this.routeIndex;
	}

	public int rejectMark() {
		return this.rejectMark;
	}

	public void setRejectMark(int rejectMark) {
		this.rejectMark = rejectMark;
	}

	public void advanceRoute() {
		if (!this.route.isEmpty()) {
			this.routeIndex = Math.floorMod(this.routeIndex + 1, this.route.size());
		}
	}

	public boolean hasCargo() {
		return !this.cargo.isEmpty();
	}

	public boolean hasCargoRoom() {
		return this.cargo.hasRoom();
	}

	public ItemStack cargoStack() {
		return this.cargo.head();
	}

	public @Nullable BlockPos cargoSource() {
		return this.cargo.source();
	}

	public @Nullable BlockPos cargoDest() {
		return this.cargo.dest();
	}

	public boolean cargoStuck() {
		return this.cargoStuck;
	}

	public void setCargoStuck(boolean cargoStuck) {
		this.cargoStuck = cargoStuck;
	}

	public void addCargo(ItemStack stack, @Nullable BlockPos source, @Nullable BlockPos dest) {
		this.cargo.add(stack, source, dest);
		this.syncCargoHand();
	}

	public void replaceCargoHead(ItemStack stack) {
		this.cargo.replaceHead(stack);
		this.syncCargoHand();
	}

	public void removeCargoHead() {
		this.cargo.removeHead();
		this.syncCargoHand();
	}

	public int cargoSize() {
		return this.cargo.size();
	}

	public ItemStack cargoPiece(int index) {
		return this.cargo.piece(index);
	}

	public void setCargoPiece(int index, ItemStack stack) {
		if (index < 0 || index >= CARGO_LIMIT) {
			return;
		}
		if (stack.isEmpty()) {
			this.cargo.removePiece(index);
		} else if (index < this.cargo.size()) {
			this.cargo.setPiece(index, stack);
		} else if (this.cargo.hasRoom()) {
			this.cargo.add(stack, null, null);
		}
		this.syncCargoHand();
	}

	public ItemStack removeCargoPiece(int index) {
		ItemStack removed = this.cargo.removePiece(index);
		this.syncCargoHand();
		return removed;
	}

	public void refreshCargoHand() {
		this.syncCargoHand();
	}

	private void syncCargoHand() {
		if (!this.isCourier()) {
			return;
		}
		ItemStack show = this.cargo.head();
		this.setItemSlot(EquipmentSlot.MAINHAND, show.isEmpty() ? ItemStack.EMPTY : show.copy());
	}

	public boolean approach(BlockPos pos) {
		if (!this.level().isLoaded(pos)) {
			return false;
		}
		if (this.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= 6.25) {
			this.getNavigation().stop();
			this.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			return true;
		}
		this.moveToward(this.standNear(pos));
		return false;
	}

	public boolean hasIntactTool() {
		return this.acceptsTool(this.getMainHandItem()) && hasRemainingDurability(this.getMainHandItem());
	}

	public boolean isStationed() {
		if (this.isCourier() || this.isSleeping() || this.workBlockPos == null || !isWorkDay(this.level()) || WartimeGate.isActive()) {
			return false;
		}
		if (!this.level().isLoaded(this.workBlockPos)) {
			return false;
		}
		WorkerKind kind = this.profession();
		BlockState state = this.level().getBlockState(this.workBlockPos);
		if (kind == null || !(state.getBlock() instanceof WorkerBlock workerBlock) || workerBlock.kind() != kind) {
			return false;
		}
		return this.distanceToSqr(this.workBlockPos.getX() + 0.5, this.workBlockPos.getY(), this.workBlockPos.getZ() + 0.5) <= 9.0;
	}

	public boolean isAtWorkPost() {
		return this.isStationed() && this.hasIntactTool();
	}

	public static int storedUses(WorkerKind kind, ItemStack stack) {
		if (!toolMatches(kind, stack) || !hasRemainingDurability(stack)) {
			return 0;
		}
		if (!stack.isDamageableItem()) {
			return Integer.MAX_VALUE;
		}
		return stack.getMaxDamage() - stack.getDamageValue();
	}

	public static void spendStoredUses(@Nullable HumanWorkerEntity worker, WorkerBlockEntity block, int uses) {
		int left = uses;
		if (worker != null && left > 0) {
			left -= worker.spendHandUses(left);
		}
		WorkerKind kind = block.kind();
		for (int slot = 0; slot < block.getContainerSize() && left > 0; slot++) {
			ItemStack spare = block.getItem(slot);
			int available = storedUses(kind, spare);
			if (available <= 0) {
				continue;
			}
			if (available == Integer.MAX_VALUE) {
				break;
			}
			int spend = Math.min(left, available);
			if (spare.getDamageValue() + spend >= spare.getMaxDamage()) {
				block.setItem(slot, ItemStack.EMPTY);
			} else {
				spare.setDamageValue(spare.getDamageValue() + spend);
				block.setItem(slot, spare);
			}
			left -= spend;
		}
		if (worker != null) {
			worker.equipSpareTool(block);
		}
	}

	public void playWorkSwing(double x, double y, double z) {
		this.swing(InteractionHand.MAIN_HAND);
		this.workLookX = x;
		this.workLookY = y;
		this.workLookZ = z;
		this.workLookUntil = this.tickCount + 12;
		double dx = x - this.getX();
		double dy = y - this.getEyeY();
		double dz = z - this.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float)(Mth.atan2(dz, dx) * (180.0F / (float)Math.PI)) - 90.0F;
		float pitch = (float)(-(Mth.atan2(dy, horizontal) * (180.0F / (float)Math.PI)));
		this.setYRot(yaw);
		this.setXRot(pitch);
		this.setYHeadRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRotO = yaw;
		this.yBodyRotO = yaw;
		this.xRotO = pitch;
		this.getLookControl().setLookAt(x, y, z);
	}

	public void wearEquippedTool(com.example.block.entity.WorkerBlockEntity block) {
		ItemStack tool = this.getMainHandItem();
		if (!tool.isEmpty()) {
			tool.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
		}
		tool = this.getMainHandItem();
		if (!tool.isEmpty() && tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage()) {
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
		if (this.hasIntactTool()) {
			return;
		}
		this.takeSpareTool(block);
	}

	public void refillTool(WorkerBlockEntity block) {
		ItemStack tool = this.getMainHandItem();
		if (!tool.isEmpty() && tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage()) {
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
		if (this.hasIntactTool() || !this.getMainHandItem().isEmpty()) {
			return;
		}
		this.takeSpareTool(block);
	}

	private void takeSpareTool(WorkerBlockEntity block) {
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			ItemStack spare = block.getItem(slot);
			if (this.acceptsTool(spare) && hasRemainingDurability(spare)) {
				this.setItemSlot(EquipmentSlot.MAINHAND, block.removeItem(slot, 1));
				return;
			}
		}
	}

	public void tickSchedule() {
		this.validatePosts();
		if (!this.level().isClientSide && this.tickCount % 20 == 0) {
			this.grantStatusAdvancements();
		}
		if (this.isSmelter() && this.tickCount % 20 == 0) {
			this.pushFurnaces(this.workBlockPos, this.furnaces, false);
		}
		boolean wartime = WartimeGate.isActive();
		if (this.isSleeping()) {
			if (this.bedPos == null || (isWorkDay(this.level()) && !wartime)) {
				this.wakeFromBed();
			} else {
				this.alignSleepingPos();
				return;
			}
		}
		if (this.isCourier()) {
			if (wartime || !isWorkDay(this.level())) {
				this.tickNight();
			} else {
				CourierRoute.tick(this);
			}
			return;
		}
		if (this.isSmelter()) {
			if (wartime || !isWorkDay(this.level())) {
				this.tickNight();
			} else {
				SmeltWork.tick(this);
			}
			return;
		}
		if (!isWorkDay(this.level()) || wartime) {
			this.tickNight();
		} else {
			this.tickDay();
		}
	}

	public static boolean hasRemainingDurability(ItemStack stack) {
		return !stack.isEmpty() && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage());
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && stack.isEmpty() && this.hasAssignment()) {
			if (!this.level().isClientSide) {
			if (this.isCourier()) {
				this.returnCourierScroll(player);
			} else if (this.isSmelter()) {
				this.returnSmelterScroll(player);
			} else {
				this.returnWorkerScroll(player);
			}
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		if (this.isCourier() && stack.getItem() instanceof CourierScrollItem && CourierScrollItem.isComplete(stack)) {
			if (!this.level().isClientSide) {
				WorkerAdvancements.onAssigned(player, this, stack);
				this.acceptCourierScroll(stack);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		if (this.isSmelter() && stack.getItem() instanceof SmelterScrollItem && SmelterScrollItem.isComplete(stack)) {
			if (!this.smelterScrollMatches(stack)) {
				return InteractionResult.PASS;
			}
			if (!this.level().isClientSide) {
				WorkerAdvancements.onAssigned(player, this, stack);
				this.acceptSmelterScroll(stack);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		if (!this.isCourier() && !this.isSmelter() && stack.getItem() instanceof WorkerScrollItem && WorkerScrollItem.isComplete(stack)) {
			if (!this.scrollMatchesProfession(stack)) {
				return InteractionResult.PASS;
			}
			if (!this.level().isClientSide) {
				WorkerAdvancements.onAssigned(player, this, stack);
				this.acceptWorkerScroll(stack);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		if (this.profession() == WorkerKind.LUMBERJACK && stack.getItem() instanceof ShearsItem && hasRemainingDurability(stack)) {
			if (!this.level().isClientSide) {
				ItemStack previous = this.getOffhandItem().copy();
				this.setItemSlot(EquipmentSlot.OFFHAND, stack.split(1));
				if (!previous.isEmpty() && !player.getInventory().add(previous)) {
					player.drop(previous, false);
				}
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		if (!this.isCourier() && this.workBlockPos != null && this.getMainHandItem().isEmpty() && this.acceptsTool(stack)) {
			if (!this.level().isClientSide) {
				this.setItemSlot(EquipmentSlot.MAINHAND, stack.split(1));
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		if (stack.isEmpty() && !player.isShiftKeyDown()) {
			if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
				serverPlayer.openMenu(this);
				WorkerAdvancements.grant(serverPlayer, WorkerActions.INSPECT);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		return InteractionResult.PASS;
	}

	@Override
	public WorkerInspectData getScreenOpeningData(ServerPlayer player) {
		return WorkerInspectData.capture(this);
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return WorkerInspectMenu.open(containerId, inventory, this);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.workBlockPos != null) {
			tag.putLong(ScrollData.WORK_BLOCK_POS, this.workBlockPos.asLong());
		}
		if (this.bedPos != null) {
			tag.putLong(ScrollData.BED_POS, this.bedPos.asLong());
		}
		if (this.chestPos != null) {
			tag.putLong(ScrollData.CHEST_POS, this.chestPos.asLong());
		}
		if (!this.route.isEmpty()) {
			CompoundTag routeTag = new CompoundTag();
			for (BlockPos pos : this.route) {
				ScrollData.addBlock(routeTag, pos);
			}
			tag.put(ScrollData.WORKER_BLOCKS, routeTag.get(ScrollData.WORKER_BLOCKS));
		}
		if (!this.furnaces.isEmpty()) {
			CompoundTag furnaceTag = new CompoundTag();
			for (BlockPos pos : this.furnaces) {
				ScrollData.addFurnace(furnaceTag, pos);
			}
			tag.put(ScrollData.FURNACES, furnaceTag.get(ScrollData.FURNACES));
		}
		this.saveSmeltCarry(tag);
		tag.putInt("FurnaceIndex", this.furnaceIndex);
		tag.putInt("SmeltPhase", this.smeltPhase);
		if (this.smeltTarget != null) {
			tag.putLong("SmeltTarget", this.smeltTarget.asLong());
		}
		tag.putBoolean("CourierSupply", this.courierSupply);
		tag.putBoolean("CourierSort", this.courierSort);
		tag.putBoolean("ReturningCargo", this.returningCargo);
		tag.putInt("RouteIndex", this.routeIndex);
		tag.putInt("RejectMark", this.rejectMark);
		tag.putBoolean("CargoStuck", this.cargoStuck);
		tag.putString("FactionColor", this.factionColorId());
		tag.putBoolean("Female", this.isFemale());
		if (this.assignerId != null) {
			tag.putString("Assigner", this.assignerId.toString());
		}
		this.cargo.save(tag, this.registryAccess());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.workBlockPos = tag.contains(ScrollData.WORK_BLOCK_POS) ? BlockPos.of(tag.getLong(ScrollData.WORK_BLOCK_POS)) : null;
		this.bedPos = tag.contains(ScrollData.BED_POS) ? BlockPos.of(tag.getLong(ScrollData.BED_POS)) : null;
		this.chestPos = tag.contains(ScrollData.CHEST_POS) ? BlockPos.of(tag.getLong(ScrollData.CHEST_POS)) : null;
		this.route.clear();
		this.route.addAll(ScrollData.blockList(tag));
		this.furnaces.clear();
		this.furnaces.addAll(ScrollData.furnaceList(tag));
		this.loadSmeltCarry(tag);
		this.furnaceIndex = tag.getInt("FurnaceIndex");
		this.smeltPhase = tag.getInt("SmeltPhase");
		this.smeltTarget = tag.contains("SmeltTarget") ? BlockPos.of(tag.getLong("SmeltTarget")) : null;
		this.courierSupply = tag.getBoolean("CourierSupply");
		this.courierSort = tag.getBoolean("CourierSort");
		this.returningCargo = tag.getBoolean("ReturningCargo");
		this.routeIndex = tag.getInt("RouteIndex");
		this.rejectMark = tag.contains("RejectMark") ? tag.getInt("RejectMark") : -1;
		this.cargoStuck = tag.getBoolean("CargoStuck");
		this.setFactionColorId(tag.getString("FactionColor"));
		if (tag.contains("Female")) {
			this.setFemale(tag.getBoolean("Female"));
		}
		this.assignerId = null;
		if (tag.contains("Assigner")) {
			try {
				this.assignerId = UUID.fromString(tag.getString("Assigner"));
			} catch (IllegalArgumentException ignored) {
				this.assignerId = null;
			}
		}
		if (this.hasIntactTool()) {
			WorkerAdvancements.grant(this, WorkerActions.TOOL);
		}
		ItemStack offhand = this.getOffhandItem();
		if (this.profession() == WorkerKind.LUMBERJACK && offhand.getItem() instanceof ShearsItem && hasRemainingDurability(offhand)) {
			WorkerAdvancements.grant(this, WorkerActions.SHEARS);
		}
		this.cargo.load(tag, this.registryAccess());
		if (this.isSmelter()) {
			this.pushFurnaces(this.workBlockPos, this.furnaces, false);
		}
		this.syncSmeltHand();
		this.normalizeBed();
		if (this.isCourier() && this.cargo.isEmpty() && !this.getMainHandItem().isEmpty()) {
			this.cargo.add(this.getMainHandItem(), null, null);
		}
		this.syncCargoHand();
	}

	public boolean acceptsScroll(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (this.isCourier()) {
			return stack.getItem() instanceof CourierScrollItem && CourierScrollItem.isComplete(stack);
		}
		if (this.isSmelter()) {
			return stack.getItem() instanceof SmelterScrollItem && SmelterScrollItem.isComplete(stack) && this.smelterScrollMatches(stack);
		}
		return stack.getItem() instanceof WorkerScrollItem && WorkerScrollItem.isComplete(stack) && this.scrollMatchesProfession(stack);
	}

	public ItemStack assignmentScroll() {
		if (!this.hasAssignment()) {
			return ItemStack.EMPTY;
		}
		if (this.isCourier()) {
			return this.buildCourierScroll();
		}
		if (this.isSmelter()) {
			return this.buildSmelterScroll();
		}
		return this.buildWorkerScroll();
	}

	public void applyScroll(ItemStack stack) {
		if (stack.isEmpty()) {
			this.clearOrder(false);
			return;
		}
		if (!this.acceptsScroll(stack)) {
			return;
		}
		if (this.isCourier()) {
			this.copyCourierScroll(stack);
		} else if (this.isSmelter()) {
			this.copySmelterScroll(stack);
		} else {
			this.copyWorkerScroll(stack);
		}
	}

	private boolean scrollMatchesProfession(ItemStack stack) {
		WorkerKind kind = this.profession();
		if (kind == null) {
			return false;
		}
		CompoundTag tag = ScrollData.get(stack);
		BlockPos pos = ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS).orElse(null);
		if (pos == null) {
			return false;
		}
		BlockState state = this.level().getBlockState(pos);
		return state.getBlock() instanceof WorkerBlock workerBlock && workerBlock.kind() == kind;
	}

	private boolean smelterScrollMatches(ItemStack stack) {
		CompoundTag tag = ScrollData.get(stack);
		BlockPos pos = ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS).orElse(null);
		if (pos == null) {
			return false;
		}
		BlockState state = this.level().getBlockState(pos);
		return state.getBlock() instanceof WorkerBlock workerBlock && workerBlock.kind() == WorkerKind.SMELTER;
	}

	private void acceptWorkerScroll(ItemStack stack) {
		this.copyWorkerScroll(stack);
		stack.shrink(1);
	}

	private void copyWorkerScroll(ItemStack stack) {
		CompoundTag tag = ScrollData.get(stack);
		this.workBlockPos = ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS).orElse(null);
		this.bedPos = ScrollData.pos(tag, ScrollData.BED_POS).orElse(null);
		this.normalizeBed();
	}

	private ItemStack buildWorkerScroll() {
		ItemStack scroll = new ItemStack(ModItems.WORKER_SCROLL);
		CompoundTag tag = new CompoundTag();
		if (this.workBlockPos != null) {
			tag.putLong(ScrollData.WORK_BLOCK_POS, this.workBlockPos.asLong());
		}
		if (this.bedPos != null) {
			tag.putLong(ScrollData.BED_POS, this.bedPos.asLong());
		}
		ScrollData.set(scroll, tag);
		return scroll;
	}

	private void acceptSmelterScroll(ItemStack stack) {
		this.copySmelterScroll(stack);
		stack.shrink(1);
	}

	private void copySmelterScroll(ItemStack stack) {
		this.dropSmeltCarry();
		BlockPos previous = this.workBlockPos;
		CompoundTag tag = ScrollData.get(stack);
		this.workBlockPos = ScrollData.pos(tag, ScrollData.WORK_BLOCK_POS).orElse(null);
		this.bedPos = ScrollData.pos(tag, ScrollData.BED_POS).orElse(null);
		this.furnaces.clear();
		this.furnaces.addAll(ScrollData.furnaceList(tag));
		this.furnaceIndex = 0;
		this.smeltPhase = SmeltWork.PHASE_NONE;
		this.smeltTarget = null;
		this.normalizeBed();
		if (previous != null && !previous.equals(this.workBlockPos)) {
			this.pushFurnaces(previous, List.of(), true);
		}
		this.pushFurnaces(this.workBlockPos, this.furnaces, true);
	}

	private ItemStack buildSmelterScroll() {
		ItemStack scroll = new ItemStack(ModItems.SMELTER_SCROLL);
		CompoundTag tag = new CompoundTag();
		if (this.workBlockPos != null) {
			tag.putLong(ScrollData.WORK_BLOCK_POS, this.workBlockPos.asLong());
		}
		for (BlockPos pos : this.furnaces) {
			ScrollData.addFurnace(tag, pos);
		}
		if (this.bedPos != null) {
			tag.putLong(ScrollData.BED_POS, this.bedPos.asLong());
		}
		ScrollData.set(scroll, tag);
		return scroll;
	}

	private void returnSmelterScroll(Player player) {
		ItemStack scroll = this.buildSmelterScroll();
		this.clearOrder(false);
		this.giveScroll(player, scroll);
	}

	private void returnWorkerScroll(Player player) {
		ItemStack scroll = this.buildWorkerScroll();
		this.clearOrder(false);
		this.giveScroll(player, scroll);
	}

	private void acceptCourierScroll(ItemStack stack) {
		this.copyCourierScroll(stack);
		stack.shrink(1);
	}

	private void copyCourierScroll(ItemStack stack) {
		CompoundTag tag = ScrollData.get(stack);
		this.chestPos = ScrollData.pos(tag, ScrollData.CHEST_POS).orElse(null);
		this.route.clear();
		this.route.addAll(ScrollData.blockList(tag));
		this.bedPos = ScrollData.pos(tag, ScrollData.BED_POS).orElse(null);
		this.normalizeBed();
		this.courierSort = stack.is(ModItems.COURIER_SORT_SCROLL);
		this.courierSupply = !this.courierSort && stack.is(ModItems.COURIER_SUPPLY_SCROLL);
		this.routeIndex = 0;
		this.rejectMark = -1;
		this.returningCargo = false;
	}

	private ItemStack buildCourierScroll() {
		Item item = this.courierSort ? ModItems.COURIER_SORT_SCROLL : this.courierSupply ? ModItems.COURIER_SUPPLY_SCROLL : ModItems.COURIER_SCROLL;
		ItemStack scroll = new ItemStack(item);
		CompoundTag tag = new CompoundTag();
		if (this.chestPos != null) {
			tag.putLong(ScrollData.CHEST_POS, this.chestPos.asLong());
		}
		for (BlockPos pos : this.route) {
			ScrollData.addBlock(tag, pos);
		}
		if (this.bedPos != null) {
			tag.putLong(ScrollData.BED_POS, this.bedPos.asLong());
		}
		ScrollData.set(scroll, tag);
		return scroll;
	}

	private void returnCourierScroll(Player player) {
		ItemStack scroll = this.buildCourierScroll();
		this.clearOrder(true);
		this.giveScroll(player, scroll);
	}

	private void giveScroll(Player player, ItemStack scroll) {
		if (!player.getInventory().add(scroll)) {
			player.drop(scroll, false);
		}
	}

	private void clearOrder(boolean dumpCargo) {
		BlockPos previousWork = this.workBlockPos;
		if (this.isSleeping()) {
			this.wakeFromBed();
		}
		if (dumpCargo) {
			for (ItemStack carried : this.cargo.stacks()) {
				this.spawnAtLocation(carried);
			}
			this.cargo.clear();
			this.cargoStuck = false;
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
		if (this.isSmelter()) {
			this.dropSmeltCarry();
		}
		this.workBlockPos = null;
		this.bedPos = null;
		this.chestPos = null;
		this.route.clear();
		this.furnaces.clear();
		this.furnaceIndex = 0;
		this.smeltPhase = SmeltWork.PHASE_NONE;
		this.smeltTarget = null;
		this.courierSupply = false;
		this.courierSort = false;
		this.returningCargo = false;
		this.routeIndex = 0;
		this.rejectMark = -1;
		if (this.isSmelter()) {
			this.pushFurnaces(previousWork, List.of(), true);
		}
	}

	private void pushFurnaces(@Nullable BlockPos pos, List<BlockPos> furnaces, boolean loadChunk) {
		if (this.level().isClientSide || pos == null || !(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!loadChunk && !serverLevel.isLoaded(pos)) {
			return;
		}
		if (loadChunk) {
			serverLevel.getChunkAt(pos);
		}
		BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
		if (blockEntity instanceof WorkerBlockEntity block && block.kind() == WorkerKind.SMELTER) {
			block.setFurnaces(furnaces);
		}
	}

	private void validatePosts() {
		this.normalizeBed();
	}

	private boolean workPostPresent() {
		if (this.workBlockPos == null) {
			return false;
		}
		if (!this.level().isLoaded(this.workBlockPos)) {
			return true;
		}
		WorkerKind kind = this.profession();
		BlockState state = this.level().getBlockState(this.workBlockPos);
		return kind != null && state.getBlock() instanceof WorkerBlock workerBlock && workerBlock.kind() == kind;
	}

	private void tickDay() {
		if (!this.workPostPresent()) {
			this.getNavigation().stop();
			this.keepWorkLook();
			return;
		}
		BlockPos stand = this.standNear(this.workBlockPos);
		this.moveToward(stand);
		if (this.distanceToSqr(this.workBlockPos.getX() + 0.5, this.workBlockPos.getY(), this.workBlockPos.getZ() + 0.5) <= 9.0
				&& this.level().getBlockEntity(this.workBlockPos) instanceof WorkerBlockEntity block) {
			this.refillTool(block);
		}
		if (!this.hasIntactTool() && this.profession() != WorkerKind.CRAFTER) {
			return;
		}
		if (!this.keepWorkLook()) {
			this.getLookControl().setLookAt(this.workBlockPos.getX() + 0.5, this.workBlockPos.getY() + 0.5, this.workBlockPos.getZ() + 0.5);
		}
	}

	private boolean keepWorkLook() {
		if (this.tickCount >= this.workLookUntil) {
			return false;
		}
		this.getLookControl().setLookAt(this.workLookX, this.workLookY, this.workLookZ);
		return true;
	}

	private void tickNight() {
		if (this.bedPos == null) {
			this.getNavigation().stop();
			return;
		}
		if (!this.level().isLoaded(this.bedPos)) {
			this.moveToward(this.bedPos);
			return;
		}
		if (WorkerBeds.head(this.level(), this.bedPos) == null) {
			if (this.isSleeping()) {
				this.wakeFromBed();
			}
			this.getNavigation().stop();
			return;
		}
		if (this.isBedOccupiedByOther(this.bedPos)) {
			this.moveToward(this.standNear(this.bedPos));
			return;
		}
		BlockPos stand = this.standNear(this.bedPos);
		boolean atStand = this.distanceToSqr(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5) <= ARRIVE_DISTANCE_SQR;
		boolean atBed = this.distanceToSqr(this.bedPos.getX() + 0.5, this.bedPos.getY(), this.bedPos.getZ() + 0.5) <= BED_REACH_SQR;
		if (atStand || atBed) {
			this.getNavigation().stop();
			this.startSleeping(this.bedPos);
			this.setVanillaBedOccupied(this.bedPos, true);
			return;
		}
		this.moveToward(stand);
	}

	private void alignSleepingPos() {
		if (this.bedPos == null || this.bedPos.equals(this.getSleepingPos().orElse(null))) {
			return;
		}
		this.wakeFromBed();
		this.startSleeping(this.bedPos);
		this.setVanillaBedOccupied(this.bedPos, true);
	}

	private void normalizeBed() {
		if (this.bedPos == null || WorkerBeds.waitingForChunk(this.level(), this.bedPos)) {
			return;
		}
		BlockPos head = WorkerBeds.head(this.level(), this.bedPos);
		if (head == null) {
			if (this.isSleeping()) {
				this.wakeFromBed();
			} else {
				this.setVanillaBedOccupied(this.bedPos, false);
			}
			return;
		}
		this.bedPos = head;
	}

	private void wakeFromBed() {
		BlockPos sleeping = this.getSleepingPos().orElse(this.bedPos);
		this.stopSleeping();
		if (sleeping != null) {
			this.setVanillaBedOccupied(sleeping, false);
		}
	}

	private boolean isBedOccupiedByOther(BlockPos pos) {
		if (!this.level().isLoaded(pos)) {
			return false;
		}
		BlockPos head = WorkerBeds.head(this.level(), pos);
		if (head == null) {
			head = pos;
		}
		BlockPos foot = WorkerBeds.otherHalf(this.level(), head);
		if (this.isPartOccupied(head) || (foot != null && this.isPartOccupied(foot))) {
			return true;
		}
		AABB area = new AABB(head);
		if (foot != null) {
			area = area.minmax(new AABB(foot));
		}
		area = area.inflate(3.0);
		for (HumanWorkerEntity other : this.level().getEntitiesOfClass(HumanWorkerEntity.class, area)) {
			if (other == this) {
				continue;
			}
			BlockPos sleeping = other.getSleepingPos().orElse(null);
			if (other.isSleeping() && this.coversBed(sleeping, head, foot)) {
				return true;
			}
			if (other.getId() < this.getId() && this.sameBed(other.getBedPos(), head, foot) && this.inSleepRange(other, head, foot)) {
				return true;
			}
		}
		return false;
	}

	private boolean coversBed(@Nullable BlockPos candidate, BlockPos head, @Nullable BlockPos foot) {
		return candidate != null && (candidate.equals(head) || (foot != null && candidate.equals(foot)));
	}

	private boolean sameBed(@Nullable BlockPos theirBed, BlockPos head, @Nullable BlockPos foot) {
		if (theirBed == null) {
			return false;
		}
		if (this.coversBed(theirBed, head, foot)) {
			return true;
		}
		if (!this.level().isLoaded(theirBed)) {
			return false;
		}
		return head.equals(WorkerBeds.head(this.level(), theirBed));
	}

	private boolean inSleepRange(HumanWorkerEntity other, BlockPos head, @Nullable BlockPos foot) {
		if (other.distanceToSqr(head.getX() + 0.5, head.getY(), head.getZ() + 0.5) <= BED_REACH_SQR) {
			return true;
		}
		return foot != null && other.distanceToSqr(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5) <= BED_REACH_SQR;
	}

	private boolean isPartOccupied(BlockPos pos) {
		BlockState state = this.level().getBlockState(pos);
		return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.OCCUPIED);
	}

	private void setVanillaBedOccupied(BlockPos pos, boolean occupied) {
		if (this.level().isClientSide || !this.level().isLoaded(pos)) {
			return;
		}
		this.setOccupied(pos, occupied);
		BlockPos other = WorkerBeds.otherHalf(this.level(), pos);
		if (other != null) {
			this.setOccupied(other, occupied);
		}
	}

	private void setOccupied(BlockPos pos, boolean occupied) {
		BlockState state = this.level().getBlockState(pos);
		if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.OCCUPIED) != occupied) {
			this.level().setBlock(pos, state.setValue(BedBlock.OCCUPIED, occupied), 3);
		}
	}

	private void moveToward(BlockPos pos) {
		double distance = this.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		if (distance <= ARRIVE_DISTANCE_SQR) {
			this.getNavigation().stop();
			this.routeWalker.reset();
			this.cantReach = false;
			return;
		}
		RouteWalker.Status status = this.routeWalker.tick(this, Vec3.atBottomCenterOf(pos), 1.0);
		if (status == RouteWalker.Status.BLOCKED) {
			if (!this.cantReach) {
				this.rejectedStand = pos.immutable();
				this.standDirty = true;
				this.cantReach = true;
			}
			return;
		}
		this.cantReach = false;
	}

	private BlockPos standNear(BlockPos target) {
		if (this.standTarget == null || !this.standTarget.equals(target)) {
			this.standTarget = target.immutable();
			this.rejectedStand = null;
			this.standDirty = true;
			this.cantReach = false;
			this.routeWalker.reset();
		}
		if (!this.standDirty && this.standCell != null) {
			return this.standCell;
		}
		this.standCell = StandCells.beside(this, target, this.rejectedStand).immutable();
		this.standDirty = false;
		return this.standCell;
	}

	private static boolean isWorkDay(Level level) {
		return level.getDayTime() % 24000L < 12000L;
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		super.setItemSlot(slot, stack);
		if (this.level().isClientSide) {
			return;
		}
		if (slot == EquipmentSlot.MAINHAND && this.acceptsTool(stack) && hasRemainingDurability(stack)) {
			WorkerAdvancements.grant(this, WorkerActions.TOOL);
		}
		if (slot == EquipmentSlot.OFFHAND && this.profession() == WorkerKind.LUMBERJACK && stack.getItem() instanceof ShearsItem && hasRemainingDurability(stack)) {
			WorkerAdvancements.grant(this, WorkerActions.SHEARS);
		}
	}

	private void grantStatusAdvancements() {
		String key = this.statusKey();
		if ("zk-workers.worker.status.sleeping".equals(key)) {
			WorkerAdvancements.grant(this, WorkerActions.SLEEPING);
			return;
		}
		if ("zk-workers.worker.status.working".equals(key) && this.showingWork()) {
			WorkerAdvancements.grant(this, WorkerActions.WORKING);
		}
		if (this.hasIntactTool()) {
			WorkerAdvancements.grant(this, WorkerActions.TOOL);
		}
		ItemStack offhand = this.getOffhandItem();
		if (this.profession() == WorkerKind.LUMBERJACK && offhand.getItem() instanceof ShearsItem && hasRemainingDurability(offhand)) {
			WorkerAdvancements.grant(this, WorkerActions.SHEARS);
		}
	}

	private boolean showingWork() {
		if (this.isCourier()) {
			if (this.near(this.chestPos)) {
				return true;
			}
			for (BlockPos stop : this.route) {
				if (this.near(stop)) {
					return true;
				}
			}
			return false;
		}
		return this.isStationed();
	}

	private boolean near(@Nullable BlockPos pos) {
		return pos != null && this.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= 9.0;
	}

	public String statusKey() {
		if (this.isSleeping()) {
			return "zk-workers.worker.status.sleeping";
		}
		if (WartimeGate.isActive()) {
			return "zk-workers.worker.status.wartime";
		}
		if (!isWorkDay(this.level()) && this.bedPos != null && this.isBedOccupiedByOther(this.bedPos)) {
			return "zk-workers.worker.status.bed_occupied";
		}
		if (this.cantReach) {
			return "zk-workers.worker.status.cant_reach";
		}
		if (this.cargoStuck) {
			return "zk-workers.worker.status.cargo_stuck";
		}
		if (!this.isCourier() && !this.isSmelter() && this.profession() != WorkerKind.CRAFTER && this.workBlockPos != null && !this.hasIntactTool()) {
			return "zk-workers.worker.status.no_tool";
		}
		if (this.isSmelter() && this.furnaces.isEmpty() && this.workBlockPos != null) {
			return "zk-workers.worker.status.no_furnaces";
		}
		if (this.isSmelter() && this.smeltPhase == SmeltWork.PHASE_DEPOSIT && this.smeltCarryHas(SmeltWork.KIND_RESULT) && this.workBlockPos != null
				&& this.distanceToSqr(this.workBlockPos.getX() + 0.5, this.workBlockPos.getY(), this.workBlockPos.getZ() + 0.5) <= 6.25) {
			return "zk-workers.worker.status.output_full";
		}
		WorkerBlockEntity block = this.workBlock();
		if (block != null) {
			if (block.kind() == WorkerKind.QUARRY && block.quarryExhausted()) {
				return "zk-workers.worker.status.quarry_bottom";
			}
			if (block.kind() != WorkerKind.BUILDER && outputFull(block)) {
				return "zk-workers.worker.status.output_full";
			}
			WorkerKind kind = this.profession();
			if (kind == WorkerKind.BUILDER && this.level() instanceof ServerLevel serverLevel && this.workBlockPos != null) {
				String building = BuilderTasks.statusKey(serverLevel, this.workBlockPos);
				if (building != null) {
					return building;
				}
			}
			if (kind == WorkerKind.FARMER && !hasSeed(block)) {
				return "zk-workers.worker.status.no_seeds";
			}
			if (kind == WorkerKind.CHICKEN || kind == WorkerKind.COW || kind == WorkerKind.SHEEP || kind == WorkerKind.PIG) {
				if (!hasFood(block, kind)) {
					return "zk-workers.worker.status.no_food";
				}
			}
		}
		return "zk-workers.worker.status.working";
	}

	private @Nullable WorkerBlockEntity workBlock() {
		if (this.workBlockPos == null || !this.level().isLoaded(this.workBlockPos)) {
			return null;
		}
		BlockEntity blockEntity = this.level().getBlockEntity(this.workBlockPos);
		return blockEntity instanceof WorkerBlockEntity worker ? worker : null;
	}

	private static boolean outputFull(WorkerBlockEntity block) {
		for (int slot = WorkerSlots.outputStart(block.kind()); slot < block.getContainerSize(); slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasSeed(WorkerBlockEntity block) {
		for (int slot = 0; slot < WorkerSlots.inputCount(block.kind()); slot++) {
			if (WorkerSlots.isCropSeed(block.getItem(slot))) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasFood(WorkerBlockEntity block, WorkerKind kind) {
		var tag = switch (kind) {
			case CHICKEN -> ItemTags.CHICKEN_FOOD;
			case COW -> ItemTags.COW_FOOD;
			case SHEEP -> ItemTags.SHEEP_FOOD;
			case PIG -> ItemTags.PIG_FOOD;
			default -> null;
		};
		if (tag == null) {
			return true;
		}
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			if (block.getItem(slot).is(tag)) {
				return true;
			}
		}
		return false;
	}

	private int spendHandUses(int left) {
		ItemStack hand = this.getMainHandItem();
		int available = storedUses(this.profession(), hand);
		if (available <= 0 || left <= 0) {
			return 0;
		}
		if (available == Integer.MAX_VALUE) {
			return left;
		}
		int spend = Math.min(left, available);
		if (hand.getDamageValue() + spend >= hand.getMaxDamage()) {
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		} else {
			hand.setDamageValue(hand.getDamageValue() + spend);
			this.setItemSlot(EquipmentSlot.MAINHAND, hand);
		}
		return spend;
	}

	private void equipSpareTool(WorkerBlockEntity block) {
		this.refillTool(block);
	}

	private boolean acceptsTool(ItemStack stack) {
		return toolMatches(this.profession(), stack);
	}

	private static boolean toolMatches(WorkerKind kind, ItemStack stack) {
		if (kind == null || stack.isEmpty()) {
			return false;
		}
		return switch (kind) {
			case QUARRY -> stack.getItem() instanceof PickaxeItem;
			case FARMER -> stack.getItem() instanceof HoeItem;
			case LUMBERJACK -> stack.getItem() instanceof AxeItem;
			case CHICKEN, COW, SHEEP, PIG -> stack.getItem() instanceof SwordItem;
			case BUILDER -> stack.getItem() instanceof HammerItem;
			case SMELTER, CRAFTER -> false;
			case FISHER -> stack.getItem() instanceof net.minecraft.world.item.FishingRodItem;
		};
	}

	public List<BlockPos> furnaces() {
		return this.furnaces;
	}

	public int furnaceIndex() {
		return this.furnaceIndex;
	}

	public void setFurnaceIndex(int furnaceIndex) {
		this.furnaceIndex = furnaceIndex;
	}

	public int smeltPhase() {
		return this.smeltPhase;
	}

	public void setSmeltPhase(int smeltPhase) {
		this.smeltPhase = smeltPhase;
	}

	public @Nullable BlockPos smeltTarget() {
		return this.smeltTarget;
	}

	public void setSmeltTarget(@Nullable BlockPos smeltTarget) {
		this.smeltTarget = smeltTarget == null ? null : smeltTarget.immutable();
	}

	public boolean cantReach() {
		return this.cantReach;
	}

	public int smeltCarrySize() {
		return this.smeltCarry.size();
	}

	public boolean smeltCarryRoom() {
		return this.smeltCarry.size() < SMELT_CARRY_LIMIT;
	}

	public ItemStack smeltCarryStack(int index) {
		if (index < 0 || index >= this.smeltCarry.size()) {
			return ItemStack.EMPTY;
		}
		return this.smeltCarry.get(index).stack;
	}

	public String smeltCarryKind(int index) {
		if (index < 0 || index >= this.smeltCarry.size()) {
			return "";
		}
		return this.smeltCarry.get(index).kind;
	}

	public void addSmeltCarry(ItemStack stack, String kind) {
		if (stack.isEmpty() || !this.smeltCarryRoom()) {
			return;
		}
		this.smeltCarry.add(new CarryPiece(stack.copy(), kind));
		this.syncSmeltHand();
	}

	public void setSmeltCarryStack(int index, ItemStack stack) {
		if (index < 0 || index >= this.smeltCarry.size()) {
			return;
		}
		this.smeltCarry.get(index).stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		this.syncSmeltHand();
	}

	public void compactSmeltCarry() {
		this.smeltCarry.removeIf(piece -> piece.stack.isEmpty());
		this.syncSmeltHand();
	}

	public void replaceSmeltCarry(List<ItemStack> stacks, List<String> kinds) {
		this.smeltCarry.clear();
		int count = Math.min(SMELT_CARRY_LIMIT, Math.min(stacks.size(), kinds.size()));
		for (int index = 0; index < count; index++) {
			ItemStack stack = stacks.get(index);
			if (!stack.isEmpty()) {
				this.smeltCarry.add(new CarryPiece(stack.copy(), kinds.get(index)));
			}
		}
		this.syncSmeltHand();
	}

	public void dropSmeltCarry() {
		for (CarryPiece piece : this.smeltCarry) {
			if (!piece.stack.isEmpty()) {
				this.spawnAtLocation(piece.stack.copy());
			}
		}
		this.smeltCarry.clear();
		this.syncSmeltHand();
	}

	public boolean smeltCarryHas(String kind) {
		for (CarryPiece piece : this.smeltCarry) {
			if (kind.equals(piece.kind) && !piece.stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
		super.dropCustomDeathLoot(level, source, causedByPlayer);
		this.dropSmeltCarry();
	}

	private void saveSmeltCarry(CompoundTag tag) {
		if (this.smeltCarry.isEmpty()) {
			return;
		}
		ListTag list = new ListTag();
		HolderLookup.Provider registries = this.registryAccess();
		for (CarryPiece piece : this.smeltCarry) {
			CompoundTag entry = new CompoundTag();
			entry.put(SMELT_ITEM, piece.stack.save(registries));
			entry.putString(SMELT_KIND, piece.kind);
			list.add(entry);
		}
		tag.put(SMELT_CARRY, list);
	}

	private void loadSmeltCarry(CompoundTag tag) {
		this.smeltCarry.clear();
		if (!tag.contains(SMELT_CARRY, Tag.TAG_LIST)) {
			return;
		}
		ListTag list = tag.getList(SMELT_CARRY, Tag.TAG_COMPOUND);
		HolderLookup.Provider registries = this.registryAccess();
		for (int index = 0; index < list.size() && this.smeltCarry.size() < SMELT_CARRY_LIMIT; index++) {
			CompoundTag entry = list.getCompound(index);
			ItemStack stack = ItemStack.parse(registries, entry.getCompound(SMELT_ITEM)).orElse(ItemStack.EMPTY);
			if (stack.isEmpty()) {
				continue;
			}
			this.smeltCarry.add(new CarryPiece(stack, entry.getString(SMELT_KIND)));
		}
	}

	private void syncSmeltHand() {
		if (!this.isSmelter()) {
			return;
		}
		ItemStack show = this.smeltCarry.isEmpty() ? ItemStack.EMPTY : this.smeltCarry.get(0).stack;
		this.setItemSlot(EquipmentSlot.MAINHAND, show.isEmpty() ? ItemStack.EMPTY : show.copy());
	}

	private static final class CarryPiece {
		private ItemStack stack;
		private final String kind;

		private CarryPiece(ItemStack stack, String kind) {
			this.stack = stack;
			this.kind = kind;
		}
	}
}
