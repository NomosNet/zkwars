package com.nomosiki.entity;

import com.nomosiki.squad.SquadType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class SamuraiEntity extends NomosikEntity {
	public SamuraiEntity(EntityType<? extends SamuraiEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return NomosikEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 60.0);
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.SAMURAI;
	}

	@Override
	public NomosikCulture getUnitCulture() {
		return NomosikCulture.SAMURAI;
	}
}
