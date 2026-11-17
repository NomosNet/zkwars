package com.example.block;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum WorkerKind {
	QUARRY,
	FARMER,
	LUMBERJACK,
	CHICKEN,
	COW,
	SHEEP,
	PIG,
	BUILDER,
	SMELTER,
	FISHER,
	CRAFTER;

	public static final StreamCodec<RegistryFriendlyByteBuf, WorkerKind> STREAM_CODEC = StreamCodec.of(
			RegistryFriendlyByteBuf::writeEnum,
			buffer -> buffer.readEnum(WorkerKind.class)
	);

	public boolean restrictsInput() {
		return this == FARMER || this == CHICKEN || this == COW || this == SHEEP || this == PIG || this == SMELTER || this == CRAFTER;
	}

	public boolean editableRadius() {
		return this == LUMBERJACK || this == CHICKEN || this == COW || this == SHEEP || this == PIG || this == FISHER;
	}

	public boolean isPen() {
		return this == CHICKEN || this == COW || this == SHEEP || this == PIG;
	}
}
