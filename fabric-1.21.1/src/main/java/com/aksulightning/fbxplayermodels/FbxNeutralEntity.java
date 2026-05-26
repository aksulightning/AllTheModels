package com.aksulightning.fbxplayermodels;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.world.World;

public class FbxNeutralEntity extends AbstractFbxPathAwareEntity {
    public FbxNeutralEntity(EntityType<? extends FbxNeutralEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, true));
        this.targetSelector.add(1, new RevengeGoal(this));
    }
}
