package com.aksulightning.fbxplayermodels;

import me.onethecrazy.FBXPlayerModels;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Identifier VIEW_ENTITY_ID = Identifier.fromNamespaceAndPath(FBXPlayerModels.ENTITY_NAMESPACE, FBXPlayerModels.VIEW_ENTITY_PATH);
    private static final ResourceKey<EntityType<?>> VIEW_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, VIEW_ENTITY_ID);

    public static final EntityType<ViewEntity> VIEW_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            VIEW_ENTITY_KEY,
            EntityType.Builder.of(ViewEntity::new, MobCategory.MISC)
                    .sized(0.6f, 2.0f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(VIEW_ENTITY_KEY)
    );

    private ModEntities() {
    }

    public static void register() {
    }
}
