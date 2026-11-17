package com.nomosiki.entity;

import com.nomosiki.squad.SquadType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class MonkEntity extends NomosikEntity {
	public MonkEntity(EntityType<? extends MonkEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return NomosikEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 80.0);
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.MONK;
	}

	@Override
	public NomosikCulture getUnitCulture() {
		return NomosikCulture.MONK;
	}
}
