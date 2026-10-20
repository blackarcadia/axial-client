package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the loadout surfaces after the screen background and before its controls. */
@Mixin(Screen.class)
public abstract class EmoteMenuDashboardBackgroundMixin {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    @Inject(method = "renderBackground", at = @At("RETURN"))
    private void axial_cosmetics$drawLoadoutPanels(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!"io.github.kosmx.emotes.arch.screen.EmoteMenu".equals(screen.getClass().getName())) return;

        int margin = Math.max(24, screen.width / 18);
        int gap = Math.max(12, screen.width / 110);
        int top = Math.max(76, screen.height / 11);
        int footerY = screen.height - Math.max(48, screen.height / 19);
        int contentHeight = Math.max(210, footerY - top - 18);
        int leftWidth = Math.max(270, screen.width * 31 / 100);
        int centerWidth = Math.max(250, screen.width * 22 / 100);
        int centerX = margin + leftWidth + gap;
        int rightX = centerX + centerWidth + gap;
        int rightWidth = Math.max(260, screen.width - rightX - margin);

        drawPanel(context, margin, top, leftWidth, contentHeight, 0xE8141622, 0xFFB74DFF);
        drawPanel(context, centerX, top, centerWidth, contentHeight, 0xE8101422, 0xFF46DDF4);
        drawPanel(context, rightX, top, rightWidth, contentHeight, 0xE8141622, 0xFFB74DFF);
        context.fill(rightX + 1, top + 1, rightX + rightWidth - 1, top + 54, 0x7A161427);
        context.fill(margin, top - 15, margin + 132, top - 13, 0xFFCF4BFF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("EMOTE LOADOUT"), margin, top - 34, 0xFFF4F0FF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("ASSIGN SELECTED EMOTE"), rightX + 12, top + 10, 0xFFF4F0FF);
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height, int fill, int border) {
        context.fill(x, y, x + width, y + height, fill);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x25FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, border);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
