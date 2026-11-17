package com.example.block.entity;

import com.example.block.WorkerBlock;
import com.example.block.WorkerKind;
import com.example.block.work.BuildWork;
import com.example.block.work.FarmWork;
import com.example.block.work.IdleWork;
import com.example.block.work.CraftWork;
import com.example.block.work.FisherWork;
import com.example.block.work.LumberWork;
import com.example.block.work.PenWork;
import com.example.block.work.QuarryBounds;
import com.example.block.work.QuarryWork;
import com.example.block.work.WorkArea;
import com.example.compat.WartimeGate;
import com.example.inventory.WorkerMenu;
import com.example.inventory.WorkerMenuData;
import com.example.inventory.WorkerSlots;
import com.example.item.SmelterScrollItem;
import com.example.registry.ModBlockEntities;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WorkerBlockEntity extends BlockEntity implements Container, ExtendedScreenHandlerFactory<WorkerMenuData> {
	private static final String QUARRY_Y = "QuarryY";
	private static final String QUARRY_INDEX = "QuarryIndex";
	private static final String QUARRY_EXHAUSTED = "QuarryExhausted";
	private static final String QUARRY_CLEAR_SWEEP = "QuarryClearSweep";
	private static final String QUARRY_MIN_X = "QuarryMinX";
	private static final String QUARRY_MAX_X = "QuarryMaxX";
	private static final String QUARRY_MIN_Y = "QuarryMinY";
	private static final String QUARRY_MAX_Y = "QuarryMaxY";
	private static final String QUARRY_MIN_Z = "QuarryMinZ";
	private static final String QUARRY_MAX_Z = "QuarryMaxZ";
	private static final String WORK_RADIUS = "WorkRadius";
	private static final String ADULT_LIMIT = "AdultLimit";
	private static final String LUMBER_POS = "LumberPos";
	private static final String LUMBER_BASE = "LumberBase";
	private static final String LUMBER_SPAN = "LumberSpan";
	private static final String LUMBER_WOOD = "LumberWood";
	private static final String LUMBER_QUEUE = "LumberQueue";
	private static final String LUMBER_FOLIAGE = "LumberFoliage";
	private static final String LAST_WORK_TIME = "LastWorkTime";
	private static final String WORK_BACKLOG = "WorkBacklog";
	private static final String FURNACES = "Furnaces";

	private final NonNullList<ItemStack> items;
	private int workCooldown;
	private int builderCooldown;
	private int quarryY = Integer.MIN_VALUE;
	private int quarryIndex;
	private boolean quarryExhausted;
	private boolean quarryClearSweep = true;
	private int quarryBreakCooldown;
	private int quarryMinX = QuarryBounds.DEFAULT_MIN_X;
	private int quarryMaxX = QuarryBounds.DEFAULT_MAX_X;
	private int quarryMinY = QuarryBounds.DEFAULT_MIN_Y;
	private int quarryMaxY = QuarryBounds.DEFAULT_MAX_Y;
	private int quarryMinZ = QuarryBounds.DEFAULT_MIN_Z;
	private int quarryMaxZ = QuarryBounds.DEFAULT_MAX_Z;
	private int workRadius = WorkArea.DEFAULT_RADIUS;
	private int adultLimit = PenWork.DEFAULT_ADULTS;
	private @Nullable BlockPos lumberPos;
	private @Nullable BlockPos lumberBase;
	private int lumberSpan = 1;
	private @Nullable Block lumberWood;
	private boolean lumberFoliage;
	private int lumberScan;
	private final List<BlockPos> lumberQueue = new ArrayList<>();
	private final List<BlockPos> furnaces = new ArrayList<>();
	private long lastWorkTime = -1;
	private int workBacklog;
	private int offlineGrace = -1;

	public WorkerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WORKER, pos, state);
		this.items = NonNullList.withSize(WorkerSlots.slotCount(this.kind()), ItemStack.EMPTY);
		this.workRadius = WorkArea.defaultRadius(this.kind());
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, WorkerBlockEntity block) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (WartimeGate.isActive()) {
			return;
		}
		boolean holdLive = IdleWork.tick(serverLevel, pos, block);
		if (block.kind() == WorkerKind.QUARRY) {
			QuarryWork.tick(serverLevel, pos, block);
			return;
		}
		if (block.kind() == WorkerKind.BUILDER) {
			BuildWork.tick(serverLevel, pos, block);
			return;
		}
		if (holdLive) {
			return;
		}
		if (block.workCooldown > 0) {
			block.workCooldown--;
			return;
		}
		block.workCooldown = switch (block.kind()) {
			case CHICKEN, COW, SHEEP, PIG -> 200;
			case FISHER -> 100;
			default -> 20;
		};
		switch (block.kind()) {
			case LUMBERJACK -> LumberWork.tick(serverLevel, pos, block);
			case FARMER -> FarmWork.tick(serverLevel, pos, block);
			case CHICKEN, COW, SHEEP, PIG -> PenWork.tick(serverLevel, pos, block);
			case FISHER -> FisherWork.tick(serverLevel, pos, block);
			case CRAFTER -> CraftWork.tick(serverLevel, pos, block);
			case QUARRY, BUILDER, SMELTER -> {
			}
		}
	}

	public int quarryY() {
		return this.quarryY;
	}

	public void setQuarryY(int quarryY) {
		this.quarryY = quarryY;
		this.setChanged();
	}

	public int quarryIndex() {
		return this.quarryIndex;
	}

	public boolean quarryExhausted() {
		return this.quarryExhausted;
	}

	public void setQuarryCursor(int quarryY, int quarryIndex, boolean exhausted) {
		this.quarryY = quarryY;
		this.quarryIndex = quarryIndex;
		this.quarryExhausted = exhausted;
		this.setChanged();
	}

	public boolean quarryClearSweep() {
		return this.quarryClearSweep;
	}

	public void setQuarryClearSweep(boolean quarryClearSweep) {
		this.quarryClearSweep = quarryClearSweep;
	}

	public int builderCooldown() {
		return this.builderCooldown;
	}

	public void setBuilderCooldown(int builderCooldown) {
		this.builderCooldown = Math.max(0, builderCooldown);
	}

	public void tickBuilderCooldown() {
		if (this.builderCooldown > 0) {
			this.builderCooldown--;
		}
	}

	public int quarryBreakCooldown() {
		return this.quarryBreakCooldown;
	}

	public void setQuarryBreakCooldown(int quarryBreakCooldown) {
		this.quarryBreakCooldown = Math.max(0, quarryBreakCooldown);
	}

	public void tickQuarryBreakCooldown() {
		if (this.quarryBreakCooldown > 0) {
			this.quarryBreakCooldown--;
		}
	}

	public QuarryBounds.Box quarryBounds() {
		return new QuarryBounds.Box(
				this.quarryMinX,
				this.quarryMaxX,
				this.quarryMinY,
				this.quarryMaxY,
				this.quarryMinZ,
				this.quarryMaxZ
		);
	}

	public void applyQuarryBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		QuarryBounds.Box box = QuarryBounds.clamp(minX, maxX, minY, maxY, minZ, maxZ);
		boolean changed = !box.equals(this.quarryBounds());
		this.storeQuarryBounds(box);
		if (changed) {
			this.quarryY = this.worldPosition.getY() + box.maxY();
			this.quarryIndex = 0;
			this.quarryExhausted = false;
			this.quarryClearSweep = true;
		}
		this.setChanged();
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	public int workRadius() {
		return WorkArea.radiusFor(this.kind(), this.workRadius);
	}

	public void applyWorkRadius(int radius) {
		if (!this.kind().editableRadius()) {
			return;
		}
		this.workRadius = WorkArea.clampRadius(this.kind(), radius);
		this.setChanged();
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	public int adultLimit() {
		return PenWork.clampAdults(this.adultLimit);
	}

	public void applyAdultLimit(int limit) {
		if (!this.kind().isPen()) {
			return;
		}
		this.adultLimit = PenWork.clampAdults(limit);
		this.setChanged();
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	public List<BlockPos> furnaces() {
		return this.furnaces;
	}

	public void setFurnaces(List<BlockPos> positions) {
		int limit = Math.min(positions.size(), SmelterScrollItem.MAX_FURNACES);
		if (this.sameFurnaces(positions, limit)) {
			return;
		}
		this.furnaces.clear();
		for (int index = 0; index < limit; index++) {
			this.furnaces.add(positions.get(index));
		}
		this.setChanged();
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	public @Nullable BlockPos lumberBase() {
		return this.lumberBase;
	}

	public int lumberSpan() {
		return this.lumberSpan;
	}

	public @Nullable Block lumberWood() {
		return this.lumberWood;
	}

	public boolean hasLumberQueue() {
		return !this.lumberQueue.isEmpty();
	}

	public @Nullable BlockPos lumberQueueHead() {
		return this.lumberQueue.isEmpty() ? null : this.lumberQueue.get(0);
	}

	public void popLumberQueue() {
		if (!this.lumberQueue.isEmpty()) {
			this.lumberQueue.remove(0);
			this.setChanged();
		}
	}

	public void setLumberJob(BlockPos base, int span, Block wood, List<BlockPos> logs) {
		this.lumberPos = null;
		this.lumberBase = base;
		this.lumberSpan = span;
		this.lumberWood = wood;
		this.lumberFoliage = false;
		this.lumberQueue.clear();
		this.lumberQueue.addAll(logs);
		this.setChanged();
	}

	public boolean lumberFoliage() {
		return this.lumberFoliage;
	}

	public void beginLumberFoliage(List<BlockPos> leaves) {
		this.lumberFoliage = true;
		this.lumberQueue.clear();
		this.lumberQueue.addAll(leaves);
		this.setChanged();
	}

	public int lumberScanCursor() {
		return this.lumberScan;
	}

	public void setLumberScanCursor(int cursor) {
		this.lumberScan = Math.max(0, cursor);
	}

	public void clearLumberJob() {
		this.lumberPos = null;
		this.lumberBase = null;
		this.lumberSpan = 1;
		this.lumberWood = null;
		this.lumberFoliage = false;
		this.lumberQueue.clear();
		this.setChanged();
	}

	public @Nullable BlockPos takeLumberSeed() {
		BlockPos seed = this.lumberPos;
		if (seed == null) {
			return null;
		}
		this.lumberPos = null;
		this.setChanged();
		return seed;
	}

	public long lastWorkTime() {
		return this.lastWorkTime;
	}

	public void setLastWorkTime(long lastWorkTime) {
		this.lastWorkTime = lastWorkTime;
	}

	public int offlineGrace() {
		return this.offlineGrace;
	}

	public void setOfflineGrace(int offlineGrace) {
		this.offlineGrace = offlineGrace;
	}

	public int workBacklog() {
		return this.workBacklog;
	}

	public void setWorkBacklog(int workBacklog) {
		this.workBacklog = Math.max(0, Math.min(IdleWork.MAX_BACKLOG, workBacklog));
		this.setChanged();
	}

	public WorkerKind kind() {
		if (this.getBlockState().getBlock() instanceof WorkerBlock workerBlock) {
			return workerBlock.kind();
		}
		return WorkerKind.QUARRY;
	}

	@Override
	public WorkerMenuData getScreenOpeningData(ServerPlayer player) {
		return WorkerMenuData.of(this);
	}

	@Override
	public Component getDisplayName() {
		return this.getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new WorkerMenu(containerId, playerInventory, this, WorkerMenuData.of(this));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		this.writeQuarryBounds(tag);
		tag.putInt(WORK_RADIUS, this.workRadius);
		tag.putInt(ADULT_LIMIT, this.adultLimit);
		this.writeFurnaces(tag);
		return tag;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : this.items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
		if (!stack.isEmpty()) {
			this.setChanged();
		}
		return stack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(this.items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.items.set(slot, stack);
		if (stack.getCount() > this.getMaxStackSize()) {
			stack.setCount(this.getMaxStackSize());
		}
		this.setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return WorkerSlots.mayPlace(this.kind(), slot, stack, this.getLevel());
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		this.items.clear();
		this.setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, this.items, registries);
		tag.putInt(QUARRY_Y, this.quarryY);
		tag.putInt(QUARRY_INDEX, this.quarryIndex);
		tag.putBoolean(QUARRY_EXHAUSTED, this.quarryExhausted);
		tag.putBoolean(QUARRY_CLEAR_SWEEP, this.quarryClearSweep);
		this.writeQuarryBounds(tag);
		tag.putInt(WORK_RADIUS, this.workRadius);
		tag.putInt(ADULT_LIMIT, this.adultLimit);
		if (this.lumberPos != null) {
			tag.putLong(LUMBER_POS, this.lumberPos.asLong());
		}
		if (this.lumberBase != null) {
			tag.putLong(LUMBER_BASE, this.lumberBase.asLong());
		}
		tag.putInt(LUMBER_SPAN, this.lumberSpan);
		tag.putBoolean(LUMBER_FOLIAGE, this.lumberFoliage);
		if (this.lumberWood != null) {
			tag.putString(LUMBER_WOOD, BuiltInRegistries.BLOCK.getKey(this.lumberWood).toString());
		}
		if (!this.lumberQueue.isEmpty()) {
			long[] queue = new long[this.lumberQueue.size()];
			for (int index = 0; index < queue.length; index++) {
				queue[index] = this.lumberQueue.get(index).asLong();
			}
			tag.putLongArray(LUMBER_QUEUE, queue);
		}
		if (this.lastWorkTime >= 0) {
			tag.putLong(LAST_WORK_TIME, this.lastWorkTime);
		}
		tag.putInt(WORK_BACKLOG, this.workBacklog);
		this.writeFurnaces(tag);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		for (int slot = 0; slot < this.items.size(); slot++) {
			this.items.set(slot, ItemStack.EMPTY);
		}
		ContainerHelper.loadAllItems(tag, this.items, registries);
		this.migrateFarmerOutput();
		this.quarryY = tag.contains(QUARRY_Y) ? tag.getInt(QUARRY_Y) : Integer.MIN_VALUE;
		this.quarryIndex = tag.contains(QUARRY_INDEX) ? tag.getInt(QUARRY_INDEX) : 0;
		this.quarryExhausted = tag.getBoolean(QUARRY_EXHAUSTED);
		this.quarryClearSweep = !tag.contains(QUARRY_CLEAR_SWEEP) || tag.getBoolean(QUARRY_CLEAR_SWEEP);
		this.readQuarryBounds(tag);
		this.workRadius = tag.contains(WORK_RADIUS)
				? WorkArea.clampRadius(this.kind(), tag.getInt(WORK_RADIUS))
				: WorkArea.defaultRadius(this.kind());
		this.adultLimit = tag.contains(ADULT_LIMIT)
				? PenWork.clampAdults(tag.getInt(ADULT_LIMIT))
				: PenWork.DEFAULT_ADULTS;
		this.lumberPos = tag.contains(LUMBER_POS) ? BlockPos.of(tag.getLong(LUMBER_POS)) : null;
		this.lumberBase = tag.contains(LUMBER_BASE) ? BlockPos.of(tag.getLong(LUMBER_BASE)) : null;
		this.lumberSpan = tag.contains(LUMBER_SPAN) ? Math.max(1, tag.getInt(LUMBER_SPAN)) : 1;
		this.lumberFoliage = tag.getBoolean(LUMBER_FOLIAGE);
		this.lumberWood = this.readLumberWood(tag);
		this.lumberQueue.clear();
		if (tag.contains(LUMBER_QUEUE)) {
			for (long packed : tag.getLongArray(LUMBER_QUEUE)) {
				this.lumberQueue.add(BlockPos.of(packed));
			}
		}
		if (this.lumberQueue.isEmpty() && this.lumberPos != null) {
			this.lumberBase = null;
			this.lumberSpan = 1;
			this.lumberWood = null;
		}
		this.lastWorkTime = tag.contains(LAST_WORK_TIME) ? tag.getLong(LAST_WORK_TIME) : -1;
		this.workBacklog = Math.max(0, Math.min(IdleWork.MAX_BACKLOG, tag.getInt(WORK_BACKLOG)));
		this.readFurnaces(tag);
	}

	private void migrateFarmerOutput() {
		if (this.kind() != WorkerKind.FARMER) {
			return;
		}
		boolean moved = false;
		int input = WorkerSlots.inputCount(WorkerKind.FARMER);
		for (int slot = input; slot < 9 && slot < this.items.size(); slot++) {
			ItemStack stack = this.items.get(slot);
			if (!WorkerSlots.isCropSeed(stack)) {
				continue;
			}
			if (this.compactSeed(stack, input)) {
				this.items.set(slot, ItemStack.EMPTY);
				moved = true;
			}
		}
		if (moved) {
			this.setChanged();
		}
	}

	private boolean compactSeed(ItemStack stack, int input) {
		int empty = -1;
		for (int slot = 0; slot < input; slot++) {
			ItemStack current = this.items.get(slot);
			if (current.isEmpty()) {
				if (empty < 0) {
					empty = slot;
				}
				continue;
			}
			if (!ItemStack.isSameItemSameComponents(current, stack) || current.getCount() >= current.getMaxStackSize()) {
				continue;
			}
			int move = Math.min(current.getMaxStackSize() - current.getCount(), stack.getCount());
			current.grow(move);
			stack.shrink(move);
			if (stack.isEmpty()) {
				return true;
			}
		}
		if (empty < 0 || stack.isEmpty()) {
			return stack.isEmpty();
		}
		this.items.set(empty, stack.copy());
		stack.setCount(0);
		return true;
	}

	private int farmerOutputDestination(ItemStack stack, int outputStart) {
		int empty = -1;
		for (int slot = outputStart; slot < this.items.size(); slot++) {
			ItemStack current = this.items.get(slot);
			if (current.isEmpty()) {
				if (empty < 0) {
					empty = slot;
				}
				continue;
			}
			if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize()) {
				return slot;
			}
		}
		return empty;
	}

	private void storeQuarryBounds(QuarryBounds.Box box) {
		this.quarryMinX = box.minX();
		this.quarryMaxX = box.maxX();
		this.quarryMinY = box.minY();
		this.quarryMaxY = box.maxY();
		this.quarryMinZ = box.minZ();
		this.quarryMaxZ = box.maxZ();
	}

	private boolean sameFurnaces(List<BlockPos> positions, int limit) {
		if (this.furnaces.size() != limit) {
			return false;
		}
		for (int index = 0; index < limit; index++) {
			if (!this.furnaces.get(index).equals(positions.get(index))) {
				return false;
			}
		}
		return true;
	}

	private void writeFurnaces(CompoundTag tag) {
		long[] packed = new long[this.furnaces.size()];
		for (int index = 0; index < packed.length; index++) {
			packed[index] = this.furnaces.get(index).asLong();
		}
		tag.putLongArray(FURNACES, packed);
	}

	private void readFurnaces(CompoundTag tag) {
		this.furnaces.clear();
		if (!tag.contains(FURNACES)) {
			return;
		}
		long[] packed = tag.getLongArray(FURNACES);
		int limit = Math.min(packed.length, SmelterScrollItem.MAX_FURNACES);
		for (int index = 0; index < limit; index++) {
			this.furnaces.add(BlockPos.of(packed[index]));
		}
	}

	private void writeQuarryBounds(CompoundTag tag) {
		tag.putInt(QUARRY_MIN_X, this.quarryMinX);
		tag.putInt(QUARRY_MAX_X, this.quarryMaxX);
		tag.putInt(QUARRY_MIN_Y, this.quarryMinY);
		tag.putInt(QUARRY_MAX_Y, this.quarryMaxY);
		tag.putInt(QUARRY_MIN_Z, this.quarryMinZ);
		tag.putInt(QUARRY_MAX_Z, this.quarryMaxZ);
	}

	private @Nullable Block readLumberWood(CompoundTag tag) {
		if (!tag.contains(LUMBER_WOOD)) {
			return null;
		}
		ResourceLocation id = ResourceLocation.tryParse(tag.getString(LUMBER_WOOD));
		if (id == null) {
			return null;
		}
		Block block = BuiltInRegistries.BLOCK.get(id);
		return block == Blocks.AIR ? null : block;
	}

	private void readQuarryBounds(CompoundTag tag) {
		if (!tag.contains(QUARRY_MIN_X)) {
			this.storeQuarryBounds(QuarryBounds.defaults());
			return;
		}
		this.storeQuarryBounds(QuarryBounds.clamp(
				tag.getInt(QUARRY_MIN_X),
				tag.getInt(QUARRY_MAX_X),
				tag.getInt(QUARRY_MIN_Y),
				tag.getInt(QUARRY_MAX_Y),
				tag.getInt(QUARRY_MIN_Z),
				tag.getInt(QUARRY_MAX_Z)
		));
	}
}
