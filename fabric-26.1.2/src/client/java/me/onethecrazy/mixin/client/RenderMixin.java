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
import net.minecraft.core.Direction;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
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
            CustomModelPose.LimbPose limbPose = fbx_player_models$computeMinecraftLimbPose(renderedPlayer, tickDelta, animation);
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
            float offset = renderData.eyeHeight() - 0.1F + 1.0F;
            customPose.translate((float) -renderData.bedOrientation().getStepX() * offset, 0.0F, (float) -renderData.bedOrientation().getStepZ() * offset);
        }
        fbx_player_models$applyBodyTransform(customPose, renderData);

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
    private static void fbx_player_models$applyBodyTransform(PoseStack poseStack, CustomSkinRenderData renderData) {
        if (renderData.pose() == Pose.SLEEPING) {
            float yaw = renderData.bedOrientation() == null
                    ? renderData.bodyRot()
                    : fbx_player_models$sleepDirectionToRotation(renderData.bedOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90f));
            poseStack.mulPose(Axis.YP.rotationDegrees(90f));
            return;
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-renderData.bodyRot()));
    }

    @Unique
    private static float fbx_player_models$sleepDirectionToRotation(Direction direction) {
        return switch (direction) {
            case SOUTH -> 90f;
            case WEST -> 0f;
            case NORTH -> 270f;
            case EAST -> 180f;
            default -> 0f;
        };
    }

    @Unique
    private boolean fbx_player_models$isWalking(AbstractClientPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    @Unique
    private String fbx_player_models$currentAnimation(AbstractClientPlayer renderedPlayer) {
        if (renderedPlayer.getPose() == Pose.SLEEPING) {
            return "Sleep";
        }
        if (renderedPlayer.isPassenger()) {
            return "Sit";
        }
        if (renderedPlayer.isShiftKeyDown() || renderedPlayer.isCrouching()) {
            return "Sneak";
        }
        return fbx_player_models$isWalking(renderedPlayer) ? "Walk" : "Idle";
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$computeMinecraftLimbPose(AbstractClientPlayer renderedPlayer, float tickDelta, String animation) {
        if ("Sit".equals(animation)) {
            return fbx_player_models$sittingLimbPose();
        }
        if (!"Walk".equals(animation) && !"Sneak".equals(animation)) {
            return fbx_player_models$handActionPose(renderedPlayer, tickDelta);
        }

        boolean sneaking = "Sneak".equals(animation);
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
        ).withArmAction(fbx_player_models$handActionPose(renderedPlayer, tickDelta));
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$sittingLimbPose() {
        return new CustomModelPose.LimbPose(
                new CustomModelPose.BodyPartRotation(-0.62831855f, 0f, 0f),
                new CustomModelPose.BodyPartRotation(-0.62831855f, 0f, 0f),
                CustomModelPose.BodyPartRotation.NONE,
                CustomModelPose.BodyPartRotation.NONE
        );
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$handActionPose(AbstractClientPlayer renderedPlayer, float tickDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean breakingBlock = renderedPlayer == minecraft.player
                && minecraft.gameMode != null
                && minecraft.gameMode.isDestroying();

        if (breakingBlock) {
            float phase = ((renderedPlayer.tickCount + tickDelta) % 8f) / 8f;
            InteractionHand hand = renderedPlayer.swinging ? renderedPlayer.swingingArm : InteractionHand.MAIN_HAND;
            return fbx_player_models$singleArmPose(renderedPlayer, hand, fbx_player_models$breakingRotation(phase));
        }

        float swing = renderedPlayer.getAttackAnim(tickDelta);
        if (swing <= 0f) {
            return CustomModelPose.LimbPose.NONE;
        }

        return fbx_player_models$singleArmPose(renderedPlayer, renderedPlayer.swingingArm, fbx_player_models$placingRotation(swing));
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$singleArmPose(AbstractClientPlayer player, InteractionHand hand, CustomModelPose.BodyPartRotation rotation) {
        boolean rightArm = fbx_player_models$isRightArm(player, hand);
        return rightArm
                ? CustomModelPose.LimbPose.NONE.withRightArm(rotation)
                : CustomModelPose.LimbPose.NONE.withLeftArm(rotation);
    }

    @Unique
    private boolean fbx_player_models$isRightArm(AbstractClientPlayer player, InteractionHand hand) {
        boolean mainArmRight = player.getMainArm() == HumanoidArm.RIGHT;
        return hand == InteractionHand.MAIN_HAND == mainArmRight;
    }

    @Unique
    private CustomModelPose.BodyPartRotation fbx_player_models$breakingRotation(float phase) {
        float chop = Mth.sin(phase * Mth.TWO_PI);
        return new CustomModelPose.BodyPartRotation(-1.15f - 0.55f * chop, 0.18f * chop, 0.12f * chop);
    }

    @Unique
    private CustomModelPose.BodyPartRotation fbx_player_models$placingRotation(float swing) {
        float ease = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        return new CustomModelPose.BodyPartRotation(-0.45f - 0.85f * ease, 0f, 0.18f * ease);
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
