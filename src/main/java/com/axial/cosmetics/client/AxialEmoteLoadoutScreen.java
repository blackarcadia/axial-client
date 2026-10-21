package com.axial.cosmetics.client;

import io.github.kosmx.emotes.arch.screen.EmoteMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** A bounded, three-column Emotecraft screen that scales every region together. */
public final class AxialEmoteLoadoutScreen extends EmoteMenu {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    public AxialEmoteLoadoutScreen(Screen parent) {
        super(parent);
    }

    @Override
    protected void refreshWidgetPositions() {
        super.refreshWidgetPositions();
        if (list == null || preview == null || searchBox == null || fastChoose == null) return;

        Layout layout = new Layout(width, height);
        list.setCompactMode(false);
        list.position(layout.listWidth, layout.contentHeight, layout.listX, layout.contentY);
        preview.setDimensions(layout.previewWidth, layout.contentHeight);
        preview.setPosition(layout.previewX, layout.contentY);
        // Emotecraft's default side-by-side overlap guard runs before this
        // responsive arrangement and hides the preview. These panes are now
        // distinct, so keep the native preview renderer enabled.
        preview.visible = true;
        searchBox.setDimensions(layout.searchWidth, 20);
        searchBox.setPosition(layout.searchX, layout.searchY);
        fastChoose.setSize(layout.wheelSize);
        fastChoose.setPosition(layout.wheelX, layout.wheelY);
        arrangeNativeControls(layout);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        Layout layout = new Layout(width, height);
        drawPanel(context, layout.listX, layout.contentY, layout.listWidth, layout.contentHeight, 0xE8141622, 0xFFB74DFF);
        drawPanel(context, layout.previewX, layout.contentY, layout.previewWidth, layout.contentHeight, 0xE8101422, 0xFF46DDF4);
        drawPanel(context, layout.wheelPanelX, layout.contentY, layout.wheelPanelWidth, layout.contentHeight, 0xE8141622, 0xFFB74DFF);
        context.fill(layout.listX, layout.contentY - 14, layout.listX + Math.min(172, layout.listWidth), layout.contentY - 12, 0xFFCF4BFF);
        context.fill(layout.wheelPanelX + 1, layout.contentY + 1, layout.wheelPanelX + layout.wheelPanelWidth - 1, layout.contentY + 44, 0x5A161427);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("EMOTE LOADOUT"), layout.listX, layout.contentY - 32, 0xFFF4F0FF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("PLAYER PREVIEW"), layout.previewX + 10, layout.contentY + 10, 0xFFF4F0FF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("ASSIGN TO WHEEL"), layout.wheelPanelX + 10, layout.contentY + 10, 0xFFF4F0FF);
    }

    private void arrangeNativeControls(Layout layout) {
        int footerIndex = 0;
        for (Element element : children()) {
            if (!(element instanceof ClickableWidget widget)) continue;
            String label = widget.getMessage().getString();
            if (widget instanceof MultilineTextWidget) {
                if (label.startsWith("Set a key")) {
                    widget.visible = false;
                } else if (label.startsWith("Select an emote")) {
                    widget.setPosition(layout.wheelPanelX + 10, layout.contentY + 50);
                }
                continue;
            }
            if (label.equalsIgnoreCase("Open Emotes Folder") || label.equalsIgnoreCase("Done") || label.equalsIgnoreCase("Mod Options")) {
                int buttonWidth = Math.max(88, (layout.footerWidth - layout.gap * 2) / 3);
                widget.setDimensions(buttonWidth, 20);
                widget.setPosition(layout.footerX + footerIndex * (buttonWidth + layout.gap), layout.footerY);
                footerIndex++;
            } else if (label.equalsIgnoreCase("Reset")) {
                widget.visible = false;
            } else if (widget instanceof ButtonWidget) {
                widget.visible = false;
            }
        }
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height, int fill, int border) {
        context.fill(x, y, x + width, y + height, fill);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x25FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, border);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private static final class Layout {
        final int margin, gap, contentY, contentHeight, footerX, footerY, footerWidth;
        final int listX, listWidth, previewX, previewWidth, wheelPanelX, wheelPanelWidth;
        final int searchX, searchY, searchWidth, wheelX, wheelY, wheelSize, keyWidth, resetX, resetWidth;

        Layout(int width, int height) {
            margin = Math.max(12, width / 50);
            gap = Math.max(8, width / 120);
            contentY = Math.max(50, height / 11);
            footerY = height - Math.max(30, height / 18);
            contentHeight = Math.max(250, footerY - contentY - 14);
            footerX = margin;
            footerWidth = width - margin * 2;
            int usableWidth = footerWidth - gap * 2;
            int calculatedListWidth = Math.max(210, usableWidth * 38 / 100);
            int calculatedPreviewWidth = Math.max(170, usableWidth * 29 / 100);
            int calculatedWheelWidth = Math.max(150, usableWidth - calculatedListWidth - calculatedPreviewWidth);
            // If the window is exceptionally narrow, preserve the wheel and
            // preview by reducing the library first rather than overflowing.
            if (calculatedListWidth + calculatedPreviewWidth + calculatedWheelWidth > usableWidth) {
                calculatedListWidth = Math.max(140, usableWidth - calculatedPreviewWidth - calculatedWheelWidth);
            }
            listWidth = calculatedListWidth;
            previewWidth = calculatedPreviewWidth;
            wheelPanelWidth = calculatedWheelWidth;
            listX = margin;
            previewX = listX + listWidth + gap;
            wheelPanelX = previewX + previewWidth + gap;
            searchWidth = Math.min(previewWidth, Math.max(150, width / 3));
            searchX = previewX + (previewWidth - searchWidth) / 2;
            searchY = Math.max(16, contentY - 32);
            wheelSize = Math.max(120, Math.min(260, Math.min(wheelPanelWidth - 20, contentHeight - 174)));
            wheelX = wheelPanelX + (wheelPanelWidth - wheelSize) / 2;
            wheelY = contentY + 120;
            keyWidth = Math.max(72, (wheelPanelWidth - 28) * 2 / 3);
            resetX = wheelPanelX + 10 + keyWidth + 8;
            resetWidth = Math.max(52, wheelPanelWidth - keyWidth - 28);
        }
    }
}
