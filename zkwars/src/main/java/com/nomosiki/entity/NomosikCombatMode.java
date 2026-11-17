package com.nomosiki.entity;

/** Как воин выбирает цели самостоятельно (без приказа отряда через RTS/GUI). */
public enum NomosikCombatMode {
	/** Не вступает в бой сам: без автоцелей, без ответа на урон по себе и по владельцу. */
	PASSIVE,
	/**
	 * Ищет монстров, врагов фракции при войне и мобов, атакующих владельца (радиус ~24).
	 * Отвечает на урон по себе и по владельцу.
	 */
	NEUTRAL,
	/** Расширенный автопоиск в радиусе ~40: монстры, AT_WAR и NEUTRAL по дипломатии (§4.3). */
	AGGRESSIVE,
	/**
	 * Рейд: автопоиск в радиусе ~40; атакует всех живых, кроме {@link NomosikEntity#isAllyOrSelf}.
	 * При включении снимает удержание строя (как wander в Recruits).
	 */
	RAID;

	public static NomosikCombatMode fromId(byte id) {
		NomosikCombatMode[] values = values();
		if (id < 0 || id >= values.length) {
			return NEUTRAL;
		}
		return values[id];
	}

	public byte id() {
		return (byte) ordinal();
	}

	/** Периодический автопоиск целей (фаза A2, §4.2.2). */
	public boolean runsAutoTargetSearch() {
		return this == NEUTRAL || this == AGGRESSIVE || this == RAID;
	}
}
