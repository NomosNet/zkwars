package com.nomosiki.entity.ai;

import com.nomosiki.entity.SquadOrder;
import com.nomosiki.entity.NomosikEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class NomosikMoveToBlockGoal extends Goal {
	private static final double ARRIVED_SQR = 10.0;
	private final NomosikEntity nomosik;
	private final ApproachRoute route = new ApproachRoute();

	public NomosikMoveToBlockGoal(NomosikEntity nomosik) {
		this.nomosik = nomosik;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		return this.nomosik.getSquadOrder() == SquadOrder.MOVE_TO
			&& this.nomosik.getSquadAnchorPos() != null
			&& this.nomosik.getSquadFormationTarget() == null;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.nomosik.isRidingHeldSeat()) {
			return false;
		}
		if (this.nomosik.getSquadOrder() != SquadOrder.MOVE_TO) {
			return false;
		}
		@Nullable BlockPos anchor = this.nomosik.getSquadAnchorPos();
		if (anchor == null) {
			return false;
		}
		return this.nomosik.distanceToSqr(Vec3.atBottomCenterOf(anchor)) > ARRIVED_SQR;
	}

	@Override
	public void start() {
		this.route.reset(this.nomosik);
	}

	@Override
	public void tick() {
		BlockPos anchor = this.nomosik.getSquadAnchorPos();
		if (anchor == null) {
			return;
		}
		this.route.tick(this.nomosik, Vec3.atBottomCenterOf(anchor), 1.12);
	}

	@Override
	public void stop() {
		this.route.reset(this.nomosik);
		if (this.nomosik.getSquadOrder() == SquadOrder.MOVE_TO) {
			BlockPos anchor = this.nomosik.getSquadAnchorPos();
			if (anchor != null) {
				this.nomosik.applySquadOrder(SquadOrder.HOLD, anchor, this.nomosik.getSquadZoneRadiusForGui());
			}
		}
	}
}
