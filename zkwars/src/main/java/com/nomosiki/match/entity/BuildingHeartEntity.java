package com.nomosiki.match.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nomosiki.block.MatchShellBlock;
import com.nomosiki.config.NomosikiServerConfig;
import com.nomosiki.entity.AshigaruArtilleristEntity;
import com.nomosiki.match.BuildingHeartType;
import com.nomosiki.match.MatchService;
import com.nomosiki.match.MatchSession;
import com.nomosiki.match.schematic.LitematicaSchematic;
import com.nomosiki.match.schematic.SchematicBlocks;
import com.nomosiki.match.schematic.SchematicLibrary;
import com.nomosiki.registry.ModBlocks;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Immobile building core. Shell blocks are restored while the heart lives. */
public class BuildingHeartEntity extends Mob {
	public static final String NBT_SIDE = "MatchSide";
	public static final String NBT_TYPE = "BuildingType";
	public static final String NBT_SHELL = "ShellBlocks";
	public static final String NBT_OWNER = "Owner";
	public static final String NBT_BUILD_KIND = "BuildKind";
	private static final String NBT_FOOTPRINT = "HasFootprint";
	private static final String NBT_CONSTRUCTING = "Constructing";
	private static final String NBT_CURSOR = "PlaceCursor";
	private static final String NBT_RACE = "SchematicRace";

	private static final EntityDataAccessor<Byte> DATA_SIDE =
		SynchedEntityData.defineId(BuildingHeartEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_TYPE =
		SynchedEntityData.defineId(BuildingHeartEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<String> DATA_KIND =
		SynchedEntityData.defineId(BuildingHeartEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
		SynchedEntityData.defineId(BuildingHeartEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	private final List<BlockPos> shellBlocks = new ArrayList<>();
	private final Map<BlockPos, BlockState> expectedStates = new HashMap<>();
	private final Map<BlockPos, String> pendingStateText = new HashMap<>();
	private byte lastCrackStage = -1;
	private boolean hasFootprint;
	private boolean constructing;
	private int placeCursor;
	private int spanX;
	private int spanY;
	private int spanZ;
	private BlockPos anchor = BlockPos.ZERO;
	private String schematicRace = "";
	@Nullable
	private List<LitematicaSchematic.Cell> buildQueue;

	public BuildingHeartEntity(EntityType<? extends BuildingHeartEntity> type, Level level) {
		super(type, level);
		this.setNoAi(true);
		this.setPersistenceRequired();
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, BuildingHeartType.HQ.maxHp())
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 0.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SIDE, (byte) 0);
		builder.define(DATA_TYPE, BuildingHeartType.HQ.id());
		builder.define(DATA_KIND, "hq");
		builder.define(DATA_OWNER, Optional.empty());
	}

	public byte matchSide() {
		return this.entityData.get(DATA_SIDE);
	}

	public void setMatchSide(byte side) {
		this.entityData.set(DATA_SIDE, side);
	}

	public BuildingHeartType buildingType() {
		return BuildingHeartType.fromId(this.entityData.get(DATA_TYPE));
	}

	public void setBuildingType(BuildingHeartType type) {
		this.entityData.set(DATA_TYPE, type.id());
		var max = this.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.setBaseValue(type.maxHp());
		}
		this.setHealth(type.maxHp());
	}

	@Nullable
	public UUID getOwnerUUID() {
		return this.entityData.get(DATA_OWNER).orElse(null);
	}

	public void setOwnerUUID(@Nullable UUID ownerId) {
		this.entityData.set(DATA_OWNER, Optional.ofNullable(ownerId));
	}

	public String buildKind() {
		String kind = this.entityData.get(DATA_KIND);
		return kind == null || kind.isBlank() ? "hq" : kind;
	}

	public void setBuildKind(String buildKind) {
		this.entityData.set(DATA_KIND, buildKind == null || buildKind.isBlank() ? "hq" : buildKind);
	}

	public List<BlockPos> shellBlocksView() {
		return List.copyOf(this.shellBlocks);
	}

	public void setShellBlocks(List<BlockPos> blocks) {
		this.shellBlocks.clear();
		this.shellBlocks.addAll(blocks);
	}

	public void addShellBlock(BlockPos pos) {
		this.shellBlocks.add(pos.immutable());
	}

	public boolean isComplete() {
		return !this.constructing;
	}

	public boolean isConstructing() {
		return this.constructing;
	}

	public int spanX() {
		return this.spanX;
	}

	public boolean overlaps(BlockPos origin, int sizeX, int sizeY, int sizeZ) {
		int ax;
		int ay;
		int az;
		int sx;
		int sy;
		int sz;
		if (this.hasFootprint && this.spanX > 0 && this.spanY > 0 && this.spanZ > 0) {
			ax = this.anchor.getX();
			ay = this.anchor.getY();
			az = this.anchor.getZ();
			sx = this.spanX;
			sy = this.spanY;
			sz = this.spanZ;
		} else {
			ax = this.blockPosition().getX() - 2;
			ay = this.blockPosition().getY();
			az = this.blockPosition().getZ() - 2;
			sx = 5;
			sy = 4;
			sz = 5;
		}
		return rangesOverlap(ax, sx, origin.getX(), sizeX)
			&& rangesOverlap(ay, sy, origin.getY(), sizeY)
			&& rangesOverlap(az, sz, origin.getZ(), sizeZ);
	}

	/**
	 * @param immediate place every block now (headquarters). Otherwise the match tick places a few per tick.
	 */
	public void startSchematic(LitematicaSchematic schematic, BlockPos origin, String race, boolean immediate) {
		this.anchor = origin.immutable();
		this.spanX = schematic.sizeX();
		this.spanY = schematic.sizeY();
		this.spanZ = schematic.sizeZ();
		this.hasFootprint = true;
		this.schematicRace = race == null ? "" : race;
		this.buildQueue = new ArrayList<>(schematic.blocks());
		this.placeCursor = 0;
		this.constructing = !immediate;
		if (immediate && this.level() instanceof ServerLevel level) {
			this.placeUntil(level, this.buildQueue.size(), null);
			this.constructing = false;
		}
	}

	/** @return true when this call finished the schematic */
	public boolean tickConstruction(ServerLevel level, int budget, MatchSession session) {
		if (!this.constructing) {
			return false;
		}
		this.ensureQueue(level);
		if (!this.constructing || this.buildQueue == null) {
			return false;
		}
		int before = this.placeCursor;
		this.placeUntil(level, budget, session);
		return before < this.buildQueue.size() && !this.constructing;
	}

	/** Places a small iron/stone shell around the heart for the prototype. */
	public void buildDefaultShell(ServerLevel level) {
		BlockPos origin = this.blockPosition();
		int radius = switch (this.buildingType()) {
			case HQ -> 2;
			case TOWER -> 1;
			case OUTPOST -> 1;
			default -> 2;
		};
		int height = this.buildingType() == BuildingHeartType.TOWER ? 4 : 2;
		BlockState wall = shell(switch (this.buildingType()) {
			case HQ -> MatchShellBlock.Look.IRON;
			case TOWER -> MatchShellBlock.Look.COBBLE;
			case OUTPOST -> MatchShellBlock.Look.PLANKS;
			default -> MatchShellBlock.Look.BRICKS;
		});
		List<BlockPos> placed = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
					continue;
				}
				for (int dy = 0; dy <= height; dy++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
						level.setBlock(pos, wall, 3);
						placed.add(pos.immutable());
					}
				}
			}
		}
		BlockPos roof = origin.above(height + 1);
		if (level.getBlockState(roof).isAir() || level.getBlockState(roof).canBeReplaced()) {
			BlockState roofBlock = shell(this.buildingType() == BuildingHeartType.HQ
				? MatchShellBlock.Look.GOLD
				: MatchShellBlock.Look.STONE);
			level.setBlock(roof, roofBlock, 3);
			placed.add(roof.immutable());
		}
		this.setShellBlocks(placed);
	}

	public void clearShell(ServerLevel level) {
		this.resolvePendingStates(level);
		for (BlockPos pos : this.shellBlocks) {
			BlockState current = level.getBlockState(pos);
			BlockState expected = this.expectedStates.get(pos);
			if (expected != null) {
				if (current.equals(expected)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			} else if (isShellBlock(current)) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		this.shellBlocks.clear();
		this.expectedStates.clear();
		this.pendingStateText.clear();
		this.constructing = false;
		this.buildQueue = null;
	}

	/** Current passable shell, plus the solid blocks older matches placed. */
	public static boolean isShellBlock(BlockState state) {
		return state.is(ModBlocks.MATCH_SHELL)
			|| state.is(Blocks.IRON_BLOCK)
			|| state.is(Blocks.STONE_BRICKS)
			|| state.is(Blocks.GOLD_BLOCK)
			|| state.is(Blocks.CRACKED_STONE_BRICKS)
			|| state.is(Blocks.COBBLESTONE)
			|| state.is(Blocks.OAK_PLANKS)
			|| state.is(Blocks.STONE_SLAB);
	}

	private static BlockState shell(MatchShellBlock.Look look) {
		return ModBlocks.MATCH_SHELL.defaultBlockState().setValue(MatchShellBlock.LOOK, look);
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(0.0, 0.0, 0.0);
		if (!this.level().isClientSide && this.tickCount % 20 == 0) {
			this.updateCrackVisual();
		}
	}

	private void updateCrackVisual() {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		float ratio = this.getHealth() / this.getMaxHealth();
		byte stage = 0;
		if (ratio <= 0.25f) {
			stage = 3;
		} else if (ratio <= 0.5f) {
			stage = 2;
		} else if (ratio <= 0.75f) {
			stage = 1;
		}
		if (stage == this.lastCrackStage) {
			return;
		}
		this.lastCrackStage = stage;
		if (stage >= 2) {
			BlockState cracked = shell(MatchShellBlock.Look.CRACKED);
			for (BlockPos pos : this.shellBlocks) {
				BlockState state = level.getBlockState(pos);
				if (!state.is(ModBlocks.MATCH_SHELL)) {
					continue;
				}
				MatchShellBlock.Look look = state.getValue(MatchShellBlock.LOOK);
				if (look == MatchShellBlock.Look.BRICKS || look == MatchShellBlock.Look.IRON) {
					level.setBlock(pos, cracked, 3);
				}
			}
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		float scaled = amount;
		if (source.getEntity() instanceof AshigaruArtilleristEntity) {
			scaled *= NomosikiServerConfig.INSTANCE.match.siegeDamageMultiplier;
		}
		return super.hurt(source, scaled);
	}

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);
		if (this.level() instanceof ServerLevel serverLevel) {
			this.clearShell(serverLevel);
			MatchService service = MatchService.get(serverLevel.getServer());
			service.onBuildingHeartDestroyed(this);
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte(NBT_SIDE, this.matchSide());
		tag.putByte(NBT_TYPE, this.buildingType().id());
		tag.putString(NBT_BUILD_KIND, this.buildKind());
		if (this.getOwnerUUID() != null) {
			tag.putUUID(NBT_OWNER, this.getOwnerUUID());
		}
		ListTag list = new ListTag();
		for (BlockPos pos : this.shellBlocks) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("X", pos.getX());
			entry.putInt("Y", pos.getY());
			entry.putInt("Z", pos.getZ());
			BlockState expected = this.expectedStates.get(pos);
			if (expected != null) {
				entry.putString("S", SchematicBlocks.stringify(expected));
			} else {
				String pending = this.pendingStateText.get(pos);
				if (pending != null && !pending.isBlank()) {
					entry.putString("S", pending);
				}
			}
			list.add(entry);
		}
		tag.put(NBT_SHELL, list);
		tag.putBoolean(NBT_FOOTPRINT, this.hasFootprint);
		tag.putBoolean(NBT_CONSTRUCTING, this.constructing);
		tag.putInt(NBT_CURSOR, this.placeCursor);
		tag.putInt("SpanX", this.spanX);
		tag.putInt("SpanY", this.spanY);
		tag.putInt("SpanZ", this.spanZ);
		tag.putInt("AnchorX", this.anchor.getX());
		tag.putInt("AnchorY", this.anchor.getY());
		tag.putInt("AnchorZ", this.anchor.getZ());
		tag.putString(NBT_RACE, this.schematicRace == null ? "" : this.schematicRace);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.setMatchSide(tag.getByte(NBT_SIDE));
		this.setBuildingType(BuildingHeartType.fromId(tag.getByte(NBT_TYPE)));
		if (tag.contains(NBT_BUILD_KIND)) {
			this.setBuildKind(tag.getString(NBT_BUILD_KIND));
		}
		if (tag.hasUUID(NBT_OWNER)) {
			this.setOwnerUUID(tag.getUUID(NBT_OWNER));
		}
		this.shellBlocks.clear();
		this.expectedStates.clear();
		this.pendingStateText.clear();
		ListTag list = tag.getList(NBT_SHELL, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			BlockPos pos = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
			this.shellBlocks.add(pos);
			if (entry.contains("S", Tag.TAG_STRING)) {
				this.pendingStateText.put(pos, entry.getString("S"));
			}
		}
		this.hasFootprint = tag.getBoolean(NBT_FOOTPRINT);
		this.constructing = tag.getBoolean(NBT_CONSTRUCTING);
		this.placeCursor = tag.getInt(NBT_CURSOR);
		this.spanX = tag.getInt("SpanX");
		this.spanY = tag.getInt("SpanY");
		this.spanZ = tag.getInt("SpanZ");
		this.anchor = new BlockPos(tag.getInt("AnchorX"), tag.getInt("AnchorY"), tag.getInt("AnchorZ"));
		this.schematicRace = tag.getString(NBT_RACE);
		this.buildQueue = null;
	}

	private void placeUntil(ServerLevel level, int budget, @Nullable MatchSession session) {
		if (this.buildQueue == null || budget <= 0) {
			return;
		}
		int placed = 0;
		while (placed < budget && this.placeCursor < this.buildQueue.size()) {
			LitematicaSchematic.Cell cell = this.buildQueue.get(this.placeCursor++);
			BlockPos pos = this.anchor.offset(cell.x(), cell.y(), cell.z());
			level.setBlock(pos, cell.state(), 3);
			this.shellBlocks.add(pos.immutable());
			this.expectedStates.put(pos.immutable(), cell.state());
			this.pendingStateText.remove(pos);
			if (session != null) {
				session.trackedBlocks.add(pos.immutable());
			}
			placed++;
		}
		if (this.placeCursor >= this.buildQueue.size()) {
			this.constructing = false;
		}
	}

	private void ensureQueue(ServerLevel level) {
		if (this.buildQueue != null || !this.constructing) {
			return;
		}
		SchematicLibrary.Result result = SchematicLibrary.get(level, this.schematicRace, this.buildKind());
		if (!result.ok() || result.schematic() == null) {
			this.constructing = false;
			return;
		}
		this.buildQueue = new ArrayList<>(result.schematic().blocks());
		if (this.placeCursor > this.buildQueue.size()) {
			this.placeCursor = this.buildQueue.size();
		}
		if (!this.hasFootprint) {
			this.spanX = result.schematic().sizeX();
			this.spanY = result.schematic().sizeY();
			this.spanZ = result.schematic().sizeZ();
			this.hasFootprint = true;
		}
	}

	private void resolvePendingStates(ServerLevel level) {
		if (this.pendingStateText.isEmpty()) {
			return;
		}
		HolderLookup<Block> lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
		for (Map.Entry<BlockPos, String> entry : this.pendingStateText.entrySet()) {
			try {
				BlockState state = BlockStateParser.parseForBlock(lookup, entry.getValue(), false).blockState();
				this.expectedStates.put(entry.getKey(), state);
			} catch (CommandSyntaxException | RuntimeException ignored) {
			}
		}
		this.pendingStateText.clear();
	}

	private static boolean rangesOverlap(int originA, int sizeA, int originB, int sizeB) {
		if (sizeA <= 0 || sizeB <= 0) {
			return false;
		}
		int endA = originA + sizeA - 1;
		int endB = originB + sizeB - 1;
		return originA <= endB && originB <= endA;
	}

	@Override
	public Component getName() {
		return Component.translatable("entity.zkwars.building_heart." + this.buildingType().name().toLowerCase());
	}
}
