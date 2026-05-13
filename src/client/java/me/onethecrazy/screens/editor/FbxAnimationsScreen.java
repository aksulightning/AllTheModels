package me.onethecrazy.screens.editor;

import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.SkinnedModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class FbxAnimationsScreen extends Screen {
    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 22;
    private final Screen parent;
    private List<String> clipNames = List.of();

    public FbxAnimationsScreen(Screen parent) {
        super(Text.of("FBX Animations"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clipNames = currentClips();
        AllTheSkinsClient.options().selectedSkin.clipMappings().remove("Idle");

        int x = MARGIN;
        int y = 36;
        int width = Math.min(300, this.width - MARGIN * 2);

        for (String state : List.of("Walk", "Sneak")) {
            String logicalState = state;
            addDrawableChild(ButtonWidget.builder(clipButtonText(logicalState), button -> {
                cycleClip(logicalState);
                button.setMessage(clipButtonText(logicalState));
            }).dimensions(x, y, width, 20).build());
            y += ROW_HEIGHT;
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(x, y + MARGIN, width, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, title, MARGIN, MARGIN, 0xFFFFFFFF, true);
        context.drawText(textRenderer, Text.of("Animation clips: " + clipNames.size()), MARGIN, 24, 0xFFCCCCCC, true);

        int listX = Math.min(330, this.width / 2);
        int listY = 40;
        context.drawText(textRenderer, Text.of("Imported FBX clips"), listX, listY - 16, 0xFFFFFFFF, true);
        if (clipNames.isEmpty()) {
            context.drawText(textRenderer, Text.of("No FBX clips detected."), listX, listY, 0xFFFFAAAA, true);
            return;
        }

        for (int i = 0; i < Math.min(18, clipNames.size()); i++) {
            context.drawText(textRenderer, Text.of(clipNames.get(i)), listX, listY + i * 10, 0xFFCCCCCC, false);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
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

    private Text clipButtonText(String state) {
        String value = AllTheSkinsClient.options().selectedSkin.clipMappings().getOrDefault(state, "");
        return Text.of(state + " Clip: " + (value.isBlank() ? "Procedural/default" : value));
    }

    private List<String> currentClips() {
        CacheSkin cache = currentCache();
        if (cache == null || cache.skinnedModel == null) {
            return List.of();
        }

        List<String> clips = new ArrayList<>();
        for (var entry : cache.skinnedModel.animations.entrySet()) {
            SkinnedModel.Animation animation = entry.getValue();
            if (!animation.logicalRigDriven()) {
                clips.add(entry.getKey());
            }
        }
        return clips;
    }

    private CacheSkin currentCache() {
        var uuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
        return uuid == null ? null : SkinManager.skinCache.get(uuid.toString());
    }
}
