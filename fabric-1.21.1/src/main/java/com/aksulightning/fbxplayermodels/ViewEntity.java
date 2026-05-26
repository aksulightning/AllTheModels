package com.aksulightning.fbxplayermodels;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class ViewEntity extends Entity implements FbxModelEntity {
    public static final String MODEL_NBT_KEY = "Model";
    private static final TrackedData<String> MODEL = DataTracker.registerData(ViewEntity.class, TrackedDataHandlerRegistry.STRING);

    public ViewEntity(EntityType<? extends ViewEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
    }

    public String getModel() {
        return getDataTracker().get(MODEL);
    }

    public void setModel(String model) {
        getDataTracker().set(MODEL, ViewEntityModelPath.safeModelOrEmpty(model));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(MODEL, "");
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setModel(nbt.getString(MODEL_NBT_KEY));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        String model = getModel();
        if (!model.isBlank()) {
            nbt.putString(MODEL_NBT_KEY, model);
        }
    }

    @Override
    public boolean isCollidable() {
        return false;
    }
}
