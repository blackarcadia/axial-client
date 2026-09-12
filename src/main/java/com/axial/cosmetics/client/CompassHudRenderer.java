package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class CompassHudRenderer {
    private static final int WIDTH = 300;
    public static final int HEIGHT = 34;
    private static final float PIXELS_PER_DEGREE = 2.1f;
    private static final int BACKGROUND = 0x7A101217;
    private static final int CENTER_COLOR = 0xFFFFFFFF;
    private static final int MAJOR_COLOR = 0xFFEFEFF7;
    private static final int MINOR_COLOR = 0xB8D7D7E0;
    private static final Mark[] MARKS = {
            new Mark("N", 180),
            new Mark("NE", 225),
            new Mark("E", 270),
            new Mark("SE", 315),
            new Mark("S", 0),
            new Mark("SW", 45),
            new Mark("W", 90),
            new Mark("NW", 135)
    };

    private CompassHudRenderer() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!CompassConfig.isEnabled() || client.options.hudHidden || client.player == null || client.currentScreen != null) {
            return;
        }

        renderCompass(context, client, normalize(client.player.getYaw(tickCounter.getTickProgress(true))));
    }

    public static int getWidth(int screenWidth) {
        return Math.min(screenWidth, Math.min(WIDTH, Math.max(120, screenWidth - 8)));
    }

    public static void renderPreview(DrawContext context, MinecraftClient client) {
        if (CompassConfig.isEnabled()) {
            renderCompass(context, client, client.player == null ? 180.0f : normalize(client.player.getYaw()));
        }
    }

    private static void renderCompass(DrawContext context, MinecraftClient client, float yaw) {
        int screenWidth = client.getWindow().getScaledWidth();
        int compassWidth = getWidth(screenWidth);
        int left = CompassConfig.getX(screenWidth, compassWidth);
        int top = CompassConfig.getY(client.getWindow().getScaledHeight(), HEIGHT);
        int centerX = left + compassWidth / 2;

        context.fill(left, top, left + compassWidth, top + HEIGHT, BACKGROUND);
        context.fill(left + 1, top + 1, left + compassWidth - 1, top + 2, 0x30FFFFFF);
        context.fill(left, top + HEIGHT - 1, left + compassWidth, top + HEIGHT, 0x33000000);

        drawDegreeMarks(context, client.textRenderer, yaw, centerX, left, left + compassWidth, compassWidth, top);
        drawCardinalMarks(context, client.textRenderer, yaw, centerX, left, left + compassWidth, compassWidth, top);

        context.fill(centerX - 1, top + 3, centerX + 1, top + 20, CENTER_COLOR);
        context.fill(centerX - 3, top + 3, centerX + 4, top + 5, CENTER_COLOR);
    }

    private static void drawDegreeMarks(DrawContext context, TextRenderer textRenderer, float yaw, int centerX, int minX, int maxX, int compassWidth, int top) {
        int start = ((int) Math.floor((yaw - 75.0f) / 15.0f)) * 15;
        int end = ((int) Math.ceil((yaw + 75.0f) / 15.0f)) * 15;
        for (int degrees = start; degrees <= end; degrees += 15) {
            float offset = wrappedDelta(degrees, yaw) * PIXELS_PER_DEGREE;
            int x = Math.round(centerX + offset);
            if (x < minX + 4 || x > maxX - 4) {
                continue;
            }

            boolean major = Math.floorMod(degrees, 45) == 0;
            int color = alphaForDistance(x, centerX, compassWidth, major ? MAJOR_COLOR : MINOR_COLOR);
            context.fill(x, top + 4, x + 1, major ? top + 19 : top + 13, color);
            if (!major) {
                String label = Integer.toString(Math.floorMod(degrees, 360));
                int width = textRenderer.getWidth(label);
                int labelX = x - width / 2;
                if (labelX >= minX + 2 && labelX + width <= maxX - 2) {
                    context.drawTextWithShadow(textRenderer, Text.literal(label), labelX, top + 20, color);
                }
            }
        }
    }

    private static void drawCardinalMarks(DrawContext context, TextRenderer textRenderer, float yaw, int centerX, int minX, int maxX, int compassWidth, int top) {
        for (Mark mark : MARKS) {
            float offset = wrappedDelta(mark.degrees, yaw) * PIXELS_PER_DEGREE;
            int x = Math.round(centerX + offset);
            if (x < minX + 8 || x > maxX - 8) {
                continue;
            }

            int color = alphaForDistance(x, centerX, compassWidth, MAJOR_COLOR);
            int width = textRenderer.getWidth(mark.label);
            context.drawTextWithShadow(textRenderer, Text.literal(mark.label), x - width / 2, top + 17, color);
        }
    }

    private static int alphaForDistance(int x, int centerX, int compassWidth, int color) {
        float edgeFade = 1.0f - Math.min(1.0f, Math.abs(x - centerX) / (compassWidth * 0.5f));
        int alpha = MathHelper.clamp(Math.round(((color >>> 24) & 0xFF) * (0.35f + edgeFade * 0.65f)), 0, 255);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static float normalize(float degrees) {
        return MathHelper.wrapDegrees(degrees) + 180.0f;
    }

    private static float wrappedDelta(float degrees, float yaw) {
        return MathHelper.wrapDegrees(degrees - yaw);
    }

    private record Mark(String label, int degrees) {
    }
}
