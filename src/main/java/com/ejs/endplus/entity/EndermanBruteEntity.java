package com.ejs.endplus.entity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.Durations;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.UniversalAngerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.IntRange;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class EndermanBruteEntity extends HostileEntity implements Angerable {
	private static final UUID ATTACKING_SPEED_BOOST_ID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
	private static final EntityAttributeModifier ATTACKING_SPEED_BOOST;
	private static final TrackedData<Optional<BlockState>> CARRIED_BLOCK;
	private static final TrackedData<Boolean> ANGRY;
	private static final TrackedData<Boolean> PROVOKED;
	private static final Predicate<LivingEntity> PLAYER_ENDERMITE_PREDICATE;
	private int lastAngrySoundAge = Integer.MIN_VALUE;
	private int ageWhenTargetSet;
	private static final IntRange ANGER_TIME_RANGE;
	private int angerTime;
	private UUID targetUuid;

	public EndermanBruteEntity(EntityType<? extends EndermanBruteEntity> entityType, World world) {
		super(entityType, world);
		this.stepHeight = 1.0F;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new EndermanBruteEntity.ChasePlayerGoal(this));
		this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
		this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0D, 0.0F));
		this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
		this.goalSelector.add(10, new EndermanBruteEntity.PlaceBlockGoal(this));
		this.goalSelector.add(11, new EndermanBruteEntity.PickUpBlockGoal(this));
		this.targetSelector.add(1, new EndermanBruteEntity.TeleportTowardsPlayerGoal(this, this::shouldAngerAt));
		this.targetSelector.add(2, new RevengeGoal(this, new Class[0]));
		this.targetSelector.add(3,
				new FollowTargetGoal(this, EndermiteEntity.class, 10, true, false, PLAYER_ENDERMITE_PREDICATE));
		this.targetSelector.add(4, new UniversalAngerGoal(this, false));
	}

	public static DefaultAttributeContainer.Builder createEndermanAttributes() {
		return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8D)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D);
	}

	public void setTarget(@Nullable LivingEntity target) {
		super.setTarget(target);
		EntityAttributeInstance entityAttributeInstance = this
				.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (target == null) {
			this.ageWhenTargetSet = 0;
			this.dataTracker.set(ANGRY, false);
			this.dataTracker.set(PROVOKED, false);
			entityAttributeInstance.removeModifier(ATTACKING_SPEED_BOOST);
		} else {
			this.ageWhenTargetSet = this.age;
			this.dataTracker.set(ANGRY, true);
			if (!entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
				entityAttributeInstance.addTemporaryModifier(ATTACKING_SPEED_BOOST);
			}
		}

	}

	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(CARRIED_BLOCK, Optional.empty());
		this.dataTracker.startTracking(ANGRY, false);
		this.dataTracker.startTracking(PROVOKED, false);
	}

	public void chooseRandomAngerTime() {
		this.setAngerTime(ANGER_TIME_RANGE.choose(this.random));
	}

	public void setAngerTime(int ticks) {
		this.angerTime = ticks;
	}

	public int getAngerTime() {
		return this.angerTime;
	}

	public void setAngryAt(@Nullable UUID uuid) {
		this.targetUuid = uuid;
	}

	public UUID getAngryAt() {
		return this.targetUuid;
	}

	public void playAngrySound() {
		if (this.age >= this.lastAngrySoundAge + 400) {
			this.lastAngrySoundAge = this.age;
			if (!this.isSilent()) {
				this.world.playSound(this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_STARE,
						this.getSoundCategory(), 2.5F, 1.0F, false);
			}
		}

	}

	public void onTrackedDataSet(TrackedData<?> data) {
		if (ANGRY.equals(data) && this.isProvoked() && this.world.isClient) {
			this.playAngrySound();
		}

		super.onTrackedDataSet(data);
	}

	public void writeCustomDataToTag(CompoundTag tag) {
		super.writeCustomDataToTag(tag);
		BlockState blockState = this.getCarriedBlock();
		if (blockState != null) {
			tag.put("carriedBlockState", NbtHelper.fromBlockState(blockState));
		}

		this.angerToTag(tag);
	}

	public void readCustomDataFromTag(CompoundTag tag) {
		super.readCustomDataFromTag(tag);
		BlockState blockState = null;
		if (tag.contains("carriedBlockState", 10)) {
			blockState = NbtHelper.toBlockState(tag.getCompound("carriedBlockState"));
			if (blockState.isAir()) {
				blockState = null;
			}
		}

		this.setCarriedBlock(blockState);
		this.angerFromTag((ServerWorld) this.world, tag);
	}

	private boolean isPlayerStaring(PlayerEntity player) {
		ItemStack itemStack = (ItemStack) player.inventory.armor.get(3);
		if (itemStack.getItem() == Blocks.CARVED_PUMPKIN.asItem()) {
			return false;
		} else {
			Vec3d vec3d = player.getRotationVec(1.0F).normalize();
			Vec3d vec3d2 = new Vec3d(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(),
					this.getZ() - player.getZ());
			double d = vec3d2.length();
			vec3d2 = vec3d2.normalize();
			double e = vec3d.dotProduct(vec3d2);
			return e > 1.0D - 0.025D / d ? player.canSee(this) : false;
		}
	}

	protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
		return 2.55F;
	}

	public void tickMovement() {
		if (this.world.isClient) {
			for (int i = 0; i < 2; ++i) {
				this.world.addParticle(ParticleTypes.PORTAL, this.getParticleX(0.5D), this.getRandomBodyY() - 0.25D,
						this.getParticleZ(0.5D), (this.random.nextDouble() - 0.5D) * 2.0D, -this.random.nextDouble(),
						(this.random.nextDouble() - 0.5D) * 2.0D);
			}
		}

		this.jumping = false;
		if (!this.world.isClient) {
			this.tickAngerLogic((ServerWorld) this.world, true);
		}

		super.tickMovement();
	}

	public boolean hurtByWater() {
		return false;
	}

	protected void mobTick() {
		if (this.world.isDay() && this.age >= this.ageWhenTargetSet + 600) {
			float f = this.getBrightnessAtEyes();
			if (f > 0.5F && this.world.isSkyVisible(this.getBlockPos())
					&& this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F) {
				this.setTarget((LivingEntity) null);
				this.teleportRandomly();
			}
		}

		super.mobTick();
	}

	protected boolean teleportRandomly() {
		if (!this.world.isClient() && this.isAlive()) {
			double d = this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
			double e = this.getY() + (double) (this.random.nextInt(64) - 32);
			double f = this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
			return this.teleportTo(d, e, f);
		} else {
			return false;
		}
	}

	private boolean teleportTo(Entity entity) {
		Vec3d vec3d = new Vec3d(this.getX() - entity.getX(), this.getBodyY(0.5D) - entity.getEyeY(),
				this.getZ() - entity.getZ());
		vec3d = vec3d.normalize();
		double e = this.getX() + (this.random.nextDouble() - 0.5D) * 8.0D - vec3d.x * 16.0D;
		double f = this.getY() + (double) (this.random.nextInt(16) - 8) - vec3d.y * 16.0D;
		double g = this.getZ() + (this.random.nextDouble() - 0.5D) * 8.0D - vec3d.z * 16.0D;
		return this.teleportTo(e, f, g);
	}

	private boolean teleportTo(double x, double y, double z) {
		BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

		while (mutable.getY() > 0 && !this.world.getBlockState(mutable).getMaterial().blocksMovement()) {
			mutable.move(Direction.DOWN);
		}

		BlockState blockState = this.world.getBlockState(mutable);
		boolean bl = blockState.getMaterial().blocksMovement();
		boolean bl2 = false;
		if (bl && !bl2) {
			boolean bl3 = this.teleport(x, y, z, true);
			if (bl3 && !this.isSilent()) {
				this.world.playSound((PlayerEntity) null, this.prevX, this.prevY, this.prevZ,
						SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0F, 1.0F);
				this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
			}

			return bl3;
		} else {
			return false;
		}
	}

	protected SoundEvent getAmbientSound() {
		return this.isAngry() ? SoundEvents.ENTITY_ENDERMAN_SCREAM : SoundEvents.ENTITY_ENDERMAN_AMBIENT;
	}

	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ENDERMAN_HURT;
	}

	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ENDERMAN_DEATH;
	}

	protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
		super.dropEquipment(source, lootingMultiplier, allowDrops);
		BlockState blockState = this.getCarriedBlock();
		if (blockState != null) {
			this.dropItem(blockState.getBlock());
		}

	}

	public void setCarriedBlock(@Nullable BlockState state) {
		this.dataTracker.set(CARRIED_BLOCK, Optional.ofNullable(state));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Nullable
	public BlockState getCarriedBlock() {
		return (BlockState) ((Optional) this.dataTracker.get(CARRIED_BLOCK)).orElse((Object) null);
	}

	public boolean damage(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		} else if (source instanceof ProjectileDamageSource) {
			for (int i = 0; i < 64; ++i) {
				if (this.teleportRandomly()) {
					return true;
				}
			}

			return false;
		} else {
			boolean bl = super.damage(source, amount);
			if (!this.world.isClient() && !(source.getAttacker() instanceof LivingEntity)
					&& this.random.nextInt(10) != 0) {
				this.teleportRandomly();
			}

			return bl;
		}
	}

	public boolean isAngry() {
		return true;
	}

	public boolean isProvoked() {
		return (Boolean) this.dataTracker.get(PROVOKED);
	}

	public void setProvoked() {
		this.dataTracker.set(PROVOKED, true);
	}

	public boolean cannotDespawn() {
		return super.cannotDespawn() || this.getCarriedBlock() != null;
	}

	static {
		ATTACKING_SPEED_BOOST = new EntityAttributeModifier(ATTACKING_SPEED_BOOST_ID, "Attacking speed boost",
				0.15000000596046448D, EntityAttributeModifier.Operation.ADDITION);
		CARRIED_BLOCK = DataTracker.registerData(EndermanBruteEntity.class,
				TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
		ANGRY = DataTracker.registerData(EndermanBruteEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
		PROVOKED = DataTracker.registerData(EndermanBruteEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
		PLAYER_ENDERMITE_PREDICATE = (livingEntity) -> {
			return livingEntity instanceof EndermiteEntity && ((EndermiteEntity) livingEntity).isPlayerSpawned();
		};
		ANGER_TIME_RANGE = Durations.betweenSeconds(20, 39);
	}

	static class PickUpBlockGoal extends Goal {
		private final EndermanBruteEntity enderman;

		public PickUpBlockGoal(EndermanBruteEntity enderman) {
			this.enderman = enderman;
		}

		public boolean canStart() {
			if (this.enderman.getCarriedBlock() != null) {
				return false;
			} else if (!this.enderman.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
				return false;
			} else {
				return this.enderman.getRandom().nextInt(20) == 0;
			}
		}

		public void tick() {
			Random random = this.enderman.getRandom();
			World world = this.enderman.world;
			int i = MathHelper.floor(this.enderman.getX() - 2.0D + random.nextDouble() * 4.0D);
			int j = MathHelper.floor(this.enderman.getY() + random.nextDouble() * 3.0D);
			int k = MathHelper.floor(this.enderman.getZ() - 2.0D + random.nextDouble() * 4.0D);
			BlockPos blockPos = new BlockPos(i, j, k);
			BlockState blockState = world.getBlockState(blockPos);
			Block block = blockState.getBlock();
			Vec3d vec3d = new Vec3d((double) MathHelper.floor(this.enderman.getX()) + 0.5D, (double) j + 0.5D,
					(double) MathHelper.floor(this.enderman.getZ()) + 0.5D);
			Vec3d vec3d2 = new Vec3d((double) i + 0.5D, (double) j + 0.5D, (double) k + 0.5D);
			BlockHitResult blockHitResult = world.raycast(new RaycastContext(vec3d, vec3d2,
					RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, this.enderman));
			boolean bl = blockHitResult.getBlockPos().equals(blockPos);
			if (block.isIn(BlockTags.ENDERMAN_HOLDABLE) && bl) {
				world.removeBlock(blockPos, false);
				this.enderman.setCarriedBlock(blockState.getBlock().getDefaultState());
			}

		}
	}

	static class PlaceBlockGoal extends Goal {
		private final EndermanBruteEntity enderman;

		public PlaceBlockGoal(EndermanBruteEntity enderman) {
			this.enderman = enderman;
		}

		public boolean canStart() {
			if (this.enderman.getCarriedBlock() == null) {
				return false;
			} else if (!this.enderman.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
				return false;
			} else {
				return this.enderman.getRandom().nextInt(2000) == 0;
			}
		}

		public void tick() {
			Random random = this.enderman.getRandom();
			World world = this.enderman.world;
			int i = MathHelper.floor(this.enderman.getX() - 1.0D + random.nextDouble() * 2.0D);
			int j = MathHelper.floor(this.enderman.getY() + random.nextDouble() * 2.0D);
			int k = MathHelper.floor(this.enderman.getZ() - 1.0D + random.nextDouble() * 2.0D);
			BlockPos blockPos = new BlockPos(i, j, k);
			BlockState blockState = world.getBlockState(blockPos);
			BlockPos blockPos2 = blockPos.down();
			BlockState blockState2 = world.getBlockState(blockPos2);
			BlockState blockState3 = this.enderman.getCarriedBlock();
			if (blockState3 != null) {
				blockState3 = Block.postProcessState(blockState3, this.enderman.world, blockPos);
				if (this.canPlaceOn(world, blockPos, blockState3, blockState, blockState2, blockPos2)) {
					world.setBlockState(blockPos, blockState3, 3);
					this.enderman.setCarriedBlock((BlockState) null);
				}

			}
		}

		private boolean canPlaceOn(World world, BlockPos posAbove, BlockState carriedState, BlockState stateAbove,
				BlockState state, BlockPos pos) {
			return stateAbove.isAir() && !state.isAir() && !state.isOf(Blocks.BEDROCK) && state.isFullCube(world, pos)
					&& carriedState.canPlaceAt(world, posAbove)
					&& world.getOtherEntities(this.enderman, Box.method_29968(Vec3d.of(posAbove))).isEmpty();
		}
	}

	static class ChasePlayerGoal extends Goal {
		private final EndermanBruteEntity enderman;
		private LivingEntity target;

		public ChasePlayerGoal(EndermanBruteEntity enderman) {
			this.enderman = enderman;
			this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
		}

		public boolean canStart() {
			this.target = this.enderman.getTarget();
			if (!(this.target instanceof PlayerEntity)) {
				return false;
			} else {
				double d = this.target.squaredDistanceTo(this.enderman);
				return d > 256.0D ? false : this.enderman.isPlayerStaring((PlayerEntity) this.target);
			}
		}

		public void start() {
			this.enderman.getNavigation().stop();
		}

		public void tick() {
			this.enderman.getLookControl().lookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
		}
	}

	static class TeleportTowardsPlayerGoal extends FollowTargetGoal<PlayerEntity> {
		private final EndermanBruteEntity enderman;
		private PlayerEntity targetPlayer;
		private int lookAtPlayerWarmup;
		private int ticksSinceUnseenTeleport;
		private final TargetPredicate staringPlayerPredicate;
		private final TargetPredicate validTargetPredicate = (new TargetPredicate()).includeHidden();

		public TeleportTowardsPlayerGoal(EndermanBruteEntity enderman, @Nullable Predicate<LivingEntity> predicate) {
			super(enderman, PlayerEntity.class, 10, false, false, predicate);
			this.enderman = enderman;
			this.staringPlayerPredicate = (new TargetPredicate()).setBaseMaxDistance(this.getFollowRange())
					.setPredicate((playerEntity) -> {
						return true;
					});
		}

		public boolean canStart() {
			this.targetPlayer = this.enderman.world.getClosestPlayer(this.staringPlayerPredicate, this.enderman);
			return this.targetPlayer != null;
		}

		public void start() {
			this.lookAtPlayerWarmup = 5;
			this.ticksSinceUnseenTeleport = 0;
			this.enderman.setProvoked();
		}

		public void stop() {
			this.targetPlayer = null;
			super.stop();
		}

		public boolean shouldContinue() {
			if (this.targetPlayer != null) {
				this.enderman.lookAtEntity(this.targetPlayer, 10.0F, 10.0F);
				return true;
			} else {
				return this.targetEntity != null && this.validTargetPredicate.test(this.enderman, this.targetEntity)
						? true
						: super.shouldContinue();
			}
		}

		public void tick() {
			if (this.enderman.getTarget() == null) {
				super.setTargetEntity((LivingEntity) null);
			}

			if (this.targetPlayer != null) {
				if (--this.lookAtPlayerWarmup <= 0) {
					this.targetEntity = this.targetPlayer;
					this.targetPlayer = null;
					super.start();
				}
			} else {
				if (this.targetEntity != null && !this.enderman.hasVehicle()) {
					if (this.targetEntity.squaredDistanceTo(this.enderman) > 256.0D
							&& this.ticksSinceUnseenTeleport++ >= 30 && this.enderman.teleportTo(this.targetEntity)) {
						this.ticksSinceUnseenTeleport = 0;
					}
				}

				super.tick();
			}

		}
	}
}
