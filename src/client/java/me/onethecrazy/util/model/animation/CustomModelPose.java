package me.onethecrazy.util.model.animation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CustomModelPose {
    private static final float MAX_HEAD_YAW_DEGREES = 85f;
    private static final float MAX_HEAD_PITCH_DEGREES = 90f;

    public static HeadLookRotation computeHeadLookRotation(PlayerEntity player, float tickDelta) {
        float bodyYaw = MathHelper.lerpAngleDegrees(
                tickDelta,
                player.lastBodyYaw,
                player.bodyYaw
        );

        float headYaw = MathHelper.lerpAngleDegrees(
                tickDelta,
                player.lastHeadYaw,
                player.getHeadYaw()
        );

        float relativeHeadYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);

        float pitch = MathHelper.lerp(
                tickDelta,
                player.lastPitch,
                player.getPitch()
        );

        return createMinecraftHeadLookRotation(relativeHeadYaw, pitch);
    }

    public static HeadLookRotation computeHeadLookRotation(float relativeHeadYaw, float pitchDegrees) {
        return createMinecraftHeadLookRotation(relativeHeadYaw, pitchDegrees);
    }

    public static HeadLookRotation createMinecraftHeadLookRotation(float relativeHeadYaw, float pitchDegrees) {
        float yaw = MathHelper.clamp(relativeHeadYaw, -MAX_HEAD_YAW_DEGREES, MAX_HEAD_YAW_DEGREES);
        float pitch = MathHelper.clamp(pitchDegrees, -MAX_HEAD_PITCH_DEGREES, MAX_HEAD_PITCH_DEGREES);
        // Custom model skinning is already body-yaw aligned by the renderer, so local head yaw uses the inverse sign.
        return new HeadLookRotation(
                (float) Math.toRadians(-yaw),
                (float) Math.toRadians(pitch)
        );
    }

    public record HeadLookRotation(float yawRadians, float pitchRadians) {
        public static final HeadLookRotation NONE = new HeadLookRotation(0f, 0f);

        public Matrix4f toMatrix() {
            return new Matrix4f()
                    .rotateY(yawRadians)
                    .rotateX(pitchRadians);
        }

        public Matrix4f globalPivotDelta(Vector3f pivot) {
            return new Matrix4f()
                    .translation(pivot)
                    .rotateY(yawRadians)
                    .rotateX(pitchRadians)
                    .translate(-pivot.x, -pivot.y, -pivot.z);
        }
    }

    public record BodyPartRotation(float pitchRadians, float yawRadians, float rollRadians) {
        public static final BodyPartRotation NONE = new BodyPartRotation(0f, 0f, 0f);

        public boolean isNone() {
            return pitchRadians == 0f && yawRadians == 0f && rollRadians == 0f;
        }

        public Matrix4f globalPivotDelta(Vector3f pivot) {
            return new Matrix4f()
                    .translation(pivot)
                    .rotateY(yawRadians)
                    .rotateX(pitchRadians)
                    .rotateZ(rollRadians)
                    .translate(-pivot.x, -pivot.y, -pivot.z);
        }
    }

    public record LimbPose(
            BodyPartRotation rightArm,
            BodyPartRotation leftArm,
            BodyPartRotation rightLeg,
            BodyPartRotation leftLeg
    ) {
        public static final LimbPose NONE = new LimbPose(
                BodyPartRotation.NONE,
                BodyPartRotation.NONE,
                BodyPartRotation.NONE,
                BodyPartRotation.NONE
        );
    }
}
