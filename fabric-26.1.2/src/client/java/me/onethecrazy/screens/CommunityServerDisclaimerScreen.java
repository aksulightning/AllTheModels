package me.onethecrazy.screens;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.util.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

public class CommunityServerDisclaimerScreen extends Screen {
    private static final int MARGIN = 50;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 20;
    private static final int CHECKBOX_SIZE = 20;
    private static final int LINE_SPACING = 24;

    private final Screen parent;
    private final Screen proceedScreen;
    private boolean hideNextTime = false;

    public CommunityServerDisclaimerScreen(Screen parent, Screen proceedScreen) {
        super(Component.translatable("gui.fbxplayermodels.title.community_server_disclaimer"));
        this.parent = parent;
        this.proceedScreen = proceedScreen;
    }

    @Override
    protected void init() {
        int buttonY = this.height - 52;
        int totalButtonWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int startX = this.width / 2 - totalButtonWidth / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.fbxplayermodels.proceed"),
                        button -> proceed())
                .bounds(startX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.back"),
                        button -> goBack())
                .bounds(startX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        String title = Component.translatable("gui.fbxplayermodels.title.community_server_disclaimer").getString();
        context.text(font, title, MARGIN, MARGIN, 0xFFFFFFFF, true);

        int textY = MARGIN + LINE_SPACING * 3;
        for (String line : wrap(Component.translatable("gui.fbxplayermodels.description.community_server_disclaimer").getString(), getTextWidth())) {
            context.text(font, line, MARGIN, textY, 0xFFFFFFFF, true);
            textY += LINE_SPACING;
        }

        drawCheckbox(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isInsideCheckbox(event.x(), event.y())) {
            hideNextTime = !hideNextTime;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        goBack();
    }

    private void proceed() {
        if (hideNextTime) {
            FBXPlayerModelsClient.options().hideCommunityServerDisclaimer = true;
            FileUtil.writeSave(FBXPlayerModelsClient.options());
        }

        Minecraft.getInstance().setScreen(proceedScreen);
    }

    private void goBack() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void drawCheckbox(GuiGraphicsExtractor context) {
        int checkboxX = getCheckboxX();
        int checkboxY = this.height - 88;
        int textX = checkboxX + CHECKBOX_SIZE + 8;
        String mark = hideNextTime ? "[X]" : "[ ]";

        context.text(font, mark, checkboxX, checkboxY + 6, 0xFFFFFFFF, true);
        context.text(font, Component.translatable("gui.fbxplayermodels.do_not_show_again").getString(), textX, checkboxY + 6, 0xFFFFFFFF, true);
    }

    private boolean isInsideCheckbox(double mouseX, double mouseY) {
        int checkboxX = getCheckboxX();
        int checkboxY = this.height - 88;
        int labelWidth = font.width(Component.translatable("gui.fbxplayermodels.do_not_show_again"));
        return mouseX >= checkboxX && mouseX <= checkboxX + CHECKBOX_SIZE + 8 + labelWidth
                && mouseY >= checkboxY && mouseY <= checkboxY + CHECKBOX_SIZE;
    }

    @Unique private int getTextWidth() {
        return Math.max(1, this.width - MARGIN * 2);
    }

    @Unique private int getCheckboxX() {
        int labelWidth = font.width(Component.translatable("gui.fbxplayermodels.do_not_show_again"));
        return this.width / 2 - (CHECKBOX_SIZE + 8 + labelWidth) / 2;
    }

    @Unique private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(candidate) <= maxWidth) {
                currentLine = new StringBuilder(candidate);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
