package com.nomosiki.match;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MatchBuildOrder {
	public final byte side;
	public final MatchBuildingCatalog.Kind kind;
	public final BlockPos site;
	public final int paidAuthority;
	@Nullable
	public UUID workerId;

	public MatchBuildOrder(byte side, MatchBuildingCatalog.Kind kind, BlockPos site, int paidAuthority) {
		this.side = side;
		this.kind = kind;
		this.site = site.immutable();
		this.paidAuthority = paidAuthority;
	}
}
