package com.example.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class WorkerActionTrigger extends SimpleCriterionTrigger<WorkerActionTrigger.Instance> {
	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, String action) {
		this.trigger(player, instance -> instance.action.equals(action));
	}

	public record Instance(Optional<ContextAwarePredicate> player, String action) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
				Codec.STRING.fieldOf("action").forGetter(Instance::action)
		).apply(instance, Instance::new));
	}
}
