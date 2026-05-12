package me.onethecrazy.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.onethecrazy.AllTheSkins;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.util.LivingEntityRenderExtension;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class RenderMixin<T extends LivingEntity, S extends LivingEntityRenderState> implements LivingEntityRenderExtension {
    @Unique private static final boolean all_the_skins$debugHeadLook = Boolean.getBoolean("alltheskins.debugHeadLook");
    @Unique private static boolean all_the_skins$headLookDebugLogged;
    @Unique private AbstractClientPlayer player;
    @Unique private float all_the_skins$tickDelta;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void onPlayerRender(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitter, CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState playerState)) {
            return;
        }

        if (!AllTheSkinsClient.options().isEnabled) {
            return;
        }

        String uuid;
        try {
            uuid = player.getUUID().toString();
        } catch (NullPointerException ex) {
            var screen = Minecraft.getInstance().screen;
            if (screen instanceof TitleScreen || screen instanceof ConfigScreen) {
                uuid = Minecraft.getInstance().getUser().getProfileId().toString();
            } else {
                return;
            }
        }

        if (!SkinManager.skinLookup.containsKey(uuid)) {
            AllTheSkins.LOGGER.info("Loading skin for uuid: {}", uuid);
            SkinManager.loadSkin(uuid);
            return;
        }

        @Nullable CacheSkin cacheResult = SkinManager.skinCache.get(uuid);
        if (cacheResult == null) {
            return;
        }

        @Nullable List<Vertex> vertices = cacheResult.vertices;
        if (cacheResult.skinnedModel != null) {
            String animation = all_the_skins$currentAnimation(playerState);
            float seconds = state.ageInTicks / 20f;
            boolean idle = "Idle".equals(animation);
            CustomModelPose.HeadLookRotation headLookRotation = idle && player != null
                    ? CustomModelPose.computeHeadLookRotation(player, all_the_skins$tickDelta)
                    : CustomModelPose.HeadLookRotation.NONE;
            CustomModelPose.LimbPose limbPose = "Walk".equals(animation) || "Sneak".equals(animation)
                    ? all_the_skins$computeMinecraftLimbPose(playerState, "Sneak".equals(animation))
                    : CustomModelPose.LimbPose.NONE;
            all_the_skins$logHeadLookDebug(animation, playerState, idle && player != null);
            vertices = cacheResult.skinnedModel.render(animation, seconds, headLookRotation, limbPose);
        }

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        if (state.hasPose(Pose.SLEEPING)) {
            Direction direction = state.bedOrientation;
            if (direction != null) {
                float f = state.eyeHeight - 0.1F;
                poseStack.translate((float)(-direction.getStepX()) * f, 0.0F, (float)(-direction.getStepZ()) * f);
            }
        }

        renderNameTagIfShouldRender(state, poseStack, submitter, camera);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.bodyRot));

        int light = state.lightCoords;
        for (Vertex v : vertices) {
            submitter.submitCustomGeometry(poseStack, RenderTypes.entityCutout(v.texture), (pose, buffer) ->
                    buffer.addVertex(pose, v.position.x, v.position.y, v.position.z)
                            .setColor(v.color)
                            .setUv(v.textureUV.u, v.textureUV.v)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose, v.normals.x, v.normals.y, v.normals.z)
            );
        }

        poseStack.popPose();
        ci.cancel();
    }

    @Unique
    public void all_the_skins$setPlayerAsNull() {
        player = null;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderState(T livingEntity, S livingEntityRenderState, float tickDelta, CallbackInfo ci) {
        all_the_skins$tickDelta = tickDelta;
        if (livingEntity instanceof AbstractClientPlayer abstractClientPlayer) {
            player = abstractClientPlayer;
        }
    }

    @Unique
    private boolean all_the_skins$isWalking() {
        if (player == null) {
            return false;
        }

        Vec3 velocity = player.getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    @Unique
    private String all_the_skins$currentAnimation(AvatarRenderState state) {
        if (state.isCrouching || (player != null && player.isCrouching())) {
            return "Sneak";
        }
        return all_the_skins$isWalking() ? "Walk" : "Idle";
    }

    @Unique
    private CustomModelPose.LimbPose all_the_skins$computeMinecraftLimbPose(AvatarRenderState state, boolean sneaking) {
        float limbProgress = state.walkAnimationPos;
        float limbAmplitude = state.walkAnimationSpeed;
        float armAmplitude = limbAmplitude;
        float legAmplitude = 1.4f * limbAmplitude;
        float sneakArmPitch = sneaking ? 0.4f : 0f;

        return new CustomModelPose.LimbPose(
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f + Mth.PI) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f) * legAmplitude, 0.005f, 0.005f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f + Mth.PI) * legAmplitude, -0.005f, -0.005f)
        );
    }

    @Unique
    private void all_the_skins$logHeadLookDebug(String animation, AvatarRenderState state, boolean usedPlayerLookRotation) {
        if (!all_the_skins$debugHeadLook || all_the_skins$headLookDebugLogged) {
            return;
        }

        AllTheSkins.LOGGER.info(
                "Head look debug animation={} bodyRot={} yRot={} xRot={} usedPlayerLookRotation={}",
                animation,
                state.bodyRot,
                state.yRot,
                state.xRot,
                usedPlayerLookRotation
        );
        all_the_skins$headLookDebugLogged = true;
    }

    @Unique
    private void renderNameTagIfShouldRender(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitter, CameraRenderState camera) {
        Component nameTag = state.nameTag;
        Vec3 nameTagAttachment = state.nameTagAttachment;
        if (nameTag == null || nameTagAttachment == null) {
            return;
        }

        submitter.submitNameTag(
                poseStack,
                nameTagAttachment,
                state.lightCoords,
                nameTag,
                !(state instanceof AvatarRenderState avatarRenderState) || !avatarRenderState.isCrouching,
                state.outlineColor,
                state.distanceToCameraSq,
                camera
        );
    }
}
