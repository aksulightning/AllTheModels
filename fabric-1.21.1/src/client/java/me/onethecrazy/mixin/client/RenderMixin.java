package me.onethecrazy.mixin.client;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.LivingEntityRenderExtension;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class RenderMixin <T extends LivingEntity> implements LivingEntityRenderExtension {
    @Unique private static final boolean all_the_skins$debugHeadLook = Boolean.getBoolean("alltheskins.debugHeadLook");
    @Unique private static boolean all_the_skins$headLookDebugLogged;
    @Unique private AbstractClientPlayerEntity player;
    @Unique private float all_the_skins$tickDelta;

    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"), cancellable = true)
    private void onPlayerRender(T livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci){
        // We only want to hook the player rendering
        if(livingEntity instanceof AbstractClientPlayerEntity renderedPlayer){
            player = renderedPlayer;
            all_the_skins$tickDelta = tickDelta;

            if(!AllTheSkinsClient.options().isEnabled)
                return;

            String uuid;

            try {
                uuid = player.getUuid().toString();
            }
            catch(NullPointerException ex){
                var screen = MinecraftClient.getInstance().currentScreen;

                // We are inside a screen and don't have an uuid, so we just fall back to the clients uuid
                if(screen instanceof TitleScreen || screen instanceof ConfigScreen) {
                    var sessionUuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
                    if (sessionUuid == null) {
                        return;
                    }

                    uuid = sessionUuid.toString();
                }
                // Just hand off to default rendering
                else
                    return;
            }

            // We have never encountered this user before (we don't know whether he has a skin or not) or we have never loaded the skin of this user
            if(!SkinManager.skinLookup.containsKey(uuid)){
                AllTheSkins.LOGGER.info("Loading skin for uuid: {}", uuid);
                SkinManager.loadSkin(uuid);
                return;
            }

            @Nullable CacheSkin cacheResult = SkinManager.skinCache.get(uuid);

            // We don't have the skin data yet
            if(cacheResult == null)
                return;

            @Nullable List<Vertex> vertices = cacheResult.vertices;
            if(cacheResult.skinnedModel != null){
                String animation = all_the_skins$currentAnimation(renderedPlayer);
                float seconds = (renderedPlayer.age + tickDelta) / 20f;
                boolean idle = "Idle".equals(animation);
                CustomModelPose.HeadLookRotation headLookRotation = idle && player != null
                        ? CustomModelPose.computeHeadLookRotation(player, tickDelta)
                        : CustomModelPose.HeadLookRotation.NONE;
                CustomModelPose.LimbPose limbPose = "Walk".equals(animation) || "Sneak".equals(animation)
                        ? all_the_skins$computeMinecraftLimbPose(renderedPlayer, tickDelta, "Sneak".equals(animation))
                        : CustomModelPose.LimbPose.NONE;
                all_the_skins$logHeadLookDebug(animation, renderedPlayer, tickDelta, idle && player != null);
                vertices = cacheResult.skinnedModel.render(animation, seconds, headLookRotation, limbPose);
            }

            // User didn't select a skin
            if(vertices == null || vertices.isEmpty())
                return;

            matrixStack.push();

            // --- Stolen from net.minecraft.client.render.entity.LivingEntityRenderer#render ---
            if (renderedPlayer.isInPose(EntityPose.SLEEPING)) {
                Direction direction = renderedPlayer.getSleepingDirection();
                if (direction != null) {
                    float f = renderedPlayer.getStandingEyeHeight() - 0.1F;
                    matrixStack.translate((float)(-direction.getOffsetX()) * f, 0.0F, (float)(-direction.getOffsetZ()) * f);
                }
            }

            // Render Nametag
            renderNameTagIfShouldRender(renderedPlayer, renderedPlayer.getDisplayName(), matrixStack, vertexConsumerProvider, light);

            // Apply body yaw only; custom HEAD look is applied around its own bind pivot during skinning.
            float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, renderedPlayer.prevBodyYaw, renderedPlayer.bodyYaw);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

            // Get Matrices
            MatrixStack.Entry entry = matrixStack.peek();
            Matrix4f matrix = entry.getPositionMatrix();

            for(Vertex v : vertices){
                RenderLayer layer = RenderLayer.getEntityCutoutNoCull(v.texture);
                VertexConsumer buffer = vertexConsumerProvider.getBuffer(layer);

                buffer.vertex(matrix, v.position.x, v.position.y, v.position.z).color(v.color).texture(v.textureUV.u, v.textureUV.v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, v.normals.x, v.normals.y, v.normals.z);
            }

            matrixStack.pop();

            ci.cancel();
        }
    }

    @Unique
    // Used to reset the rendered Player, since the flow is as following:
    // Update State (set player for UUID-getting) -> Render immediately -> Same process for other entity
    // To now reset the player we use this method.
    // This is used when rendering the Player Skin Preview.
    public void all_the_skins$setPlayerAsNull(){
        player = null;
    }

    @Unique
    private boolean all_the_skins$isWalking(){
        if(player == null)
            return false;

        Vec3d velocity = player.getVelocity();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    @Unique
    private String all_the_skins$currentAnimation(AbstractClientPlayerEntity renderedPlayer) {
        if (renderedPlayer.isSneaking() || renderedPlayer.isInSneakingPose()) {
            return "Sneak";
        }
        return all_the_skins$isWalking() ? "Walk" : "Idle";
    }

    @Unique
    private CustomModelPose.LimbPose all_the_skins$computeMinecraftLimbPose(AbstractClientPlayerEntity renderedPlayer, float tickDelta, boolean sneaking) {
        float limbProgress = renderedPlayer.limbAnimator.getPos(tickDelta);
        float limbAmplitude = renderedPlayer.limbAnimator.getSpeed(tickDelta);
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

    @Unique
    private void all_the_skins$logHeadLookDebug(String animation, AbstractClientPlayerEntity renderedPlayer, float tickDelta, boolean usedPlayerLookRotation) {
        if (!all_the_skins$debugHeadLook || all_the_skins$headLookDebugLogged) {
            return;
        }

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, renderedPlayer.prevBodyYaw, renderedPlayer.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, renderedPlayer.prevHeadYaw, renderedPlayer.getHeadYaw());
        float relativeHeadYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);
        float pitch = MathHelper.lerp(tickDelta, renderedPlayer.prevPitch, renderedPlayer.getPitch());

        AllTheSkins.LOGGER.info(
                "Head look debug animation={} bodyYaw={} headYaw={} relativeHeadYaw={} pitch={} usedPlayerLookRotation={}",
                animation,
                bodyYaw,
                headYaw,
                relativeHeadYaw,
                pitch,
                usedPlayerLookRotation
        );
        all_the_skins$headLookDebugLogged = true;
    }

    @Unique
    private void renderNameTagIfShouldRender(AbstractClientPlayerEntity renderedPlayer, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light){
        if (renderedPlayer == MinecraftClient.getInstance().player) {
            return;
        }

        if (!renderedPlayer.shouldRenderName()) {
            return;
        }

        boolean notSneaking = !renderedPlayer.isSneaky();
        int yOffset = "deadmau5".equals(text.getString()) ? -10 : 0;
        matrices.push();
        matrices.translate(0.0F, renderedPlayer.getHeight() + 0.5F, 0.0F);
        matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
        matrices.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float x = (float)(-textRenderer.getWidth((StringVisitable)text)) / 2.0F;
        int backgroundColor = (int)(MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
        textRenderer.draw(text, x, (float)yOffset, -2130706433, false, matrix4f, vertexConsumers, notSneaking ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, backgroundColor, light);
        if (notSneaking) {
            textRenderer.draw(text, x, (float)yOffset, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.pack(15, 15));
        }

        matrices.pop();
    }
}
