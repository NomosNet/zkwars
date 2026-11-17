package com.nomosiki.squad;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class SquadWaypointQueue {
	private final Deque<BlockPos> points = new ArrayDeque<>();

	public boolean enqueue(BlockPos pos, int maxPoints) {
		if (this.points.size() >= maxPoints) {
			return false;
		}
		this.points.addLast(pos.immutable());
		return true;
	}

	public void replaceAll(List<BlockPos> newPoints, int maxPoints) {
		this.points.clear();
		int n = Math.min(newPoints.size(), maxPoints);
		for (int i = 0; i < n; i++) {
			this.points.addLast(newPoints.get(i).immutable());
		}
	}

	public void clear() {
		this.points.clear();
	}

	public Optional<BlockPos> current() {
		return this.points.isEmpty() ? Optional.empty() : Optional.of(this.points.peekFirst());
	}

	public List<BlockPos> snapshot() {
		return List.copyOf(this.points);
	}

	public boolean advance() {
		if (this.points.isEmpty()) {
			return false;
		}
		this.points.removeFirst();
		return true;
	}

	public boolean isEmpty() {
		return this.points.isEmpty();
	}

	public int size() {
		return this.points.size();
	}
}
