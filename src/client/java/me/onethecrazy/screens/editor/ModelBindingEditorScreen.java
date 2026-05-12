package me.onethecrazy.screens.editor;

import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.model.rig.LogicalBodyPart;
import me.onethecrazy.util.model.rig.LogicalRigBinding;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.SkinnedModel;
import me.onethecrazy.util.parsing.FBXParser;
import me.onethecrazy.util.parsing.ParsingFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModelBindingEditorScreen extends Screen {
    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 22;
    private final Screen parent;
    private List<String> boneNames = List.of();
    private List<String> clipNames = List.of();

    public ModelBindingEditorScreen(Screen parent) {
        super(Component.literal("Model Rig Binding"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boneNames = currentBones();
        clipNames = currentClips();
        int x = MARGIN;
        int y = 36;
        int width = Math.min(300, this.width - MARGIN * 2);

        for (LogicalBodyPart part : LogicalBodyPart.values()) {
            LogicalBodyPart target = part;
            addRenderableWidget(Button.builder(buttonText(target), button -> {
                cycleBinding(target);
                button.setMessage(buttonText(target));
            }).bounds(x, y, width, 20).build());
            y += ROW_HEIGHT;
        }

        addRenderableWidget(Button.builder(Component.literal("Auto Bind"), button -> {
            AllTheSkinsClient.options().selectedSkin.logicalRigBinding = LogicalRigBinding.autoBind(boneNames);
            SkinManager.saveCurrentBinding();
            Minecraft.getInstance().setScreen(new ModelBindingEditorScreen(parent));
        }).bounds(x, y + MARGIN, width / 2 - 3, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x + width / 2 + 3, y + MARGIN, width / 2 - 3, 20).build());

        int clipY = y + MARGIN + ROW_HEIGHT;
        for (String state : List.of("Idle", "Walk", "Sneak")) {
            String logicalState = state;
            addRenderableWidget(Button.builder(clipButtonText(logicalState), button -> {
                cycleClip(logicalState);
                button.setMessage(clipButtonText(logicalState));
            }).bounds(x, clipY, width, 20).build());
            clipY += ROW_HEIGHT;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.text(font, title, MARGIN, MARGIN, 0xFFFFFFFF, true);
        context.text(font, Component.literal("Detected bones: " + boneNames.size()), MARGIN, 24, 0xFFCCCCCC, true);

        int x = Math.min(330, this.width / 2);
        int y = 40;
        context.text(font, Component.literal("Bones / objects"), x, y - 16, 0xFFFFFFFF, true);
        if (boneNames.isEmpty()) {
            context.text(font, Component.literal("No bindable bones detected."), x, y, 0xFFFFAAAA, true);
        } else {
            for (int i = 0; i < Math.min(18, boneNames.size()); i++) {
                context.text(font, Component.literal(boneNames.get(i)), x, y + i * 10, 0xFFCCCCCC, false);
            }
        }

        int clipY = y + Math.min(18, Math.max(1, boneNames.size())) * 10 + 24;
        context.text(font, Component.literal("Animation clips: " + clipNames.size()), x, clipY, 0xFFFFFFFF, true);
        for (int i = 0; i < Math.min(8, clipNames.size()); i++) {
            context.text(font, Component.literal(clipNames.get(i)), x, clipY + 12 + i * 10, 0xFFCCCCCC, false);
        }

        int pivotY = Math.max(220, clipY + 112);
        context.text(font, Component.literal("Logical bind pivots"), x, pivotY, 0xFFFFFFFF, true);
        int pivotRow = 0;
        for (LogicalBodyPart part : LogicalBodyPart.values()) {
            context.text(font, Component.literal(part.displayName + ": " + pivotText(part)), x, pivotY + 12 + pivotRow * 10, 0xFFCCCCCC, false);
            pivotRow++;
        }

        List<String> warnings = AllTheSkinsClient.options().selectedSkin.warnings();
        if (!warnings.isEmpty()) {
            int warningY = this.height - 56 - Math.min(2, warnings.size()) * 10;
            for (int i = 0; i < Math.min(2, warnings.size()); i++) {
                context.text(font, Component.literal(warnings.get(i)), MARGIN, warningY + i * 10, 0xFFFFFF88, false);
            }
        }

        CacheSkin cache = currentCache();
        String status = cache == null ? "Rig: none" : cache.debugStatus();
        context.text(font, Component.literal(status), MARGIN, this.height - 28, 0xFFCCCCCC, true);
        context.text(font, Component.literal("Runtime animation is rotation-only around each bind pivot."), MARGIN, this.height - 16, 0xFFAAAAAA, true);

        if (cache != null && cache.format == ParsingFormat.FBX) {
            int materialY = Math.max(40, this.height - 110);
            int materialX = Math.min(this.width - 220, Math.max(330, this.width / 2));
            List<String> diagnostics = FBXParser.lastMaterialDiagnostics();
            for (int i = 0; i < Math.min(6, diagnostics.size()); i++) {
                context.text(font, Component.literal(diagnostics.get(i)), materialX, materialY + i * 10, 0xFFCCCCCC, false);
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void cycleBinding(LogicalBodyPart part) {
        LogicalRigBinding binding = AllTheSkinsClient.options().selectedSkin.binding();
        if (boneNames.isEmpty()) {
            binding.setSingle(part, "");
            SkinManager.saveCurrentBinding();
            return;
        }

        String current = binding.firstName(part);
        int next = current.isBlank() ? 0 : boneNames.indexOf(current) + 1;
        if (next < 0 || next >= boneNames.size()) {
            binding.setSingle(part, "");
        } else {
            binding.setSingle(part, boneNames.get(next));
        }
        SkinManager.saveCurrentBinding();
    }

    private Component buttonText(LogicalBodyPart part) {
        String value = AllTheSkinsClient.options().selectedSkin.binding().firstName(part);
        return Component.literal(part.displayName + ": " + (value.isBlank() ? "Unbound" : value));
    }

    private void cycleClip(String state) {
        if (clipNames.isEmpty()) {
            AllTheSkinsClient.options().selectedSkin.clipMappings().remove(state);
            SkinManager.saveCurrentBinding();
            return;
        }

        String current = AllTheSkinsClient.options().selectedSkin.clipMappings().getOrDefault(state, "");
        int next = current.isBlank() ? 0 : clipNames.indexOf(current) + 1;
        if (next < 0 || next >= clipNames.size()) {
            AllTheSkinsClient.options().selectedSkin.clipMappings().remove(state);
        } else {
            AllTheSkinsClient.options().selectedSkin.clipMappings().put(state, clipNames.get(next));
        }
        SkinManager.saveCurrentBinding();
    }

    private Component clipButtonText(String state) {
        String value = AllTheSkinsClient.options().selectedSkin.clipMappings().getOrDefault(state, "");
        return Component.literal(state + " Clip: " + (value.isBlank() ? "Procedural/default" : value));
    }

    private List<String> currentBones() {
        CacheSkin cache = currentCache();
        if (cache == null || cache.skinnedModel == null) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (SkinnedModel.Bone bone : cache.skinnedModel.bones) {
            names.add(bone.name());
        }
        return names;
    }

    private List<String> currentClips() {
        CacheSkin cache = currentCache();
        if (cache == null || cache.skinnedModel == null) {
            return List.of();
        }
        return new ArrayList<>(cache.skinnedModel.animations.keySet());
    }

    private String pivotText(LogicalBodyPart part) {
        String bound = AllTheSkinsClient.options().selectedSkin.binding().firstName(part);
        if (bound.isBlank()) {
            return "Unbound";
        }

        CacheSkin cache = currentCache();
        if (cache == null || cache.skinnedModel == null) {
            return "Unavailable";
        }

        String normalized = LogicalRigBinding.normalize(bound);
        for (SkinnedModel.Bone bone : cache.skinnedModel.bones) {
            if (LogicalRigBinding.normalize(bone.name()).equals(normalized)) {
                Vector3f pivot = bone.localBind().getTranslation(new Vector3f());
                return String.format(Locale.ROOT, "%.3f, %.3f, %.3f", pivot.x, pivot.y, pivot.z);
            }
        }

        return "Missing bound bone";
    }

    private CacheSkin currentCache() {
        String uuid = Minecraft.getInstance().getUser().getProfileId().toString();
        return SkinManager.skinCache.get(uuid);
    }
}
