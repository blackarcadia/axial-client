package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class CrosshairSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 404;
    private static final int PANEL_HEIGHT = 310;
    private static final int PANEL_PADDING = 18;
    private static final int BACK_BUTTON_WIDTH = 24;
    private static final int BACK_BUTTON_HEIGHT = 18;
    private static final int CONTROL_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2;
    private static final int PREVIEW_HEIGHT = 64;
    private static final int PREVIEW_TOP_OFFSET = 30;
    private static final int CONTROLS_TOP_OFFSET = 108;
    private static final int ROW_GAP = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int ENABLED_ROW_OFFSET = 0;
    private static final int STYLE_ROW_OFFSET = 38;
    private static final int SIZE_ROW_OFFSET = 76;
    private static final int LENGTH_ROW_OFFSET = 114;
    private static final int WIDTH_ROW_OFFSET = 152;
    private static final int GAP_ROW_OFFSET = 190;
    private static final int COLOR_ROW_OFFSET = 228;
    private static final int OUTLINE_ROW_OFFSET = 304;
    private static final int DYNAMIC_ROW_OFFSET = 342;
    private static final int CONTENT_HEIGHT = 382;
    private static final float SIZE_MIN = 0.1f;
    private static final float SIZE_MAX = 4.0f;
    private static final float LENGTH_MIN = 0.0f;
    private static final float LENGTH_MAX = 32.0f;
    private static final float WIDTH_MIN = 2.0f;
    private static final float WIDTH_MAX = 12.0f;
    private static final float GAP_MIN = 0.0f;
    private static final float GAP_MAX = 20.0f;
    private static final Identifier BACK_ARROW_ICON = AxialCosmetics.id("textures/gui/back-arrow.png");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private final SizeSliderWidget sizeSlider;
    private final SizeSliderWidget lengthSlider;
    private final SizeSliderWidget widthSlider;
    private final SizeSliderWidget gapSlider;
    private int panelX;
    private int panelY;
    private int controlsViewportTop;
    private int controlsViewportBottom;
    private int contentHeight;
    private int scrollOffset;

    public CrosshairSettingsScreen(Screen parent) {
        super(uiText("CROSSHAIR"));
        this.parent = parent;
        this.sizeSlider = new SizeSliderWidget(0, 0, 0, 20, CrosshairField.SIZE);
        this.lengthSlider = new SizeSliderWidget(0, 0, 0, 20, CrosshairField.LENGTH);
        this.widthSlider = new SizeSliderWidget(0, 0, 0, 20, CrosshairField.WIDTH);
        this.gapSlider = new SizeSliderWidget(0, 0, 0, 20, CrosshairField.GAP);
    }

    @Override
    protected void init() {
        rebuildLayout();
        addDrawableChild(sizeSlider);
        addDrawableChild(lengthSlider);
        addDrawableChild(widthSlider);
        addDrawableChild(gapSlider);
        syncSliders();
    }

    @Override
    public void close() {
        CrosshairConfigManager.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        rebuildLayout();
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("CUSTOMIZE STYLE, COLOR, SIZE, LENGTH, WIDTH, AND OUTLINE."), panelX + PANEL_WIDTH / 2, panelY + 22, 0xFFC6D0F3);

        drawBackButton(context, mouseX, mouseY);
        drawPreview(context);

        context.enableScissor(panelX + PANEL_PADDING, controlsViewportTop, panelX + PANEL_WIDTH - PANEL_PADDING, controlsViewportBottom);
        super.render(context, mouseX, mouseY, deltaTicks);
        drawScrollableControls(context, mouseX, mouseY);
        drawScrollBar(context);
        context.disableScissor();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        rebuildLayout();
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (click.button() != 0) {
            return false;
        }

        if (inside(click.x(), click.y(), panelX + PANEL_PADDING, panelY + 6, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)) {
            close();
            return true;
        }

        CrosshairConfig config = CrosshairConfigManager.get();
        int enabledX = enabledToggleX();
        int enabledY = enabledToggleY();
        int enabledButtonWidth = enabledToggleWidth();
        int enabledButtonGap = enabledToggleGap();
        if (inside(click.x(), click.y(), enabledX, enabledY, enabledButtonWidth, 20)) {
            config.enabled = true;
            CrosshairConfigManager.save();
            return true;
        }

        if (inside(click.x(), click.y(), enabledX + enabledButtonWidth + enabledButtonGap, enabledY, enabledButtonWidth, 20)) {
            config.enabled = false;
            CrosshairConfigManager.save();
            return true;
        }

        if (inside(click.x(), click.y(), styleButtonX(), styleButtonY(), CONTROL_WIDTH, 20)) {
            cycleStyle();
            return true;
        }

        if (inside(click.x(), click.y(), colorButtonX(), colorButtonY(), CONTROL_WIDTH, 20)) {
            MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "CROSSHAIR", config.color, value -> {
                CrosshairConfigManager.get().color = value;
                CrosshairConfigManager.save();
            }));
            return true;
        }

        if (inside(click.x(), click.y(), colorButtonX(), colorButtonY() + 38, CONTROL_WIDTH, 20)) {
            MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "OUTLINE", config.outlineColor, value -> {
                CrosshairConfigManager.get().outlineColor = value;
                CrosshairConfigManager.save();
            }));
            return true;
        }

        int outlineX = outlineToggleX();
        int outlineY = outlineToggleY();
        int outlineButtonWidth = outlineToggleWidth();
        int outlineButtonGap = outlineToggleGap();
        if (inside(click.x(), click.y(), outlineX, outlineY, outlineButtonWidth, 20)) {
            config.outlineEnabled = true;
            CrosshairConfigManager.save();
            return true;
        }

        if (inside(click.x(), click.y(), outlineX + outlineButtonWidth + outlineButtonGap, outlineY, outlineButtonWidth, 20)) {
            config.outlineEnabled = false;
            CrosshairConfigManager.save();
            return true;
        }

        if (inside(click.x(), click.y(), dynamicToggleX(), dynamicToggleY(), CONTROL_WIDTH, 20)) {
            config.dynamicEnabled = !config.dynamicEnabled;
            CrosshairConfigManager.save();
            return true;
        }

        return false;
    }

    private void applyPreset(CrosshairConfig.CrosshairStyle style, float length, float width, float gap, boolean outline) {
        CrosshairConfig config = CrosshairConfigManager.get();
        config.style = style;
        config.length = length;
        config.width = width;
        config.gap = gap;
        config.outlineEnabled = outline;
        CrosshairConfigManager.save();
        syncSliders();
    }

    private void rebuildLayout() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = Math.max(16, (height - PANEL_HEIGHT) / 2);
        controlsViewportTop = panelY + CONTROLS_TOP_OFFSET;
        controlsViewportBottom = panelY + PANEL_HEIGHT - 18;
        int contentTop = controlsViewportTop;

        sizeSlider.setPosition(panelX + PANEL_PADDING, contentTop + SIZE_ROW_OFFSET - scrollOffset);
        sizeSlider.setWidth(CONTROL_WIDTH);
        sizeSlider.setY(contentTop + SIZE_ROW_OFFSET - scrollOffset);
        lengthSlider.setPosition(panelX + PANEL_PADDING, contentTop + LENGTH_ROW_OFFSET - scrollOffset);
        lengthSlider.setWidth(CONTROL_WIDTH);
        lengthSlider.setY(contentTop + LENGTH_ROW_OFFSET - scrollOffset);
        widthSlider.setPosition(panelX + PANEL_PADDING, contentTop + WIDTH_ROW_OFFSET - scrollOffset);
        widthSlider.setWidth(CONTROL_WIDTH);
        widthSlider.setY(contentTop + WIDTH_ROW_OFFSET - scrollOffset);
        gapSlider.setPosition(panelX + PANEL_PADDING, contentTop + GAP_ROW_OFFSET - scrollOffset);
        gapSlider.setWidth(CONTROL_WIDTH);
        gapSlider.setY(contentTop + GAP_ROW_OFFSET - scrollOffset);

        contentHeight = CONTENT_HEIGHT;
        int maxScroll = maxScroll();
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
            sizeSlider.setY(contentTop + SIZE_ROW_OFFSET - scrollOffset);
            lengthSlider.setY(contentTop + LENGTH_ROW_OFFSET - scrollOffset);
            widthSlider.setY(contentTop + WIDTH_ROW_OFFSET - scrollOffset);
            gapSlider.setY(contentTop + GAP_ROW_OFFSET - scrollOffset);
        }
    }

    private void syncSliders() {
        CrosshairConfig config = CrosshairConfigManager.get();
        sizeSlider.updateFromConfig(config.size);
        lengthSlider.updateFromConfig(config.length);
        widthSlider.updateFromConfig(config.width);
        gapSlider.updateFromConfig(config.gap);
    }

    private void drawBackButton(DrawContext context, int mouseX, int mouseY) {
        int buttonX = panelX + PANEL_PADDING;
        int buttonY = panelY + 6;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
        drawButton(context, buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, hovered, true);
        int iconWidth = 16;
        int iconHeight = 13;
        int iconX = buttonX + (BACK_BUTTON_WIDTH - iconWidth) / 2 - 4;
        int iconY = buttonY + (BACK_BUTTON_HEIGHT - iconHeight) / 2 - 1;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACK_ARROW_ICON, iconX, iconY, 0.0f, 0.0f, iconWidth, iconHeight, 64, 64, 64, 64);
    }

    private void drawPreview(DrawContext context) {
        int previewX = panelX + PANEL_PADDING;
        int previewY = panelY + PREVIEW_TOP_OFFSET;
        int previewWidth = CONTROL_WIDTH;
        int previewHeight = PREVIEW_HEIGHT;
        context.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xAA141822);
        context.drawStrokedRectangle(previewX, previewY, previewWidth, previewHeight, 0xD08F5DFF);
        context.fill(previewX + 1, previewY + previewHeight / 2, previewX + previewWidth - 1, previewY + previewHeight / 2 + 1, 0x22FFFFFF);
        context.fill(previewX + previewWidth / 2, previewY + 1, previewX + previewWidth / 2 + 1, previewY + previewHeight - 1, 0x22FFFFFF);
        CrosshairRenderer.render(context, previewX + previewWidth / 2, previewY + previewHeight / 2, CrosshairConfigManager.get(), 1.0f);
        context.drawTextWithShadow(textRenderer, uiText("PREVIEW"), previewX + 8, previewY + 6, 0xFFC6D0F3);
    }

    private void drawStyleCycleButton(DrawContext context, int mouseX, int mouseY) {
        int x = styleButtonX();
        int y = styleButtonY();
        CrosshairConfig config = CrosshairConfigManager.get();
        boolean hovered = inside(mouseX, mouseY, x, y, CONTROL_WIDTH, 20);
        drawButton(context, x, y, CONTROL_WIDTH, 20, hovered, true);
        context.drawTextWithShadow(textRenderer, uiText("STYLE"), x + 8, y + 5, 0xFFC6D0F3);
        context.drawCenteredTextWithShadow(textRenderer, uiText(config.style.name()), x + CONTROL_WIDTH / 2, y + 5, 0xFFF7F7FF);
    }

    private void drawScrollableControls(DrawContext context, int mouseX, int mouseY) {
        drawEnabledControls(context, mouseX, mouseY);
        drawStyleCycleButton(context, mouseX, mouseY);
        drawSliderLabels(context);
        drawSliderChrome(context, sizeSlider, mouseX, mouseY);
        drawSliderChrome(context, lengthSlider, mouseX, mouseY);
        drawSliderChrome(context, widthSlider, mouseX, mouseY);
        drawSliderChrome(context, gapSlider, mouseX, mouseY);
        drawColorButtons(context, mouseX, mouseY);
        drawOutlineControls(context, mouseX, mouseY);
        drawDynamicControl(context, mouseX, mouseY);
    }

    private void drawSliderLabels(DrawContext context) {
        drawSliderLabel(context, "SIZE", sizeSlider.getX(), sizeSlider.getY());
        drawSliderLabel(context, "LENGTH", lengthSlider.getX(), lengthSlider.getY());
        drawSliderLabel(context, "WIDTH", widthSlider.getX(), widthSlider.getY());
        drawSliderLabel(context, "GAP", gapSlider.getX(), gapSlider.getY());
    }

    private void drawSliderLabel(DrawContext context, String label, int sliderX, int sliderY) {
        context.drawTextWithShadow(textRenderer, uiText(label), sliderX + 2, sliderY - 12, 0xFFC6D0F3);
    }

    private void drawSliderChrome(DrawContext context, SizeSliderWidget slider, int mouseX, int mouseY) {
        int x = slider.getX();
        int y = slider.getY();
        int width = slider.getWidth();
        int height = slider.getHeight();
        float progress = Math.max(0.0f, Math.min(1.0f, slider.sliderValue()));
        int thumbX = x + Math.round(progress * (width - 8));
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);

        context.fill(x, y, x + width, y + height, 0xF00E1018);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x66FFFFFF);
        context.fill(x + 2, y + height / 2 - 2, x + width - 2, y + height / 2 + 2, 0xCC2A2F3C);
        context.fill(x + 2, y + height / 2 - 2, thumbX + 4, y + height / 2 + 2, 0xFF8AF0C2);
        context.fill(thumbX, y + 2, thumbX + 8, y + height - 2, 0xFFE9D9FF);
        context.drawStrokedRectangle(thumbX, y + 2, 8, height - 4, 0xFF8F5DFF);
        context.drawStrokedRectangle(x, y, width, height, 0xFF8F5DFF);
    }

    private void drawColorButtons(DrawContext context, int mouseX, int mouseY) {
        CrosshairConfig config = CrosshairConfigManager.get();
        drawColorButton(context, mouseX, mouseY, colorButtonX(), colorButtonY(), CONTROL_WIDTH, 20, "CROSSHAIR COLOR", config.color);
        drawColorButton(context, mouseX, mouseY, colorButtonX(), colorButtonY() + 38, CONTROL_WIDTH, 20, "OUTLINE COLOR", config.outlineColor);
    }

    private void drawColorButton(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int height, String label, int color) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        drawButton(context, x, y, width, height, hovered, false);
        context.drawTextWithShadow(textRenderer, uiText(label), x + 8, y + 5, 0xFFC6D0F3);
        context.fill(x + width - 20, y + 4, x + width - 4, y + height - 4, color);
        context.drawStrokedRectangle(x + width - 20, y + 4, 16, height - 8, 0xFFFFFFFF);
    }

    private void drawEnabledControls(DrawContext context, int mouseX, int mouseY) {
        CrosshairConfig config = CrosshairConfigManager.get();
        int x = enabledToggleX();
        int y = enabledToggleY();
        int buttonWidth = enabledToggleWidth();
        int gap = enabledToggleGap();
        boolean onHovered = inside(mouseX, mouseY, x, y, buttonWidth, 20);
        boolean offHovered = inside(mouseX, mouseY, x + buttonWidth + gap, y, buttonWidth, 20);

        context.drawTextWithShadow(textRenderer, uiText("CUSTOM CROSSHAIR"), x + 2, y - 12, 0xFFC6D0F3);
        drawButton(context, x, y, buttonWidth, 20, onHovered, config.enabled);
        drawButton(context, x + buttonWidth + gap, y, buttonWidth, 20, offHovered, !config.enabled);
        context.drawCenteredTextWithShadow(textRenderer, uiText("ON"), x + buttonWidth / 2, y + 5, config.enabled ? 0xFFF7F7FF : 0xFFC6D0F3);
        context.drawCenteredTextWithShadow(textRenderer, uiText("OFF"), x + buttonWidth + gap + buttonWidth / 2, y + 5, !config.enabled ? 0xFFF7F7FF : 0xFFC6D0F3);
    }

    private void drawOutlineControls(DrawContext context, int mouseX, int mouseY) {
        CrosshairConfig config = CrosshairConfigManager.get();
        int x = outlineToggleX();
        int y = outlineToggleY();
        int buttonWidth = outlineToggleWidth();
        int gap = outlineToggleGap();
        boolean onHovered = inside(mouseX, mouseY, x, y, buttonWidth, 20);
        boolean offHovered = inside(mouseX, mouseY, x + buttonWidth + gap, y, buttonWidth, 20);

        context.drawTextWithShadow(textRenderer, uiText("OUTLINE"), x + 2, y - 12, 0xFFC6D0F3);
        drawButton(context, x, y, buttonWidth, 20, onHovered, config.outlineEnabled);
        drawButton(context, x + buttonWidth + gap, y, buttonWidth, 20, offHovered, !config.outlineEnabled);
        context.drawCenteredTextWithShadow(textRenderer, uiText("ON"), x + buttonWidth / 2, y + 5, config.outlineEnabled ? 0xFFF7F7FF : 0xFFC6D0F3);
        context.drawCenteredTextWithShadow(textRenderer, uiText("OFF"), x + buttonWidth + gap + buttonWidth / 2, y + 5, !config.outlineEnabled ? 0xFFF7F7FF : 0xFFC6D0F3);
    }

    private void drawDynamicControl(DrawContext context, int mouseX, int mouseY) {
        CrosshairConfig config = CrosshairConfigManager.get();
        int x = dynamicToggleX();
        int y = dynamicToggleY();
        boolean hovered = inside(mouseX, mouseY, x, y, CONTROL_WIDTH, 20);
        drawButton(context, x, y, CONTROL_WIDTH, 20, hovered, config.dynamicEnabled);
        context.drawTextWithShadow(textRenderer, uiText("DYNAMIC CROSSHAIR"), x + 8, y + 5, 0xFFC6D0F3);
        context.drawCenteredTextWithShadow(textRenderer, uiText(config.dynamicEnabled ? "ON" : "OFF"), x + CONTROL_WIDTH - 28, y + 5, config.dynamicEnabled ? 0xFFF7F7FF : 0xFFC6D0F3);
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
    }

    private void drawButton(DrawContext context, int x, int y, int width, int height, boolean hovered, boolean active) {
        int background = hovered ? 0xBC20283A : 0xA0181D2C;
        int border = active ? 0xFFE7D9FF : 0xD08F5DFF;
        context.fill(x, y, x + width, y + height, background);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, border);
    }

    private int colorButtonX() {
        return panelX + PANEL_PADDING;
    }

    private int colorButtonY() {
        return controlsViewportTop + COLOR_ROW_OFFSET - scrollOffset;
    }

    private int enabledToggleX() {
        return panelX + PANEL_PADDING;
    }

    private int enabledToggleY() {
        return controlsViewportTop + ENABLED_ROW_OFFSET - scrollOffset;
    }

    private int enabledToggleWidth() {
        return (CONTROL_WIDTH - 8) / 2;
    }

    private int enabledToggleGap() {
        return 8;
    }

    private int outlineToggleX() {
        return panelX + PANEL_PADDING;
    }

    private int outlineToggleY() {
        return controlsViewportTop + OUTLINE_ROW_OFFSET - scrollOffset;
    }

    private int outlineToggleWidth() {
        return (CONTROL_WIDTH - 8) / 2;
    }

    private int outlineToggleGap() {
        return 8;
    }

    private int styleButtonX() {
        return panelX + PANEL_PADDING;
    }

    private int styleButtonY() {
        return controlsViewportTop + STYLE_ROW_OFFSET - scrollOffset;
    }

    private int dynamicToggleX() {
        return panelX + PANEL_PADDING;
    }

    private int dynamicToggleY() {
        return controlsViewportTop + DYNAMIC_ROW_OFFSET - scrollOffset;
    }

    private void cycleStyle() {
        CrosshairConfig config = CrosshairConfigManager.get();
        CrosshairConfig.CrosshairStyle next = switch (config.style) {
            case CLASSIC -> CrosshairConfig.CrosshairStyle.LUNAR;
            case LUNAR -> CrosshairConfig.CrosshairStyle.DOT;
            case DOT -> CrosshairConfig.CrosshairStyle.SMALL_DOT;
            case SMALL_DOT -> CrosshairConfig.CrosshairStyle.PLUS;
            case PLUS -> CrosshairConfig.CrosshairStyle.T;
            case T -> CrosshairConfig.CrosshairStyle.X;
            case X -> CrosshairConfig.CrosshairStyle.CIRCLE;
            case CIRCLE -> CrosshairConfig.CrosshairStyle.SMALL_CROSS;
            case SMALL_CROSS -> CrosshairConfig.CrosshairStyle.CLASSIC;
        };

        switch (next) {
            case CLASSIC -> applyPreset(CrosshairConfig.CrosshairStyle.CLASSIC, 4.0f, 2.0f, 0.0f, true);
            case LUNAR -> applyPreset(CrosshairConfig.CrosshairStyle.LUNAR, 4.0f, 2.0f, 2.0f, true);
            case DOT -> applyPreset(CrosshairConfig.CrosshairStyle.DOT, 0.0f, 2.0f, 0.0f, false);
            case SMALL_DOT -> applyPreset(CrosshairConfig.CrosshairStyle.SMALL_DOT, 0.0f, 1.0f, 0.0f, false);
            case PLUS -> applyPreset(CrosshairConfig.CrosshairStyle.PLUS, 4.0f, 2.0f, 0.0f, true);
            case T -> applyPreset(CrosshairConfig.CrosshairStyle.T, 4.0f, 2.0f, 2.0f, true);
            case X -> applyPreset(CrosshairConfig.CrosshairStyle.X, 4.0f, 2.0f, 2.0f, true);
            case CIRCLE -> applyPreset(CrosshairConfig.CrosshairStyle.CIRCLE, 3.0f, 4.0f, 0.0f, true);
            case SMALL_CROSS -> applyPreset(CrosshairConfig.CrosshairStyle.SMALL_CROSS, 2.0f, 2.0f, 2.0f, true);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        rebuildLayout();
        if (inside(mouseX, mouseY, panelX, controlsViewportTop, PANEL_WIDTH, controlsViewportBottom - controlsViewportTop) && maxScroll() > 0) {
            int step = 18;
            int next = scrollOffset + (verticalAmount < 0.0 ? step : -step);
            scrollOffset = clamp(next, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawScrollBar(DrawContext context) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }

        int trackX = panelX + PANEL_WIDTH - 12;
        int trackY = controlsViewportTop;
        int trackHeight = controlsViewportBottom - controlsViewportTop;
        int trackWidth = 8;
        int thumbWidth = 8;
        int thumbHeight = Math.max(20, Math.round((trackHeight * (float) trackHeight) / contentHeight));
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + Math.round((scrollOffset / (float) maxScroll) * thumbTravel);

        context.fill(trackX - 2, trackY, trackX + trackWidth + 2, trackY + trackHeight, 0xB3161720);
        context.fill(trackX - 1, trackY + 1, trackX + trackWidth + 1, trackY + trackHeight - 1, 0x4B2A2F3C);
        context.fill(trackX, thumbY, trackX + thumbWidth, thumbY + thumbHeight, 0xFF9D5AF6);
        context.drawStrokedRectangle(trackX - 1, thumbY - 1, thumbWidth + 2, thumbHeight + 2, 0xFFDCC7FF);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (controlsViewportBottom - controlsViewportTop));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private enum CrosshairField {
        SIZE,
        LENGTH,
        WIDTH,
        GAP
    }

    private final class SizeSliderWidget extends SliderWidget {
        private final CrosshairField field;

        private SizeSliderWidget(int x, int y, int width, int height, CrosshairField field) {
            super(x, y, width, height, Text.empty(), 0.0);
            this.field = field;
            updateMessage();
        }

        private void updateFromConfig(float value) {
            setValue(toSliderValue(value));
            updateMessage();
        }

        private float sliderValue() {
            return (float) value;
        }

        @Override
        protected void updateMessage() {
            setMessage(uiText(field.name() + ": " + formatValue(fromSliderValue((float) value))));
        }

        @Override
        protected void applyValue() {
            float configured = fromSliderValue((float) value);
            switch (field) {
                case SIZE -> CrosshairConfigManager.get().size = configured;
                case LENGTH -> CrosshairConfigManager.get().length = configured;
                case WIDTH -> CrosshairConfigManager.get().width = configured;
                case GAP -> CrosshairConfigManager.get().gap = configured;
            }
            CrosshairConfigManager.save();
            updateMessage();
        }

        private float toSliderValue(float configured) {
            return switch (field) {
                case SIZE -> (Math.max(SIZE_MIN, Math.min(SIZE_MAX, configured)) - SIZE_MIN) / (SIZE_MAX - SIZE_MIN);
                case LENGTH -> snapEven(configured, LENGTH_MIN, LENGTH_MAX) / LENGTH_MAX;
                case WIDTH -> (snapEven(configured, WIDTH_MIN, WIDTH_MAX) - WIDTH_MIN) / (WIDTH_MAX - WIDTH_MIN);
                case GAP -> snapEven(configured, GAP_MIN, GAP_MAX) / GAP_MAX;
            };
        }

        private float fromSliderValue(float sliderValue) {
            float clamped = Math.max(0.0f, Math.min(1.0f, sliderValue));
            return switch (field) {
                case SIZE -> SIZE_MIN + clamped * (SIZE_MAX - SIZE_MIN);
                case LENGTH -> snapEven(clamped * LENGTH_MAX, LENGTH_MIN, LENGTH_MAX);
                case WIDTH -> snapEven(WIDTH_MIN + clamped * (WIDTH_MAX - WIDTH_MIN), WIDTH_MIN, WIDTH_MAX);
                case GAP -> snapEven(clamped * GAP_MAX, GAP_MIN, GAP_MAX);
            };
        }

        private String formatValue(float value) {
            if (field == CrosshairField.SIZE) {
                return String.format(Locale.ROOT, "%.2fx", value);
            }
            return value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.1f", value);
        }

        private float snapEven(float value, float min, float max) {
            float clamped = Math.max(min, Math.min(max, value));
            return Math.max(min, Math.min(max, Math.round(clamped / 2.0f) * 2.0f));
        }
    }
}
