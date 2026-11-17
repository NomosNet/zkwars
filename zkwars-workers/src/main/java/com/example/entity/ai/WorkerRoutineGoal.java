package com.example.entity.ai;

import com.example.entity.HumanWorkerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class WorkerRoutineGoal extends Goal {
	private final HumanWorkerEntity worker;

	public WorkerRoutineGoal(HumanWorkerEntity worker) {
		this.worker = worker;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return this.worker.isAlive() && this.worker.followsSchedule();
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void tick() {
		this.worker.tickSchedule();
	}

	@Override
	public void stop() {
		this.worker.getNavigation().stop();
	}
}
