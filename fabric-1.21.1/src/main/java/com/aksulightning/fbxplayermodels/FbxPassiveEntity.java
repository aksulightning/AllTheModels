package com.aksulightning.fbxplayermodels;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

public class FbxPassiveEntity extends AbstractFbxPathAwareEntity {
    public FbxPassiveEntity(EntityType<? extends FbxPassiveEntity> entityType, World world) {
        super(entityType, world);
    }
}
