package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;
import java.util.function.IntConsumer;

public final class CrosshairColorPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 286;
    private static final int PANEL_PADDING = 18;
    private static final int COLOR_SQUARE_SIZE = 144;
    private static final int HUE_STRIP_WIDTH = 18;
    private static final Identifier BACK_ARROW_ICON = AxialCosmetics.id("textures/gui/back-arrow.png");
    private static final Identifier COLOR_SQUARE_TEXTURE = AxialCosmetics.id("textures/gui/color_square");
    private static final Identifier HUE_STRIP_TEXTURE = AxialCosmetics.id("textures/gui/hue_strip");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private final String label;
    private final IntConsumer onChange;
    private final ColorPickerSaveAction saveAction;
    private final int initialColor;
    private NativeImage colorSquareImage;
    private NativeImageBackedTexture colorSquareTexture;
    private NativeImage hueStripImage;
    private NativeImageBackedTexture hueStripTexture;
    private int panelX;
    private int panelY;
    private int squareX;
    private int squareY;
    private int hueX;
    private int hueY;
    private float hue;
    private float saturation;
    private float value;
    private boolean draggingSquare;
    private boolean draggingHue;

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
        float[] hsv = rgbToHsv(initialColor);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    @Override
    protected void init() {
        rebuildLayout();
        buildHueStripTexture();
        buildColorSquareTexture();
    }

    @Override
    public void close() {
        destroyTextures();
        saveAction.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        destroyTextures();
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        rebuildLayout();
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + panelWidth() / 2, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("DRAG THE SQUARE OR HUE STRIP."), panelX + panelWidth() / 2, panelY + 22, 0xFFC6D0F3);
        drawColorSquare(context, squareX, squareY);
        drawHueStrip(context, hueX, hueY);
        drawSelectionMarkers(context, squareX, squareY, hueX, hueY);
        drawPreview(context, squareX, squareY);
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

        if (inside(click.x(), click.y(), squareX, squareY, 132, 132)) {
            draggingSquare = true;
            updateSquare((int) click.x(), (int) click.y());
            return true;
        }

        if (inside(click.x(), click.y(), hueX, hueY, 14, 132)) {
            draggingHue = true;
            updateHue((int) click.y());
            return true;
        }

        int backButtonX = panelX + PANEL_PADDING;
        int backButtonY = panelY + 6;
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

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingSquare) {
            updateSquare((int) click.x(), (int) click.y());
            return true;
        }
        if (draggingHue) {
            updateHue((int) click.y());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            draggingSquare = false;
            draggingHue = false;
        }
        return super.mouseReleased(click);
    }

    private void rebuildLayout() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = Math.max(16, (height - PANEL_HEIGHT) / 2);
        squareX = panelX + PANEL_PADDING;
        squareY = panelY + 42;
        hueX = squareX + COLOR_SQUARE_SIZE + 14;
        hueY = squareY;
    }

    private int panelWidth() {
        return PANEL_WIDTH;
    }

    private int panelHeight() {
        return PANEL_HEIGHT;
    }

    private void drawBackButton(DrawContext context, int mouseX, int mouseY) {
        int buttonX = panelX + PANEL_PADDING;
        int buttonY = panelY + 6;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, 24, 18);
        drawButton(context, buttonX, buttonY, 24, 18, hovered, true);
        int iconWidth = 16;
        int iconHeight = 13;
        int iconX = buttonX + (24 - iconWidth) / 2 - 4;
        int iconY = buttonY + (18 - iconHeight) / 2 - 1;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACK_ARROW_ICON, iconX, iconY, 0.0f, 0.0f, iconWidth, iconHeight, 64, 64, 64, 64);
    }

    private void drawResetButton(DrawContext context, int mouseX, int mouseY) {
        int resetX = panelX + PANEL_WIDTH - PANEL_PADDING - 80;
        int resetY = panelY + PANEL_HEIGHT - PANEL_PADDING - 18;
        boolean hovered = inside(mouseX, mouseY, resetX, resetY, 80, 18);
        drawButton(context, resetX, resetY, 80, 18, hovered, false);
        context.drawCenteredTextWithShadow(textRenderer, uiText("RESET"), resetX + 40, resetY + 5, hovered ? 0xFFF7F7FF : 0xFFC6D0F3);
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
    }

    private void drawButton(DrawContext context, int x, int y, int width, int height, boolean hovered, boolean back) {
        int background = hovered ? (back ? 0xBC20283A : 0xBC20283A) : 0xA0181D2C;
        int border = hovered ? 0xFFE7D9FF : 0xD08F5DFF;
        context.fill(x, y, x + width, y + height, background);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, border);
    }

    private void drawPreview(DrawContext context, int x, int y) {
        int previewY = y + COLOR_SQUARE_SIZE + 42;
        int previewWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        int previewColor = currentColor();
        context.fill(panelX + PANEL_PADDING, previewY, panelX + PANEL_PADDING + previewWidth, previewY + 22, 0xAA141822);
        context.drawStrokedRectangle(panelX + PANEL_PADDING, previewY, previewWidth, 22, 0xD08F5DFF);
        context.fill(panelX + PANEL_PADDING + 5, previewY + 5, panelX + PANEL_PADDING + 21, previewY + 17, previewColor);
        context.drawStrokedRectangle(panelX + PANEL_PADDING + 5, previewY + 5, 16, 12, 0xCCFFFFFF);
        context.drawTextWithShadow(textRenderer, uiText(label.toUpperCase(Locale.ROOT)), panelX + PANEL_PADDING + 28, previewY + 6, previewColor);
    }

    private void drawColorSquare(DrawContext context, int x, int y) {
        context.fill(x - 1, y - 1, x + COLOR_SQUARE_SIZE + 1, y + COLOR_SQUARE_SIZE + 1, 0xFF10131A);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, COLOR_SQUARE_TEXTURE, x, y, 0.0f, 0.0f, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE);
        context.drawStrokedRectangle(x - 1, y - 1, COLOR_SQUARE_SIZE + 2, COLOR_SQUARE_SIZE + 2, 0xCCFFFFFF);
    }

    private void drawHueStrip(DrawContext context, int x, int y) {
        context.fill(x - 1, y - 1, x + HUE_STRIP_WIDTH + 1, y + COLOR_SQUARE_SIZE + 1, 0xFF10131A);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, HUE_STRIP_TEXTURE, x, y, 0.0f, 0.0f, HUE_STRIP_WIDTH, COLOR_SQUARE_SIZE, HUE_STRIP_WIDTH, COLOR_SQUARE_SIZE, HUE_STRIP_WIDTH, COLOR_SQUARE_SIZE);
        context.drawStrokedRectangle(x - 1, y - 1, HUE_STRIP_WIDTH + 2, COLOR_SQUARE_SIZE + 2, 0xE6FFFFFF);
    }

    private void drawSelectionMarkers(DrawContext context, int squareX, int squareY, int hueX, int hueY) {
        int markerX = squareX + Math.round(saturation * (COLOR_SQUARE_SIZE - 1));
        int markerY = squareY + Math.round((1.0f - value) * (COLOR_SQUARE_SIZE - 1));
        context.drawHorizontalLine(markerX - 4, markerX + 4, markerY, 0xFFFFFFFF);
        context.drawVerticalLine(markerX, markerY - 4, markerY + 4, 0xFFFFFFFF);
        int hueMarkerY = hueY + Math.round(hue * (COLOR_SQUARE_SIZE - 1));
        context.drawHorizontalLine(hueX - 2, hueX + HUE_STRIP_WIDTH + 1, hueMarkerY, 0xFFFFFFFF);
    }

    private void updateSquare(int mouseX, int mouseY) {
        float newSaturation = (mouseX - squareX) / (float) (COLOR_SQUARE_SIZE - 1);
        float newValue = 1.0f - (mouseY - squareY) / (float) (COLOR_SQUARE_SIZE - 1);
        saturation = MathHelper.clamp(newSaturation, 0.0f, 1.0f);
        value = MathHelper.clamp(newValue, 0.0f, 1.0f);
        pushColor();
    }

    private void updateHue(int mouseY) {
        float newHue = (mouseY - hueY) / (float) (COLOR_SQUARE_SIZE - 1);
        hue = MathHelper.clamp(newHue, 0.0f, 1.0f);
        buildColorSquareTexture();
        pushColor();
    }

    private void setColor(int argb) {
        float[] hsv = rgbToHsv(argb);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        buildColorSquareTexture();
    }

    private void pushColor() {
        onChange.accept(currentColor());
        saveAction.save();
    }

    private int currentColor() {
        return 0xFF000000 | MathHelper.hsvToRgb(hue, saturation, value);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private static float[] rgbToHsv(int argb) {
        float r = ((argb >> 16) & 255) / 255.0f;
        float g = ((argb >> 8) & 255) / 255.0f;
        float b = (argb & 255) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0.0f) {
            hue = 0.0f;
        } else if (max == r) {
            hue = ((g - b) / delta) % 6.0f;
        } else if (max == g) {
            hue = (b - r) / delta + 2.0f;
        } else {
            hue = (r - g) / delta + 4.0f;
        }
        hue /= 6.0f;
        if (hue < 0.0f) {
            hue += 1.0f;
        }
        float saturation = max == 0.0f ? 0.0f : delta / max;
        return new float[]{hue, saturation, max};
    }

    private void buildHueStripTexture() {
        if (hueStripTexture == null) {
            hueStripImage = new NativeImage(NativeImage.Format.RGBA, HUE_STRIP_WIDTH, COLOR_SQUARE_SIZE, false);
            hueStripTexture = new NativeImageBackedTexture(() -> "axial_cosmetics/hue_strip", hueStripImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(HUE_STRIP_TEXTURE, hueStripTexture);
        }
        for (int row = 0; row < COLOR_SQUARE_SIZE; row++) {
            float rowHue = row / (float) (COLOR_SQUARE_SIZE - 1);
            int color = 0xFF000000 | MathHelper.hsvToRgb(rowHue, 1.0f, 1.0f);
            for (int col = 0; col < HUE_STRIP_WIDTH; col++) {
                hueStripImage.setColorArgb(col, row, color);
            }
        }
        hueStripTexture.upload();
    }

    private void buildColorSquareTexture() {
        if (colorSquareTexture == null) {
            colorSquareImage = new NativeImage(NativeImage.Format.RGBA, COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE, false);
            colorSquareTexture = new NativeImageBackedTexture(() -> "axial_cosmetics/color_square", colorSquareImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(COLOR_SQUARE_TEXTURE, colorSquareTexture);
        }
        for (int row = 0; row < COLOR_SQUARE_SIZE; row++) {
            float rowValue = 1.0f - row / (float) (COLOR_SQUARE_SIZE - 1);
            for (int col = 0; col < COLOR_SQUARE_SIZE; col++) {
                float colSaturation = col / (float) (COLOR_SQUARE_SIZE - 1);
                int color = 0xFF000000 | MathHelper.hsvToRgb(hue, colSaturation, rowValue);
                colorSquareImage.setColorArgb(col, row, color);
            }
        }
        colorSquareTexture.upload();
    }

    private void destroyTextures() {
        destroyColorSquareTexture();
        destroyHueTexture();
    }

    private void destroyColorSquareTexture() {
        if (colorSquareTexture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(COLOR_SQUARE_TEXTURE);
            colorSquareTexture.close();
            colorSquareTexture = null;
            colorSquareImage = null;
        }
    }

    private void destroyHueTexture() {
        if (hueStripTexture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(HUE_STRIP_TEXTURE);
            hueStripTexture.close();
            hueStripTexture = null;
            hueStripImage = null;
        }
    }
}
