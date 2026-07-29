package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenButtonSkinMixin {
    private static final Identifier BUTTON_1 = AxialCosmetics.id("textures/gui/title/button1.png");
    private static final Identifier BUTTON_2 = AxialCosmetics.id("textures/gui/title/button2.png");
    private static final Identifier QUIT_ICON = AxialCosmetics.id("textures/gui/title/quit-button-icon.png");
    private static final Identifier QUIT_ICON_HOVER = AxialCosmetics.id("textures/gui/title/quit-button-icon-hover.png");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private static final int BUTTON_TEXTURE_WIDTH = 208;
    private static final int BUTTON_TEXTURE_HEIGHT = 22;
    private static final int BUTTON_X = 20;
    private static final int SINGLE_Y = 144;
    private static final int BUTTON_SPACING = 28;
    private static final int QUIT_Y = 20;
    private static final int QUIT_SIZE = 32;
    private static final int RIGHT_MARGIN = 20;

    @Inject(method = "render", at = @At("TAIL"))
    private void axial_cosmetics$renderFixedTitleOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof TitleScreen screen)) {
            return;
        }

        double scaleFactor = client.getWindow().getScaleFactor();
        int rawMouseX = (int) Math.round(mouseX * scaleFactor);
        int rawMouseY = (int) Math.round(mouseY * scaleFactor);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale((float) (1.0 / scaleFactor), (float) (1.0 / scaleFactor));

        axial_cosmetics$drawButton(context, rawMouseX, rawMouseY, BUTTON_X, SINGLE_Y, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT, "single");
        axial_cosmetics$drawButton(context, rawMouseX, rawMouseY, BUTTON_X, SINGLE_Y + BUTTON_SPACING, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT, "multi");
        axial_cosmetics$drawButton(context, rawMouseX, rawMouseY, BUTTON_X, SINGLE_Y + (BUTTON_SPACING * 2), BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT, "options");

        int quitX = Math.max(RIGHT_MARGIN, client.getWindow().getWidth() - RIGHT_MARGIN - QUIT_SIZE);
        axial_cosmetics$drawQuitButton(context, rawMouseX, rawMouseY, quitX, QUIT_Y, QUIT_SIZE);

        matrices.popMatrix();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$handleFixedTitleClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof TitleScreen screen)) {
            return;
        }

        double scaleFactor = client.getWindow().getScaleFactor();
        int rawMouseX = (int) Math.round(click.x() * scaleFactor);
        int rawMouseY = (int) Math.round(click.y() * scaleFactor);

        if (axial_cosmetics$clickTitleButton(screen.children(), rawMouseX, rawMouseY, "single")
                || axial_cosmetics$clickTitleButton(screen.children(), rawMouseX, rawMouseY, "multi")
                || axial_cosmetics$clickTitleButton(screen.children(), rawMouseX, rawMouseY, "options")
                || axial_cosmetics$clickQuitButton(screen.children(), rawMouseX, rawMouseY)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private static void axial_cosmetics$drawButton(DrawContext context, int rawMouseX, int rawMouseY, int x, int y, int width, int height, String label) {
        boolean hovered = axial_cosmetics$inRawRect(rawMouseX, rawMouseY, x, y, width, height);
        Identifier texture = hovered ? BUTTON_2 : BUTTON_1;
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                0.0f,
                0.0f,
                width,
                height,
                BUTTON_TEXTURE_WIDTH,
                BUTTON_TEXTURE_HEIGHT,
                BUTTON_TEXTURE_WIDTH,
                BUTTON_TEXTURE_HEIGHT
        );

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textColor = ColorHelper.getWhite(1.0f);
        Text buttonText = Text.literal(label.toUpperCase(Locale.ROOT)).styled(style -> style.withFont(UI_FONT));
        context.drawTextWithShadow(textRenderer, buttonText, x + 12, y + (height - 8) / 2, textColor);
    }

    private static void axial_cosmetics$drawQuitButton(DrawContext context, int rawMouseX, int rawMouseY, int x, int y, int size) {
        boolean hovered = axial_cosmetics$inRawRect(rawMouseX, rawMouseY, x, y, size, size);
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                hovered ? QUIT_ICON_HOVER : QUIT_ICON,
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                1024,
                1024,
                1024,
                1024
        );
    }

    private static boolean axial_cosmetics$clickTitleButton(List<?> children, int rawMouseX, int rawMouseY, String labelNeedle) {
        ButtonWidget button = axial_cosmetics$findTitleButton(children, labelNeedle);
        if (button == null) {
            return false;
        }

        int x = BUTTON_X;
        int y = switch (labelNeedle) {
            case "single" -> SINGLE_Y;
            case "multi" -> SINGLE_Y + BUTTON_SPACING;
            case "options" -> SINGLE_Y + (BUTTON_SPACING * 2);
            default -> SINGLE_Y;
        };

        if (!axial_cosmetics$inRawRect(rawMouseX, rawMouseY, x, y, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT)) {
            return false;
        }

        button.onPress(new MouseInput(0, 0));
        return true;
    }

    private static boolean axial_cosmetics$clickQuitButton(List<?> children, int rawMouseX, int rawMouseY) {
        ButtonWidget button = axial_cosmetics$findTitleButton(children, "quit");
        if (button == null) {
            return false;
        }

        int x = Math.max(RIGHT_MARGIN, MinecraftClient.getInstance().getWindow().getWidth() - RIGHT_MARGIN - QUIT_SIZE);
        if (!axial_cosmetics$inRawRect(rawMouseX, rawMouseY, x, QUIT_Y, QUIT_SIZE, QUIT_SIZE)) {
            return false;
        }

        button.onPress(new MouseInput(0, 0));
        return true;
    }

    private static ButtonWidget axial_cosmetics$findTitleButton(List<?> children, String labelNeedle) {
        for (Object child : children) {
            if (!(child instanceof ButtonWidget button)) {
                continue;
            }

            String lower = button.getMessage().getString().toLowerCase(Locale.ROOT);
            if (lower.contains(labelNeedle)) {
                return button;
            }
        }

        return null;
    }

    private static boolean axial_cosmetics$inRawRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < (x + width) && mouseY >= y && mouseY < (y + height);
    }
}
