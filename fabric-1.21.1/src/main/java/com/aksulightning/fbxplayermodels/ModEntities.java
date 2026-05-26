package com.aksulightning.fbxplayermodels;

import me.onethecrazy.FBXPlayerModels;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final Identifier VIEW_ENTITY_ID = Identifier.of(FBXPlayerModels.ENTITY_NAMESPACE, FBXPlayerModels.VIEW_ENTITY_PATH);
    public static final Identifier PASSIVE_ENTITY_ID = Identifier.of(FBXPlayerModels.ENTITY_NAMESPACE, "passive_entity");
    public static final Identifier TAMEABLE_ENTITY_ID = Identifier.of(FBXPlayerModels.ENTITY_NAMESPACE, "tameable_entity");
    public static final Identifier NEUTRAL_ENTITY_ID = Identifier.of(FBXPlayerModels.ENTITY_NAMESPACE, "neutral_entity");
    public static final Identifier HOSTILE_ENTITY_ID = Identifier.of(FBXPlayerModels.ENTITY_NAMESPACE, "hostile_entity");

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

    public static final EntityType<FbxPassiveEntity> PASSIVE_ENTITY = registerMob(
            PASSIVE_ENTITY_ID,
            EntityType.Builder.create(FbxPassiveEntity::new, SpawnGroup.CREATURE)
    );

    public static final EntityType<FbxTameableEntity> TAMEABLE_ENTITY = registerMob(
            TAMEABLE_ENTITY_ID,
            EntityType.Builder.create(FbxTameableEntity::new, SpawnGroup.CREATURE)
    );

    public static final EntityType<FbxNeutralEntity> NEUTRAL_ENTITY = registerMob(
            NEUTRAL_ENTITY_ID,
            EntityType.Builder.create(FbxNeutralEntity::new, SpawnGroup.CREATURE)
    );

    public static final EntityType<FbxHostileEntity> HOSTILE_ENTITY = registerMob(
            HOSTILE_ENTITY_ID,
            EntityType.Builder.create(FbxHostileEntity::new, SpawnGroup.MONSTER)
    );

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(PASSIVE_ENTITY, AbstractFbxPathAwareEntity.createFbxMobAttributes());
        FabricDefaultAttributeRegistry.register(TAMEABLE_ENTITY, FbxTameableEntity.createFbxTameableAttributes());
        FabricDefaultAttributeRegistry.register(NEUTRAL_ENTITY, AbstractFbxPathAwareEntity.createFbxMobAttributes());
        FabricDefaultAttributeRegistry.register(HOSTILE_ENTITY, FbxHostileEntity.createFbxHostileAttributes());
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> registerMob(Identifier id, EntityType.Builder<T> builder) {
        return Registry.register(
                Registries.ENTITY_TYPE,
                id,
                builder.dimensions(0.6f, 2.0f)
                        .eyeHeight(1.62f)
                        .maxTrackingRange(10)
                        .trackingTickInterval(1)
                        .build(id.toString())
        );
    }
}
