package com.nomosiki.network.match.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MatchHudPayload(
	byte phase,
	int remainingSeconds,
	int authority,
	int income,
	int ownPoints,
	int totalPoints,
	float ownHqHp,
	float ownHqMaxHp,
	float enemyHqHp,
	float enemyHqMaxHp,
	byte endReason,
	byte winnerSide,
	byte yourSide,
	int pop,
	int popCap,
	boolean showEnd,
	byte raceId
) implements CustomPacketPayload {
	public static final Type<MatchHudPayload> TYPE = new Type<>(NomosikiMod.id("match_hud"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MatchHudPayload> CODEC = StreamCodec.of(
		MatchHudPayload::encode,
		MatchHudPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, MatchHudPayload p) {
		buf.writeByte(p.phase);
		buf.writeVarInt(p.remainingSeconds);
		buf.writeVarInt(p.authority);
		buf.writeVarInt(p.income);
		buf.writeVarInt(p.ownPoints);
		buf.writeVarInt(p.totalPoints);
		buf.writeFloat(p.ownHqHp);
		buf.writeFloat(p.ownHqMaxHp);
		buf.writeFloat(p.enemyHqHp);
		buf.writeFloat(p.enemyHqMaxHp);
		buf.writeByte(p.endReason);
		buf.writeByte(p.winnerSide);
		buf.writeByte(p.yourSide);
		buf.writeVarInt(p.pop);
		buf.writeVarInt(p.popCap);
		buf.writeBoolean(p.showEnd);
		buf.writeByte(p.raceId);
	}

	private static MatchHudPayload decode(RegistryFriendlyByteBuf buf) {
		return new MatchHudPayload(
			buf.readByte(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readByte(),
			buf.readByte(),
			buf.readByte(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readBoolean(),
			buf.readByte()
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
