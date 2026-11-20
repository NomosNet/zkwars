package com.nomosiki.match;

import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.squad.NomosikSquad;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Squad wargear bought once with authority. One purchase per slot; a second weapon in the same slot is refused.
 */
public final class MatchWargear {
	public enum Slot {
		WEAPON,
		ARMOR
	}

	public record Upgrade(
		String id,
		MatchRecruitment.Role role,
		Slot slot,
		int cost,
		Item item,
		EquipmentSlot equipment
	) {
		public ItemStack stack() {
			return new ItemStack(this.item);
		}

		public int priced(NomosikRace race) {
			double mult = switch (race) {
				case ORC -> 0.75;
				case DWARF -> 1.3;
				default -> 1.0;
			};
			int raw = (int) Math.round(this.cost * mult);
			return Math.max(5, ((raw + 2) / 5) * 5);
		}
	}

	private static final List<Upgrade> ALL = List.of(
		upgrade("infantry_sword", MatchRecruitment.Role.INFANTRY, Slot.WEAPON, 40, Items.IRON_SWORD, EquipmentSlot.MAINHAND),
		upgrade("infantry_axe", MatchRecruitment.Role.INFANTRY, Slot.WEAPON, 40, Items.IRON_AXE, EquipmentSlot.MAINHAND),
		upgrade("infantry_mail", MatchRecruitment.Role.INFANTRY, Slot.ARMOR, 50, Items.IRON_CHESTPLATE, EquipmentSlot.CHEST),
		upgrade("anti_armor_axe", MatchRecruitment.Role.ANTI_ARMOR, Slot.WEAPON, 50, Items.IRON_AXE, EquipmentSlot.MAINHAND),
		upgrade("anti_armor_mail", MatchRecruitment.Role.ANTI_ARMOR, Slot.ARMOR, 50, Items.IRON_CHESTPLATE, EquipmentSlot.CHEST),
		upgrade("archer_bow", MatchRecruitment.Role.ARCHER, Slot.WEAPON, 40, Items.BOW, EquipmentSlot.MAINHAND),
		upgrade("archer_mail", MatchRecruitment.Role.ARCHER, Slot.ARMOR, 40, Items.CHAINMAIL_CHESTPLATE, EquipmentSlot.CHEST),
		upgrade("cavalry_sword", MatchRecruitment.Role.CAVALRY, Slot.WEAPON, 50, Items.IRON_SWORD, EquipmentSlot.MAINHAND),
		upgrade("cavalry_mail", MatchRecruitment.Role.CAVALRY, Slot.ARMOR, 60, Items.IRON_CHESTPLATE, EquipmentSlot.CHEST),
		upgrade("siege_mail", MatchRecruitment.Role.SIEGE, Slot.ARMOR, 50, Items.IRON_CHESTPLATE, EquipmentSlot.CHEST),
		upgrade("support_sword", MatchRecruitment.Role.SUPPORT, Slot.WEAPON, 40, Items.IRON_SWORD, EquipmentSlot.MAINHAND),
		upgrade("support_mail", MatchRecruitment.Role.SUPPORT, Slot.ARMOR, 40, Items.GOLDEN_CHESTPLATE, EquipmentSlot.CHEST),
		upgrade("elite_sword", MatchRecruitment.Role.ELITE, Slot.WEAPON, 80, Items.DIAMOND_SWORD, EquipmentSlot.MAINHAND),
		upgrade("elite_mail", MatchRecruitment.Role.ELITE, Slot.ARMOR, 80, Items.DIAMOND_CHESTPLATE, EquipmentSlot.CHEST)
	);

	private MatchWargear() {
	}

	public static List<Upgrade> all() {
		return ALL;
	}

	public static List<Upgrade> forRole(MatchRecruitment.Role role) {
		if (role == null || role == MatchRecruitment.Role.WORKER) {
			return List.of();
		}
		List<Upgrade> out = new ArrayList<>();
		for (Upgrade upgrade : ALL) {
			if (upgrade.role == role) {
				out.add(upgrade);
			}
		}
		return out;
	}

	@Nullable
	public static Upgrade byId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		for (Upgrade upgrade : ALL) {
			if (upgrade.id.equals(id)) {
				return upgrade;
			}
		}
		return null;
	}

	public static boolean slotTaken(NomosikSquad squad, Upgrade upgrade) {
		for (Upgrade other : ALL) {
			if (other.slot == upgrade.slot && !other.id.equals(upgrade.id) && squad.hasWargear(other.id)) {
				return true;
			}
		}
		return false;
	}

	public static void equip(NomosikEntity entity, Upgrade upgrade) {
		if (entity == null || !entity.isAlive() || upgrade == null) {
			return;
		}
		entity.setItemSlot(upgrade.equipment, upgrade.stack());
	}

	public static void equipAll(NomosikSquad squad, List<NomosikEntity> members) {
		if (squad == null || members == null) {
			return;
		}
		for (Upgrade upgrade : ALL) {
			if (!squad.hasWargear(upgrade.id)) {
				continue;
			}
			for (NomosikEntity member : members) {
				equip(member, upgrade);
			}
		}
	}

	private static Upgrade upgrade(
		String id,
		MatchRecruitment.Role role,
		Slot slot,
		int cost,
		Item item,
		EquipmentSlot equipment
	) {
		return new Upgrade(id, role, slot, cost, item, equipment);
	}
}
