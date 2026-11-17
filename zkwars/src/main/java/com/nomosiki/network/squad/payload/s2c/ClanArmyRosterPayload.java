package com.nomosiki.network.squad.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ClanArmyRosterPayload(boolean admin, boolean canTransfer, List<Member> members) implements CustomPacketPayload {
	public record Member(UUID id, String name, boolean online) {
	}

	public static final CustomPacketPayload.Type<ClanArmyRosterPayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("clan_army_roster"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClanArmyRosterPayload> CODEC = StreamCodec.of(
		ClanArmyRosterPayload::encode,
		ClanArmyRosterPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, ClanArmyRosterPayload payload) {
		buf.writeBoolean(payload.admin);
		buf.writeBoolean(payload.canTransfer);
		buf.writeVarInt(payload.members.size());
		for (Member member : payload.members) {
			buf.writeUUID(member.id);
			buf.writeUtf(member.name, 64);
			buf.writeBoolean(member.online);
		}
	}

	private static ClanArmyRosterPayload decode(RegistryFriendlyByteBuf buf) {
		boolean admin = buf.readBoolean();
		boolean canTransfer = buf.readBoolean();
		int count = buf.readVarInt();
		List<Member> members = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			members.add(new Member(buf.readUUID(), buf.readUtf(64), buf.readBoolean()));
		}
		return new ClanArmyRosterPayload(admin, canTransfer, List.copyOf(members));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
