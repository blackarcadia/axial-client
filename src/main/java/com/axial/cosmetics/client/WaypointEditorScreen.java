package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Low-opacity editor for all saved waypoints. */
public final class WaypointEditorScreen extends Screen {
    private final Screen parent;
    private int x;
    private int y;
    private int scroll;
    private TextFieldWidget nameField;

    public WaypointEditorScreen(Screen parent) { super(Text.literal("WAYPOINT EDITOR")); this.parent = parent; }

    @Override protected void init() {
        nameField = new TextFieldWidget(textRenderer, 0, 0, 230, 20, Text.literal("Waypoint name"));
        nameField.setMaxLength(40);
        nameField.setSuggestion("New waypoint name");
        addDrawableChild(nameField);
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        nameField.setPosition(x + 18, y + 44);
        WaypointSettingsScreen.panel(context, x, y, 460, 300);
        context.drawCenteredTextWithShadow(textRenderer, title, x + 230, y + 12, 0xFFF7F7FF);
        context.drawTextWithShadow(textRenderer, Text.literal("ADD A WAYPOINT AT YOUR CURRENT POSITION"), x + 18, y + 30, 0xFFC6D0F3);
        super.render(context, mouseX, mouseY, delta);
        WaypointSettingsScreen.button(context, x + 258, y + 44, 184, 20, mouseX, mouseY, "CREATE HERE", true);
        context.drawTextWithShadow(textRenderer, Text.literal("SAVED WAYPOINTS"), x + 18, y + 82, 0xFFC6D0F3);
        context.enableScissor(x + 18, y + 98, x + 442, y + 270);
        List<Waypoint> list = WaypointConfig.waypoints();
        for (int i = 0; i < list.size(); i++) drawRow(context, list.get(i), i, mouseX, mouseY);
        context.disableScissor();
        context.drawTextWithShadow(textRenderer, Text.literal("ESC  BACK"), x + 370, y + 278, 0xFFC6D0F3);
    }

    private void drawRow(DrawContext c, Waypoint point, int index, int mx, int my) {
        int rowY = y + 98 + index * 28 - scroll;
        if (rowY < y + 74 || rowY > y + 270) return;
        boolean hover = WaypointSettingsScreen.inside(mx, my, x + 18, rowY, 424, 22);
        c.fill(x + 18, rowY, x + 442, rowY + 22, hover ? 0xB020283A : 0x78181D2C);
        int swatch = 0xFF000000 | (point.color & 0xFFFFFF);
        c.fill(x + 20, rowY + 3, x + 24, rowY + 19, swatch);
        c.drawTextWithShadow(textRenderer, Text.literal(point.name + "  " + point.x + ", " + point.y + ", " + point.z), x + 31, rowY + 7, 0xFFF7F7FF);
        WaypointSettingsScreen.button(c, x + 330, rowY + 2, 48, 18, mx, my, "EDIT", true);
        WaypointSettingsScreen.button(c, x + 382, rowY + 2, 58, 18, mx, my, "DELETE", false);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        layout();
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        if (WaypointSettingsScreen.inside(click.x(), click.y(), x + 258, y + 44, 184, 20)) { createAtPlayer(); return true; }
        List<Waypoint> list = WaypointConfig.waypoints();
        for (int i = list.size() - 1; i >= 0; i--) {
            int rowY = y + 98 + i * 28 - scroll;
            Waypoint point = list.get(i);
            if (WaypointSettingsScreen.inside(click.x(), click.y(), x + 382, rowY + 2, 58, 18)) { WaypointConfig.remove(point); return true; }
            if (WaypointSettingsScreen.inside(click.x(), click.y(), x + 330, rowY + 2, 48, 18)) { nameField.setText(point.name); nameField.setChangedListener(value -> { point.name = value; WaypointConfig.changed(); }); return true; }
        }
        return false;
    }

    private void createAtPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Waypoint " + (WaypointConfig.waypoints().size() + 1);
        WaypointConfig.add(new Waypoint(name, client.world.getRegistryKey().getValue().toString(), client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ(), 0x61D8FF));
        nameField.setText("");
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(Math.max(0, WaypointConfig.waypoints().size() * 28 - 172), scroll - (int) vertical * 16));
        return true;
    }
    private void layout() { x = (width - 460) / 2; y = Math.max(16, (height - 300) / 2); }
}
