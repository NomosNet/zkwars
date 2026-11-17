package com.nomosiki.work.gun;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class DeferredDamage {
	private static final Map<Entity, Entry> ENTRIES = new HashMap<>();

	private DeferredDamage() {
	}

	public static void hurt(Entity target, DamageSource source, float damage, float igniteSeconds) {
		Entry entry = ENTRIES.get(target);
		if (entry == null) {
			entry = new Entry();
			ENTRIES.put(target, entry);
		}
		entry.source = source;
		entry.damage += damage;
		entry.igniteSeconds = Math.min(BulletEntity.IGNITE_SECONDS, entry.igniteSeconds + igniteSeconds);
	}

	public static void apply() {
		ENTRIES.forEach((target, entry) -> {
			target.invulnerableTime = 0;
			target.hurt(entry.source, entry.damage);
			if (entry.igniteSeconds > 0) {
				target.igniteForSeconds(entry.igniteSeconds);
			}
		});
		ENTRIES.clear();
	}

	private static final class Entry {
		DamageSource source;
		float damage;
		float igniteSeconds;
	}
}
