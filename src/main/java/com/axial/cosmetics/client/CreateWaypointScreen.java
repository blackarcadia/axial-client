package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Prompt shown when the create-waypoint key is pressed in-game. */
public final class CreateWaypointScreen extends Screen {
    private static final int[] COLORS = { 0x61D8FF, 0x8AF0C2, 0xF5A5D8, 0xFFD166, 0xB89CFF };
    private final Screen parent;
    private int x, y, colorIndex;
    private TextFieldWidget field;
    public CreateWaypointScreen(Screen parent) { super(Text.literal("NEW WAYPOINT")); this.parent = parent; }
    @Override protected void init() { field = new TextFieldWidget(textRenderer, 0, 0, 300, 20, Text.literal("Waypoint name")); field.setMaxLength(40); field.setSuggestion("Waypoint name"); addDrawableChild(field); setInitialFocus(field); }
    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }
    @Override public void render(DrawContext c, int mx, int my, float delta) {
        x = (width - 360) / 2; y = Math.max(16, (height - 164) / 2); field.setPosition(x + 30, y + 48);
        WaypointSettingsScreen.panel(c, x, y, 360, 164);
        c.drawCenteredTextWithShadow(textRenderer, title, x + 180, y + 13, 0xFFF7F7FF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("MARK YOUR CURRENT POSITION"), x + 180, y + 29, 0xFFC6D0F3);
        super.render(c, mx, my, delta);
        int color = 0xFF000000 | COLORS[colorIndex]; c.fill(x + 30, y + 82, x + 330, y + 102, color); c.drawStrokedRectangle(x + 30, y + 82, 300, 20, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("BEACON COLOR (CLICK TO CHANGE)"), x + 180, y + 88, 0xFF101018);
        WaypointSettingsScreen.button(c, x + 30, y + 114, 145, 20, mx, my, "CREATE", true);
        WaypointSettingsScreen.button(c, x + 185, y + 114, 145, 20, mx, my, "CANCEL", false);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("ENTER  CREATE     ESC  CANCEL"), x + 180, y + 144, 0xFFC6D0F3);
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() == 0 && WaypointSettingsScreen.inside(click.x(), click.y(), x + 30, y + 82, 300, 20)) { colorIndex = (colorIndex + 1) % COLORS.length; return true; }
        if (click.button() == 0 && WaypointSettingsScreen.inside(click.x(), click.y(), x + 30, y + 114, 145, 20)) { create(); return true; }
        if (click.button() == 0 && WaypointSettingsScreen.inside(click.x(), click.y(), x + 185, y + 114, 145, 20)) { close(); return true; }
        return false;
    }
    @Override public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) { create(); return true; }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(input);
    }
    private void create() { MinecraftClient client = MinecraftClient.getInstance(); if (client.player == null) { close(); return; } String name = field.getText().trim(); if (name.isEmpty()) name = "Waypoint " + (WaypointConfig.waypoints().size() + 1); WaypointConfig.add(new Waypoint(name, client.world.getRegistryKey().getValue().toString(), client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ(), COLORS[colorIndex])); close(); }
}
