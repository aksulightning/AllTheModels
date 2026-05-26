package com.aksulightning.fbxplayermodels.client;

import com.aksulightning.fbxplayermodels.ViewEntity;
import me.onethecrazy.FBXPlayerModels;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;

public class ViewEntityRenderer extends EntityRenderer<ViewEntity> {
    private static final Identifier FALLBACK_TEXTURE = Identifier.of(FBXPlayerModels.MOD_ID, "textures/white_pixel.png");

    public ViewEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ViewEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        CacheSkin model = ViewEntityModelCache.get(entity.getModel());
        if (model == null) {
            return;
        }

        List<Vertex> vertices = model.vertices;
        if (model.skinnedModel != null) {
            float seconds = (entity.age + tickDelta) / 20f;
            vertices = model.skinnedModel.render("Idle", seconds, CustomModelPose.HeadLookRotation.NONE, CustomModelPose.LimbPose.NONE);
        }
        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw(tickDelta)));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        for (Vertex vertex : vertices) {
            RenderLayer layer = RenderLayer.getEntityCutoutNoCull(vertex.texture);
            VertexConsumer buffer = vertexConsumers.getBuffer(layer);
            buffer.vertex(matrix, vertex.position.x, vertex.position.y, vertex.position.z)
                    .color(vertex.color)
                    .texture(vertex.textureUV.u, vertex.textureUV.v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(entry, vertex.normals.x, vertex.normals.y, vertex.normals.z);
        }
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(ViewEntity entity) {
        return FALLBACK_TEXTURE;
    }
}
