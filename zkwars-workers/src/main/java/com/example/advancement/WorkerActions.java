package com.example.advancement;

import com.example.block.WorkerKind;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class WorkerActions {
	public static final String WORK_BLOCK_SET = "work_block_set";
	public static final String BED_SET = "bed_set";
	public static final String ASSIGNED = "assigned";
	public static final String INSPECT = "inspect";
	public static final String TOOL = "tool";
	public static final String WORKING = "working";
	public static final String SLEEPING = "sleeping";
	public static final String OUTPUT_TAKEN = "output_taken";
	public static final String COURIER_ROUTE = "courier_route";
	public static final String COURIER_DELIVERED = "courier_delivered";
	public static final String SUPPLY_DELIVERED = "supply_delivered";
	public static final String COURIER_FIVE = "courier_five";
	public static final String QUARRY_APPLIED = "quarry_applied";
	public static final String QUARRY_MINED = "quarry_mined";
	public static final String QUARRY_BOTTOM = "quarry_bottom";
	public static final String FARM_TILLED = "farm_tilled";
	public static final String FARM_HARVEST = "farm_harvest";
	public static final String IDLE = "idle";
	public static final String TREE_FELLED = "tree_felled";
	public static final String SAPLING_PLANTED = "sapling_planted";
	public static final String SHEARS = "shears";
	public static final String PEN_BRED = "pen_bred";
	public static final String PEN_FEATHER = "pen_feather";
	public static final String PEN_LEATHER = "pen_leather";
	public static final String PEN_WOOL = "pen_wool";
	public static final String PEN_PORK = "pen_pork";
	public static final String SHEEP_SHEARED = "sheep_sheared";
	public static final String PEN_CULLED = "pen_culled";
	public static final String FISH_CAUGHT = "fish_caught";
	public static final String FURNACE_ADDED = "furnace_added";
	public static final String SMELT_RETURNED = "smelt_returned";
	public static final String SMELTER_TEN = "smelter_ten";
	public static final String SCHEMATIC = "schematic_accepted";
	public static final String BUILDER_PLACED = "builder_placed";
	public static final String BUILDER_DONE = "builder_done";
	public static final String ASSIGNED_COURIER = "assigned_courier";
	public static final String CRAFTED = "crafted";
	public static final String SORT_ASSIGNED = "sort_assigned";
	public static final String SORT_DELIVERED = "sort_delivered";
	public static final String SORT_FULL = "sort_full";

	private WorkerActions() {
	}

	public static String assigned(WorkerKind kind) {
		return "assigned_" + kind.name().toLowerCase(Locale.ROOT);
	}

	public static @Nullable String penProduct(WorkerKind kind) {
		return switch (kind) {
			case CHICKEN -> PEN_FEATHER;
			case COW -> PEN_LEATHER;
			case SHEEP -> PEN_WOOL;
			case PIG -> PEN_PORK;
			default -> null;
		};
	}
}
