package com.nomosiki.entity;

import com.nomosiki.squad.SquadType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class AshigaruArtilleristEntity extends ArtilleristEntity {
	public AshigaruArtilleristEntity(EntityType<? extends AshigaruArtilleristEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return NomosikEntity.createAttributes()
			.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	public SquadType getSquadBranchType() {
		return SquadType.ARTILLERIST;
	}

	@Override
	public NomosikCulture getUnitCulture() {
		return NomosikCulture.ASHIGARU;
	}
}
