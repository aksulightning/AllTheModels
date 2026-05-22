package me.onethecrazy.screens;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.util.FileUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
        super(Text.translatable("gui.fbxplayermodels.title.community_server_disclaimer"));
        this.parent = parent;
        this.proceedScreen = proceedScreen;
    }

    @Override
    protected void init() {
        int buttonY = this.height - 52;
        int totalButtonWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int startX = this.width / 2 - totalButtonWidth / 2;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.fbxplayermodels.proceed"),
                        button -> proceed())
                .dimensions(startX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.back"),
                        button -> goBack())
                .dimensions(startX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        String title = Text.translatable("gui.fbxplayermodels.title.community_server_disclaimer").getString();
        context.drawText(textRenderer, title, MARGIN, MARGIN, 0xFFFFFFFF, true);

        int textY = MARGIN + LINE_SPACING * 3;
        for (String line : wrap(Text.translatable("gui.fbxplayermodels.description.community_server_disclaimer").getString(), getTextWidth())) {
            context.drawText(textRenderer, line, MARGIN, textY, 0xFFFFFFFF, true);
            textY += LINE_SPACING;
        }

        drawCheckbox(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideCheckbox(mouseX, mouseY)) {
            hideNextTime = !hideNextTime;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        goBack();
    }

    private void proceed() {
        if (hideNextTime) {
            FBXPlayerModelsClient.options().hideCommunityServerDisclaimer = true;
            FileUtil.writeSave(FBXPlayerModelsClient.options());
        }

        MinecraftClient.getInstance().setScreen(proceedScreen);
    }

    private void goBack() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void drawCheckbox(DrawContext context) {
        int checkboxX = getCheckboxX();
        int checkboxY = this.height - 88;
        int textX = checkboxX + CHECKBOX_SIZE + 8;
        String label = Text.translatable("gui.fbxplayermodels.do_not_show_again").getString();

        context.fill(checkboxX, checkboxY, checkboxX + CHECKBOX_SIZE, checkboxY + CHECKBOX_SIZE, 0xFF000000);
        context.drawBorder(checkboxX, checkboxY, CHECKBOX_SIZE, CHECKBOX_SIZE, 0xFF808080);
        if (hideNextTime) {
            context.drawText(textRenderer, "X", checkboxX + 6, checkboxY + 6, 0xFFFFFFFF, true);
        }

        context.drawText(textRenderer, label, textX, checkboxY + 6, 0xFFFFFFFF, true);
    }

    private boolean isInsideCheckbox(double mouseX, double mouseY) {
        int checkboxX = getCheckboxX();
        int checkboxY = this.height - 88;
        int labelWidth = textRenderer.getWidth(Text.translatable("gui.fbxplayermodels.do_not_show_again"));
        return mouseX >= checkboxX && mouseX <= checkboxX + CHECKBOX_SIZE + 8 + labelWidth
                && mouseY >= checkboxY && mouseY <= checkboxY + CHECKBOX_SIZE;
    }

    @Unique private int getTextWidth() {
        return Math.max(1, this.width - MARGIN * 2);
    }

    @Unique private int getCheckboxX() {
        int labelWidth = textRenderer.getWidth(Text.translatable("gui.fbxplayermodels.do_not_show_again"));
        return this.width / 2 - (CHECKBOX_SIZE + 8 + labelWidth) / 2;
    }

    @Unique private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (textRenderer.getWidth(candidate) <= maxWidth) {
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
