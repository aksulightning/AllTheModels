package me.onethecrazy.screens;

import me.onethecrazy.AllTheSkinsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.editor.ModelBindingEditorScreen;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import me.onethecrazy.util.objects.CacheSkin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

public class ConfigScreen extends Screen {
    // Constants
    private static final int SKIN_PREVIEW_DIMENSIONS = 300;
    private static final int MARGIN = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int Y_SPACING = 24;
    private static final float YAW_SENS   = 0.6f;
    private static final float PITCH_SENS = 0.6f;

    // State stuff
    private SkinPreviewRenderer skinPreviewRenderer;
    private Button selectSkinButton;
    private Button resetButton;
    private Button toggleButton;
    private Button editorButton;
    private Button doneButton;
    private boolean rotating = false;

    public ConfigScreen() {
        super(Component.literal("All The Models"));
    }

    @Override
    protected void init(){
        // init skinPreviewRenderer
        skinPreviewRenderer = new SkinPreviewRenderer(getCellOriginX(), getCellOriginY(), getScreenFriendlyDimensions(), getScreenFriendlyScale());

        // Init Buttons
        selectSkinButton = Button.builder(Component.empty(),
                (button) -> SkinManager.pickClientSkin()
        ).bounds(getCellOriginX(), getCellOriginY() + getScreenFriendlyDimensions() + Y_SPACING, getScreenFriendlyDimensions(), BUTTON_HEIGHT).build();

        resetButton = Button.builder(
                    Component.translatable("gui.alltheskins.reset"),
                    (button) -> SkinManager.resetSelfSkin())
                    .bounds(getCellOriginX(), selectSkinButton.getY() + Y_SPACING, getScreenFriendlyDimensions() / 2 - MARGIN / 2, BUTTON_HEIGHT).build();

        toggleButton = Button.builder(Component.empty(), (button) -> {
            AllTheSkinsClient.options().isEnabled = !AllTheSkinsClient.options().isEnabled;

            // Update Text
            updateEnabledButtonText();
        }).bounds(getCellOriginX() + getScreenFriendlyDimensions() / 2 + MARGIN / 2, selectSkinButton.getY() + Y_SPACING, getScreenFriendlyDimensions() / 2 - MARGIN / 2, BUTTON_HEIGHT).build();

        editorButton = Button.builder(Component.literal("Edit Model Rig"), (button) ->
                Minecraft.getInstance().setScreen(new ModelBindingEditorScreen(this))
        ).bounds(getCellOriginX(), resetButton.getY() + Y_SPACING, getScreenFriendlyDimensions(), BUTTON_HEIGHT).build();

        doneButton = Button.builder(
                Component.translatable("gui.done"),
                (button) -> onClose())
                .bounds(getCellOriginX(), editorButton.getY() + Y_SPACING + 2 * MARGIN, getScreenFriendlyDimensions(), BUTTON_HEIGHT).build();

        this.addRenderableWidget(selectSkinButton);
        this.addRenderableWidget(resetButton);
        this.addRenderableWidget(toggleButton);
        this.addRenderableWidget(editorButton);
        this.addRenderableWidget(doneButton);

        // Set Button Texts
        updateSelectButtonText();
        updateEnabledButtonText();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        // Render the skin preview
        skinPreviewRenderer.renderPreview(context, delta);

        // Update Text
        updateSelectButtonText();

        // Render the banner text
        if (AllTheSkinsClient.bannerText != null && !AllTheSkinsClient.bannerText.isBlank()) {
            context.text(font, AllTheSkinsClient.bannerText, getCellOriginX(), getCellOriginY() + getScreenFriendlyDimensions() + MARGIN, 0xFFFFFFFF, true);
        }

        CacheSkin cacheSkin = SkinManager.skinCache.get(Minecraft.getInstance().getUser().getProfileId().toString());
        if(cacheSkin != null)
            context.text(font, cacheSkin.debugStatus(), getCellOriginX(), editorButton.getY() + BUTTON_HEIGHT + MARGIN, 0xFFFFFFFF, true);

        // Render Mod Title
        String title = "All The Models";
        context.text(font, title, this.width / 2 - font.width(title) / 2, MARGIN, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && isInsideCell(event.x(), event.y())) {
            rotating = true;
            return true; // start drag mode
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (rotating && event.button() == 0) {
            rotating = false;
            return true; // stop rotation mode
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (rotating && event.button() == 0) {
            // convert mouse motion to yaw/pitch deltas
            float yawDelta   = (float) (deltaX * YAW_SENS);
            float pitchDelta = (float) (-deltaY * PITCH_SENS);

            // apply directly
            skinPreviewRenderer.addRotation(yawDelta, pitchDelta);
            return true; // consume drag
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    // Position Helpers
    @Unique
    private int getCellOriginY(){
        return MARGIN * 4;
    }

    @Unique private int getCellOriginX(){
        return this.width / 2 - getScreenFriendlyDimensions() / 2;
    }

    @Unique private int getScreenFriendlyDimensions(){
        return Math.min(SKIN_PREVIEW_DIMENSIONS, this.height - 175);
    }

    @Unique private float getScreenFriendlyScale(){
        return getScreenFriendlyDimensions() / 2f - 10;
    }

    private boolean isInsideCell(double x, double y) {
        int x0 = getCellOriginX();
        int y0 = getCellOriginY();
        int s  = getScreenFriendlyDimensions();
        return x >= x0 && x <= x0 + s && y >= y0 && y <= y0 + s;
    }

    // Button Text helpers
    @Unique private void updateSelectButtonText(){
        Component text = Objects.equals(AllTheSkinsClient.options().selectedSkin.hash, "") ? Component.translatable("gui.alltheskins.select_skin") : Component.literal(AllTheSkinsClient.options().selectedSkin.name);

        selectSkinButton.setMessage(text);
    }

    @Unique private void updateEnabledButtonText(){
        Component text = AllTheSkinsClient.options().isEnabled ? Component.translatable("gui.alltheskins.mod_enabled") : Component.translatable("gui.alltheskins.mod_disabled");

        toggleButton.setMessage(text);
    }

}
