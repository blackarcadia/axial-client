package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;

/** Settings for the HUD minimap and its full-map shortcut. */
public final class MinimapSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 452;
    private static final int PANEL_HEIGHT = 284;
    private static final int PADDING = 18;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private int panelX, panelY;
    private boolean capturingMapKey;

    public MinimapSettingsScreen(Screen parent) {
        super(uiText("MINIMAP"));
        this.parent = parent;
    }

    @Override protected void init() { layout(); }

    @Override public void close() {
        MinimapConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 10, 0xFFF7F7FF);
        ModMenuBackButton.draw(context, panelX + PADDING, panelY + 6, mouseX, mouseY);
        drawEnabled(context, mouseX, mouseY);
        divider(context, panelY + 66, "OPTIONS");
        drawOption(context, mouseX, mouseY, "SIZE", MinimapConfig.sizeLabel(), optionY());
        drawOption(context, mouseX, mouseY, "STYLE", MinimapConfig.styleLabel(), optionY() + 30);
        drawOption(context, mouseX, mouseY, "ZOOM", MinimapConfig.zoomLabel(), optionY() + 60);
        divider(context, panelY + 184, "KEYBIND");
        drawKeybind(context, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (inside(click.x(), click.y(), panelX + PADDING, panelY + 6, 24, 18)) { close(); return true; }
        if (inside(click.x(), click.y(), panelX + PADDING, panelY + 30, 416, 20)) { MinimapConfig.toggle(); return true; }
        if (inside(click.x(), click.y(), panelX + PADDING, optionY(), 416, 20)) { MinimapConfig.cycleSize(); return true; }
        if (inside(click.x(), click.y(), panelX + PADDING, optionY() + 30, 416, 20)) { MinimapConfig.cycleStyle(); return true; }
        if (inside(click.x(), click.y(), panelX + PADDING, optionY() + 60, 416, 20)) { MinimapConfig.cycleZoom(); return true; }
        if (inside(click.x(), click.y(), keybindX(), keybindY(), 92, 20)) { capturingMapKey = true; return true; }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (!capturingMapKey) return super.keyPressed(input);
        capturingMapKey = false;
        if (input.key() != 256) {
            KeyBinding binding = AxialCosmetics.minimapMapKey();
            binding.setBoundKey(InputUtil.fromKeyCode(input));
            KeyBinding.updateKeysByCode();
            MinecraftClient.getInstance().options.write();
        }
        return true;
    }

    private void drawEnabled(DrawContext context, int mouseX, int mouseY) {
        int x = panelX + PADDING, y = panelY + 30;
        boolean enabled = MinimapConfig.isEnabled();
        AxialUiTheme.drawButton(context, textRenderer, x, y, 416, 20, "", "", inside(mouseX, mouseY, x, y, 416, 20), false,
                enabled ? AxialUiTheme.TOGGLE_ON : AxialUiTheme.TOGGLE_OFF);
        context.drawCenteredTextWithShadow(textRenderer, uiText(enabled ? "ENABLED" : "DISABLED"), x + 208, y + 5, 0xFFF7F7FF);
    }

    private void drawOption(DrawContext context, int mouseX, int mouseY, String label, String value, int y) {
        int x = panelX + PADDING;
        context.drawTextWithShadow(textRenderer, uiText(label), x + 2, y + 6, 0xFFC6D0F3);
        int buttonX = panelX + PANEL_WIDTH - PADDING - 118;
        boolean hovered = inside(mouseX, mouseY, buttonX, y, 118, 20);
        context.fill(buttonX, y, buttonX + 118, y + 20, hovered ? 0xBC20283A : 0xA0181D2C);
        context.fill(buttonX + 1, y + 1, buttonX + 117, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(buttonX, y, 118, 20, 0xFFE7D9FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText(value), buttonX + 59, y + 5, 0xFFF7F7FF);
    }

    private void drawKeybind(DrawContext context, int mouseX, int mouseY) {
        int y = keybindY(), x = keybindX();
        context.drawTextWithShadow(textRenderer, uiText("OPEN FULL MAP"), panelX + PADDING + 2, y + 6, 0xFFC6D0F3);
        boolean hovered = inside(mouseX, mouseY, x, y, 92, 20);
        context.fill(x, y, x + 92, y + 20, hovered ? 0xBC20283A : 0xA0181D2C);
        context.fill(x + 1, y + 1, x + 91, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(x, y, 92, 20, 0xFFE7D9FF);
        String label = capturingMapKey ? "PRESS A KEY" : AxialCosmetics.minimapMapKey().getBoundKeyLocalizedText().getString().toUpperCase();
        context.drawCenteredTextWithShadow(textRenderer, uiText(label), x + 46, y + 5, 0xFFF7F7FF);
    }

    private void divider(DrawContext context, int y, String label) {
        Text text = uiText(label);
        int x = panelX + PADDING;
        context.drawTextWithShadow(textRenderer, text, x + 2, y, 0xFFC6D0F3);
        context.fill(x + textRenderer.getWidth(text) + 12, y + 5, panelX + PANEL_WIDTH - PADDING, y + 6, 0x998F5DFF);
    }

    private void layout() { panelX = (width - PANEL_WIDTH) / 2; panelY = Math.max(16, (height - PANEL_HEIGHT) / 2); }
    private int optionY() { return panelY + 82; }
    private int keybindY() { return panelY + 200; }
    private int keybindX() { return panelX + PANEL_WIDTH - PADDING - 92; }
    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) { return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height; }
    private static Text uiText(String value) { return Text.literal(value).styled(style -> style.withFont(UI_FONT)); }
}
