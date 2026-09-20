package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** The entry screen opened from the Axial main menu. */
public final class WaypointSettingsScreen extends Screen {
    private final Screen parent;
    private int x;
    private int y;
    private boolean waitingForEditorKey;

    public WaypointSettingsScreen(Screen parent) {
        super(Text.literal("WAYPOINTS"));
        this.parent = parent;
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        panel(context, x, y, 404, 178);
        context.drawCenteredTextWithShadow(textRenderer, title, x + 202, y + 12, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("PERSONAL MARKERS, STORED ON THIS CLIENT."), x + 202, y + 28, 0xFFC6D0F3);
        button(context, x + 18, y + 62, 368, 20, mouseX, mouseY, "WAYPOINTS: " + (WaypointConfig.enabled() ? "ON" : "OFF"), WaypointConfig.enabled());
        String editorKey = waitingForEditorKey ? "PRESS A KEY..." : "EDITOR KEY: " + WaypointKeys.EDITOR.getBoundKeyLocalizedText().getString();
        button(context, x + 18, y + 92, 368, 20, mouseX, mouseY, editorKey, waitingForEditorKey);
        button(context, x + 18, y + 122, 368, 20, mouseX, mouseY, "OPEN WAYPOINT EDITOR", true);
        context.drawTextWithShadow(textRenderer, Text.literal("CREATE WAYPOINT: " + WaypointKeys.CREATE.getBoundKeyLocalizedText().getString()), x + 18, y + 153, 0xFFC6D0F3);
        context.drawTextWithShadow(textRenderer, Text.literal("ESC  BACK"), x + 314, y + 153, 0xFFC6D0F3);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        layout();
        if (click.button() == 0) {
            if (inside(click.x(), click.y(), x + 18, y + 62, 368, 20)) { WaypointConfig.setEnabled(!WaypointConfig.enabled()); return true; }
            if (inside(click.x(), click.y(), x + 18, y + 92, 368, 20)) { waitingForEditorKey = true; return true; }
            if (inside(click.x(), click.y(), x + 18, y + 122, 368, 20)) { MinecraftClient.getInstance().setScreen(new WaypointEditorScreen(this)); return true; }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (waitingForEditorKey && input.key() != GLFW.GLFW_KEY_ESCAPE) {
            WaypointKeys.EDITOR.setBoundKey(InputUtil.fromKeyCode(input));
            KeyBindingUpdate.save();
            waitingForEditorKey = false;
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) { if (waitingForEditorKey) { waitingForEditorKey = false; } else { close(); } return true; }
        return super.keyPressed(input);
    }

    private void layout() { x = (width - 404) / 2; y = Math.max(16, (height - 178) / 2); }
    static void panel(DrawContext c, int x, int y, int w, int h) { c.fill(x, y, x + w, y + h, 0xC8101018); c.fill(x + 1, y + 1, x + w - 1, y + 2, 0x33FFFFFF); c.drawStrokedRectangle(x, y, w, h, 0xD08F5DFF); }
    static void button(DrawContext c, int x, int y, int w, int h, int mx, int my, String label, boolean accent) { boolean hover = inside(mx, my, x, y, w, h); c.fill(x, y, x + w, y + h, hover ? 0xBC20283A : 0xA0181D2C); c.drawStrokedRectangle(x, y, w, h, accent ? 0xFF8AF0C2 : 0xD08F5DFF); c.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(label), x + w / 2, y + 6, 0xFFF7F7FF); }
    static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
}
