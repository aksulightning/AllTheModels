package com.aksulightning.fbxplayermodels;

import me.onethecrazy.FBXPlayerModels;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final Identifier VIEW_ENTITY_ID = Identifier.of(FBXPlayerModels.ENTITY_NAMESPACE, FBXPlayerModels.VIEW_ENTITY_PATH);

    public static final EntityType<ViewEntity> VIEW_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            VIEW_ENTITY_ID,
            EntityType.Builder.create(ViewEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 2.0f)
                    .eyeHeight(1.62f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(1)
                    .build(VIEW_ENTITY_ID.toString())
    );

    private ModEntities() {
    }

    public static void register() {
    }
}
