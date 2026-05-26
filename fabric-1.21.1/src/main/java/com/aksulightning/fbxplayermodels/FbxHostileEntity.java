package com.aksulightning.fbxplayermodels;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FbxHostileEntity extends HostileEntity implements FbxModelEntity {
    private static final TrackedData<String> MODEL = DataTracker.registerData(FbxHostileEntity.class, TrackedDataHandlerRegistry.STRING);

    public FbxHostileEntity(EntityType<? extends FbxHostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createFbxHostileAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
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
