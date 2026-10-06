package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws an Axial-styled wheel behind Emotecraft's untouched radial-menu controls. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget", remap = false)
public abstract class EmoteRadialWheelThemeMixin {
    private static final int SEGMENT_COUNT = 8;
    private static final int WHEEL_OUTLINE = 0xFF393442;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    @Inject(method = "method_48579", at = @At("HEAD"), remap = false)
    private void axial_cosmetics$drawAxialWheel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) return;

        ClickableWidget widget = (ClickableWidget) (Object) this;
        int size = Math.min(widget.getWidth(), widget.getHeight());
        int centerX = widget.getX() + widget.getWidth() / 2;
        int centerY = widget.getY() + widget.getHeight() / 2;
        int innerRadius = Math.max(46, Math.round(size * 0.19f));
        int outerRadius = Math.round(size * 0.49f);
        int hoveredSegment = segmentAt(centerX, centerY, innerRadius, outerRadius, mouseX, mouseY);

        drawWheel(context, centerX, centerY, innerRadius, outerRadius, hoveredSegment);
        drawSlotLabels(context, MinecraftClient.getInstance(), centerX, centerY, outerRadius);
    }

    private static void drawWheel(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, int hoveredSegment) {
        drawRing(context, centerX, centerY, innerRadius, outerRadius, 0xD0161924);
        drawRing(context, centerX, centerY, outerRadius - 2, outerRadius, WHEEL_OUTLINE);
        drawRing(context, centerX, centerY, innerRadius, innerRadius + 2, WHEEL_OUTLINE);
        for (int index = 0; index < SEGMENT_COUNT; index++) {
            double angle = Math.PI / 2 - Math.PI * 2 * (index + 0.5) / SEGMENT_COUNT;
            drawRadialLine(context, centerX, centerY, innerRadius, outerRadius, angle, 0xFF312A45, 1);
        }
        if (hoveredSegment >= 0) {
            double middle = Math.PI / 2 - Math.PI * 2 * hoveredSegment / SEGMENT_COUNT;
            double edge = Math.PI / SEGMENT_COUNT / 2;
            drawRadialLine(context, centerX, centerY, innerRadius + 2, outerRadius - 2, middle - edge, 0xFFB06AF3, 2);
            drawRadialLine(context, centerX, centerY, innerRadius + 2, outerRadius - 2, middle + edge, 0xFFB06AF3, 2);
            drawRadialLine(context, centerX, centerY, innerRadius + 8, outerRadius - 8, middle, 0xA86F3AA9, 7);
        }
    }

    private static void drawRing(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, int color) {
        int outerSquared = outerRadius * outerRadius;
        int innerSquared = innerRadius * innerRadius;
        for (int relativeY = -outerRadius; relativeY <= outerRadius; relativeY++) {
            int outerWidth = (int) Math.sqrt(Math.max(0, outerSquared - relativeY * relativeY));
            int innerWidth = Math.abs(relativeY) < innerRadius
                    ? (int) Math.sqrt(Math.max(0, innerSquared - relativeY * relativeY)) : 0;
            context.fill(centerX - outerWidth, centerY + relativeY, centerX - innerWidth, centerY + relativeY + 1, color);
            context.fill(centerX + innerWidth, centerY + relativeY, centerX + outerWidth + 1, centerY + relativeY + 1, color);
        }
    }

    private static void drawRadialLine(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, double angle, int color, int thickness) {
        int startX = centerX + (int) Math.round(Math.cos(angle) * innerRadius);
        int startY = centerY + (int) Math.round(Math.sin(angle) * innerRadius);
        int endX = centerX + (int) Math.round(Math.cos(angle) * outerRadius);
        int endY = centerY + (int) Math.round(Math.sin(angle) * outerRadius);
        int steps = Math.max(Math.abs(endX - startX), Math.abs(endY - startY));
        int radius = Math.max(0, thickness / 2);
        for (int step = 0; step <= steps; step++) {
            float progress = steps == 0 ? 0 : step / (float) steps;
            int x = Math.round(startX + (endX - startX) * progress);
            int y = Math.round(startY + (endY - startY) * progress);
            context.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, color);
        }
    }

    private static int segmentAt(int centerX, int centerY, int innerRadius, int outerRadius, int mouseX, int mouseY) {
        double x = mouseX - centerX;
        double y = mouseY - centerY;
        double distance = Math.hypot(x, y);
        if (distance < innerRadius || distance > outerRadius) return -1;
        double segmentSize = Math.PI * 2 / SEGMENT_COUNT;
        return Math.floorMod((int) Math.floor((Math.PI / 2 - Math.atan2(y, x) + segmentSize / 2) / segmentSize), SEGMENT_COUNT);
    }

    private static void drawSlotLabels(DrawContext context, MinecraftClient client, int centerX, int centerY, int outerRadius) {
        int labelRadius = outerRadius - 17;
        for (int index = 0; index < SEGMENT_COUNT; index++) {
            double angle = Math.PI / 2 - Math.PI * 2 * index / SEGMENT_COUNT;
            int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius);
            int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius);
            context.drawCenteredTextWithShadow(client.textRenderer, uiText(Integer.toString(index + 1)), x, y - 4, 0xFFBFA0FF);
        }
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
