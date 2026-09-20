package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.ArrayList;

/** The editable list of player-created waypoints. */
public final class WaypointSettingsScreen extends Screen {
    private static final int WIDTH = 460;
    private static final int HEIGHT = 260;
    private final Screen parent;
    private List<WaypointConfig.Entry> waypoints;
    private final List<TextFieldWidget> nameFields = new ArrayList<>();
    private int x, y;

    public WaypointSettingsScreen(Screen parent) {
        super(Text.literal("WAYPOINTS"));
        this.parent = parent;
    }

    @Override protected void init() {
        waypoints = WaypointConfig.waypoints();
        layout();
        for (int i = 0; i < waypoints.size(); i++) {
            int index = i;
            TextFieldWidget field = new TextFieldWidget(textRenderer, x + 46, rowY(i) + 2, 160, 20, Text.literal("NAME"));
            field.setText(waypoints.get(i).name());
            field.setMaxLength(48);
            field.setChangedListener(value -> WaypointConfig.rename(index, value));
            addDrawableChild(field);
            nameFields.add(field);
        }
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        context.fill(x, y, x + WIDTH, y + HEIGHT, 0xE8101018);
        context.drawStrokedRectangle(x, y, WIDTH, HEIGHT, 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, title, x + WIDTH / 2, y + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, "CLICK A NAME TO EDIT", x + WIDTH / 2, y + 25, 0xFFC6D0F3);
        ModMenuBackButton.draw(context, x + 18, y + 6, mouseX, mouseY);
        if (waypoints.isEmpty()) context.drawCenteredTextWithShadow(textRenderer, "NO WAYPOINTS CREATED", x + WIDTH / 2, y + 64, 0xFFC6D0F3);
        for (int i = 0; i < waypoints.size(); i++) drawRow(context, mouseX, mouseY, i);
        for (int i = 0; i < nameFields.size(); i++) {
            TextFieldWidget field = nameFields.get(i);
            field.setPosition(x + 46, rowY(i) + 2);
            field.render(context, mouseX, mouseY, delta);
        }
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && inside(click, x + 18, y + 6, 24, 18)) { close(); return true; }
        if (click.button() == 0) for (int i = 0; i < waypoints.size(); i++) {
            if (inside(click, deleteX(), rowY(i) + 2, 90, 20)) {
                WaypointConfig.delete(i);
                MinecraftClient.getInstance().setScreen(new WaypointSettingsScreen(parent));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }

    private void drawRow(DrawContext c, int mx, int my, int i) {
        WaypointConfig.Entry waypoint = waypoints.get(i);
        int rowY = rowY(i);
        c.fill(x + 18, rowY, x + WIDTH - 18, rowY + 24, 0xA0181D2C);
        c.drawStrokedRectangle(x + 18, rowY, WIDTH - 36, 24, waypoint.color());
        c.fill(x + 24, rowY + 6, x + 36, rowY + 18, waypoint.color());
        c.drawTextWithShadow(textRenderer, "X: " + waypoint.x() + "  Y: " + waypoint.y() + "  Z: " + waypoint.z(), x + 214, rowY + 8, 0xFFC6D0F3);
        int deleteX = deleteX(); boolean hover = mx >= deleteX && mx <= deleteX + 90 && my >= rowY + 2 && my <= rowY + 22;
        c.fill(deleteX, rowY + 2, deleteX + 90, rowY + 22, hover ? 0xC06E2635 : 0xA04A1E28);
        c.drawStrokedRectangle(deleteX, rowY + 2, 90, 20, 0xFFE85D75);
        c.drawCenteredTextWithShadow(textRenderer, "DELETE", deleteX + 45, rowY + 8, 0xFFF7F7FF);
    }

    private void layout() { x = (width - WIDTH) / 2; y = Math.max(16, (height - HEIGHT) / 2); }
    private int rowY(int index) { return y + 52 + index * 30; }
    private int deleteX() { return x + WIDTH - 108; }
    private static boolean inside(Click click, int x, int y, int width, int height) { return click.x() >= x && click.x() <= x + width && click.y() >= y && click.y() <= y + height; }
}
