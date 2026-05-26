package com.aksulightning.fbxplayermodels;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FbxTameableEntity extends TameableEntity implements FbxModelEntity {
    private static final TrackedData<String> MODEL = DataTracker.registerData(FbxTameableEntity.class, TrackedDataHandlerRegistry.STRING);

    public FbxTameableEntity(EntityType<? extends FbxTameableEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createFbxTameableAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(5, new FollowOwnerGoal(this, 1.1, 6.0f, 2.0f));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MODEL, "");
    }

    @Override
    public String getModel() {
        return getDataTracker().get(MODEL);
    }

    public void setModel(String model) {
        getDataTracker().set(MODEL, ViewEntityModelPath.safeModelOrEmpty(model));
    }

    @Override
    public String getFbxAnimation(float tickDelta) {
        Vec3d velocity = getVelocity();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004 ? "Walk" : "Idle";
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!isTamed() && stack.isOf(Items.BONE)) {
            if (!getWorld().isClient()) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                setOwner(player);
                setSitting(true);
                getWorld().sendEntityStatus(this, (byte) 7);
            }
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setModel(nbt.getString(ViewEntity.MODEL_NBT_KEY));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        String model = getModel();
        if (!model.isBlank()) {
            nbt.putString(ViewEntity.MODEL_NBT_KEY, model);
        }
    }
}
