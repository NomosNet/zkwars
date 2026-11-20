package com.nomosiki.network.squad.payload.s2c;

import com.nomosiki.NomosikiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SquadArmyStatePayload(Optional<UUID> activeSquadId, List<SquadArmyStatePayload.SquadRow> squads, int tickId)
	implements CustomPacketPayload {

	public record SquadRow(
		UUID squadId,
		byte typeId,
		byte iconId,
		String name,
		int alive,
		int total,
		float avgHealthPct,
		byte orderId,
		byte combatModeId,
		byte formationId,
		float formationSpacing,
		int formationMaxInRow,
		double centerX,
		double centerY,
		double centerZ,
		double distanceToPlayer,
		byte rangedFireModeId,
		boolean shieldStance,
		List<BlockPos> waypoints,
		boolean hasFireZone,
		BlockPos fireAnchor,
		int fireRadius,
		String matchRole,
		List<String> wargearIds
	) {
	}

	private static final int MAX_WAYPOINTS = 8;

	public static final CustomPacketPayload.Type<SquadArmyStatePayload> TYPE =
		new CustomPacketPayload.Type<>(NomosikiMod.id("squad_army_state"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SquadArmyStatePayload> CODEC = StreamCodec.of(
		SquadArmyStatePayload::encode,
		SquadArmyStatePayload::decode
	);

	private static void encode(RegistryFriendlyByteBuf buf, SquadArmyStatePayload p) {
		buf.writeBoolean(p.activeSquadId.isPresent());
		p.activeSquadId.ifPresent(buf::writeUUID);
		buf.writeVarInt(p.squads.size());
		for (SquadRow row : p.squads) {
			buf.writeUUID(row.squadId());
			buf.writeByte(row.typeId());
			buf.writeByte(row.iconId());
			buf.writeUtf(row.name(), 32);
			buf.writeVarInt(row.alive());
			buf.writeVarInt(row.total());
			buf.writeFloat(row.avgHealthPct());
			buf.writeByte(row.orderId());
			buf.writeByte(row.combatModeId());
			buf.writeByte(row.formationId());
			buf.writeFloat(row.formationSpacing());
			buf.writeVarInt(row.formationMaxInRow());
			buf.writeDouble(row.centerX());
			buf.writeDouble(row.centerY());
			buf.writeDouble(row.centerZ());
			buf.writeDouble(row.distanceToPlayer());
			buf.writeByte(row.rangedFireModeId());
			buf.writeBoolean(row.shieldStance());
			buf.writeBoolean(row.hasFireZone());
			BlockPos.STREAM_CODEC.encode(buf, row.fireAnchor());
			buf.writeVarInt(row.fireRadius());
			int waypointCount = Math.min(row.waypoints().size(), MAX_WAYPOINTS);
			buf.writeVarInt(waypointCount);
			for (int i = 0; i < waypointCount; i++) {
				BlockPos.STREAM_CODEC.encode(buf, row.waypoints().get(i));
			}
			buf.writeUtf(row.matchRole() == null ? "" : row.matchRole(), 32);
			int gearCount = Math.min(row.wargearIds().size(), 8);
			buf.writeVarInt(gearCount);
			for (int g = 0; g < gearCount; g++) {
				buf.writeUtf(row.wargearIds().get(g), 32);
			}
		}
		buf.writeVarInt(p.tickId);
	}

	private static SquadArmyStatePayload decode(RegistryFriendlyByteBuf buf) {
		Optional<UUID> active = buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty();
		int n = Math.min(buf.readVarInt(), 16);
		List<SquadRow> rows = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			UUID squadId = buf.readUUID();
			byte typeId = buf.readByte();
			byte iconId = buf.readByte();
			String name = buf.readUtf(32);
			int alive = buf.readVarInt();
			int total = buf.readVarInt();
			float avgHealthPct = buf.readFloat();
			byte orderId = buf.readByte();
			byte combatModeId = buf.readByte();
			byte formationId = buf.readByte();
			float formationSpacing = buf.readFloat();
			int formationMaxInRow = buf.readVarInt();
			double centerX = buf.readDouble();
			double centerY = buf.readDouble();
			double centerZ = buf.readDouble();
			double distanceToPlayer = buf.readDouble();
			byte rangedFireModeId = buf.readByte();
			boolean shieldStance = buf.readBoolean();
			boolean hasFireZone = buf.readBoolean();
			BlockPos fireAnchor = BlockPos.STREAM_CODEC.decode(buf);
			int fireRadius = buf.readVarInt();
			int waypointCount = Math.min(buf.readVarInt(), MAX_WAYPOINTS);
			List<BlockPos> parsed = new ArrayList<>(waypointCount);
			for (int w = 0; w < waypointCount; w++) {
				parsed.add(BlockPos.STREAM_CODEC.decode(buf));
			}
			List<BlockPos> waypoints = List.copyOf(parsed);
			String matchRole = buf.readUtf(32);
			int gearCount = Math.min(buf.readVarInt(), 8);
			List<String> wargearIds = new ArrayList<>(gearCount);
			for (int g = 0; g < gearCount; g++) {
				wargearIds.add(buf.readUtf(32));
			}
			rows.add(new SquadRow(
				squadId, typeId, iconId, name, alive, total, avgHealthPct, orderId, combatModeId, formationId,
				formationSpacing, formationMaxInRow, centerX, centerY, centerZ, distanceToPlayer, rangedFireModeId, shieldStance, waypoints,
				hasFireZone, fireAnchor, fireRadius, matchRole, List.copyOf(wargearIds)
			));
		}
		int tickId = buf.readVarInt();
		return new SquadArmyStatePayload(active, List.copyOf(rows), tickId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
