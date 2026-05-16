package me.onethecrazy.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.onethecrazy.FBXPlayerModelsMod;
import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.LivingEntityRenderExtension;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.render.CustomSkinRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
public abstract class RenderMixin implements LivingEntityRenderExtension {
    @Unique private static final int FULL_BRIGHT_LIGHT = 0xF000F0;
    @Unique private static final boolean fbx_player_models$debugHeadLook = Boolean.getBoolean("fbxplayermodels.debugHeadLook");
    @Unique private static boolean fbx_player_models$headLookDebugLogged;

    @Override
    public void fbx_player_models$setPlayerAsNull() {
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void fbx_player_models$extractCustomModel(LivingEntity livingEntity, LivingEntityRenderState livingState, float tickDelta, CallbackInfo ci) {
        if (!(livingEntity instanceof AbstractClientPlayer renderedPlayer) || !(livingState instanceof AvatarRenderState state)) {
            return;
        }

        if (!FBXPlayerModelsClient.options().isEnabled) {
            return;
        }

        String uuid = renderedPlayer.getUUID().toString();
        if (!SkinManager.skinLookup.containsKey(uuid)) {
            FBXPlayerModelsMod.LOGGER.info("Loading skin for uuid: {}", uuid);
            SkinManager.loadSkin(uuid);
            return;
        }

        @Nullable CacheSkin cacheResult = SkinManager.skinCache.get(uuid);
        if (cacheResult == null) {
            return;
        }

        @Nullable List<Vertex> vertices = cacheResult.vertices;
        String animation = fbx_player_models$currentAnimation(renderedPlayer);
        if (cacheResult.skinnedModel != null) {
            float seconds = (renderedPlayer.tickCount + tickDelta) / 20f;
            boolean idle = "Idle".equals(animation);
            CustomModelPose.HeadLookRotation headLookRotation = idle
                    ? CustomModelPose.computeHeadLookRotation(renderedPlayer, tickDelta)
                    : CustomModelPose.HeadLookRotation.NONE;
            CustomModelPose.LimbPose limbPose = "Walk".equals(animation) || "Sneak".equals(animation)
                    ? fbx_player_models$computeMinecraftLimbPose(renderedPlayer, tickDelta, "Sneak".equals(animation))
                    : CustomModelPose.LimbPose.NONE;
            fbx_player_models$logHeadLookDebug(animation, renderedPlayer, tickDelta, idle);
            vertices = cacheResult.skinnedModel.render(animation, seconds, headLookRotation, limbPose);
        }

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        state.setData(CustomSkinRenderData.KEY, new CustomSkinRenderData(vertices, state.bodyRot, state.pose, state.bedOrientation, state.eyeHeight, state.lightCoords));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void fbx_player_models$submitCustomModel(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        CustomSkinRenderData renderData = state.getData(CustomSkinRenderData.KEY);
        if (renderData == null) {
            return;
        }

        PoseStack customPose = poseStack;
        customPose.pushPose();
        if (renderData.pose() == Pose.SLEEPING && renderData.bedOrientation() != null) {
            float offset = renderData.eyeHeight() - 0.1F;
            customPose.translate((float) -renderData.bedOrientation().getStepX() * offset, 0.0F, (float) -renderData.bedOrientation().getStepZ() * offset);
        }
        customPose.mulPose(Axis.YP.rotationDegrees(-renderData.bodyRot()));

        for (Vertex vertex : renderData.vertices()) {
            RenderType layer = RenderTypes.entityCutout(vertex.texture);
            submitNodeCollector.submitCustomGeometry(customPose, layer, (entry, buffer) -> fbx_player_models$writeVertex(entry, buffer, vertex, renderData.light()));
        }
        customPose.popPose();

        if (state.nameTag != null && !state.isDiscrete) {
            submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, 0, state.nameTag, true, FULL_BRIGHT_LIGHT, state.distanceToCameraSq, cameraRenderState);
        }

        ci.cancel();
    }

    @Unique
    private static void fbx_player_models$writeVertex(PoseStack.Pose entry, VertexConsumer buffer, Vertex vertex, int light) {
        Matrix4f matrix = entry.pose();
        buffer.addVertex(matrix, vertex.position.x, vertex.position.y, vertex.position.z)
                .setColor(vertex.color)
                .setUv(vertex.textureUV.u, vertex.textureUV.v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, vertex.normals.x, vertex.normals.y, vertex.normals.z);
    }

    @Unique
    private boolean fbx_player_models$isWalking(AbstractClientPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    @Unique
    private String fbx_player_models$currentAnimation(AbstractClientPlayer renderedPlayer) {
        if (renderedPlayer.isShiftKeyDown() || renderedPlayer.isCrouching()) {
            return "Sneak";
        }
        return fbx_player_models$isWalking(renderedPlayer) ? "Walk" : "Idle";
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$computeMinecraftLimbPose(AbstractClientPlayer renderedPlayer, float tickDelta, boolean sneaking) {
        float limbProgress = renderedPlayer.walkAnimation.position(tickDelta);
        float limbAmplitude = renderedPlayer.walkAnimation.speed(tickDelta);
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
    private void fbx_player_models$logHeadLookDebug(String animation, AbstractClientPlayer renderedPlayer, float tickDelta, boolean usedPlayerLookRotation) {
        if (!fbx_player_models$debugHeadLook || fbx_player_models$headLookDebugLogged) {
            return;
        }

        float bodyYaw = Mth.rotLerp(tickDelta, renderedPlayer.yBodyRotO, renderedPlayer.yBodyRot);
        float headYaw = Mth.rotLerp(tickDelta, renderedPlayer.yHeadRotO, renderedPlayer.getYHeadRot());
        float relativeHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw);
        float pitch = Mth.lerp(tickDelta, renderedPlayer.xRotO, renderedPlayer.getXRot());

        FBXPlayerModelsMod.LOGGER.info(
                "Head look debug animation={} bodyYaw={} headYaw={} relativeHeadYaw={} pitch={} usedPlayerLookRotation={}",
                animation,
                bodyYaw,
                headYaw,
                relativeHeadYaw,
                pitch,
                usedPlayerLookRotation
        );
        fbx_player_models$headLookDebugLogged = true;
    }

}
