package me.onethecrazy.util.render;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.model.animation.CustomModelPose;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;

public final class FirstPersonSelfModelRenderer {
    private FirstPersonSelfModelRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(FirstPersonSelfModelRenderer::render);
    }

    public static boolean shouldRenderFor(ClientPlayerEntity player) {
        return getRenderVertices(player, 0.0f) != null;
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (player == null || matrices == null || consumers == null) {
            return;
        }

        float tickDelta = context.tickCounter().getTickDelta(false);
        @Nullable List<Vertex> vertices = getRenderVertices(player, tickDelta);
        if (vertices == null) {
            return;
        }

        Vec3d playerPos = player.getLerpedPos(tickDelta);
        Vec3d cameraPos = context.camera().getPos();
        int light = client.world == null
                ? LightmapTextureManager.pack(15, 15)
                : WorldRenderer.getLightmapCoordinates(client.world, BlockPos.ofFloored(playerPos));

        matrices.push();
        matrices.translate(playerPos.x - cameraPos.x, playerPos.y - cameraPos.y, playerPos.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-context.camera().getYaw()));

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        for (Vertex vertex : vertices) {
            RenderLayer layer = RenderLayer.getEntityCutoutNoCull(vertex.texture);
            VertexConsumer buffer = consumers.getBuffer(layer);
            buffer.vertex(matrix, vertex.position.x, vertex.position.y, vertex.position.z)
                    .color(vertex.color)
                    .texture(vertex.textureUV.u, vertex.textureUV.v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(entry, vertex.normals.x, vertex.normals.y, vertex.normals.z);
        }
        matrices.pop();
    }

    private static @Nullable List<Vertex> getRenderVertices(ClientPlayerEntity player, float tickDelta) {
        if (!isRenderAllowed(player)) {
            return null;
        }

        CacheSkin cacheSkin = SkinManager.skinCache.get(player.getUuidAsString());
        if (cacheSkin == null) {
            return null;
        }

        @Nullable List<Vertex> vertices = cacheSkin.vertices;
        if (cacheSkin.skinnedModel != null) {
            String animation = currentAnimation(player);
            float seconds = (player.age + tickDelta) / 20f;
            boolean idle = "Idle".equals(animation);
            CustomModelPose.HeadLookRotation headLookRotation = idle
                    ? CustomModelPose.computeHeadLookRotation(player, tickDelta)
                    : CustomModelPose.HeadLookRotation.NONE;
            CustomModelPose.LimbPose limbPose = "Walk".equals(animation) || "Sneak".equals(animation)
                    ? computeMinecraftLimbPose(player, tickDelta, "Sneak".equals(animation))
                    : CustomModelPose.LimbPose.NONE;
            vertices = cacheSkin.skinnedModel.renderWithHiddenHead(animation, seconds, headLookRotation, limbPose);
        }

        return vertices == null || vertices.isEmpty() ? null : vertices;
    }

    private static boolean isRenderAllowed(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return FBXPlayerModelsClient.options().isEnabled
                && FBXPlayerModelsClient.options().renderSelfModelInFirstPerson
                && client.getCameraEntity() == player
                && client.options.getPerspective() == Perspective.FIRST_PERSON
                && !player.isSpectator()
                && !player.isInvisible()
                && !player.isInPose(EntityPose.SLEEPING);
    }

    private static boolean isWalking(ClientPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    private static String currentAnimation(ClientPlayerEntity player) {
        if (player.isSneaking() || player.isInSneakingPose()) {
            return "Sneak";
        }
        return isWalking(player) ? "Walk" : "Idle";
    }

    private static CustomModelPose.LimbPose computeMinecraftLimbPose(ClientPlayerEntity player, float tickDelta, boolean sneaking) {
        float limbProgress = player.limbAnimator.getPos(tickDelta);
        float limbAmplitude = player.limbAnimator.getSpeed(tickDelta);
        float armAmplitude = limbAmplitude;
        float legAmplitude = 1.4f * limbAmplitude;
        float sneakArmPitch = sneaking ? 0.4f : 0f;

        return new CustomModelPose.LimbPose(
                new CustomModelPose.BodyPartRotation(MathHelper.cos(limbProgress * 0.6662f + MathHelper.PI) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(MathHelper.cos(limbProgress * 0.6662f) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(MathHelper.cos(limbProgress * 0.6662f) * legAmplitude, 0.005f, 0.005f),
                new CustomModelPose.BodyPartRotation(MathHelper.cos(limbProgress * 0.6662f + MathHelper.PI) * legAmplitude, -0.005f, -0.005f)
        );
    }

}
