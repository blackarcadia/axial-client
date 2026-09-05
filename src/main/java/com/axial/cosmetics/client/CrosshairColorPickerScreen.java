package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.function.IntConsumer;

public final class CrosshairColorPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 236;
    private static final int PANEL_PADDING = 18;
    private static final int ROW_START_Y = 54;
    private static final int ROW_GAP = 36;
    private static final int BAR_X_OFFSET = 72;
    private static final int BAR_WIDTH = 210;
    private static final int BAR_HEIGHT = 12;
    private static final int STEP_BUTTON_SIZE = 18;
    private static final int PREVIEW_HEIGHT = 24;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private final String label;
    private final IntConsumer onChange;
    private final ColorPickerSaveAction saveAction;
    private final int initialColor;
    private int panelX;
    private int panelY;
    private int red;
    private int green;
    private int blue;
    private int draggingChannel = -1;

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
        context.drawCenteredTextWithShadow(textRenderer, uiText("ADJUST RED, GREEN, AND BLUE."), panelX + PANEL_WIDTH / 2, panelY + 24, 0xFFC6D0F3);
        drawChannelRow(context, mouseX, mouseY, 0, "RED", red, 0xFFE05252);
        drawChannelRow(context, mouseX, mouseY, 1, "GREEN", green, 0xFF4DC86A);
        drawChannelRow(context, mouseX, mouseY, 2, "BLUE", blue, 0xFF5792FF);
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

        for (int channel = 0; channel < 3; channel++) {
            int rowY = channelRowY(channel);
            if (inside(click.x(), click.y(), panelX + BAR_X_OFFSET, rowY + 11, BAR_WIDTH, BAR_HEIGHT)) {
                draggingChannel = channel;
                updateChannelFromMouse(channel, (int) click.x());
                return true;
            }
            if (inside(click.x(), click.y(), panelX + BAR_X_OFFSET - 28, rowY + 8, STEP_BUTTON_SIZE, STEP_BUTTON_SIZE)) {
                stepChannel(channel, -8);
                return true;
            }
            if (inside(click.x(), click.y(), panelX + BAR_X_OFFSET + BAR_WIDTH + 10, rowY + 8, STEP_BUTTON_SIZE, STEP_BUTTON_SIZE)) {
                stepChannel(channel, 8);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingChannel >= 0) {
            updateChannelFromMouse(draggingChannel, (int) click.x());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            draggingChannel = -1;
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

    private void drawChannelRow(DrawContext context, int mouseX, int mouseY, int channel, String name, int value, int accent) {
        int rowY = channelRowY(channel);
        int labelX = panelX + PANEL_PADDING;
        int barX = panelX + BAR_X_OFFSET;
        int barY = rowY + 11;
        context.drawTextWithShadow(textRenderer, uiText(name), labelX, rowY + 10, accent);
        drawStepButton(context, barX - 28, rowY + 8, "-", inside(mouseX, mouseY, barX - 28, rowY + 8, STEP_BUTTON_SIZE, STEP_BUTTON_SIZE));
        drawStepButton(context, barX + BAR_WIDTH + 10, rowY + 8, "+", inside(mouseX, mouseY, barX + BAR_WIDTH + 10, rowY + 8, STEP_BUTTON_SIZE, STEP_BUTTON_SIZE));
        context.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF141822);
        context.fill(barX, barY, barX + Math.round(value / 255.0f * BAR_WIDTH), barY + BAR_HEIGHT, accent);
        context.drawStrokedRectangle(barX, barY, BAR_WIDTH, BAR_HEIGHT, 0xCCFFFFFF);
        int knobX = barX + Math.round(value / 255.0f * BAR_WIDTH);
        context.fill(knobX - 2, barY - 3, knobX + 3, barY + BAR_HEIGHT + 3, 0xFFF7F7FF);
        context.drawTextWithShadow(textRenderer, uiText(String.format(Locale.ROOT, "%03d", value)), barX + BAR_WIDTH + 38, rowY + 10, 0xFFC6D0F3);
    }

    private void drawStepButton(DrawContext context, int x, int y, String text, boolean hovered) {
        drawButton(context, x, y, STEP_BUTTON_SIZE, STEP_BUTTON_SIZE, hovered);
        context.drawCenteredTextWithShadow(textRenderer, uiText(text), x + STEP_BUTTON_SIZE / 2, y + 5, hovered ? 0xFFF7F7FF : 0xFFC6D0F3);
    }

    private void drawBackButton(DrawContext context, int mouseX, int mouseY) {
        int buttonX = panelX + PANEL_PADDING;
        int buttonY = panelY + 7;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, 24, 18);
        drawButton(context, buttonX, buttonY, 24, 18, hovered);
        context.drawCenteredTextWithShadow(textRenderer, uiText("<"), buttonX + 12, buttonY + 5, hovered ? 0xFFF7F7FF : 0xFFC6D0F3);
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
        int previewY = panelY + ROW_START_Y + ROW_GAP * 3 + 12;
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

    private int channelRowY(int channel) {
        return panelY + ROW_START_Y + channel * ROW_GAP;
    }

    private void stepChannel(int channel, int delta) {
        setChannel(channel, getChannel(channel) + delta);
        pushColor();
    }

    private void updateChannelFromMouse(int channel, int mouseX) {
        int barX = panelX + BAR_X_OFFSET;
        int value = Math.round((mouseX - barX) / (float) BAR_WIDTH * 255.0f);
        setChannel(channel, value);
        pushColor();
    }

    private int getChannel(int channel) {
        return switch (channel) {
            case 0 -> red;
            case 1 -> green;
            case 2 -> blue;
            default -> 0;
        };
    }

    private void setChannel(int channel, int value) {
        int clamped = Math.max(0, Math.min(255, value));
        switch (channel) {
            case 0 -> red = clamped;
            case 1 -> green = clamped;
            case 2 -> blue = clamped;
            default -> {
            }
        }
    }

    private void setColor(int argb) {
        red = (argb >> 16) & 255;
        green = (argb >> 8) & 255;
        blue = argb & 255;
    }

    private void pushColor() {
        onChange.accept(currentColor());
        saveAction.save();
    }

    private int currentColor() {
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
