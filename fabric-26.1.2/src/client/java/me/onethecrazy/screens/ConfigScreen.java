package me.onethecrazy.screens;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.editor.ModelBindingEditorScreen;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import me.onethecrazy.util.objects.CacheSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

public class ConfigScreen extends Screen {
    // Constants
    private static final int MAX_SKIN_PREVIEW_DIMENSIONS = 300;
    private static final int MIN_SKIN_PREVIEW_DIMENSIONS = 120;
    private static final int MAX_CONTENT_WIDTH = 300;
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
    private Button uploadAgainButton;
    private Button editorButton;
    private Button doneButton;
    private final Screen parent;
    private boolean rotating = false;

    public ConfigScreen() {
        this(null);
    }

    public ConfigScreen(Screen parent) {
        super(Component.nullToEmpty("FBX Player Models"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        ConfigScreen configScreen = new ConfigScreen(parent);
        return FBXPlayerModelsClient.options().hideCommunityServerDisclaimer
                ? configScreen
                : new CommunityServerDisclaimerScreen(parent, configScreen);
    }

    @Override
    protected void init(){
        // init skinPreviewRenderer
        skinPreviewRenderer = new SkinPreviewRenderer(getCellOriginX(), getCellOriginY(), getScreenFriendlyDimensions(), getScreenFriendlyScale());

        // Init Buttons
        selectSkinButton = Button.builder(Component.empty(),
                (button) -> SkinManager.pickClientSkin()
        ).bounds(getContentOriginX(), getButtonsStartY(), getContentWidth(), BUTTON_HEIGHT).build();

        resetButton = Button.builder(
                    Component.empty(),
                    (button) -> SkinManager.resetSelfSkin())
                    .bounds(getContentOriginX(), selectSkinButton.getY() + Y_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        toggleButton = Button.builder(Component.empty(), (button) -> {
            FBXPlayerModelsClient.options().isEnabled = !FBXPlayerModelsClient.options().isEnabled;

            // Update Text
            updateEnabledButtonText();
        }).bounds(getContentOriginX() + getHalfButtonWidth() + MARGIN, selectSkinButton.getY() + Y_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        uploadAgainButton = Button.builder(Component.empty(), (button) ->
                SkinManager.uploadSelectedSkin(false)
        ).bounds(getContentOriginX(), resetButton.getY() + Y_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        editorButton = Button.builder(Component.nullToEmpty("Settings"), (button) ->
                Minecraft.getInstance().setScreen(new ModelBindingEditorScreen(this))
        ).bounds(getContentOriginX() + getHalfButtonWidth() + MARGIN, resetButton.getY() + Y_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        doneButton = Button.builder(
                Component.translatable("gui.done"),
                (button) -> onClose())
                .bounds(getContentOriginX(), editorButton.getY() + Y_SPACING + font.lineHeight + 2 * MARGIN, getContentWidth(), BUTTON_HEIGHT).build();

        this.addRenderableOnly((context, mouseX, mouseY, delta) -> skinPreviewRenderer.renderPreview(context, delta));
        this.addRenderableWidget(selectSkinButton);
        this.addRenderableWidget(resetButton);
        this.addRenderableWidget(toggleButton);
        this.addRenderableWidget(uploadAgainButton);
        this.addRenderableWidget(editorButton);
        this.addRenderableWidget(doneButton);
        this.addRenderableOnly(this::renderText);

        // Set Button Texts
        updateSelectButtonText();
        updateResetButtonText();
        updateUploadAgainButtonText();
        updateEnabledButtonText();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Update Text
        updateSelectButtonText();
        updateUploadAgainButtonText();

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void renderText(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Render the banner text
        context.text(font, trimmed(FBXPlayerModelsClient.bannerText, getContentWidth()), getContentOriginX(), getCellOriginY() + getScreenFriendlyDimensions() + MARGIN, 0xFFFFFFFF, true);

        CacheSkin cacheSkin = getSelfCacheSkin();
        if(cacheSkin != null)
            context.text(font, trimmed(cacheSkin.debugStatus(), getContentWidth()), getContentOriginX(), editorButton.getY() + BUTTON_HEIGHT + MARGIN, 0xFFFFFFFF, true);

        // Render Mod Title
        String title = "FBX Player Models";
        context.text(font, title, this.width / 2 - font.width(title) / 2, MARGIN, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && parent != null) {
            this.minecraft.setScreen(parent);
            return;
        }

        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isInsideCell(event.x(), event.y())) {
            rotating = true;
            return true; // start drag mode
        }

        return super.mouseClicked(event, doubleClick);
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
        int availableHeight = this.height - getCellOriginY() - getControlsHeight() - MARGIN;
        int availableWidth = this.width - 2 * MARGIN;
        return Math.max(MIN_SKIN_PREVIEW_DIMENSIONS, Math.min(MAX_SKIN_PREVIEW_DIMENSIONS, Math.min(availableHeight, availableWidth)));
    }

    @Unique private float getScreenFriendlyScale(){
        return getScreenFriendlyDimensions() / 2f - 10;
    }

    @Unique private int getContentWidth(){
        int availableWidth = Math.max(1, this.width - 2 * MARGIN);
        return Math.min(MAX_CONTENT_WIDTH, availableWidth);
    }

    @Unique private int getContentOriginX(){
        return this.width / 2 - getContentWidth() / 2;
    }

    @Unique private int getButtonsStartY(){
        return getCellOriginY() + getScreenFriendlyDimensions() + Y_SPACING;
    }

    @Unique private int getHalfButtonWidth(){
        return (getContentWidth() - MARGIN) / 2;
    }

    @Unique private int getControlsHeight(){
        return Y_SPACING
                + BUTTON_HEIGHT
                + Y_SPACING
                + BUTTON_HEIGHT
                + Y_SPACING
                + BUTTON_HEIGHT
                + Y_SPACING
                + BUTTON_HEIGHT
                + font.lineHeight
                + 2 * MARGIN
                + BUTTON_HEIGHT;
    }

    private boolean isInsideCell(double x, double y) {
        int x0 = getCellOriginX();
        int y0 = getCellOriginY();
        int s  = getScreenFriendlyDimensions();
        return x >= x0 && x <= x0 + s && y >= y0 && y <= y0 + s;
    }

    // Button Text helpers
    @Unique private void updateSelectButtonText(){
        Component text = Objects.equals(FBXPlayerModelsClient.options().selectedSkin.hash, "")
                ? Component.translatable("gui.fbxplayermodels.select_skin")
                : Component.nullToEmpty(trimmed(FBXPlayerModelsClient.options().selectedSkin.name, selectSkinButton.getWidth() - 12));

        selectSkinButton.setMessage(text);
    }

    @Unique private void updateResetButtonText(){
        resetButton.setMessage(Component.nullToEmpty(trimmed(Component.translatable("gui.fbxplayermodels.reset").getString(), resetButton.getWidth() - 12)));
    }

    @Unique private void updateUploadAgainButtonText(){
        uploadAgainButton.active = !Objects.equals(FBXPlayerModelsClient.options().selectedSkin.hash, "");
        uploadAgainButton.setMessage(Component.nullToEmpty(trimmed(Component.translatable("gui.fbxplayermodels.upload_again").getString(), uploadAgainButton.getWidth() - 12)));
    }

    @Unique private void updateEnabledButtonText(){
        Component text = FBXPlayerModelsClient.options().isEnabled ? Component.translatable("gui.fbxplayermodels.mod_enabled") : Component.translatable("gui.fbxplayermodels.mod_disabled");

        toggleButton.setMessage(text);
    }

    @Unique private String trimmed(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    @Unique private CacheSkin getSelfCacheSkin() {
        var uuid = Minecraft.getInstance().getUser().getProfileId();
        return uuid == null ? null : SkinManager.skinCache.get(uuid.toString());
    }

}
