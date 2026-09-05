package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class CompassHudRenderer {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 34;
    private static final int TOP = 6;
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

        int screenWidth = client.getWindow().getScaledWidth();
        int compassWidth = Math.min(WIDTH, Math.max(120, screenWidth - 8));
        int left = (screenWidth - compassWidth) / 2;
        int centerX = screenWidth / 2;
        float yaw = normalize(client.player.getYaw(tickCounter.getTickProgress(true)));

        context.fill(left, TOP, left + compassWidth, TOP + HEIGHT, BACKGROUND);
        context.fill(left + 1, TOP + 1, left + compassWidth - 1, TOP + 2, 0x30FFFFFF);
        context.fill(left, TOP + HEIGHT - 1, left + compassWidth, TOP + HEIGHT, 0x33000000);

        drawDegreeMarks(context, client.textRenderer, yaw, centerX, left, left + compassWidth, compassWidth);
        drawCardinalMarks(context, client.textRenderer, yaw, centerX, left, left + compassWidth, compassWidth);

        context.fill(centerX - 1, TOP + 3, centerX + 1, TOP + 20, CENTER_COLOR);
        context.fill(centerX - 3, TOP + 3, centerX + 4, TOP + 5, CENTER_COLOR);
    }

    private static void drawDegreeMarks(DrawContext context, TextRenderer textRenderer, float yaw, int centerX, int minX, int maxX, int compassWidth) {
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
            context.fill(x, TOP + 4, x + 1, major ? TOP + 19 : TOP + 13, color);
            if (!major) {
                String label = Integer.toString(Math.floorMod(degrees, 360));
                int width = textRenderer.getWidth(label);
                int labelX = x - width / 2;
                if (labelX >= minX + 2 && labelX + width <= maxX - 2) {
                    context.drawTextWithShadow(textRenderer, Text.literal(label), labelX, TOP + 20, color);
                }
            }
        }
    }

    private static void drawCardinalMarks(DrawContext context, TextRenderer textRenderer, float yaw, int centerX, int minX, int maxX, int compassWidth) {
        for (Mark mark : MARKS) {
            float offset = wrappedDelta(mark.degrees, yaw) * PIXELS_PER_DEGREE;
            int x = Math.round(centerX + offset);
            if (x < minX + 8 || x > maxX - 8) {
                continue;
            }

            int color = alphaForDistance(x, centerX, compassWidth, MAJOR_COLOR);
            int width = textRenderer.getWidth(mark.label);
            context.drawTextWithShadow(textRenderer, Text.literal(mark.label), x - width / 2, TOP + 17, color);
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
