package com.nomosiki.entity;

import com.nomosiki.squad.SquadType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class SamuraiArcherEntity extends ArcherEntity {
	public SamuraiArcherEntity(EntityType<? extends SamuraiArcherEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return ArcherEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 60.0);
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.SAMURAI_ARCHER;
	}

	@Override
	public NomosikCulture getUnitCulture() {
		return NomosikCulture.SAMURAI;
	}
}
