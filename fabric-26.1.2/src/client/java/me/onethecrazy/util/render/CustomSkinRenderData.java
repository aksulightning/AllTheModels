package me.onethecrazy.util.render;

import me.onethecrazy.util.objects.Vertex;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CustomSkinRenderData(
        List<Vertex> vertices,
        float bodyRot,
        Pose pose,
        @Nullable Direction bedOrientation,
        float eyeHeight,
        int light
) {
    public static final RenderStateDataKey<CustomSkinRenderData> KEY =
            RenderStateDataKey.create(() -> "fbx_player_models:custom_skin_render_data");
}
