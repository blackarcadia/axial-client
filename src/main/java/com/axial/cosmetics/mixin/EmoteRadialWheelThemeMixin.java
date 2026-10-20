package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import io.github.kosmx.emotes.arch.screen.widget.IChooseElement;
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

/** Draws a compact card-style wheel behind Emotecraft's untouched radial-menu controls. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget", remap = false)
public abstract class EmoteRadialWheelThemeMixin {
    private static final int SEGMENT_COUNT = 8;
    private static final int CARD_BACKGROUND = 0xE6181A28;
    private static final int CARD_OUTLINE = 0xFF8359EE;
    private static final int HOVERED_BACKGROUND = 0xE52B3550;
    private static final int HOVERED_OUTLINE = 0xFF38DDF5;
    private static final int MEDALLION_BACKGROUND = 0xF0212035;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    @Inject(method = "method_48579", at = @At("HEAD"), remap = false)
    private void axial_cosmetics$drawAxialWheel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) return;

        ClickableWidget widget = (ClickableWidget) (Object) this;
        int size = Math.min(widget.getWidth(), widget.getHeight());
        int centerX = widget.getX() + widget.getWidth() / 2;
        int centerY = widget.getY() + widget.getHeight() / 2;
        int innerRadius = Math.max(44, Math.round(size * 0.18f));
        int outerRadius = Math.round(size * 0.47f);
        int hoveredSegment = segmentAt(centerX, centerY, innerRadius, outerRadius, mouseX, mouseY);

        AbstractFastChooseWidget wheel = (AbstractFastChooseWidget) (Object) this;
        drawWheel(context, centerX, centerY, innerRadius, outerRadius, hoveredSegment);
        drawSlotLabels(context, MinecraftClient.getInstance(), centerX, centerY, outerRadius);
        if (hoveredSegment >= 0) drawTooltip(context, MinecraftClient.getInstance(), wheel, hoveredSegment, centerX, centerY, outerRadius);
    }

    private static void drawWheel(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, int hoveredSegment) {
        drawCircle(context, centerX, centerY, outerRadius + 8, 0x18000000);
        for (int index = 0; index < SEGMENT_COUNT; index++) {
            boolean hovered = index == hoveredSegment;
            drawSegmentCard(context, centerX, centerY, innerRadius, outerRadius, index,
                    hovered ? HOVERED_BACKGROUND : CARD_BACKGROUND,
                    hovered ? HOVERED_OUTLINE : CARD_OUTLINE);
        }
        drawCenterControl(context, centerX, centerY, innerRadius);
    }

    private static void drawSegmentCard(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius,
                                        int segment, int background, int outline) {
        double segmentSize = Math.PI * 2 / SEGMENT_COUNT;
        double middle = Math.PI / 2 - segmentSize * segment;
        double gap = 0.045;
        double start = middle - segmentSize / 2 + gap;
        double end = middle + segmentSize / 2 - gap;
        int strokes = Math.max(24, outerRadius / 3);
        for (int stroke = 0; stroke <= strokes; stroke++) {
            double progress = stroke / (double) strokes;
            drawRadialLine(context, centerX, centerY, innerRadius, outerRadius,
                    start + (end - start) * progress, background, 2);
        }
        drawRadialLine(context, centerX, centerY, innerRadius, outerRadius, start, outline, 2);
        drawRadialLine(context, centerX, centerY, innerRadius, outerRadius, end, outline, 2);
        drawArc(context, centerX, centerY, innerRadius, start, end, outline);
        drawArc(context, centerX, centerY, outerRadius, start, end, outline);

        int medallionRadius = Math.max(20, (outerRadius - innerRadius) / 4);
        int medallionDistance = (innerRadius + outerRadius) / 2;
        int iconX = centerX + (int) Math.round(Math.cos(middle) * medallionDistance);
        int iconY = centerY + (int) Math.round(Math.sin(middle) * medallionDistance);
        drawCircle(context, iconX, iconY, medallionRadius + 2, outline);
        drawCircle(context, iconX, iconY, medallionRadius, MEDALLION_BACKGROUND);
    }

    private static void drawCenterControl(DrawContext context, int centerX, int centerY, int radius) {
        drawCircle(context, centerX, centerY, radius + 4, CARD_OUTLINE);
        drawCircle(context, centerX, centerY, radius, 0xF01A1B2D);
        int face = Math.max(4, radius / 10);
        context.fill(centerX - face * 2, centerY - face, centerX - face, centerY + face, 0xFFF6F4FF);
        context.fill(centerX + face, centerY - face, centerX + face * 2, centerY + face, 0xFFF6F4FF);
        context.fill(centerX - face * 2, centerY + face * 2, centerX + face * 2, centerY + face * 3, 0xFFF6F4FF);
    }

    private static void drawCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        int squaredRadius = radius * radius;
        for (int relativeY = -radius; relativeY <= radius; relativeY++) {
            int width = (int) Math.sqrt(Math.max(0, squaredRadius - relativeY * relativeY));
            context.fill(centerX - width, centerY + relativeY, centerX + width + 1, centerY + relativeY + 1, color);
        }
    }

    private static void drawArc(DrawContext context, int centerX, int centerY, int radius, double start, double end, int color) {
        int steps = Math.max(20, radius / 3);
        for (int step = 0; step <= steps; step++) {
            double angle = start + (end - start) * step / steps;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            context.fill(x - 1, y - 1, x + 2, y + 2, color);
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
        int labelRadius = outerRadius + 16;
        for (int index = 0; index < SEGMENT_COUNT; index++) {
            double angle = Math.PI / 2 - Math.PI * 2 * index / SEGMENT_COUNT;
            int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius);
            int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius);
            drawCircle(context, x, y, 11, 0xEE171725);
            drawCircle(context, x, y, 10, CARD_OUTLINE);
            drawCircle(context, x, y, 8, 0xFF171725);
            context.drawCenteredTextWithShadow(client.textRenderer, uiText(Integer.toString(index + 1)), x, y - 4, 0xFFBFA0FF);
        }
    }

    private static void drawTooltip(DrawContext context, MinecraftClient client, AbstractFastChooseWidget wheel,
                                    int hoveredSegment, int centerX, int centerY, int outerRadius) {
        if (hoveredSegment >= wheel.getChooseElements().size()) return;
        IChooseElement element = wheel.getChooseElements().get(hoveredSegment);
        if (!element.hasEmote() || element.getEmote() == null) return;
        String name = element.getEmote().name.getString();
        int width = Math.max(112, client.textRenderer.getWidth(name) + 28);
        int x = centerX - width / 2;
        int y = centerY + outerRadius + 36;
        context.fill(x - 2, y - 2, x + width + 2, y + 22, HOVERED_OUTLINE);
        context.fill(x, y, x + width, y + 20, 0xF0101420);
        context.drawCenteredTextWithShadow(client.textRenderer, uiText(name), centerX, y + 6, 0xFFF6F4FF);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
