package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;

/** Routes both title-screen account controls to the shared account manager. */
public final class TitleScreenAccountsDropdown {
    private TitleScreenAccountsDropdown() {}

    public static void toggle() {
        var client = MinecraftClient.getInstance();
        client.setScreen(new AccountsScreen(client.currentScreen));
    }

    public static boolean isOpen() { return false; }

    public static boolean clickToggleButton(Click click) {
        if (click.x() < 20 || click.x() >= 228 || click.y() < 236 || click.y() >= 258) return false;
        toggle();
        return true;
    }

    public static void render(DrawContext context, TitleScreen screen, TextRenderer renderer, int mouseX, int mouseY) {}
    public static boolean mouseClicked(Click click) { return false; }
}
