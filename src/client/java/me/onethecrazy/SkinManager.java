package me.onethecrazy;

import me.onethecrazy.util.*;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.LookupSkin;
import me.onethecrazy.util.objects.SkinnedModel;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.objects.save.ClientSkin;
import me.onethecrazy.util.model.animation.LogicalRigAnimator;
import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.parsing.UniversalParser;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SkinManager {
    public static Map<String, LookupSkin> skinLookup = new HashMap<>();
    public static Map<String, CacheSkin> skinCache = new HashMap<>();

    private static final Minecraft client = Minecraft.getInstance();

    public static void pickClientSkin(){

        // Open File picker dialogue
        ClientFileUtil.modelPickerDialog()
                // Execute when user completes File-Selection
                .thenAccept(f -> {
                    if(f == null || Objects.equals(f, ""))
                        return;

                    try{
                        long fileSize = Files.size(Path.of(f));

                        long MAX_FILE_SIZE = 20L * 1024 * 1024;

                        // Restrict File size to 20mb
                        if(fileSize > MAX_FILE_SIZE){
                            ToastUtil.showFileTooLargeToast();
                            return;
                        }
                    }
                    catch(Exception ex) {
                        AllTheSkins.LOGGER.info("Ran into error while getting file size in client skin picker: {0}", ex);
                        return;
                    }

                    // Execute on Render Thread
                    Minecraft.getInstance().execute(() -> SkinManager.selectSelfSkin(Path.of(f)));
                });
    }

    public static void selectSelfSkin(Path dataPath){
        try{
            String uuid = client.getUser().getProfileId().toString();

            byte[] data3D = FileUtil.read3DDataFile(dataPath);
            String name = dataPath.getFileName().toString();
            String hash = FileUtil.getSha256(data3D);
            ParsingFormat format = UniversalParser.getParsingFormat(dataPath);

            assert format != null;

            FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash, format), data3D);

            ClientSkin skin = new ClientSkin(hash, name, format);
            AllTheSkinsClient.options().selectedSkin = skin;

            // Save the updated options:
            FileUtil.writeSave(AllTheSkinsClient.options());

            // Reload self skin
            loadSelfSkin();

            // Send update to server
            BackendInteractor.setSkinData(uuid, data3D, format);
        }
        catch(Exception ex){
            AllTheSkins.LOGGER.info("Ran into error while setting self skin: {0}", ex);
        }
    }

    public static void resetSelfSkin(){
        String uuid = client.getUser().getProfileId().toString();

        AllTheSkinsClient.options().selectedSkin = new ClientSkin();

        // Reload self skin
        loadSelfSkin();

        // Save in options
        FileUtil.writeSave(AllTheSkinsClient.options());

        // Send update to server
        BackendInteractor.setSkinData(uuid, new byte[0], ParsingFormat.OBJ);
    }

    public static void loadSelfSkin(){
        String uuid = client.getUser().getProfileId().toString();

        // Set self skin to empty if we don't have a selected skin
        var selectedSkin = AllTheSkinsClient.options().selectedSkin;

        if(Objects.equals(selectedSkin.hash, "")){
            putLookupEntry(uuid, new LookupSkin("", null));
            putCacheEntry(uuid, (List<Vertex>) null, null);

            return;
        }

        // Load self skin
        try{
            Path data3DPath = FileUtil.getSkinPath(selectedSkin.hash, selectedSkin.format);
            var skinnedModel = UniversalParser.parseSkinned(data3DPath, selectedSkin.format)
                    .map(ModelNormalizer::normalize)
                    .map(model -> withSavedAnimationSettings(model, selectedSkin));
            List<Vertex> vertices = ModelNormalizer.normalize(UniversalParser.parse(data3DPath, selectedSkin.format));

            putLookupEntry(uuid, new LookupSkin(selectedSkin.hash, selectedSkin.format));
            skinnedModel.ifPresentOrElse(
                    model -> putCacheEntry(uuid, model, selectedSkin.format),
                    () -> putCacheEntry(uuid, vertices, selectedSkin.format)
            );
        }
        catch(Exception e){
            AllTheSkins.LOGGER.info("Ran into error while loading self skin data3D content:", e);
        }
    }

    public static void loadSkin(String uuid){
        // /getSkin
        // -> check local skins
        //      If not in local skins -> /files
        //          If response empty -> set entry to null
        //          Else -> entry to deserialized data3D

        // Put uuid into cache so that we don't request for this uuid again in RenderMixin
        skinCache.put(uuid, null);
        skinLookup.put(uuid, new LookupSkin("", null));

        // Player was never encountered before
        BackendInteractor.getSkinIDs(List.of(uuid))
                .thenAccept(map -> {
                    putLookupEntry(uuid, map.getOrDefault(uuid, null));

                    // We don't have the skin loaded (or want it to be updated)
                    loadSkinIntoCache(uuid);
                });
    }

    private static void loadSkinIntoCache(String uuid){
        // Load from I/O cache
        if(FileUtil.isSkinCached(skinLookup.get(uuid).hash))
        {
            try {
                Path path = FileUtil.getSkinPath(skinLookup.get(uuid).hash, skinLookup.get(uuid).format);
                var skinnedModel = UniversalParser.parseSkinned(path, skinLookup.get(uuid).format).map(ModelNormalizer::normalize);

                List<Vertex> vertices = ModelNormalizer.normalize(
                        UniversalParser.parse(
                            path
                        )
                );

                skinnedModel.ifPresentOrElse(
                        model -> putCacheEntry(uuid, model, skinLookup.get(uuid).format),
                        () -> putCacheEntry(uuid, vertices, skinLookup.get(uuid).format)
                );
            } catch (Exception e) {
                putCacheEntry(uuid, (List<Vertex>) null, null);

                AllTheSkins.LOGGER.error("Ran into error while loading skin from I/O Cache: {0}", e);
            }
        }
        // Request from Server
        else{
            BackendInteractor.getSkinData(skinLookup.get(uuid), (data3D) -> {
                if(data3D.length != 0){
                    var lookupResult = skinLookup.get(uuid);
                    String hash = lookupResult.hash;

                    // Try saving to local Cache
                    try {
                        FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash, lookupResult.format), data3D);
                    } catch (IOException e) {
                        AllTheSkins.LOGGER.error("Ran into error while saving skin to I/O cache: ", e);
                    }

                    Path path = FileUtil.getSkinPath(hash, lookupResult.format);
                    var skinnedModel = UniversalParser.parseSkinned(path, lookupResult.format).map(ModelNormalizer::normalize);
                    List<Vertex> vertices = ModelNormalizer.normalize(UniversalParser.parse(path));
                    skinnedModel.ifPresentOrElse(
                            model -> putCacheEntry(uuid, model, lookupResult.format),
                            () -> putCacheEntry(uuid, vertices, lookupResult.format)
                    );
                }
                else{
                    putCacheEntry(uuid, (List<Vertex>) null, null);
                }
            });
        }
    }

    public static void putCacheEntry(String uuid, @Nullable List<Vertex> vertices, ParsingFormat format){
        skinCache.put(uuid, new CacheSkin(vertices, format));
    }

    public static void putCacheEntry(String uuid, @Nullable SkinnedModel skinnedModel, ParsingFormat format){
        skinCache.put(uuid, new CacheSkin(skinnedModel, format));
    }

    public static void putLookupEntry(String uuid, LookupSkin lookupSkin){
        skinLookup.put(uuid, lookupSkin);
    }

    public static void saveCurrentBinding() {
        FileUtil.writeSave(AllTheSkinsClient.options());
        loadSelfSkin();
    }

    private static SkinnedModel withSavedAnimationSettings(SkinnedModel model, ClientSkin selectedSkin) {
        Map<String, SkinnedModel.Animation> importedAnimations = rotationOnlyAnimations(model.animations, selectedSkin);
        Map<String, SkinnedModel.Animation> animations = new LinkedHashMap<>(importedAnimations);
        LogicalRigAnimator.proceduralAnimations(model.bones, selectedSkin.binding()).forEach(animations::putIfAbsent);

        for (Map.Entry<String, String> entry : selectedSkin.clipMappings().entrySet()) {
            SkinnedModel.Animation mapped = importedAnimations.get(entry.getValue());
            if (mapped != null) {
                animations.put(entry.getKey(), mapped);
            }
        }

        return model.withLogicalRigBinding(selectedSkin.binding()).withAnimations(animations);
    }

    private static Map<String, SkinnedModel.Animation> rotationOnlyAnimations(Map<String, SkinnedModel.Animation> source, ClientSkin selectedSkin) {
        Map<String, SkinnedModel.Animation> stripped = new LinkedHashMap<>();
        boolean ignoredTranslations = false;

        for (Map.Entry<String, SkinnedModel.Animation> entry : source.entrySet()) {
            SkinnedModel.Animation animation = entry.getValue();
            if (animation.hasTranslationKeys()) {
                ignoredTranslations = true;
            }
            stripped.put(entry.getKey(), animation.rotationOnly());
        }

        if (ignoredTranslations) {
            String warning = "This animation contains translation keys. AllTheSkins only supports rotation-only logical rig animation, so translation keys were ignored.";
            if (!selectedSkin.warnings().contains(warning)) {
                selectedSkin.warnings().add(warning);
                FileUtil.writeSave(AllTheSkinsClient.options());
            }
        }

        return stripped;
    }
}
