package com.nomosiki.advancement;

import com.nomosiki.squad.SquadType;

import java.util.Locale;

public final class NomosikiActions {
	public static final String FACTION_CREATED = "faction_created";
	public static final String FACTION_JOINED = "faction_joined";
	public static final String HIRED = "hired";
	public static final String FOLLOW = "follow";
	public static final String EQUIP = "equip";
	public static final String ORDER = "order";
	public static final String FORMATION = "formation";
	public static final String STANCE = "stance";
	public static final String WAYPOINT = "waypoint";
	public static final String BOMBARD = "bombard";
	public static final String ATTACK_ORDER = "attack_order";
	public static final String FIRST_BLOOD = "first_blood";
	public static final String KILLS_25 = "kills_25";
	public static final String KILLS_100 = "kills_100";
	public static final String COMPANY = "company";
	public static final String ARMY = "army";
	public static final String FOUR_ROLES = "four_roles";
	public static final String ALLIANCE = "alliance";
	public static final String WAR = "war";
	public static final String PEACE = "peace";
	public static final String DUEL = "duel_won";
	public static final String DUEL_FIVE = "duel_five";
	public static final String GIVE_SQUAD = "give_squad";
	public static final String RAID = "raid";
	public static final String CASCADE = "cascade";
	public static final String NINJA_MARK = "ninja_mark";
	public static final String SIEGE = "siege";
	public static final String HELM = "helm";
	public static final String SIXTEEN = "sixteen";

	public static final int COMPANY_SIZE = 48;
	public static final int KILL_GOAL = 25;
	public static final int KILL_CHALLENGE = 100;
	public static final int DUEL_CHALLENGE = 5;
	public static final int SIXTEEN_SQUADS = 16;

	private NomosikiActions() {
	}

	public static String hired(SquadType type) {
		return "hired_" + type.name().toLowerCase(Locale.ROOT);
	}
}
