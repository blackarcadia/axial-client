package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Brief Island XP gain notification, fed by AxialSky's private system marker. */
public final class IslandXpPopup {
    private static final long DISPLAY_MILLIS = 1_350L;
    private static String amount = "";
    private static long shownAt;

    private IslandXpPopup() { }

    public static void show(String gained) {
        amount = gained;
        shownAt = System.currentTimeMillis();
    }

    public static void render(DrawContext context) {
        if (amount.isEmpty()) return;
        if (MinecraftClient.getInstance().currentScreen instanceof HudEditorAccess editor && editor.axial$isEditingHud()) return;
        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed >= DISPLAY_MILLIS) { amount = ""; return; }
        MinecraftClient client = MinecraftClient.getInstance();
        float progress = elapsed / (float) DISPLAY_MILLIS;
        float fadeIn = ease(Math.min(1.0f, elapsed / 160.0f));
        float fadeOut = ease(Math.min(1.0f, (DISPLAY_MILLIS - elapsed) / 280.0f));
        int alpha = (int) (255 * Math.min(fadeIn, fadeOut)) << 24;
        int lift = Math.round(ease(progress) * 12.0f);
        Text text = Text.literal("+" + amount + " XP");
        var bounds = bounds(client);
        int left = Math.clamp(bounds.x() + (bounds.width() - client.textRenderer.getWidth(text)) / 2,
                0, Math.max(0, client.getWindow().getScaledWidth() - client.textRenderer.getWidth(text)));
        context.drawTextWithShadow(client.textRenderer, text, left, bounds.y() + 12 - lift, alpha | colorFor(amount));
    }

    public static HudSnap.Box bounds(MinecraftClient client) {
        int width = Math.min(client.getWindow().getScaledWidth(), Math.max(84, client.textRenderer.getWidth(Text.literal("+" + amount + " XP")) + 8));
        int height = client.textRenderer.fontHeight + 13;
        return new HudSnap.Box(IslandXpHudConfig.x(client.getWindow().getScaledWidth(), width),
                IslandXpHudConfig.y(client.getWindow().getScaledHeight(), height), width, height);
    }

    public static void preview(DrawContext context, MinecraftClient client) {
        var bounds = bounds(client);
        Text text = Text.literal("+1,250 XP");
        context.drawTextWithShadow(client.textRenderer, text, bounds.x() + (bounds.width() - client.textRenderer.getWidth(text)) / 2,
                bounds.y() + 6, 0xFFFFAD5C);
    }

    private static int colorFor(String amount) {
        double value = parseAmount(amount);
        if (value < 100) return 0x00D5D9E2;
        if (value < 500) return 0x008DD7FF;
        if (value < 800) return 0x00FFE36C;
        if (value <= 1500) return 0x00FFAD5C;
        return 0x00FF9AA5;
    }

    private static double parseAmount(String amount) {
        String normalized = amount.replace(",", "").trim().toUpperCase(java.util.Locale.ROOT);
        try {
            if (normalized.endsWith("K")) return Double.parseDouble(normalized.substring(0, normalized.length() - 1)) * 1_000;
            if (normalized.endsWith("M")) return Double.parseDouble(normalized.substring(0, normalized.length() - 1)) * 1_000_000;
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static float ease(float progress) {
        return progress * progress * (3.0f - 2.0f * progress);
    }
}
