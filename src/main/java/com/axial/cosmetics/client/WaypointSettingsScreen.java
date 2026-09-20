package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

/** The editable list of player-created waypoints. */
public final class WaypointSettingsScreen extends Screen {
    private static final int WIDTH = 460;
    private static final int HEIGHT = 260;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private final Screen parent;
    private List<WaypointConfig.Entry> waypoints;
    private final List<TextFieldWidget> nameFields = new ArrayList<>();
    private int x, y;
    private int scrollOffset;

    public WaypointSettingsScreen(Screen parent) {
        super(uiText("WAYPOINTS"));
        this.parent = parent;
    }

    @Override protected void init() {
        waypoints = WaypointConfig.waypointsFor(MinecraftClient.getInstance());
        layout();
        for (int i = 0; i < waypoints.size(); i++) {
            int index = i;
            TextFieldWidget field = new TextFieldWidget(textRenderer, x + 46, rowY(i) + 2, 160, 20, uiText("NAME"));
            field.setText(waypoints.get(i).name());
            field.setMaxLength(48);
            field.setChangedListener(value -> WaypointConfig.rename(waypoints.get(index), value));
            addDrawableChild(field);
            nameFields.add(field);
        }
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        context.fill(x, y, x + WIDTH, y + HEIGHT, 0xE8101018);
        context.drawStrokedRectangle(x, y, WIDTH, HEIGHT, 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, title, x + WIDTH / 2, y + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("VIEW YOUR AVAILABLE WAYPOINTS, EDIT, CREATE OR DELETE WAYPOINTS"), x + WIDTH / 2, y + 25, 0xFFC6D0F3);
        ModMenuBackButton.draw(context, x + 18, y + 6, mouseX, mouseY);
        drawFeatureToggle(context, mouseX, mouseY);
        if (waypoints.isEmpty()) context.drawCenteredTextWithShadow(textRenderer, uiText("NO WAYPOINTS CREATED"), x + WIDTH / 2, y + 94, 0xFFC6D0F3);
        context.enableScissor(x + 18, contentTop(), x + WIDTH - 18, contentBottom());
        for (int i = 0; i < waypoints.size(); i++) drawRow(context, mouseX, mouseY, i);
        for (int i = 0; i < nameFields.size(); i++) {
            TextFieldWidget field = nameFields.get(i);
            field.setPosition(x + 46, rowY(i) + 2);
            field.render(context, mouseX, mouseY, delta);
        }
        context.disableScissor();
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && inside(click, x + 18, y + 6, 24, 18)) { close(); return true; }
        if (click.button() == 0 && inside(click, x + 18, y + 46, WIDTH - 36, 20)) {
            WaypointConfig.setEnabled(!WaypointConfig.enabled());
            return true;
        }
        if (click.button() == 0) for (int i = 0; i < waypoints.size(); i++) {
            if (inside(click, deleteX(), rowY(i) + 2, 90, 20)) {
                WaypointConfig.delete(waypoints.get(i));
                MinecraftClient.getInstance().setScreen(new WaypointSettingsScreen(parent));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < x + 18 || mouseX > x + WIDTH - 18 || mouseY < contentTop() || mouseY > contentBottom() || verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int next = scrollOffset + (verticalAmount < 0 ? 18 : -18);
        scrollOffset = Math.max(0, Math.min(next, maxScroll()));
        return true;
    }

    private void drawRow(DrawContext c, int mx, int my, int i) {
        WaypointConfig.Entry waypoint = waypoints.get(i);
        int rowY = rowY(i);
        c.fill(x + 18, rowY, x + WIDTH - 18, rowY + 24, 0xA0181D2C);
        c.drawStrokedRectangle(x + 18, rowY, WIDTH - 36, 24, waypoint.color());
        c.fill(x + 24, rowY + 6, x + 36, rowY + 18, waypoint.color());
        c.drawTextWithShadow(textRenderer, uiText("X: " + waypoint.x() + "  Y: " + waypoint.y() + "  Z: " + waypoint.z()), x + 214, rowY + 8, 0xFFC6D0F3);
        int deleteX = deleteX(); boolean hover = mx >= deleteX && mx <= deleteX + 90 && my >= rowY + 2 && my <= rowY + 22;
        c.fill(deleteX, rowY + 2, deleteX + 90, rowY + 22, hover ? 0xC06E2635 : 0xA04A1E28);
        c.drawStrokedRectangle(deleteX, rowY + 2, 90, 20, 0xFFE85D75);
        c.drawCenteredTextWithShadow(textRenderer, uiText("DELETE"), deleteX + 45, rowY + 8, 0xFFF7F7FF);
    }

    private void drawFeatureToggle(DrawContext c, int mouseX, int mouseY) {
        int toggleX = x + 18;
        int toggleY = y + 46;
        boolean enabled = WaypointConfig.enabled();
        boolean hovered = mouseX >= toggleX && mouseX <= toggleX + WIDTH - 36 && mouseY >= toggleY && mouseY <= toggleY + 20;
        c.fill(toggleX, toggleY, toggleX + WIDTH - 36, toggleY + 20, hovered ? 0xBC20283A : 0xA0181D2C);
        c.drawStrokedRectangle(toggleX, toggleY, WIDTH - 36, 20, enabled ? 0xFF8AF0C2 : 0xFFE85D75);
        c.drawCenteredTextWithShadow(textRenderer, uiText(enabled ? "WAYPOINTS: ENABLED" : "WAYPOINTS: DISABLED"), toggleX + (WIDTH - 36) / 2, toggleY + 6, 0xFFF7F7FF);
    }

    private void layout() { x = (width - WIDTH) / 2; y = Math.max(16, (height - HEIGHT) / 2); }
    private int rowY(int index) { return contentTop() + index * 30 - scrollOffset; }
    private int contentTop() { return y + 76; }
    private int contentBottom() { return y + HEIGHT - 12; }
    private int maxScroll() { return Math.max(0, waypoints.size() * 30 - (contentBottom() - contentTop())); }
    private int deleteX() { return x + WIDTH - 108; }
    private static boolean inside(Click click, int x, int y, int width, int height) { return click.x() >= x && click.x() <= x + width && click.y() >= y && click.y() <= y + height; }
    private static Text uiText(String value) { return Text.literal(value).styled(style -> style.withFont(UI_FONT)); }
}
