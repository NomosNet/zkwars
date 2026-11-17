package com.nomosiki.network.faction.payload.s2c;

import com.nomosiki.NomosikiMod;
import com.nomosiki.faction.FactionManager;
import com.nomosiki.network.faction.FactionDiplomacyRow;
import com.nomosiki.network.faction.FactionRelationEdge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record FactionScreenDataPayload(
	String factionName,
	String colorId,
	ItemStack myBanner,
	boolean isOwner,
	List<String> members,
	List<FactionManager.InviteView> invites,
	List<FactionDiplomacyRow> diplomacy,
	List<FactionRelationEdge> relationEdges
) implements CustomPacketPayload {
	public static final Type<FactionScreenDataPayload> TYPE = new Type<>(NomosikiMod.id("faction_screen_data"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FactionScreenDataPayload> CODEC = StreamCodec.of(
		FactionScreenDataPayload::encode,
		FactionScreenDataPayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, FactionScreenDataPayload payload) {
		buf.writeUtf(payload.factionName, 32);
		buf.writeUtf(payload.colorId, 16);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.myBanner());
		buf.writeBoolean(payload.isOwner);
		buf.writeVarInt(payload.members.size());
		for (String member : payload.members) {
			buf.writeUtf(member, 32);
		}
		buf.writeVarInt(payload.invites.size());
		for (FactionManager.InviteView invite : payload.invites) {
			buf.writeUtf(invite.factionName(), 32);
			buf.writeUtf(invite.ownerName(), 32);
			buf.writeUtf(invite.colorId(), 16);
		}
		buf.writeVarInt(payload.diplomacy.size());
		for (FactionDiplomacyRow row : payload.diplomacy) {
			buf.writeUtf(row.factionName(), 32);
			buf.writeUtf(row.colorId(), 16);
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, row.banner());
			buf.writeByte(row.relationStatusId());
			buf.writeBoolean(row.alliancePendingIncoming());
			buf.writeBoolean(row.alliancePendingOutgoing());
			buf.writeByte(row.alliancePendingTierId());
			buf.writeBoolean(row.warJoinPendingIncoming());
			buf.writeBoolean(row.warSideChoicePendingIncoming());
			buf.writeUtf(row.warSideDefenderName(), 32);
			buf.writeVarInt(row.memberCount());
			buf.writeBoolean(row.peacePendingIncoming());
			buf.writeBoolean(row.peacePendingOutgoing());
		}
		buf.writeVarInt(payload.relationEdges.size());
		for (FactionRelationEdge edge : payload.relationEdges) {
			buf.writeUtf(edge.factionA(), 32);
			buf.writeUtf(edge.factionB(), 32);
			buf.writeByte(edge.relationStatusId());
		}
	}

	private static FactionScreenDataPayload decode(RegistryFriendlyByteBuf buf) {
		String factionName = buf.readUtf(32);
		String colorId = buf.readUtf(16);
		ItemStack myBanner = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
		boolean isOwner = buf.readBoolean();
		int membersSize = buf.readVarInt();
		List<String> members = new ArrayList<>(Math.min(membersSize, 256));
		for (int i = 0; i < membersSize; i++) {
			members.add(buf.readUtf(32));
		}
		int invitesSize = buf.readVarInt();
		List<FactionManager.InviteView> invites = new ArrayList<>(Math.min(invitesSize, 256));
		for (int i = 0; i < invitesSize; i++) {
			invites.add(new FactionManager.InviteView(
				buf.readUtf(32),
				buf.readUtf(32),
				buf.readUtf(16)
			));
		}
		int diplomacySize = buf.readVarInt();
		List<FactionDiplomacyRow> diplomacy = new ArrayList<>(Math.min(diplomacySize, 256));
		for (int i = 0; i < diplomacySize; i++) {
			diplomacy.add(new FactionDiplomacyRow(
				buf.readUtf(32),
				buf.readUtf(16),
				ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
				buf.readByte(),
				buf.readBoolean(),
				buf.readBoolean(),
				buf.readByte(),
				buf.readBoolean(),
				buf.readBoolean(),
				buf.readUtf(32),
				buf.readVarInt(),
				buf.readBoolean(),
				buf.readBoolean()
			));
		}
		int edgeSize = buf.readVarInt();
		List<FactionRelationEdge> relationEdges = new ArrayList<>(Math.min(edgeSize, 512));
		for (int i = 0; i < edgeSize; i++) {
			relationEdges.add(new FactionRelationEdge(
				buf.readUtf(32),
				buf.readUtf(32),
				buf.readByte()
			));
		}
		return new FactionScreenDataPayload(
			factionName,
			colorId,
			myBanner,
			isOwner,
			List.copyOf(members),
			List.copyOf(invites),
			List.copyOf(diplomacy),
			List.copyOf(relationEdges)
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
