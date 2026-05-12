package me.onethecrazy.mixin.client;

import me.onethecrazy.*;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import me.onethecrazy.util.ToastUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class MainMenuMixin extends Screen{
    // Constants to be compatible with the TitleScreen
    @Unique private static final int MARGIN = 6;
    @Unique private static final int BUTTON_WIDTH = 98;
    @Unique private static final int Y_SPACING = 24;
    @Unique private static final int SKIN_CELL_DIMENSIONS = 68;

    // Other Constants
    @Unique private static final float PLAYER_SKIN_PREVIEW_SCALE = 30f;

    @Unique private boolean hasModerationNoticeBeenShown = false;
    @Unique private SkinPreviewRenderer skinPreviewRenderer;

    protected MainMenuMixin(Component title) {
        super(title);
    }

    @Inject(method = "init*", at = @At("TAIL"))
    private void onInit(CallbackInfo ci){
        // Create a SkinPreviewRenderer instance
        skinPreviewRenderer = new SkinPreviewRenderer(getCellOriginX(), getCellOriginY(), SKIN_CELL_DIMENSIONS, PLAYER_SKIN_PREVIEW_SCALE);

        // Show Moderation Notice everytime we open Main Menu
        if(!hasModerationNoticeBeenShown && AllTheSkinsClient.isFirstStartup){
            ToastUtil.showModerationNoticeToast();
            hasModerationNoticeBeenShown = true;
        }
    }


    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci){
        // Draw skin Preview
        skinPreviewRenderer.renderPreview(ctx, deltaTicks);
    }

    @Inject(method = "mouseClicked", at=@At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir){
        // If we're inside the skin cell, open config screen
        if(event.x() > getCellOriginX() && event.x() < getCellOriginX() + SKIN_CELL_DIMENSIONS && event.y() > getCellOriginY() && event.y() < getCellOriginY() + SKIN_CELL_DIMENSIONS){
            Minecraft.getInstance().setScreen(new ConfigScreen());

            // We handled the click
            cir.setReturnValue(true);
        }
    }

    // Position Helpers
    @Unique private int getCellOriginY(){
        return this.height / 4 + Y_SPACING * 2;
    }

    @Unique private int getCellOriginX(){
        return this.width / 2 - MARGIN - BUTTON_WIDTH - SKIN_CELL_DIMENSIONS;
    }
}
