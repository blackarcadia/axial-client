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
        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed >= DISPLAY_MILLIS) { amount = ""; return; }
        MinecraftClient client = MinecraftClient.getInstance();
        int alpha = (int) (Math.min(1.0, (DISPLAY_MILLIS - elapsed) / 260.0) * 255.0) << 24;
        Text text = Text.literal("+" + amount + " ISLAND XP");
        context.drawCenteredTextWithShadow(client.textRenderer, text, client.getWindow().getScaledWidth() / 2,
                client.getWindow().getScaledHeight() / 2 + 18, alpha | 0x00A8F5C8);
    }
}
