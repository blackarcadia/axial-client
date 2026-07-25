package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialConfigManager;
import org.axial.axialutils.client.AxialUiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Locale;

@Mixin(targets = "org.axial.axialutils.client.TitleOverlaySettingsScreen", remap = false)
public abstract class TitleOverlaySettingsScreenMixin {
    private static final int UNIFIED_PANEL_WIDTH = 452;
    private static final int UNIFIED_PANEL_HEIGHT = 168;
    private static final int CONTROL_START_Y_WITHOUT_PREVIEW = 58;
    private static final int CONTROL_COLUMN_WIDTH = 203;
    private static final int CONTROL_COLUMN_GAP = 10;
    private static final int BACK_BUTTON_WIDTH = 24;
    private static final int BACK_BUTTON_HEIGHT = 18;
    private static final Identifier BACK_ARROW_ICON = AxialCosmetics.id("textures/gui/back-arrow.png");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    @Shadow
    private int panelX;

    @Shadow
    private int panelY;

    @Shadow
    private int panelHeight;

    @Shadow
    private int panelWidth;

    @Shadow
    public abstract void method_25419();

    private static Field axial_cosmetics$scaleSliderField;

    @Inject(method = "drawPreview", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$hideTitlePreview(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "drawControls", at = @At("HEAD"))
    private void axial_cosmetics$brightenScaleSlider(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        try {
            if (axial_cosmetics$scaleSliderField == null) {
                axial_cosmetics$scaleSliderField = this.getClass().getDeclaredField("scaleSlider");
                axial_cosmetics$scaleSliderField.setAccessible(true);
            }

            Object slider = axial_cosmetics$scaleSliderField.get(this);
            if (slider instanceof ClickableWidget widget) {
                widget.setAlpha(1.0f);
            }
        } catch (ReflectiveOperationException ignored) {
            // Keep the screen usable if axialutils changes the slider field.
        }
    }

    @Inject(method = "drawControls", at = @At("RETURN"))
    private void axial_cosmetics$drawVisibleScaleSlider(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        int x = panelX + 18;
        int y = panelY + CONTROL_START_Y_WITHOUT_PREVIEW;
        int width = panelWidth - 36;
        int height = 20;

        float scale = Math.max(0.5f, Math.min(3.0f, AxialConfigManager.get().titleOverlayScale));
        float progress = (scale - 0.5f) / 2.5f;
        int handleX = x + Math.round(progress * (width - 8));

        context.fill(x, y, x + width, y + height, 0xF00E1018);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x66FFFFFF);
        context.fill(x + 2, y + height / 2 - 2, x + width - 2, y + height / 2 + 2, 0xCC2A2F3C);
        context.fill(x + 2, y + height / 2 - 2, handleX + 4, y + height / 2 + 2, 0xFF8AF0C2);
        context.fill(handleX, y + 2, handleX + 8, y + height - 2, 0xFFE9D9FF);
        context.drawStrokedRectangle(handleX, y + 2, 8, height - 4, 0xFF8F5DFF);
        context.drawStrokedRectangle(x, y, width, height, 0xFF8F5DFF);

        Screen screen = (Screen) (Object) this;
        context.drawTextWithShadow(screen.getTextRenderer(), axial_cosmetics$uiText("SCALE"), x + 2, y - 12, 0xFFC6D0F3);
        String label = "SCALE: " + String.format(Locale.ROOT, "%.2fx", scale);
        context.drawCenteredTextWithShadow(screen.getTextRenderer(), axial_cosmetics$uiText(label), x + width / 2, y + 6, 0xFFFFFFFF);
    }

    @Inject(method = "method_25394", at = @At("RETURN"))
    private void axial_cosmetics$drawBackButton(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        int buttonX = panelX + 18;
        int buttonY = panelY + 6;
        boolean hovered = axial_cosmetics$inside(mouseX, mouseY, buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);

        axial_cosmetics$drawIconButton(context, buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, hovered);

        int iconWidth = 16;
        int iconHeight = 13;
        int iconX = buttonX + (BACK_BUTTON_WIDTH - iconWidth) / 2 - 4;
        int iconY = buttonY + (BACK_BUTTON_HEIGHT - iconHeight) / 2 - 1;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACK_ARROW_ICON, iconX, iconY, 0.0f, 0.0f, iconWidth, iconHeight, 64, 64, 64, 64);
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialUiTheme;drawButton(Lnet/minecraft/class_332;Lnet/minecraft/class_327;IIIILjava/lang/String;Ljava/lang/String;ZZI)V"
            ),
            index = 2
    )
    private int axial_cosmetics$hideDoneButtonX(int x) {
        return -10000;
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialUiTheme;drawButton(Lnet/minecraft/class_332;Lnet/minecraft/class_327;IIIILjava/lang/String;Ljava/lang/String;ZZI)V"
            ),
            index = 3
    )
    private int axial_cosmetics$hideDoneButtonY(int y) {
        return -10000;
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialUiTheme;drawButton(Lnet/minecraft/class_332;Lnet/minecraft/class_327;IIIILjava/lang/String;Ljava/lang/String;ZZI)V"
            ),
            index = 4
    )
    private int axial_cosmetics$hideDoneButtonWidth(int width) {
        return 0;
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialUiTheme;drawButton(Lnet/minecraft/class_332;Lnet/minecraft/class_327;IIIILjava/lang/String;Ljava/lang/String;ZZI)V"
            ),
            index = 5
    )
    private int axial_cosmetics$hideDoneButtonHeight(int height) {
        return 0;
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/AxialUiTheme;drawButton(Lnet/minecraft/class_332;Lnet/minecraft/class_327;IIIILjava/lang/String;Ljava/lang/String;ZZI)V"
            ),
            index = 6
    )
    private String axial_cosmetics$hideDoneButtonLabel(String label) {
        return "";
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/TitleOverlaySettingsScreen;drawStyledButton(Lnet/minecraft/class_332;IIIIIILnet/minecraft/class_2561;ZJ)V"
            ),
            index = 9
    )
    private long axial_cosmetics$removeToggleButtonPressAnimation(long pressedUntilMs) {
        return 0L;
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/TitleOverlaySettingsScreen;drawStyledButton(Lnet/minecraft/class_332;IIIIIILnet/minecraft/class_2561;ZJ)V",
                    ordinal = 1
            ),
            index = 3
    )
    private int axial_cosmetics$alignSecondToggleButtonX(int x) {
        return panelX + 18 + CONTROL_COLUMN_WIDTH + CONTROL_COLUMN_GAP;
    }

    @ModifyArg(
            method = "drawControls",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/axial/axialutils/client/TitleOverlaySettingsScreen;drawStyledButton(Lnet/minecraft/class_332;IIIIIILnet/minecraft/class_2561;ZJ)V"
            ),
            index = 5
    )
    private int axial_cosmetics$alignToggleButtonWidth(int width) {
        return CONTROL_COLUMN_WIDTH;
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$handleBackButton(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0) {
            return;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        if (axial_cosmetics$inside(mouseX, mouseY, panelX + 18, panelY + 6, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)) {
            AxialUiTheme.playButtonClickSound();
            axial_cosmetics$returnToModsMenu();
            cir.setReturnValue(true);
            return;
        }

        int doneY = panelY + panelHeight - 18 - 18;
        if (axial_cosmetics$inside(mouseX, mouseY, panelX + 18, doneY, 80, 18)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyConstant(method = "rebuildLayout", constant = @Constant(intValue = 316))
    private int axial_cosmetics$widenPanel(int original) {
        return UNIFIED_PANEL_WIDTH;
    }

    @ModifyConstant(method = "rebuildLayout", constant = @Constant(intValue = 278))
    private int axial_cosmetics$shrinkPanelHeight(int original) {
        return UNIFIED_PANEL_HEIGHT;
    }

    @ModifyConstant(method = "rebuildLayout", constant = @Constant(intValue = 130))
    private int axial_cosmetics$moveSliderWidgetUp(int original) {
        return CONTROL_START_Y_WITHOUT_PREVIEW;
    }

    @ModifyConstant(method = {"drawControls", "method_25402"}, constant = @Constant(intValue = 78))
    private int axial_cosmetics$removePreviewSpaceFromControls(int original) {
        return CONTROL_START_Y_WITHOUT_PREVIEW - 30 - 20;
    }

    private static boolean axial_cosmetics$inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text axial_cosmetics$uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private static void axial_cosmetics$drawIconButton(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        context.fill(x, y, x + width, y + height, hovered ? -1138749382 : -1609233884);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 872415231 : 385875967);
        context.drawStrokedRectangle(x, y, width, height, hovered ? -1583361 : -796105729);
    }

    private void axial_cosmetics$returnToModsMenu() {
        try {
            Object rootParent = axial_cosmetics$getParentScreen((Object) this);
            if (rootParent != null && "org.axial.axialutils.client.AxialConfigScreen".equals(rootParent.getClass().getName())) {
                rootParent = axial_cosmetics$getParentScreen(rootParent);
            }

            Class<?> screenClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen");
            Class<?> modeClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$Mode");
            Object mainMode = Enum.valueOf((Class<? extends Enum>) modeClass.asSubclass(Enum.class), "MAIN");
            Object screen = screenClass.getConstructor(Screen.class, modeClass).newInstance(rootParent, mainMode);
            if (screen instanceof Screen modsScreen) {
                MinecraftClient.getInstance().setScreen(modsScreen);
                return;
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Fall back to the base title overlay close behavior if axialutils internals change.
        }

        method_25419();
    }

    private static Object axial_cosmetics$getParentScreen(Object screen) throws ReflectiveOperationException {
        var parentField = screen.getClass().getDeclaredField("parent");
        parentField.setAccessible(true);
        return parentField.get(screen);
    }
}
