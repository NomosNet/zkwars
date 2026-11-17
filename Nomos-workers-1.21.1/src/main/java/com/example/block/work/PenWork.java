package com.example.block.work;

import com.example.advancement.WorkerActions;
import com.example.advancement.WorkerAdvancements;
import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.entity.HumanWorkerEntity;
import com.example.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class PenWork {
	public static final int MIN_ADULTS = 2;
	public static final int MAX_ADULTS = 32;
	public static final int DEFAULT_ADULTS = 8;

	private PenWork() {
	}

	public static int clampAdults(int limit) {
		return Math.max(MIN_ADULTS, Math.min(MAX_ADULTS, limit));
	}

	public static void tick(ServerLevel level, BlockPos origin, WorkerBlockEntity block) {
		WorkerKind kind = block.kind();
		EntityType<HumanWorkerEntity> workerType = workerType(kind);
		HumanWorkerEntity worker = null;
		if (workerType != null) {
			worker = WorkerSupport.findWorker(level, origin, workerType, false);
			if (worker == null) {
				return;
			}
		}
		List<Animal> adults = adults(level, origin, animalClass(kind), block);
		if (adults.size() > block.adultLimit() && worker != null && worker.hasIntactTool()) {
			Animal extra = adults.get(adults.size() - 1);
			boolean struck = extra.hurt(level.damageSources().mobAttack(worker), 40.0F);
			if (struck) {
				worker.wearEquippedTool(block);
				worker.playWorkSwing(extra.getX(), extra.getEyeY(), extra.getZ());
				if (!extra.isAlive()) {
					keepFreshDrops(level, block, extra);
					WorkerAdvancements.grant(worker, WorkerActions.PEN_CULLED);
				}
			}
			return;
		}
		if (findFood(block, kind) < 0) {
			return;
		}
		if (adults.size() >= 2) {
			Animal first = null;
			Animal second = null;
			for (Animal animal : adults) {
				if (animal.getAge() != 0 || animal.isInLove()) {
					continue;
				}
				if (first == null) {
					first = animal;
				} else {
					second = animal;
					break;
				}
			}
			if (first != null && second != null) {
				first.setInLove(null);
				second.setInLove(null);
				block.removeItem(findFood(block, kind), 1);
				WorkerAdvancements.grant(worker, WorkerActions.PEN_BRED);
			}
		}
		if (adults.isEmpty() || findFood(block, kind) < 0) {
			return;
		}
		if (kind == WorkerKind.SHEEP) {
			shearSheep(level, origin, block, worker);
			return;
		}
		ItemStack product = new ItemStack(product(kind));
		if (WorkerSupport.canFitOutput(block, List.of(product))) {
			int food = findFood(block, kind);
			if (food >= 0) {
				WorkerSupport.insertOutput(block, List.of(product));
				WorkerAdvancements.grant(worker, WorkerActions.penProduct(kind));
				block.removeItem(food, 1);
			}
		}
	}

	private static void shearSheep(ServerLevel level, BlockPos origin, WorkerBlockEntity block, HumanWorkerEntity worker) {
		if (findFood(block, WorkerKind.SHEEP) < 0) {
			return;
		}
		Sheep sheep = null;
		double best = Double.MAX_VALUE;
		AABB box = area(origin, block);
		for (Sheep candidate : level.getEntitiesOfClass(Sheep.class, box, animal -> animal.isAlive() && !animal.isBaby() && animal.readyForShearing())) {
			double distance = candidate.distanceToSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
			if (distance < best) {
				best = distance;
				sheep = candidate;
			}
		}
		if (sheep == null) {
			return;
		}
		ItemStack stack = new ItemStack(woolItem(sheep.getColor()), 1 + level.getRandom().nextInt(3));
		if (!WorkerSupport.canFitOutput(block, List.of(stack))) {
			return;
		}
		int food = findFood(block, WorkerKind.SHEEP);
		if (food < 0) {
			return;
		}
		sheep.setSheared(true);
		level.playSound(null, sheep.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
		if (worker != null) {
			worker.playWorkSwing(sheep.getX(), sheep.getEyeY(), sheep.getZ());
		}
		WorkerSupport.insertOutput(block, List.of(stack));
		WorkerAdvancements.grant(worker, WorkerActions.SHEEP_SHEARED);
		WorkerAdvancements.grant(worker, WorkerActions.PEN_WOOL);
		block.removeItem(food, 1);
	}

	private static Item woolItem(DyeColor color) {
		return switch (color) {
			case WHITE -> Items.WHITE_WOOL;
			case ORANGE -> Items.ORANGE_WOOL;
			case MAGENTA -> Items.MAGENTA_WOOL;
			case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
			case YELLOW -> Items.YELLOW_WOOL;
			case LIME -> Items.LIME_WOOL;
			case PINK -> Items.PINK_WOOL;
			case GRAY -> Items.GRAY_WOOL;
			case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
			case CYAN -> Items.CYAN_WOOL;
			case PURPLE -> Items.PURPLE_WOOL;
			case BLUE -> Items.BLUE_WOOL;
			case BROWN -> Items.BROWN_WOOL;
			case GREEN -> Items.GREEN_WOOL;
			case RED -> Items.RED_WOOL;
			case BLACK -> Items.BLACK_WOOL;
		};
	}

	private static void keepFreshDrops(ServerLevel level, WorkerBlockEntity block, Animal animal) {
		AABB box = animal.getBoundingBox().inflate(1.0);
		List<ItemEntity> fresh = level.getEntitiesOfClass(ItemEntity.class, box, entity -> entity.isAlive() && entity.tickCount == 0);
		if (fresh.isEmpty()) {
			return;
		}
		List<ItemStack> drops = new ArrayList<>();
		for (ItemEntity entity : fresh) {
			if (!entity.getItem().isEmpty()) {
				drops.add(entity.getItem().copy());
			}
		}
		if (drops.isEmpty() || !WorkerSupport.canFitOutput(block, drops)) {
			return;
		}
		WorkerSupport.insertOutput(block, drops);
		for (ItemEntity entity : fresh) {
			entity.discard();
		}
	}

	private static AABB area(BlockPos origin, WorkerBlockEntity block) {
		return WorkArea.volume(origin, WorkArea.box(block.kind(), block.workRadius()));
	}

	private static List<Animal> adults(ServerLevel level, BlockPos origin, Class<? extends Animal> type, WorkerBlockEntity block) {
		AABB box = area(origin, block);
		List<Animal> adults = new ArrayList<>();
		for (Animal animal : level.getEntitiesOfClass(type, box, entity -> entity.isAlive() && !entity.isBaby())) {
			adults.add(animal);
		}
		return adults;
	}

	private static int findFood(WorkerBlockEntity block, WorkerKind kind) {
		TagKey<Item> tag = foodTag(kind);
		for (int slot = 0; slot < block.getContainerSize(); slot++) {
			if (block.getItem(slot).is(tag)) {
				return slot;
			}
		}
		return -1;
	}

	private static TagKey<Item> foodTag(WorkerKind kind) {
		return switch (kind) {
			case CHICKEN -> ItemTags.CHICKEN_FOOD;
			case COW -> ItemTags.COW_FOOD;
			case SHEEP -> ItemTags.SHEEP_FOOD;
			case PIG -> ItemTags.PIG_FOOD;
			default -> ItemTags.PIG_FOOD;
		};
	}

	private static Item product(WorkerKind kind) {
		return switch (kind) {
			case CHICKEN -> Items.FEATHER;
			case COW -> Items.LEATHER;
			case SHEEP -> Items.AIR;
			case PIG -> Items.PORKCHOP;
			default -> Items.AIR;
		};
	}

	private static Class<? extends Animal> animalClass(WorkerKind kind) {
		return switch (kind) {
			case CHICKEN -> Chicken.class;
			case COW -> Cow.class;
			case SHEEP -> Sheep.class;
			case PIG -> Pig.class;
			default -> Pig.class;
		};
	}

	private static EntityType<HumanWorkerEntity> workerType(WorkerKind kind) {
		return switch (kind) {
			case CHICKEN -> ModEntities.HUMAN_CHICKEN_BREEDER;
			case COW -> ModEntities.HUMAN_COW_BREEDER;
			case SHEEP -> ModEntities.HUMAN_SHEEP_BREEDER;
			case PIG -> ModEntities.HUMAN_PIG_BREEDER;
			default -> null;
		};
	}
}
