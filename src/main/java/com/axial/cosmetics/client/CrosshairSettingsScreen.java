package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;

import java.util.Locale;

/** Compact, scrollable settings screen for the custom crosshair. */
public final class CrosshairSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 452, PANEL_HEIGHT = 168, PANEL_PADDING = 28, CONTROL_WIDTH = 370;
    private static final int VIEWPORT_TOP = 26, VIEWPORT_BOTTOM = 14, CONTENT_HEIGHT = 506, ROW_HEIGHT = 20;
    private static final int STYLE_DIVIDER_Y = 58, STYLE_Y = 84, SIZE_Y = 114, LENGTH_Y = 152, WIDTH_Y = 190, GAP_Y = 228;
    private static final int COLOR_DIVIDER_Y = 258, COLOR_Y = 284, OUTLINE_COLOR_Y = 314;
    private static final int ADDITIONAL_DIVIDER_Y = 344, ADDITIONAL_Y = 370, PREVIEW_DIVIDER_Y = 400, PREVIEW_Y = 426, PREVIEW_HEIGHT = 64;
    private static final int COMPACT_WIDTH = 170, COMPACT_GAP = 42;
    private static final float SIZE_MIN = 0.1f, SIZE_MAX = 4.0f, LENGTH_MIN = 0.0f, LENGTH_MAX = 32.0f, WIDTH_MIN = 2.0f, WIDTH_MAX = 12.0f, GAP_MIN = 0.0f, GAP_MAX = 20.0f;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private final SizeSliderWidget sizeSlider = new SizeSliderWidget(0, 0, 0, ROW_HEIGHT, CrosshairField.SIZE);
    private final SizeSliderWidget lengthSlider = new SizeSliderWidget(0, 0, 0, ROW_HEIGHT, CrosshairField.LENGTH);
    private final SizeSliderWidget widthSlider = new SizeSliderWidget(0, 0, 0, ROW_HEIGHT, CrosshairField.WIDTH);
    private final SizeSliderWidget gapSlider = new SizeSliderWidget(0, 0, 0, ROW_HEIGHT, CrosshairField.GAP);
    private int panelX, panelY, scrollOffset;

    public CrosshairSettingsScreen(Screen parent) { super(uiText("CROSSHAIR")); this.parent = parent; }

    @Override protected void init() { addDrawableChild(sizeSlider); addDrawableChild(lengthSlider); addDrawableChild(widthSlider); addDrawableChild(gapSlider); syncSliders(); rebuildLayout(); }
    @Override public void close() { CrosshairConfigManager.save(); MinecraftClient.getInstance().setScreen(parent); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        rebuildLayout(); drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 8, 0xFFF7F7FF);
        drawBackButton(context, mouseX, mouseY);
        context.enableScissor(panelX, viewportTop(), panelX + PANEL_WIDTH, viewportBottom());
        super.render(context, mouseX, mouseY, deltaTicks);
        drawControls(context, mouseX, mouseY);
        drawScrollBar(context); context.disableScissor();
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        rebuildLayout(); if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        if (inside(click.x(), click.y(), panelX + 18, panelY + 6, 24, 18)) { close(); return true; }
        CrosshairConfig config = CrosshairConfigManager.get();
        if (inside(click.x(), click.y(), enabledX(), contentY(30), 416, ROW_HEIGHT)) { config.enabled = !config.enabled; CrosshairConfigManager.save(); return true; }
        if (inside(click.x(), click.y(), controlX(), contentY(STYLE_Y), COMPACT_WIDTH, ROW_HEIGHT)) { cycleStyle(); return true; }
        if (inside(click.x(), click.y(), colorSwatchX(), contentY(COLOR_Y), 20, ROW_HEIGHT)) { openColorPicker("CROSSHAIR", config.color, value -> CrosshairConfigManager.get().color = value); return true; }
        if (inside(click.x(), click.y(), colorSwatchX(), contentY(OUTLINE_COLOR_Y), 20, ROW_HEIGHT)) { openColorPicker("OUTLINE", config.outlineColor, value -> CrosshairConfigManager.get().outlineColor = value); return true; }
        if (inside(click.x(), click.y(), controlX(), contentY(ADDITIONAL_Y), COMPACT_WIDTH, ROW_HEIGHT)) { config.outlineEnabled = !config.outlineEnabled; CrosshairConfigManager.save(); return true; }
        if (inside(click.x(), click.y(), controlX() + COMPACT_WIDTH + COMPACT_GAP, contentY(ADDITIONAL_Y), COMPACT_WIDTH, ROW_HEIGHT)) { config.dynamicEnabled = !config.dynamicEnabled; CrosshairConfigManager.save(); return true; }
        return false;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        rebuildLayout();
        if (!inside(mouseX, mouseY, panelX, viewportTop(), PANEL_WIDTH, viewportBottom() - viewportTop()) || maxScroll() == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        scrollOffset = clamp(scrollOffset + (verticalAmount < 0 ? 18 : -18), 0, maxScroll()); return true;
    }

    private void rebuildLayout() {
        panelX = (width - PANEL_WIDTH) / 2; panelY = (height - PANEL_HEIGHT) / 2; scrollOffset = clamp(scrollOffset, 0, maxScroll());
        positionSlider(sizeSlider, SIZE_Y); positionSlider(lengthSlider, LENGTH_Y); positionSlider(widthSlider, WIDTH_Y); positionSlider(gapSlider, GAP_Y);
    }
    private void positionSlider(SizeSliderWidget slider, int y) { int positionedY = contentY(y); slider.setPosition(controlX(), positionedY); slider.setWidth(CONTROL_WIDTH); slider.setY(positionedY); slider.visible = positionedY >= viewportTop() && positionedY + ROW_HEIGHT <= viewportBottom(); }

    private void drawControls(DrawContext context, int mouseX, int mouseY) {
        CrosshairConfig config = CrosshairConfigManager.get();
        drawFullEnabled(context, mouseX, mouseY, config.enabled);
        divider(context, STYLE_DIVIDER_Y, "STYLE"); drawStyleButton(context, mouseX, mouseY, config);
        drawSlider(context, sizeSlider, "SIZE SCALE"); drawSlider(context, lengthSlider, "LENGTH SCALE"); drawSlider(context, widthSlider, "WIDTH SCALE"); drawSlider(context, gapSlider, "GAP SCALE");
        divider(context, COLOR_DIVIDER_Y, "COLOR"); drawColorRow(context, mouseX, mouseY, COLOR_Y, "CROSSHAIR COLOR", config.color); drawColorRow(context, mouseX, mouseY, OUTLINE_COLOR_Y, "OUTLINE COLOR", config.outlineColor);
        divider(context, ADDITIONAL_DIVIDER_Y, "ADDITIONAL OPTIONS");
        drawBooleanButton(context, mouseX, mouseY, controlX(), contentY(ADDITIONAL_Y), "OUTLINE", config.outlineEnabled);
        drawBooleanButton(context, mouseX, mouseY, controlX() + COMPACT_WIDTH + COMPACT_GAP, contentY(ADDITIONAL_Y), "DYNAMIC", config.dynamicEnabled);
        divider(context, PREVIEW_DIVIDER_Y, "PREVIEW"); drawPreview(context, contentY(PREVIEW_Y));
    }

    private void drawFullEnabled(DrawContext context, int mouseX, int mouseY, boolean enabled) {
        int x = enabledX(), y = contentY(30);
        AxialUiTheme.drawButton(context, textRenderer, x, y, 416, ROW_HEIGHT, "", "",
                inside(mouseX, mouseY, x, y, 416, ROW_HEIGHT), false,
                enabled ? AxialUiTheme.TOGGLE_ON : AxialUiTheme.TOGGLE_OFF);
        context.drawCenteredTextWithShadow(textRenderer, uiText(enabled ? "ENABLED" : "DISABLED"), x + 208, y + 5, 0xFFF7F7FF);
    }
    private void drawStyleButton(DrawContext context, int mouseX, int mouseY, CrosshairConfig config) {
        int x = controlX(), y = contentY(STYLE_Y); drawPurpleButton(context, x, y, COMPACT_WIDTH, inside(mouseX, mouseY, x, y, COMPACT_WIDTH, ROW_HEIGHT));
        context.drawTextWithShadow(textRenderer, uiText("STYLE"), x + 8, y + 5, 0xFFC6D0F3); context.drawTextWithShadow(textRenderer, uiText(config.style.name()), x + 58, y + 5, 0xFFF7F7FF);
    }
    private void drawSlider(DrawContext context, SizeSliderWidget slider, String label) {
        if (!slider.visible) return; int x = slider.getX(), y = slider.getY(), width = slider.getWidth();
        context.drawTextWithShadow(textRenderer, uiText(label), x + 2, y - 12, 0xFFC6D0F3);
        float progress = Math.max(0, Math.min(1, slider.sliderValue())); int thumbX = x + Math.round(progress * (width - 8));
        context.fill(x, y, x + width, y + ROW_HEIGHT, 0xF00E1018); context.fill(x + 2, y + 8, x + width - 2, y + 12, 0xCC2A2F3C); context.fill(x + 2, y + 8, thumbX + 4, y + 12, 0xFF8AF0C2); context.fill(thumbX, y + 2, thumbX + 8, y + 18, 0xFFE9D9FF);
        context.drawStrokedRectangle(thumbX, y + 2, 8, 16, 0xFF8F5DFF); context.drawStrokedRectangle(x, y, width, ROW_HEIGHT, 0xFF8F5DFF);
        String value = slider.displayValue(); Text text = uiText(value); context.drawTextWithShadow(textRenderer, text, x + width - textRenderer.getWidth(text) - 8, y - 12, 0xFFF7F7FF);
    }
    private void drawColorRow(DrawContext context, int mouseX, int mouseY, int baseY, String label, int color) {
        int x = controlX(), y = contentY(baseY), swatchX = colorSwatchX(); context.drawTextWithShadow(textRenderer, uiText(label), x + 2, y + 6, 0xFFC6D0F3);
        Text hex = uiText(String.format("#%06X", color & 0xFFFFFF)); context.drawTextWithShadow(textRenderer, hex, swatchX - textRenderer.getWidth(hex) - 10, y + 6, 0xFFC6D0F3);
        boolean hovered = inside(mouseX, mouseY, swatchX, y, 20, ROW_HEIGHT); context.fill(swatchX, y + 2, swatchX + 16, y + 18, color); context.drawStrokedRectangle(swatchX, y + 2, 16, 16, hovered ? 0xFFFFFFFF : 0xFFB9C5E8);
    }
    private void drawBooleanButton(DrawContext context, int mouseX, int mouseY, int x, int y, String label, boolean enabled) {
        boolean hovered = inside(mouseX, mouseY, x, y, COMPACT_WIDTH, ROW_HEIGHT); int fill = enabled ? (hovered ? 0xE42D7450 : 0xCC205239) : (hovered ? 0xE46D2D43 : 0xCC4E2432); int border = enabled ? 0xFF8AF0C2 : 0xFFFF7B97;
        context.fill(x, y, x + COMPACT_WIDTH, y + ROW_HEIGHT, fill); context.drawStrokedRectangle(x, y, COMPACT_WIDTH, ROW_HEIGHT, border); context.drawTextWithShadow(textRenderer, uiText(label), x + 8, y + 5, 0xFFF7F7FF);
        String state = enabled ? "ENABLED" : "DISABLED"; context.drawTextWithShadow(textRenderer, uiText(state), x + COMPACT_WIDTH - (enabled ? 58 : 66), y + 5, 0xFFF7F7FF);
    }
    private void drawPreview(DrawContext context, int y) { int x = controlX(); context.fill(x, y, x + CONTROL_WIDTH, y + PREVIEW_HEIGHT, 0xAA141822); context.drawStrokedRectangle(x, y, CONTROL_WIDTH, PREVIEW_HEIGHT, 0xD08F5DFF); context.fill(x + 1, y + PREVIEW_HEIGHT / 2, x + CONTROL_WIDTH - 1, y + PREVIEW_HEIGHT / 2 + 1, 0x22FFFFFF); context.fill(x + CONTROL_WIDTH / 2, y + 1, x + CONTROL_WIDTH / 2 + 1, y + PREVIEW_HEIGHT - 1, 0x22FFFFFF); CrosshairRenderer.render(context, x + CONTROL_WIDTH / 2, y + PREVIEW_HEIGHT / 2, CrosshairConfigManager.get(), 1.0f); }
    private void divider(DrawContext context, int baseY, String label) { int x = panelX + 18, y = contentY(baseY); Text text = uiText(label); context.drawTextWithShadow(textRenderer, text, x + 2, y, 0xFFC6D0F3); context.fill(x + textRenderer.getWidth(text) + 12, y + 5, panelX + PANEL_WIDTH - 28, y + 6, 0x998F5DFF); }
    private void drawBackButton(DrawContext context, int mouseX, int mouseY) { ModMenuBackButton.draw(context, panelX + 18, panelY + 6, mouseX, mouseY); }
    private void drawPanel(DrawContext context) { context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018); context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF); context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF); }
    private void drawPurpleButton(DrawContext context, int x, int y, int width, boolean hovered) { context.fill(x, y, x + width, y + ROW_HEIGHT, hovered ? 0xBC20283A : 0xA0181D2C); context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF); context.drawStrokedRectangle(x, y, width, ROW_HEIGHT, 0xFFE7D9FF); }
    private void drawScrollBar(DrawContext context) { int max = maxScroll(); if (max == 0) return; int top = viewportTop(), height = viewportBottom() - top, content = Math.max(height + 1, CONTENT_HEIGHT - 48), thumb = Math.max(18, Math.round(height * (height / (float) content))), travel = Math.max(1, height - thumb), y = top + Math.round(scrollOffset / (float) max * travel), x = panelX + PANEL_WIDTH - 12; context.fill(x, top, x + 4, top + height, 0x2AFFFFFF); context.fill(x, y, x + 4, y + thumb, 0xFFB06AF3); }
    private void openColorPicker(String label, int color, java.util.function.IntConsumer setter) { MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, label, color, value -> { setter.accept(value); CrosshairConfigManager.save(); })); }
    private void syncSliders() { CrosshairConfig c = CrosshairConfigManager.get(); sizeSlider.updateFromConfig(c.size); lengthSlider.updateFromConfig(c.length); widthSlider.updateFromConfig(c.width); gapSlider.updateFromConfig(c.gap); }
    private int controlX() { return panelX + PANEL_PADDING; } private int enabledX() { return panelX + 18; } private int colorSwatchX() { return controlX() + CONTROL_WIDTH - 20; } private int contentY(int y) { return panelY + y - scrollOffset; } private int viewportTop() { return panelY + VIEWPORT_TOP; } private int viewportBottom() { return panelY + PANEL_HEIGHT - VIEWPORT_BOTTOM; } private int maxScroll() { return Math.max(0, CONTENT_HEIGHT - PANEL_HEIGHT); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); } private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) { return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height; } private static Text uiText(String value) { return Text.literal(value).styled(style -> style.withFont(UI_FONT)); }

    private void cycleStyle() {
        CrosshairConfig.CrosshairStyle next = switch (CrosshairConfigManager.get().style) { case CLASSIC -> CrosshairConfig.CrosshairStyle.LUNAR; case LUNAR -> CrosshairConfig.CrosshairStyle.DOT; case DOT -> CrosshairConfig.CrosshairStyle.SMALL_DOT; case SMALL_DOT -> CrosshairConfig.CrosshairStyle.PLUS; case PLUS -> CrosshairConfig.CrosshairStyle.T; case T -> CrosshairConfig.CrosshairStyle.X; case X -> CrosshairConfig.CrosshairStyle.CIRCLE; case CIRCLE -> CrosshairConfig.CrosshairStyle.SMALL_CROSS; case SMALL_CROSS -> CrosshairConfig.CrosshairStyle.CLASSIC; };
        switch (next) { case CLASSIC -> applyPreset(next, 4, 2, 0, true); case LUNAR -> applyPreset(next, 4, 2, 2, true); case DOT -> applyPreset(next, 0, 2, 0, false); case SMALL_DOT -> applyPreset(next, 0, 1, 0, false); case PLUS -> applyPreset(next, 4, 2, 0, true); case T, X -> applyPreset(next, 4, 2, 2, true); case CIRCLE -> applyPreset(next, 3, 4, 0, true); case SMALL_CROSS -> applyPreset(next, 2, 2, 2, true); }
    }
    private void applyPreset(CrosshairConfig.CrosshairStyle style, float length, float width, float gap, boolean outline) { CrosshairConfig c = CrosshairConfigManager.get(); c.style = style; c.length = length; c.width = width; c.gap = gap; c.outlineEnabled = outline; CrosshairConfigManager.save(); syncSliders(); }
    private enum CrosshairField { SIZE, LENGTH, WIDTH, GAP }
    private final class SizeSliderWidget extends SliderWidget {
        private final CrosshairField field;
        private SizeSliderWidget(int x, int y, int width, int height, CrosshairField field) { super(x, y, width, height, Text.empty(), 0); this.field = field; }
        private void updateFromConfig(float configured) { setValue(toSliderValue(configured)); updateMessage(); } private float sliderValue() { return (float) value; } private String displayValue() { return formatValue(fromSliderValue((float) value)); }
        @Override protected void updateMessage() { setMessage(Text.empty()); }
        @Override protected void applyValue() { float configured = fromSliderValue((float) value); switch (field) { case SIZE -> CrosshairConfigManager.get().size = configured; case LENGTH -> CrosshairConfigManager.get().length = configured; case WIDTH -> CrosshairConfigManager.get().width = configured; case GAP -> CrosshairConfigManager.get().gap = configured; } CrosshairConfigManager.save(); }
        private float toSliderValue(float configured) { return switch (field) { case SIZE -> (Math.max(SIZE_MIN, Math.min(SIZE_MAX, configured)) - SIZE_MIN) / (SIZE_MAX - SIZE_MIN); case LENGTH -> snapEven(configured, LENGTH_MIN, LENGTH_MAX) / LENGTH_MAX; case WIDTH -> (snapEven(configured, WIDTH_MIN, WIDTH_MAX) - WIDTH_MIN) / (WIDTH_MAX - WIDTH_MIN); case GAP -> snapEven(configured, GAP_MIN, GAP_MAX) / GAP_MAX; }; }
        private float fromSliderValue(float value) { float clamped = Math.max(0, Math.min(1, value)); return switch (field) { case SIZE -> SIZE_MIN + clamped * (SIZE_MAX - SIZE_MIN); case LENGTH -> snapEven(clamped * LENGTH_MAX, LENGTH_MIN, LENGTH_MAX); case WIDTH -> snapEven(WIDTH_MIN + clamped * (WIDTH_MAX - WIDTH_MIN), WIDTH_MIN, WIDTH_MAX); case GAP -> snapEven(clamped * GAP_MAX, GAP_MIN, GAP_MAX); }; }
        private String formatValue(float value) { return field == CrosshairField.SIZE ? String.format(Locale.ROOT, "%.2fx", value) : value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.1f", value); } private float snapEven(float value, float min, float max) { float clamped = Math.max(min, Math.min(max, value)); return Math.max(min, Math.min(max, Math.round(clamped / 2f) * 2f)); }
    }
}
