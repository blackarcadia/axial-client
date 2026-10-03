package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;

public final class ChunkBordersSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 452;
    private static final int PANEL_HEIGHT = 220;
    private static final int PANEL_PADDING = 18;
    private static final int BACK_BUTTON_WIDTH = 24;
    private static final int BACK_BUTTON_HEIGHT = 18;
    private static final int TOGGLE_WIDTH = 416;
    private static final int TOGGLE_HEIGHT = 20;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private int panelX;
    private int panelY;
    private boolean capturingToggleKey;

    public ChunkBordersSettingsScreen(Screen parent) {
        super(uiText("CHUNK BORDERS"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    @Override
    public void close() {
        ChunkBordersConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        rebuildLayout();
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 10, 0xFFF7F7FF);
        drawBackButton(context, mouseX, mouseY);
        drawToggleButton(context, mouseX, mouseY);
        drawDivider(context, panelY + 66, "KEYBIND");
        drawKeybindRow(context, mouseX, mouseY);
        drawDivider(context, panelY + 118, "COLOR");
        drawColorRow(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        if (inside(click.x(), click.y(), panelX + PANEL_PADDING, panelY + 6, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)) {
            close();
            return true;
        }

        if (inside(click.x(), click.y(), toggleX(), toggleY(), TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            ChunkBordersConfig.setEnabled(!ChunkBordersConfig.enabled(), MinecraftClient.getInstance());
            return true;
        }

        if (inside(click.x(), click.y(), keybindButtonX(), keybindRowY(), 92, 20)) {
            capturingToggleKey = true;
            return true;
        }

        if (inside(click.x(), click.y(), colorSwatchX(), colorRowY(), 20, 20)) {
            MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(
                    this,
                    "CHUNK BORDERS",
                    ChunkBordersConfig.color(),
                    ChunkBordersConfig::setColor,
                    ChunkBordersConfig::save));
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!capturingToggleKey) {
            return super.keyPressed(input);
        }

        capturingToggleKey = false;
        if (input.key() != 256) { // Escape cancels without changing the binding.
            KeyBinding binding = AxialCosmetics.chunkBordersToggleKey();
            binding.setBoundKey(InputUtil.fromKeyCode(input));
            KeyBinding.updateKeysByCode();
            MinecraftClient.getInstance().options.write();
        }
        return true;
    }

    private void rebuildLayout() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = Math.max(16, (height - PANEL_HEIGHT) / 2);
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
    }

    private void drawBackButton(DrawContext context, int mouseX, int mouseY) {
        ModMenuBackButton.draw(context, panelX + PANEL_PADDING, panelY + 6, mouseX, mouseY);
    }

    private void drawToggleButton(DrawContext context, int mouseX, int mouseY) {
        int x = toggleX();
        int y = toggleY();
        boolean enabled = ChunkBordersConfig.enabled();
        boolean hovered = inside(mouseX, mouseY, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        AxialUiTheme.drawButton(context, textRenderer, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, "", "", hovered, false,
                enabled ? AxialUiTheme.TOGGLE_ON : AxialUiTheme.TOGGLE_OFF);
        context.drawCenteredTextWithShadow(textRenderer, uiText(enabled ? "ENABLED" : "DISABLED"), x + TOGGLE_WIDTH / 2, y + 5, 0xFFF7F7FF);
    }

    private int toggleX() {
        return panelX + 18;
    }

    private int toggleY() {
        return panelY + 30;
    }

    private int colorRowY() {
        return panelY + 134;
    }

    private int colorSwatchX() {
        return panelX + PANEL_WIDTH - PANEL_PADDING - 28;
    }

    private int keybindRowY() {
        return panelY + 82;
    }

    private int keybindButtonX() {
        return panelX + PANEL_WIDTH - PANEL_PADDING - 92;
    }

    private void drawDivider(DrawContext context, int y, String label) {
        Text text = uiText(label);
        int x = panelX + PANEL_PADDING;
        context.drawTextWithShadow(textRenderer, text, x + 2, y, 0xFFC6D0F3);
        context.fill(x + textRenderer.getWidth(text) + 12, y + 5, panelX + PANEL_WIDTH - PANEL_PADDING, y + 6, 0x998F5DFF);
    }

    private void drawColorRow(DrawContext context, int mouseX, int mouseY) {
        int y = colorRowY();
        int swatchX = colorSwatchX();
        context.drawTextWithShadow(textRenderer, uiText("BORDER COLOR"), panelX + PANEL_PADDING + 2, y + 6, 0xFFC6D0F3);
        Text hex = uiText(String.format("#%06X", ChunkBordersConfig.color() & 0xFFFFFF));
        context.drawTextWithShadow(textRenderer, hex, swatchX - textRenderer.getWidth(hex) - 10, y + 6, 0xFFC6D0F3);
        boolean hovered = inside(mouseX, mouseY, swatchX, y, 20, 20);
        context.fill(swatchX, y + 2, swatchX + 16, y + 18, ChunkBordersConfig.color());
        context.drawStrokedRectangle(swatchX, y + 2, 16, 16, hovered ? 0xFFFFFFFF : 0xFFB9C5E8);
    }

    private void drawKeybindRow(DrawContext context, int mouseX, int mouseY) {
        int y = keybindRowY();
        int buttonX = keybindButtonX();
        context.drawTextWithShadow(textRenderer, uiText("TOGGLE KEY"), panelX + PANEL_PADDING + 2, y + 6, 0xFFC6D0F3);
        boolean hovered = inside(mouseX, mouseY, buttonX, y, 92, 20);
        context.fill(buttonX, y, buttonX + 92, y + 20, hovered ? 0xBC20283A : 0xA0181D2C);
        context.fill(buttonX + 1, y + 1, buttonX + 91, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(buttonX, y, 92, 20, 0xFFE7D9FF);
        String label = capturingToggleKey ? "PRESS A KEY" : AxialCosmetics.chunkBordersToggleKey().getBoundKeyLocalizedText().getString().toUpperCase();
        context.drawCenteredTextWithShadow(textRenderer, uiText(label), buttonX + 46, y + 5, 0xFFF7F7FF);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
