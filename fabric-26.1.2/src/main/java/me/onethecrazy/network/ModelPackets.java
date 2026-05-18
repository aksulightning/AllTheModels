package me.onethecrazy.network;

import me.onethecrazy.FBXPlayerModels;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModelPackets {
    public static final int MAX_MODEL_BYTES = 3 * 1024 * 1024;

    private ModelPackets() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FBXPlayerModels.MOD_ID, path);
    }

    public record UploadModelPayload(String fileName, String format, byte[] data) implements CustomPacketPayload {
        public static final Type<UploadModelPayload> TYPE = new Type<>(id("upload_model"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UploadModelPayload> CODEC = StreamCodec.of(
                ModelPackets::writeUploadModel,
                ModelPackets::readUploadModel
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestLookupPayload(List<String> uuids) implements CustomPacketPayload {
        public static final Type<RequestLookupPayload> TYPE = new Type<>(id("request_lookup"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestLookupPayload> CODEC = StreamCodec.of(
                ModelPackets::writeRequestLookup,
                ModelPackets::readRequestLookup
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestModelPayload(String hash, String format) implements CustomPacketPayload {
        public static final Type<RequestModelPayload> TYPE = new Type<>(id("request_model"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestModelPayload> CODEC = StreamCodec.of(
                ModelPackets::writeRequestModel,
                ModelPackets::readRequestModel
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LookupResponsePayload(Map<String, ModelLookup> models) implements CustomPacketPayload {
        public static final Type<LookupResponsePayload> TYPE = new Type<>(id("lookup_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LookupResponsePayload> CODEC = StreamCodec.of(
                ModelPackets::writeLookupResponse,
                ModelPackets::readLookupResponse
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ModelDataPayload(String hash, String format, byte[] data, String message) implements CustomPacketPayload {
        public static final Type<ModelDataPayload> TYPE = new Type<>(id("model_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModelDataPayload> CODEC = StreamCodec.of(
                ModelPackets::writeModelData,
                ModelPackets::readModelData
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UploadResultPayload(boolean success, String message) implements CustomPacketPayload {
        public static final Type<UploadResultPayload> TYPE = new Type<>(id("upload_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UploadResultPayload> CODEC = StreamCodec.of(
                ModelPackets::writeUploadResult,
                ModelPackets::readUploadResult
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ModelLookup(String hash, String format) {
    }

    private static UploadModelPayload readUploadModel(RegistryFriendlyByteBuf buf) {
        return new UploadModelPayload(buf.readUtf(160), buf.readUtf(16), buf.readByteArray(MAX_MODEL_BYTES + 1));
    }

    private static void writeUploadModel(RegistryFriendlyByteBuf buf, UploadModelPayload payload) {
        buf.writeUtf(payload.fileName(), 160);
        buf.writeUtf(payload.format(), 16);
        buf.writeByteArray(payload.data());
    }

    private static RequestLookupPayload readRequestLookup(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 128) {
            throw new IllegalArgumentException("Invalid lookup request size");
        }
        List<String> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(buf.readUtf(64));
        }
        return new RequestLookupPayload(uuids);
    }

    private static void writeRequestLookup(RegistryFriendlyByteBuf buf, RequestLookupPayload payload) {
        buf.writeVarInt(payload.uuids().size());
        for (String uuid : payload.uuids()) {
            buf.writeUtf(uuid, 64);
        }
    }

    private static RequestModelPayload readRequestModel(RegistryFriendlyByteBuf buf) {
        return new RequestModelPayload(buf.readUtf(80), buf.readUtf(16));
    }

    private static void writeRequestModel(RegistryFriendlyByteBuf buf, RequestModelPayload payload) {
        buf.writeUtf(payload.hash(), 80);
        buf.writeUtf(payload.format(), 16);
    }

    private static LookupResponsePayload readLookupResponse(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 128) {
            throw new IllegalArgumentException("Invalid lookup response size");
        }
        Map<String, ModelLookup> models = new HashMap<>();
        for (int i = 0; i < count; i++) {
            models.put(buf.readUtf(64), new ModelLookup(buf.readUtf(80), buf.readUtf(16)));
        }
        return new LookupResponsePayload(models);
    }

    private static void writeLookupResponse(RegistryFriendlyByteBuf buf, LookupResponsePayload payload) {
        buf.writeVarInt(payload.models().size());
        for (Map.Entry<String, ModelLookup> entry : payload.models().entrySet()) {
            buf.writeUtf(entry.getKey(), 64);
            buf.writeUtf(entry.getValue().hash(), 80);
            buf.writeUtf(entry.getValue().format(), 16);
        }
    }

    private static ModelDataPayload readModelData(RegistryFriendlyByteBuf buf) {
        return new ModelDataPayload(buf.readUtf(80), buf.readUtf(16), buf.readByteArray(MAX_MODEL_BYTES), buf.readUtf(256));
    }

    private static void writeModelData(RegistryFriendlyByteBuf buf, ModelDataPayload payload) {
        buf.writeUtf(payload.hash(), 80);
        buf.writeUtf(payload.format(), 16);
        buf.writeByteArray(payload.data());
        buf.writeUtf(payload.message(), 256);
    }

    private static UploadResultPayload readUploadResult(RegistryFriendlyByteBuf buf) {
        return new UploadResultPayload(buf.readBoolean(), buf.readUtf(256));
    }

    private static void writeUploadResult(RegistryFriendlyByteBuf buf, UploadResultPayload payload) {
        buf.writeBoolean(payload.success());
        buf.writeUtf(payload.message(), 256);
    }
}
