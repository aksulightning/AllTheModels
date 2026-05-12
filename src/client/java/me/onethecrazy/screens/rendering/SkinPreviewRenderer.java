package me.onethecrazy.screens.rendering;

import com.mojang.authlib.GameProfile;
import me.onethecrazy.util.LivingEntityRenderExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.EntityType;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SkinPreviewRenderer {
    private final AvatarRenderState skinPreviewRenderState;
    private final int x, y;
    private final int dimensions;
    private final float scale;
    private float yaw, pitch = 0;

    public SkinPreviewRenderer(int x, int y, int dimensions, float scale){
        var mc = Minecraft.getInstance();
        var user = mc.getUser();
        var playerProfile = new GameProfile(user.getProfileId(), user.getName());

        // Init render state
        skinPreviewRenderState = new AvatarRenderState();
        skinPreviewRenderState.entityType = EntityType.PLAYER;
        skinPreviewRenderState.distanceToCameraSq = 1;
        skinPreviewRenderState.x = skinPreviewRenderState.y = skinPreviewRenderState.z = 0.0;
        skinPreviewRenderState.skin = mc.getSkinManager().createLookup(playerProfile, false).get();
        if (skinPreviewRenderState.skin == null) {
            skinPreviewRenderState.skin = DefaultPlayerSkin.get(playerProfile);
        }

        this.x = x;
        this.y = y;
        this.dimensions = dimensions;
        this.scale = scale;
    }

    public void renderPreview(GuiGraphicsExtractor ctx, float deltaTicks){
        // Tick the animation state
        skinPreviewRenderState.ageInTicks += deltaTicks;

        // Reset Player to render the correct Skin
        resetPlayerOnLivingEntityRenderer();

        // Render the Preview of the player skin
        ctx.entity(
                skinPreviewRenderState,
                scale,
                new Vector3f(0f, 1.0f, 0f),
                new Quaternionf()
                        .rotateAxis(Math.toRadians(180f), 0f, 0f, 1f) // Z correction
                        .rotateAxis(Math.toRadians(180f + yaw), 0f, 1f, 0f) // Y correction
                        .rotateAxis(Math.toRadians(-pitch), 1f, 0f, 0f),
                null,
                x, y,
                x + dimensions, y + dimensions
        );

        // Render the border where the Mesh is placed inside
        ctx.outline(x, y, dimensions, dimensions, 0xFFFFFFFF);
    }

    public void addRotation(float yaw, float pitch){
        this.yaw += yaw;
        this.pitch += pitch;
    }

    private void resetPlayerOnLivingEntityRenderer(){
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher disp = mc.getEntityRenderDispatcher();

        AvatarRenderer<?> playerRenderer = (AvatarRenderer<?>) disp.getRenderer(skinPreviewRenderState);

        ((LivingEntityRenderExtension)playerRenderer).all_the_skins$setPlayerAsNull();
    }
}
