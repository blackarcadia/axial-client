package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.Locale;
import java.util.function.IntConsumer;

public final class CrosshairColorPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 236;
    private static final int PANEL_PADDING = 18;
    private static final int PICKER_Y = 44;
    private static final int PICKER_WIDTH = 288;
    private static final int PICKER_HEIGHT = 120;
    private static final int HUE_WIDTH = 18;
    private static final int PREVIEW_HEIGHT = 24;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private final String label;
    private final IntConsumer onChange;
    private final ColorPickerSaveAction saveAction;
    private final int initialColor;
    private int panelX;
    private int panelY;
    private float hue;
    private float saturation;
    private float brightness;
    private int draggingControl = -1;

    public CrosshairColorPickerScreen(Screen parent, String label, int initialColor, IntConsumer onChange) {
        this(parent, label, initialColor, onChange, CrosshairConfigManager::save);
    }

    public CrosshairColorPickerScreen(Screen parent, String label, int initialColor, IntConsumer onChange, ColorPickerSaveAction saveAction) {
        super(uiText((label + " COLOR").toUpperCase(Locale.ROOT)));
        this.parent = parent;
        this.label = label;
        this.onChange = onChange;
        this.saveAction = saveAction;
        this.initialColor = initialColor;
        setColor(initialColor);
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    @Override
    public void close() {
        saveAction.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        rebuildLayout();
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("CHOOSE A SHADE AND HUE."), panelX + PANEL_WIDTH / 2, panelY + 26, 0xFFC6D0F3);
        drawPicker(context);
        drawPreview(context);
        drawBackButton(context, mouseX, mouseY);
        drawResetButton(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != 0) {
            return false;
        }

        int backButtonX = panelX + PANEL_PADDING;
        int backButtonY = panelY + 7;
        if (inside(click.x(), click.y(), backButtonX, backButtonY, 24, 18)) {
            close();
            return true;
        }

        int resetX = panelX + PANEL_WIDTH - PANEL_PADDING - 80;
        int resetY = panelY + PANEL_HEIGHT - PANEL_PADDING - 18;
        if (inside(click.x(), click.y(), resetX, resetY, 80, 18)) {
            setColor(initialColor);
            pushColor();
            return true;
        }

        if (inside(click.x(), click.y(), panelX + PANEL_PADDING, panelY + PICKER_Y, PICKER_WIDTH, PICKER_HEIGHT)) {
            draggingControl = 0;
            updateFromMouse(click.x(), click.y());
            return true;
        }
        if (inside(click.x(), click.y(), hueX(), panelY + PICKER_Y, HUE_WIDTH, PICKER_HEIGHT)) {
            draggingControl = 1;
            updateFromMouse(click.x(), click.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingControl >= 0) {
            updateFromMouse(click.x(), click.y());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            draggingControl = -1;
        }
        return super.mouseReleased(click);
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

    private void drawPicker(DrawContext context) {
        int x = panelX + PANEL_PADDING;
        int y = panelY + PICKER_Y;
        // Each column fades from its fully bright shade to black.
        for (int column = 0; column < PICKER_WIDTH; column++) {
            int color = Color.HSBtoRGB(hue, column / (float) (PICKER_WIDTH - 1), 1.0f);
            context.fillGradient(x + column, y, x + column + 1, y + PICKER_HEIGHT, color, 0xFF000000);
        }
        context.drawStrokedRectangle(x - 1, y - 1, PICKER_WIDTH + 2, PICKER_HEIGHT + 2, 0xCCFFFFFF);
        for (int row = 0; row < PICKER_HEIGHT; row++) {
            int color = Color.HSBtoRGB(row / (float) (PICKER_HEIGHT - 1), 1.0f, 1.0f);
            context.fill(hueX(), y + row, hueX() + HUE_WIDTH, y + row + 1, color);
        }
        context.drawStrokedRectangle(hueX() - 1, y - 1, HUE_WIDTH + 2, PICKER_HEIGHT + 2, 0xCCFFFFFF);
        int selectedX = x + Math.round(saturation * (PICKER_WIDTH - 1));
        int selectedY = y + Math.round((1.0f - brightness) * (PICKER_HEIGHT - 1));
        context.drawStrokedRectangle(selectedX - 3, selectedY - 3, 7, 7, 0xFF000000);
        context.drawStrokedRectangle(selectedX - 2, selectedY - 2, 5, 5, 0xFFFFFFFF);
        int hueY = y + Math.round(hue * (PICKER_HEIGHT - 1));
        context.drawStrokedRectangle(hueX() - 2, hueY - 2, HUE_WIDTH + 4, 5, 0xFF000000);
        context.drawStrokedRectangle(hueX() - 1, hueY - 1, HUE_WIDTH + 2, 3, 0xFFFFFFFF);
    }

    private int hueX() {
        return panelX + PANEL_WIDTH - PANEL_PADDING - HUE_WIDTH;
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        float vertical = clamp((float) (mouseY - panelY - PICKER_Y) / (PICKER_HEIGHT - 1));
        if (draggingControl == 0) {
            saturation = clamp((float) (mouseX - panelX - PANEL_PADDING) / (PICKER_WIDTH - 1));
            brightness = 1.0f - vertical;
        } else {
            hue = vertical;
        }
        pushColor();
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private void drawBackButton(DrawContext context, int mouseX, int mouseY) {
        ModMenuBackButton.draw(context, panelX + PANEL_PADDING, panelY + 7, mouseX, mouseY);
    }

    private void drawResetButton(DrawContext context, int mouseX, int mouseY) {
        int resetX = panelX + PANEL_WIDTH - PANEL_PADDING - 80;
        int resetY = panelY + PANEL_HEIGHT - PANEL_PADDING - 18;
        boolean hovered = inside(mouseX, mouseY, resetX, resetY, 80, 18);
        drawButton(context, resetX, resetY, 80, 18, hovered);
        context.drawCenteredTextWithShadow(textRenderer, uiText("RESET"), resetX + 40, resetY + 5, hovered ? 0xFFF7F7FF : 0xFFC6D0F3);
    }

    private void drawButton(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        int background = hovered ? 0xBC20283A : 0xA0181D2C;
        int border = hovered ? 0xFFE7D9FF : 0xD08F5DFF;
        context.fill(x, y, x + width, y + height, background);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, border);
    }

    private void drawPreview(DrawContext context) {
        int previewY = panelY + PICKER_Y + PICKER_HEIGHT + 10;
        int previewX = panelX + PANEL_PADDING;
        int previewWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        int previewColor = currentColor();
        context.fill(previewX, previewY, previewX + previewWidth, previewY + PREVIEW_HEIGHT, 0xAA141822);
        context.drawStrokedRectangle(previewX, previewY, previewWidth, PREVIEW_HEIGHT, 0xD08F5DFF);
        context.fill(previewX + 5, previewY + 5, previewX + 23, previewY + 19, previewColor);
        context.drawStrokedRectangle(previewX + 5, previewY + 5, 18, 14, 0xCCFFFFFF);
        context.drawTextWithShadow(textRenderer, uiText(label.toUpperCase(Locale.ROOT)), previewX + 30, previewY + 7, previewColor);
        context.drawTextWithShadow(textRenderer, uiText(String.format(Locale.ROOT, "#%06X", previewColor & 0xFFFFFF)), previewX + 178, previewY + 7, 0xFFC6D0F3);
    }

    private void setColor(int argb) {
        float[] hsb = Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    private void pushColor() {
        onChange.accept(currentColor());
        saveAction.save();
    }

    private int currentColor() {
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
