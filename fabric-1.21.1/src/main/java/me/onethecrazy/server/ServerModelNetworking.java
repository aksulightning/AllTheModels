package me.onethecrazy.server;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.onethecrazy.FBXPlayerModelsMod;
import me.onethecrazy.network.ModelPackets;
import me.onethecrazy.network.ModelPackets.LookupResponsePayload;
import me.onethecrazy.network.ModelPackets.ModelDataPayload;
import me.onethecrazy.network.ModelPackets.ModelLookup;
import me.onethecrazy.network.ModelPackets.UploadResultPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ServerModelNetworking {
    private static final Map<MinecraftServer, ServerModelStore> STORES = new WeakHashMap<>();

    private ServerModelNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(ModelPackets.UploadModelPayload.ID, ModelPackets.UploadModelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelPackets.RequestLookupPayload.ID, ModelPackets.RequestLookupPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelPackets.RequestModelPayload.ID, ModelPackets.RequestModelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.LookupResponsePayload.ID, ModelPackets.LookupResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.ModelDataPayload.ID, ModelPackets.ModelDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.UploadResultPayload.ID, ModelPackets.UploadResultPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.UploadModelPayload.ID, (payload, context) ->
                context.server().execute(() -> handleUpload(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestLookupPayload.ID, (payload, context) ->
                context.server().execute(() -> handleLookup(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestModelPayload.ID, (payload, context) ->
                context.server().execute(() -> handleModelRequest(context.player(), payload)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("fbxplayermodels")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("uploadperm")
                                .then(CommandManager.argument("playername", StringArgumentType.word())
                                        .then(CommandManager.literal("yes").executes(context -> setUploadPerm(context.getSource(), StringArgumentType.getString(context, "playername"), true)))
                                        .then(CommandManager.literal("no").executes(context -> setUploadPerm(context.getSource(), StringArgumentType.getString(context, "playername"), false)))))));
    }

    private static void handleUpload(ServerPlayerEntity player, ModelPackets.UploadModelPayload payload) {
        ServerModelStore store = store(player.getServer());
        ServerModelStore.UploadSaveResult result = store.saveUpload(player, payload.fileName(), payload.format(), payload.data());
        ServerPlayNetworking.send(player, new UploadResultPayload(result.success(), result.message()));

        if (result.success() && result.model() != null) {
            broadcastLookup(player.getServer(), result.model());
        }
    }

    private static void handleLookup(ServerPlayerEntity player, ModelPackets.RequestLookupPayload payload) {
        ServerModelStore store = store(player.getServer());
        Map<String, ModelLookup> response = new HashMap<>();
        for (String uuid : payload.uuids()) {
            response.put(uuid, store.lookup(uuid)
                    .map(model -> new ModelLookup(model.hash(), model.format()))
                    .orElseGet(() -> new ModelLookup("", "")));
        }
        ServerPlayNetworking.send(player, new LookupResponsePayload(response));
    }

    private static void handleModelRequest(ServerPlayerEntity player, ModelPackets.RequestModelPayload payload) {
        try {
            byte[] data = store(player.getServer()).readModel(payload.hash(), payload.format()).orElse(null);
            if (data == null) {
                ServerPlayNetworking.send(player, new ModelDataPayload(payload.hash(), payload.format(), new byte[0], "Model not found."));
                return;
            }
            ServerPlayNetworking.send(player, new ModelDataPayload(payload.hash(), payload.format(), data, "Saved successfully."));
        } catch (IOException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while reading model", e);
            ServerPlayNetworking.send(player, new ModelDataPayload(payload.hash(), payload.format(), new byte[0], "Server-side failure: could not read model."));
        }
    }

    private static int setUploadPerm(ServerCommandSource source, String playerName, boolean allowed) {
        try {
            store(source.getServer()).setUploadPermission(playerName, allowed);
            if (allowed) {
                source.sendFeedback(() -> Text.literal("Granted upload permission to " + playerName + "."), true);
            } else {
                source.sendFeedback(() -> Text.literal("Removed upload permission from " + playerName + "."), true);
            }
            return 1;
        } catch (IOException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while saving upload permission", e);
            source.sendError(Text.literal("Server-side failure: could not save upload permission."));
            return 0;
        }
    }

    private static void broadcastLookup(MinecraftServer server, ServerModelStore.StoredModel model) {
        LookupResponsePayload payload = new LookupResponsePayload(Map.of(model.uuid(), new ModelLookup(model.hash(), model.format())));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static synchronized ServerModelStore store(MinecraftServer server) {
        return STORES.computeIfAbsent(server, ServerModelStore::new);
    }
}
