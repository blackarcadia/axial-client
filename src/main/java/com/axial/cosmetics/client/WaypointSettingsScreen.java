package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;

import java.util.ArrayList;
import java.util.List;

/** The editable list of player-created waypoints. */
public final class WaypointSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 452;
    private static final int PANEL_HEIGHT = 300;
    private static final int PADDING = 18;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private List<WaypointConfig.Entry> waypoints = List.of();
    private final List<TextFieldWidget> nameFields = new ArrayList<>();
    private int panelX, panelY, scrollOffset;
    private boolean capturingCreateKey;

    public WaypointSettingsScreen(Screen parent) {
        super(uiText("WAYPOINTS"));
        this.parent = parent;
    }

    @Override protected void init() {
        nameFields.clear();
        waypoints = new ArrayList<>(WaypointConfig.waypointsFor(MinecraftClient.getInstance()));
        layout();
        for (int i = 0; i < waypoints.size(); i++) {
            int index = i;
            TextFieldWidget field = new TextFieldWidget(textRenderer, panelX + 28, rowY(i) + 2, 138, 20, uiText("NAME"));
            field.setText(waypoints.get(i).name());
            field.setMaxLength(48);
            field.setChangedListener(value -> WaypointConfig.rename(waypoints.get(index), value));
            addDrawableChild(field);
            nameFields.add(field);
        }
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 10, 0xFFF7F7FF);
        ModMenuBackButton.draw(context, panelX + PADDING, panelY + 6, mouseX, mouseY);
        drawFeatureToggle(context, mouseX, mouseY);
        divider(context, panelY + 66, "KEYBIND");
        drawKeybindRow(context, mouseX, mouseY);
        divider(context, panelY + 118, "WAYPOINTS");

        context.enableScissor(panelX + PADDING, contentTop(), panelX + PANEL_WIDTH - PADDING, contentBottom());
        if (waypoints.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, uiText("NO CURRENT WAYPOINTS SET"), panelX + PANEL_WIDTH / 2, contentTop() + 22, 0xFFC6D0F3);
        } else {
            for (int i = 0; i < waypoints.size(); i++) drawRow(context, mouseX, mouseY, i);
            for (int i = 0; i < nameFields.size(); i++) {
                TextFieldWidget field = nameFields.get(i);
                field.setPosition(panelX + 28, rowY(i) + 2);
                field.render(context, mouseX, mouseY, delta);
            }
        }
        context.disableScissor();
        drawScrollBar(context);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (inside(click, panelX + PADDING, panelY + 6, 24, 18)) { close(); return true; }
        if (inside(click, toggleX(), toggleY(), 416, 20)) { WaypointConfig.setEnabled(!WaypointConfig.enabled()); return true; }
        if (inside(click, keybindX(), keybindY(), 92, 20)) { capturingCreateKey = true; return true; }
        for (int i = 0; i < waypoints.size(); i++) {
            if (inside(click, panelX + 28, rowY(i) + 2, 138, 20)) {
                TextFieldWidget field = nameFields.get(i); field.setFocused(true); setFocused(field); return super.mouseClicked(click, doubled);
            }
            if (inside(click, colorX(), rowY(i) + 2, 54, 20)) {
                int index = i; WaypointConfig.Entry waypoint = waypoints.get(i);
                MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "WAYPOINT", waypoint.color(), color -> waypoints.set(index, WaypointConfig.setColor(waypoint, color)), () -> { }));
                return true;
            }
            if (inside(click, deleteX(), rowY(i) + 2, 72, 20)) {
                WaypointConfig.delete(waypoints.get(i)); MinecraftClient.getInstance().setScreen(new WaypointSettingsScreen(parent)); return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (!capturingCreateKey) return super.keyPressed(input);
        capturingCreateKey = false;
        if (input.key() != 256) {
            KeyBinding binding = AxialCosmetics.createWaypointKey();
            binding.setBoundKey(InputUtil.fromKeyCode(input));
            KeyBinding.updateKeysByCode();
            MinecraftClient.getInstance().options.write();
        }
        return true;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < panelX + PADDING || mouseX > panelX + PANEL_WIDTH - PADDING || mouseY < contentTop() || mouseY > contentBottom() || verticalAmount == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        scrollOffset = Math.max(0, Math.min(scrollOffset + (verticalAmount < 0 ? 24 : -24), maxScroll()));
        return true;
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
    }

    private void drawFeatureToggle(DrawContext context, int mouseX, int mouseY) {
        int x = toggleX(), y = toggleY(); boolean enabled = WaypointConfig.enabled();
        AxialUiTheme.drawButton(context, textRenderer, x, y, 416, 20, "", "", inside(mouseX, mouseY, x, y, 416, 20), false, enabled ? AxialUiTheme.TOGGLE_ON : AxialUiTheme.TOGGLE_OFF);
        context.drawCenteredTextWithShadow(textRenderer, uiText(enabled ? "ENABLED" : "DISABLED"), x + 208, y + 5, 0xFFF7F7FF);
    }

    private void divider(DrawContext context, int y, String label) {
        Text text = uiText(label); int x = panelX + PADDING;
        context.drawTextWithShadow(textRenderer, text, x + 2, y, 0xFFC6D0F3);
        context.fill(x + textRenderer.getWidth(text) + 12, y + 5, panelX + PANEL_WIDTH - PADDING, y + 6, 0x998F5DFF);
    }

    private void drawKeybindRow(DrawContext context, int mouseX, int mouseY) {
        int y = keybindY(), x = keybindX();
        context.drawTextWithShadow(textRenderer, uiText("CREATE WAYPOINT"), panelX + PADDING + 2, y + 6, 0xFFC6D0F3);
        boolean hovered = inside(mouseX, mouseY, x, y, 92, 20);
        context.fill(x, y, x + 92, y + 20, hovered ? 0xBC20283A : 0xA0181D2C);
        context.fill(x + 1, y + 1, x + 91, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(x, y, 92, 20, 0xFFE7D9FF);
        String label = capturingCreateKey ? "PRESS A KEY" : AxialCosmetics.createWaypointKey().getBoundKeyLocalizedText().getString().toUpperCase();
        context.drawCenteredTextWithShadow(textRenderer, uiText(label), x + 46, y + 5, 0xFFF7F7FF);
    }

    private void drawRow(DrawContext context, int mouseX, int mouseY, int index) {
        WaypointConfig.Entry waypoint = waypoints.get(index); int y = rowY(index);
        context.fill(panelX + PADDING, y, panelX + PANEL_WIDTH - PADDING, y + 24, 0xA0181D2C);
        context.drawStrokedRectangle(panelX + PADDING, y, PANEL_WIDTH - PADDING * 2, 24, waypoint.color());
        int colorX = colorX();
        context.fill(colorX, y + 2, colorX + 54, y + 22, 0xA0181D2C);
        context.drawStrokedRectangle(colorX, y + 2, 54, 20, waypoint.color());
        context.fill(colorX + 5, y + 6, colorX + 15, y + 16, waypoint.color());
        context.drawTextWithShadow(textRenderer, uiText("COLOR"), colorX + 19, y + 8, 0xFFF7F7FF);
        context.drawTextWithShadow(textRenderer, uiText("X " + waypoint.x() + "  Y " + waypoint.y() + "  Z " + waypoint.z()), panelX + 244, y + 8, 0xFFC6D0F3);
        int deleteX = deleteX(); boolean hovered = inside(mouseX, mouseY, deleteX, y + 2, 72, 20);
        context.fill(deleteX, y + 2, deleteX + 72, y + 22, hovered ? 0xC06E2635 : 0xA04A1E28);
        context.drawStrokedRectangle(deleteX, y + 2, 72, 20, 0xFFE85D75);
        context.drawCenteredTextWithShadow(textRenderer, uiText("DELETE"), deleteX + 36, y + 8, 0xFFF7F7FF);
    }

    private void drawScrollBar(DrawContext context) {
        int max = maxScroll(); if (max == 0) return;
        int top = contentTop(), height = contentBottom() - top, contentHeight = waypoints.size() * 30;
        int thumbHeight = Math.max(18, Math.round(height * (height / (float) contentHeight)));
        int thumbY = top + Math.round(scrollOffset / (float) max * (height - thumbHeight)); int x = panelX + PANEL_WIDTH - 12;
        context.fill(x, top, x + 4, top + height, 0x2AFFFFFF); context.fill(x, thumbY, x + 4, thumbY + thumbHeight, 0xFFB06AF3);
    }

    private void layout() { panelX = (width - PANEL_WIDTH) / 2; panelY = Math.max(16, (height - PANEL_HEIGHT) / 2); }
    private int toggleX() { return panelX + PADDING; } private int toggleY() { return panelY + 30; }
    private int keybindY() { return panelY + 82; } private int keybindX() { return panelX + PANEL_WIDTH - PADDING - 92; }
    private int contentTop() { return panelY + 134; } private int contentBottom() { return panelY + PANEL_HEIGHT - 12; }
    private int rowY(int index) { return contentTop() + index * 30 - scrollOffset; }
    private int maxScroll() { return Math.max(0, waypoints.size() * 30 - (contentBottom() - contentTop())); }
    private int colorX() { return panelX + 174; } private int deleteX() { return panelX + PANEL_WIDTH - PADDING - 76; }
    private static boolean inside(Click click, int x, int y, int width, int height) { return inside(click.x(), click.y(), x, y, width, height); }
    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) { return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height; }
    private static Text uiText(String value) { return Text.literal(value).styled(style -> style.withFont(UI_FONT)); }
}
