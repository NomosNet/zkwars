package com.nomosiki.network.faction;

import com.nomosiki.faction.FactionAllianceTier;
import com.nomosiki.faction.FactionRelationStatus;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

public record FactionDiplomacyRow(
	String factionName,
	String colorId,
	ItemStack banner,
	byte relationStatusId,
	boolean alliancePendingIncoming,
	boolean alliancePendingOutgoing,
	byte alliancePendingTierId,
	boolean warJoinPendingIncoming,
	boolean warSideChoicePendingIncoming,
	String warSideDefenderName,
	int memberCount,
	boolean peacePendingIncoming,
	boolean peacePendingOutgoing
) {
	public FactionDiplomacyRow(
		String factionName,
		String colorId,
		ItemStack banner,
		byte relationStatusId,
		boolean alliancePendingIncoming,
		boolean alliancePendingOutgoing,
		int memberCount
	) {
		this(factionName, colorId, banner, relationStatusId, alliancePendingIncoming, alliancePendingOutgoing,
			FactionAllianceTier.REGULAR.id(), false, false, "", memberCount, false, false);
	}

	public FactionDiplomacyRow(
		String factionName,
		String colorId,
		ItemStack banner,
		byte relationStatusId,
		boolean alliancePendingIncoming,
		boolean alliancePendingOutgoing,
		byte alliancePendingTierId,
		boolean warJoinPendingIncoming,
		boolean warSideChoicePendingIncoming,
		String warSideDefenderName,
		int memberCount,
		boolean peacePendingIncoming,
		boolean peacePendingOutgoing
	) {
		this.factionName = factionName;
		this.colorId = colorId;
		this.banner = banner == null || banner.isEmpty() ? ItemStack.EMPTY : banner.copy();
		this.relationStatusId = relationStatusId;
		this.alliancePendingIncoming = alliancePendingIncoming;
		this.alliancePendingOutgoing = alliancePendingOutgoing;
		this.alliancePendingTierId = alliancePendingTierId;
		this.warJoinPendingIncoming = warJoinPendingIncoming;
		this.warSideChoicePendingIncoming = warSideChoicePendingIncoming;
		this.warSideDefenderName = warSideDefenderName;
		this.memberCount = Math.max(0, memberCount);
		this.peacePendingIncoming = peacePendingIncoming;
		this.peacePendingOutgoing = peacePendingOutgoing;
	}

	public boolean hasBanner() {
		return !this.banner.isEmpty() && this.banner.getItem() instanceof BannerItem;
	}

	public FactionRelationStatus relationStatus() {
		return FactionRelationStatus.fromId(this.relationStatusId);
	}

	public FactionAllianceTier alliancePendingTier() {
		return FactionAllianceTier.fromId(this.alliancePendingTierId);
	}

	public boolean atWar() {
		return this.relationStatus() == FactionRelationStatus.AT_WAR;
	}
}
