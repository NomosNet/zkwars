package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.compat.GensokyoFoodCompat;
import com.example.compat.WartimeGate;
import com.example.entity.HumanWorkerEntity;
import com.example.inventory.WorkerSlots;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class IdleWork {
	public static final int MAX_BACKLOG = 12_000;
	private static final int GRACE_TICKS = 20;
	private static final int SAVE_INTERVAL = 20;

	private IdleWork() {
	}

	public static boolean tick(ServerLevel level, BlockPos pos, WorkerBlockEntity block) {
		if (block.kind() == WorkerKind.QUARRY || block.kind() == WorkerKind.BUILDER || block.kind() == WorkerKind.SMELTER || block.kind() == WorkerKind.LUMBERJACK || block.kind() == WorkerKind.FISHER || block.kind() == WorkerKind.CRAFTER) {
			if (block.workBacklog() > 0) {
				block.setWorkBacklog(0);
			}
			return false;
		}
		if (accrue(level, block)) {
			EntityType<HumanWorkerEntity> type = workerType(block.kind());
			HumanWorkerEntity worker = type == null ? null : WorkerSupport.findAssigned(level, pos, type);
			block.setOfflineGrace(worker == null ? GRACE_TICKS : 0);
		}
		if (block.offlineGrace() < 0) {
			return false;
		}
		EntityType<HumanWorkerEntity> type = workerType(block.kind());
		HumanWorkerEntity worker = type == null ? null : WorkerSupport.findAssigned(level, pos, type);
		if (worker == null && block.offlineGrace() > 1) {
			block.setOfflineGrace(block.offlineGrace() - 1);
			return true;
		}
		settle(block, worker);
		block.setOfflineGrace(-1);
		return true;
	}

	private static boolean accrue(ServerLevel level, WorkerBlockEntity block) {
		long now = level.getGameTime();
		long previous = block.lastWorkTime();
		if (previous < 0) {
			block.setLastWorkTime(now);
			block.setChanged();
			return false;
		}
		long gap = now - previous - WartimeGate.skippedTicks(level, previous, now);
		boolean opened = gap > 1;
		if (opened) {
			long room = MAX_BACKLOG - (long) block.workBacklog();
			if (room > 0) {
				block.setWorkBacklog((int) (block.workBacklog() + Math.min(room, gap)));
			}
		}
		block.setLastWorkTime(now);
		if (now % SAVE_INTERVAL == 0L) {
			block.setChanged();
		}
		return opened;
	}

	private static void settle(WorkerBlockEntity block, @Nullable HumanWorkerEntity worker) {
		WorkerKind kind = block.kind();
		int cycles = block.workBacklog() / cycleTicks(kind);
		ItemStack one = cycles > 0 ? product(block, kind) : ItemStack.EMPTY;
		int pay = one.isEmpty() ? 0 : Math.min(cycles, WorkerSupport.fittingCycles(block, one, cycles));
		if (foodSlotRequired(kind)) {
			pay = Math.min(pay, countFood(block, kind));
		}
		if (usesTool(kind)) {
			pay = Math.min(pay, toolBudget(kind, worker, block));
		}
		if (pay > 0) {
			List<ItemStack> drops = new ArrayList<>();
			for (int i = 0; i < pay; i++) {
				drops.add(one.copy());
			}
			WorkerSupport.insertOutput(block, drops);
			if (worker != null) {
				WorkerAdvancements.grant(worker, WorkerActions.IDLE);
				WorkerAdvancements.grant(worker, WorkerActions.penProduct(kind));
			}
			if (foodSlotRequired(kind)) {
				consumeFood(block, kind, pay);
			}
			if (usesTool(kind)) {
				HumanWorkerEntity.spendStoredUses(worker, block, pay);
			}
		}
		if (block.workBacklog() > 0) {
			block.setWorkBacklog(0);
		}
	}

	private static boolean usesTool(WorkerKind kind) {
		return kind == WorkerKind.FARMER || kind == WorkerKind.LUMBERJACK;
	}

	private static int toolBudget(WorkerKind kind, @Nullable HumanWorkerEntity worker, WorkerBlockEntity block) {
		long total = 0;
		if (worker != null) {
			int hand = HumanWorkerEntity.storedUses(kind, worker.getMainHandItem());
			if (hand == Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			total += hand;
		}
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			int uses = HumanWorkerEntity.storedUses(kind, block.getItem(slot));
			if (uses == Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			total += uses;
		}
		return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
	}

	private static int countFood(WorkerBlockEntity block, WorkerKind kind) {
		TagKey<Item> tag = foodTag(kind);
		if (tag == null) {
			return 0;
		}
		int count = 0;
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			ItemStack stack = block.getItem(slot);
			if (stack.is(tag)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static void consumeFood(WorkerBlockEntity block, WorkerKind kind, int cycles) {
		TagKey<Item> tag = foodTag(kind);
		if (tag == null) {
			return;
		}
		int left = cycles;
		for (int slot = 0; slot < block.getContainerSize() && left > 0; slot++) {
			ItemStack stack = block.getItem(slot);
			if (!stack.is(tag)) {
				continue;
			}
			int take = Math.min(left, stack.getCount());
			block.removeItem(slot, take);
			left -= take;
		}
	}

	private static boolean foodSlotRequired(WorkerKind kind) {
		return kind == WorkerKind.CHICKEN || kind == WorkerKind.COW || kind == WorkerKind.SHEEP || kind == WorkerKind.PIG;
	}

	private static ItemStack product(WorkerBlockEntity block, WorkerKind kind) {
		return switch (kind) {
			case QUARRY -> new ItemStack(Items.COBBLESTONE);
			case LUMBERJACK -> ItemStack.EMPTY;
			case FARMER -> farmProduct(block);
			case CHICKEN -> new ItemStack(Items.FEATHER);
			case COW -> new ItemStack(Items.LEATHER);
			case SHEEP -> new ItemStack(Items.WHITE_WOOL);
			case PIG -> new ItemStack(Items.PORKCHOP);
			case BUILDER, SMELTER, FISHER, CRAFTER -> ItemStack.EMPTY;
		};
	}

	private static ItemStack farmProduct(WorkerBlockEntity block) {
		for (int slot = 0; slot < WorkerSlots.inputCount(block.kind()); slot++) {
			ItemStack seed = block.getItem(slot);
			if (!WorkerSlots.isCropSeed(seed)) {
				continue;
			}
			ItemStack product = cropProduct(seed.getItem());
			if (!product.isEmpty()) {
				return product;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack cropProduct(Item seed) {
		if (seed == Items.WHEAT_SEEDS) {
			return new ItemStack(Items.WHEAT);
		}
		if (seed == Items.BEETROOT_SEEDS) {
			return new ItemStack(Items.BEETROOT);
		}
		if (seed == Items.CARROT) {
			return new ItemStack(Items.CARROT);
		}
		if (seed == Items.POTATO) {
			return new ItemStack(Items.POTATO);
		}
		if (seed == Items.PUMPKIN_SEEDS) {
			return new ItemStack(Items.PUMPKIN);
		}
		if (seed == Items.MELON_SEEDS) {
			return new ItemStack(Items.MELON_SLICE, 4);
		}
		if (seed == Items.SWEET_BERRIES) {
			return new ItemStack(Items.SWEET_BERRIES);
		}
		if (seed == Items.NETHER_WART) {
			return new ItemStack(Items.NETHER_WART);
		}
		if (seed == Items.TORCHFLOWER_SEEDS) {
			return new ItemStack(Items.TORCHFLOWER);
		}
		return GensokyoFoodCompat.idleProduct(seed);
	}

	private static TagKey<Item> foodTag(WorkerKind kind) {
		return switch (kind) {
			case CHICKEN -> ItemTags.CHICKEN_FOOD;
			case COW -> ItemTags.COW_FOOD;
			case SHEEP -> ItemTags.SHEEP_FOOD;
			case PIG -> ItemTags.PIG_FOOD;
			default -> null;
		};
	}

	private static EntityType<HumanWorkerEntity> workerType(WorkerKind kind) {
		return switch (kind) {
			case QUARRY -> ModEntities.HUMAN_MINER;
			case FARMER -> ModEntities.HUMAN_FARMER;
			case LUMBERJACK -> ModEntities.HUMAN_LUMBERJACK;
			case CHICKEN -> ModEntities.HUMAN_CHICKEN_BREEDER;
			case COW -> ModEntities.HUMAN_COW_BREEDER;
			case SHEEP -> ModEntities.HUMAN_SHEEP_BREEDER;
			case PIG -> ModEntities.HUMAN_PIG_BREEDER;
			case BUILDER -> ModEntities.HUMAN_BUILDER;
			case SMELTER -> ModEntities.HUMAN_SMELTER;
			case FISHER -> ModEntities.HUMAN_FISHER;
			case CRAFTER -> ModEntities.HUMAN_CRAFTER;
		};
	}

	private static int cycleTicks(WorkerKind kind) {
		return switch (kind) {
			case CHICKEN, COW, SHEEP, PIG -> 200;
			case FISHER -> 100;
			case QUARRY, FARMER, LUMBERJACK, BUILDER, SMELTER, CRAFTER -> 20;
		};
	}
}
