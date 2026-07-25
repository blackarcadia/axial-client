package com.axial.cosmetics.client;

import net.minecraft.client.gui.DrawContext;
public final class CrosshairRenderer {
    private CrosshairRenderer() {
    }

    public static void render(DrawContext context, int centerX, int centerY, CrosshairConfig config) {
        render(context, centerX, centerY, config, 1.0f);
    }

    public static void render(DrawContext context, int centerX, int centerY, CrosshairConfig config, float scaleMultiplier) {
        if (config == null || !config.enabled) {
            return;
        }

        float baseScale = Math.max(0.1f, config.size);
        float pulseScale = Math.max(1.0f, scaleMultiplier);
        int length = evenPixelCount(Math.max(0, Math.round(config.length * baseScale)));
        int width = evenPixelCount(Math.max(1, Math.round(config.width * baseScale)));
        int gap = evenPixelCount(Math.max(0, Math.round((config.gap * baseScale) + ((pulseScale - 1.0f) * 8.0f))));
        int color = config.color;
        int outlineColor = config.outlineColor;

        switch (config.style) {
            case DOT -> drawDot(context, centerX, centerY, width, color, outlineColor, config.outlineEnabled);
            case SMALL_DOT -> drawDot(context, centerX, centerY, Math.max(1, width / 2), color, outlineColor, config.outlineEnabled);
            case PLUS -> drawOrthogonalCross(context, centerX, centerY, Math.max(0, length), width, 0, color, outlineColor, config.outlineEnabled, false);
            case X -> drawDiagonalCross(context, centerX, centerY, length, width, gap, color, outlineColor, config.outlineEnabled);
            case CIRCLE -> drawCircle(context, centerX, centerY, width, color, outlineColor, config.outlineEnabled);
            case SMALL_CROSS -> drawOrthogonalCross(context, centerX, centerY, Math.max(1, length / 2), width, Math.max(0, gap / 2), color, outlineColor, config.outlineEnabled, false);
            case CLASSIC, LUNAR, T -> drawOrthogonalCross(context, centerX, centerY, length, width, gap, color, outlineColor, config.outlineEnabled, config.style == CrosshairConfig.CrosshairStyle.T);
        }
    }

    private static void drawDot(DrawContext context, int centerX, int centerY, int size, int color, int outlineColor, boolean outline) {
        int half = Math.max(0, size / 2);
        if (outline) {
            context.fill(centerX - half - 1, centerY - half - 1, centerX + half + 2, centerY + half + 2, outlineColor);
        }
        context.fill(centerX - half, centerY - half, centerX + half + 1, centerY + half + 1, color);
    }

    private static void drawOrthogonalCross(
            DrawContext context,
            int centerX,
            int centerY,
            int length,
            int width,
            int gap,
            int color,
            int outlineColor,
            boolean outline,
            boolean tStyle
    ) {
        if (length <= 0) {
            drawDot(context, centerX, centerY, width, color, outlineColor, outline);
            return;
        }

        drawVerticalArm(context, centerX, centerY, width, length, gap, true, color, outlineColor, outline);
        drawVerticalArm(context, centerX, centerY, width, length, gap, false, color, outlineColor, outline);
        drawHorizontalArm(context, centerX, centerY, width, length, gap, true, color, outlineColor, outline);
        if (!tStyle) {
            drawHorizontalArm(context, centerX, centerY, width, length, gap, false, color, outlineColor, outline);
        }
    }

    private static void drawVerticalArm(
            DrawContext context,
            int centerX,
            int centerY,
            int width,
            int length,
            int gap,
            boolean top,
            int color,
            int outlineColor,
            boolean outline
    ) {
        int halfWidth = width / 2;
        int left = centerX - halfWidth;
        int right = centerX + ((width & 1) == 0 ? halfWidth : halfWidth + 1);
        int innerLeft = left;
        int innerRight = right;
        int outerLeft = left - 1;
        int outerRight = right + 1;

        if (outline) {
            int y1 = top ? centerY - gap - length - 1 : centerY + gap;
            int y2 = top ? centerY - gap + 1 : centerY + gap + length + 1;
            context.fill(outerLeft, y1, outerRight, y2, outlineColor);
        }

        int y1 = top ? centerY - gap - length : centerY + gap;
        int y2 = top ? centerY - gap : centerY + gap + length;
        context.fill(innerLeft, y1, innerRight, y2, color);
    }

    private static void drawHorizontalArm(
            DrawContext context,
            int centerX,
            int centerY,
            int width,
            int length,
            int gap,
            boolean left,
            int color,
            int outlineColor,
            boolean outline
    ) {
        int halfWidth = width / 2;
        int top = centerY - halfWidth;
        int bottom = centerY + ((width & 1) == 0 ? halfWidth : halfWidth + 1);
        int outerTop = top - 1;
        int outerBottom = bottom + 1;

        if (outline) {
            int x1 = left ? centerX - gap - length - 1 : centerX + gap;
            int x2 = left ? centerX - gap + 1 : centerX + gap + length + 1;
            context.fill(x1, outerTop, x2, outerBottom, outlineColor);
        }

        int x1 = left ? centerX - gap - length : centerX + gap;
        int x2 = left ? centerX - gap : centerX + gap + length;
        context.fill(x1, top, x2, bottom, color);
    }

    private static void drawDiagonalCross(DrawContext context, int centerX, int centerY, int length, int width, int gap, int color, int outlineColor, boolean outline) {
        int thickness = Math.max(1, width);
        if (outline) {
            drawDiagonalArm(context, centerX, centerY, length, thickness + 2, gap, outlineColor);
        }
        drawDiagonalArm(context, centerX, centerY, length, thickness, gap, color);
    }

    private static void drawCircle(DrawContext context, int centerX, int centerY, int size, int color, int outlineColor, boolean outline) {
        int radius = Math.max(1, size / 2);
        int outer = radius + 1;
        if (outline) {
            context.fill(centerX - outer, centerY - outer, centerX + outer + 1, centerY - radius, outlineColor);
            context.fill(centerX - outer, centerY + radius, centerX + outer + 1, centerY + outer + 1, outlineColor);
            context.fill(centerX - outer, centerY - radius, centerX - radius, centerY + radius + 1, outlineColor);
            context.fill(centerX + radius, centerY - radius, centerX + outer + 1, centerY + radius + 1, outlineColor);
        }

        context.fill(centerX - radius, centerY - radius, centerX + radius + 1, centerY - radius + 1, color);
        context.fill(centerX - radius, centerY + radius, centerX + radius + 1, centerY + radius + 1, color);
        context.fill(centerX - radius, centerY - radius, centerX - radius + 1, centerY + radius + 1, color);
        context.fill(centerX + radius, centerY - radius, centerX + radius + 1, centerY + radius + 1, color);
    }

    private static void drawDiagonalArm(DrawContext context, int centerX, int centerY, int length, int thickness, int gap, int color) {
        int half = Math.max(0, thickness / 2);
        for (int step = 1; step <= length; step++) {
            drawSquare(context, centerX + gap + step, centerY + gap + step, half, color);
            drawSquare(context, centerX - gap - step, centerY + gap + step, half, color);
            drawSquare(context, centerX + gap + step, centerY - gap - step, half, color);
            drawSquare(context, centerX - gap - step, centerY - gap - step, half, color);
        }
    }

    private static void drawSquare(DrawContext context, int centerX, int centerY, int half, int color) {
        context.fill(centerX - half, centerY - half, centerX + half + 1, centerY + half + 1, color);
    }

    private static int evenPixelCount(int value) {
        if (value <= 0) {
            return 0;
        }
        return (value & 1) == 0 ? value : value + 1;
    }
}
