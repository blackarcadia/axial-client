package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds an Axial visual layer behind Emotecraft's existing radial selector. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget", remap = false)
public abstract class EmoteRadialMenuThemeMixin {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private static final int SEGMENTS = 8;

    @Inject(method = "method_48579", at = @At("HEAD"), remap = false)
    private void axial_cosmetics$drawRadialTheme(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;
        int outerRadius = Math.min(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight()) / 3;
        int innerRadius = Math.max(48, outerRadius - 70);

        drawRing(context, centerX, centerY, innerRadius, outerRadius, 0x8C101018);
        drawRing(context, centerX, centerY, outerRadius - 3, outerRadius, 0xD08F5DFF);
        drawHoverWedge(context, centerX, centerY, innerRadius, outerRadius, mouseX, mouseY);
        drawLabels(context, client, centerX, centerY, innerRadius, outerRadius);

        context.fill(centerX - 34, centerY - 11, centerX + 34, centerY + 11, 0xD8101018);
        context.drawStrokedRectangle(centerX - 34, centerY - 11, 68, 22, 0xD08F5DFF);
        Text title = uiText("EMOTES");
        context.drawCenteredTextWithShadow(client.textRenderer, title, centerX, centerY - 3, 0xFFF7F7FF);
    }

    private static void drawRing(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, int color) {
        int outerSquared = outerRadius * outerRadius;
        int innerSquared = innerRadius * innerRadius;
        for (int y = -outerRadius; y <= outerRadius; y++) {
            int outerHalfWidth = (int) Math.sqrt(Math.max(0, outerSquared - y * y));
            int innerHalfWidth = Math.abs(y) < innerRadius
                    ? (int) Math.sqrt(Math.max(0, innerSquared - y * y)) : 0;
            context.fill(centerX - outerHalfWidth, centerY + y, centerX - innerHalfWidth, centerY + y + 1, color);
            context.fill(centerX + innerHalfWidth, centerY + y, centerX + outerHalfWidth + 1, centerY + y + 1, color);
        }
    }

    private static void drawHoverWedge(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, int mouseX, int mouseY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        if (distance < innerRadius || distance > outerRadius + 32) return;

        double angle = Math.atan2(dy, dx);
        for (int radius = innerRadius + 8; radius < outerRadius - 5; radius += 6) {
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            context.fill(x - 4, y - 4, x + 5, y + 5, 0xB88F5DFF);
        }
    }

    private static void drawLabels(DrawContext context, MinecraftClient client, int centerX, int centerY, int innerRadius, int outerRadius) {
        int labelRadius = (innerRadius + outerRadius) / 2;
        for (int index = 0; index < SEGMENTS; index++) {
            double angle = -Math.PI / 2 + (Math.PI * 2 * index / SEGMENTS);
            int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius);
            int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius);
            Text label = uiText(Integer.toString(index + 1));
            context.drawCenteredTextWithShadow(client.textRenderer, label, x, y - 4, 0xFFC6D0F3);
        }
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
