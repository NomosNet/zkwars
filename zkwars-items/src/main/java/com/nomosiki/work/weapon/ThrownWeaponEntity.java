package com.nomosiki.work.weapon;

import com.nomosiki.work.NomosikiWork;
import com.nomosiki.work.registry.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownWeaponEntity extends ThrowableItemProjectile {
	public static final int THROW_COOLDOWN = 20;
	public static final float THROW_VELOCITY = 1.5F;
	public static final float THROW_INACCURACY = 1.0F;

	public static EntityType<ThrownWeaponEntity> ENTITY_TYPE;

	private float damage;

	public static void register() {
		EntityType.Builder<ThrownWeaponEntity> builder = EntityType.Builder.<ThrownWeaponEntity>of(
				ThrownWeaponEntity::new,
				MobCategory.MISC)
			.sized(0.25F, 0.25F)
			.clientTrackingRange(4)
			.updateInterval(10);
		ENTITY_TYPE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			NomosikiWork.id("thrown_weapon"),
			builder.build("thrown_weapon"));
	}

	public static void throwFrom(Player player, ItemStack stack, float damage) {
		Level level = player.level();
		level.playSound(
			null,
			player.getX(),
			player.getY(),
			player.getZ(),
			SoundEvents.SNOWBALL_THROW,
			SoundSource.NEUTRAL,
			0.5F,
			0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		player.getCooldowns().addCooldown(stack.getItem(), THROW_COOLDOWN);
		if (!level.isClientSide) {
			ThrownWeaponEntity projectile = new ThrownWeaponEntity(level, player);
			projectile.setItem(stack);
			projectile.damage = damage;
			projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_VELOCITY, THROW_INACCURACY);
			level.addFreshEntity(projectile);
		}
		player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		stack.consume(1, player);
	}

	public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> entityType, Level level) {
		super(entityType, level);
	}

	public ThrownWeaponEntity(Level level, LivingEntity owner) {
		super(ENTITY_TYPE, owner, level);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.SHURIKEN;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		Entity target = result.getEntity();
		target.hurt(damageSources().thrown(this, getOwner()), damage);
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!level().isClientSide) {
			discard();
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putFloat("damage", damage);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		damage = tag.getFloat("damage");
	}
}
