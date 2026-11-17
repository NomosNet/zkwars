package com.nomosiki.network.faction.payload.c2s;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FactionAllianceActionPayload(byte actionId, String targetFactionName) implements CustomPacketPayload {
	public static final byte REQUEST = 0;
	public static final byte ACCEPT = 1;
	public static final byte DECLINE = 2;
	public static final byte CANCEL = 3;
	public static final byte BREAK = 4;
	public static final byte REQUEST_MILITARY = 5;
	public static final byte UPGRADE_TO_MILITARY = 6;
	public static final byte DOWNGRADE_TO_REGULAR = 7;
	public static final byte JOIN_WAR = 8;
	public static final byte DECLINE_WAR_JOIN = 9;
	public static final byte WAR_SIDE_SUPPORT_AGGRESSOR = 10;
	public static final byte WAR_SIDE_SUPPORT_DEFENDER = 11;
	public static final byte WAR_SIDE_STAY_NEUTRAL = 12;
	public static final byte REQUEST_PEACE = 13;
	public static final byte ACCEPT_PEACE = 14;
	public static final byte DECLINE_PEACE = 15;
	public static final byte CANCEL_PEACE = 16;

	public static final Type<FactionAllianceActionPayload> TYPE = new Type<>(NomosikiMod.id("faction_alliance_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionAllianceActionPayload> CODEC = StreamCodec.of(
		FactionAllianceActionPayload::encode,
		FactionAllianceActionPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, FactionAllianceActionPayload payload) {
		buf.writeByte(payload.actionId);
		buf.writeUtf(payload.targetFactionName, 32);
	}

	private static FactionAllianceActionPayload decode(RegistryFriendlyByteBuf buf) {
		return new FactionAllianceActionPayload(buf.readByte(), buf.readUtf(32));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
