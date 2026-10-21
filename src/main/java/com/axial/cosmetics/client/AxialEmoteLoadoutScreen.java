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

/** Responsive Axial presentation for Emotecraft's native selection screen. */
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
        list.position(layout.libraryWidth, layout.contentHeight, layout.libraryX, layout.contentY);
        preview.setDimensions(layout.previewWidth, layout.previewHeight);
        preview.setPosition(layout.previewX, layout.previewY);
        searchBox.setDimensions(layout.searchWidth, 20);
        searchBox.setPosition(layout.searchX, layout.outerY + 30);
        fastChoose.setSize(layout.wheelSize);
        fastChoose.setPosition(layout.wheelX, layout.wheelY);
        arrangeNativeControls(layout);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        Layout layout = new Layout(width, height);
        context.fill(layout.outerX, layout.outerY, layout.outerRight, layout.outerBottom, 0xEB10121D);
        context.fill(layout.outerX + 1, layout.outerY + 1, layout.outerRight - 1, layout.outerY + 2, 0x40FFFFFF);
        context.drawStrokedRectangle(layout.outerX, layout.outerY, layout.dialogWidth, layout.dialogHeight, 0xFFB74DFF);
        context.fill(layout.outerX + 26, layout.outerY + 72, layout.outerX + 280, layout.outerY + 75, 0xFFCF4BFF);

        drawPanel(context, layout.libraryX, layout.contentY, layout.libraryWidth, layout.contentHeight, 0xC8101421, 0x804F4272);
        drawPanel(context, layout.previewX, layout.previewY, layout.previewWidth, layout.previewHeight, 0xC80D1622, 0xFF46DDF4);
        drawPanel(context, layout.assignmentX, layout.assignmentY, layout.assignmentWidth, layout.assignmentHeight, 0xC8141622, 0xFFB74DFF);

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("EMOTE LOADOUT"), layout.outerX + 26, layout.outerY + 30, 0xFFF4F0FF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("EMOTE LIBRARY"), layout.libraryX + 14, layout.contentY + 14, 0xFFF4F0FF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("PLAYER PREVIEW"), layout.previewX + 14, layout.previewY + 14, 0xFFF4F0FF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("ASSIGN TO WHEEL"), layout.assignmentX + 14, layout.assignmentY + 14, 0xFFF4F0FF);
        context.fill(layout.libraryX + 14, layout.contentY + 39, layout.libraryX + Math.min(178, layout.libraryWidth - 28), layout.contentY + 41, 0xFF46DDF4);
        context.fill(layout.previewX + 14, layout.previewY + 39, layout.previewX + Math.min(178, layout.previewWidth - 28), layout.previewY + 41, 0xFF46DDF4);
        context.fill(layout.assignmentX + 14, layout.assignmentY + 39, layout.assignmentX + Math.min(178, layout.assignmentWidth - 28), layout.assignmentY + 41, 0xFF46DDF4);
    }

    private void arrangeNativeControls(Layout layout) {
        int footerIndex = 0;
        for (Element element : children()) {
            if (!(element instanceof ClickableWidget widget)) continue;
            String label = widget.getMessage().getString();
            if (widget instanceof MultilineTextWidget) {
                if (label.startsWith("Set a key")) widget.setPosition(layout.assignmentX + 14, layout.assignmentY + 55);
                else if (label.startsWith("Select an emote")) widget.setPosition(layout.assignmentX + 14, layout.assignmentY + 104);
                continue;
            }
            if (label.equalsIgnoreCase("Open Emotes Folder") || label.equalsIgnoreCase("Done") || label.equalsIgnoreCase("Mod Options")) {
                int buttonWidth = Math.max(110, (layout.footerWidth - layout.gap * 2) / 3);
                widget.setDimensions(buttonWidth, 20);
                widget.setPosition(layout.footerX + footerIndex * (buttonWidth + layout.gap), layout.footerY);
                footerIndex++;
            } else if (label.equalsIgnoreCase("Reset")) {
                widget.setDimensions(layout.resetWidth, 20);
                widget.setPosition(layout.resetX, layout.assignmentY + 78);
            } else if (widget instanceof ButtonWidget) {
                widget.setDimensions(layout.keyWidth, 20);
                widget.setPosition(layout.assignmentX + 14, layout.assignmentY + 78);
            }
        }
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height, int fill, int border) {
        context.fill(x, y, x + width, y + height, fill);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x28FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, border);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private static final class Layout {
        final int outerX, outerY, outerRight, outerBottom, dialogWidth, dialogHeight;
        final int gap, contentY, contentHeight;
        final int libraryX, libraryWidth;
        final int previewX, previewY, previewWidth, previewHeight;
        final int assignmentX, assignmentY, assignmentWidth, assignmentHeight;
        final int searchX, searchWidth;
        final int wheelX, wheelY, wheelSize;
        final int footerX, footerY, footerWidth, keyWidth, resetX, resetWidth;

        Layout(int screenWidth, int screenHeight) {
            dialogWidth = Math.max(1, Math.min(1440, screenWidth - 48));
            dialogHeight = Math.max(1, Math.min(720, screenHeight - 48));
            outerX = (screenWidth - dialogWidth) / 2;
            outerY = (screenHeight - dialogHeight) / 2;
            outerRight = outerX + dialogWidth;
            outerBottom = outerY + dialogHeight;
            gap = Math.max(10, dialogWidth / 110);
            int innerX = outerX + 20;
            int innerWidth = dialogWidth - 40;
            contentY = outerY + 94;
            contentHeight = dialogHeight - 170;
            footerX = innerX;
            footerY = outerBottom - 42;
            footerWidth = innerWidth;
            searchWidth = Math.min(420, Math.max(220, innerWidth * 34 / 100));
            searchX = outerRight - 26 - searchWidth;

            boolean wide = innerWidth >= 980;
            libraryWidth = wide ? innerWidth * 42 / 100 : Math.max(220, innerWidth * 35 / 100);
            libraryX = innerX;
            int rightX = libraryX + libraryWidth + gap;
            int rightWidth = innerX + innerWidth - rightX;
            if (wide) {
                previewX = rightX;
                previewY = contentY;
                previewWidth = Math.max(230, (rightWidth - gap) * 55 / 100);
                previewHeight = contentHeight;
                assignmentX = previewX + previewWidth + gap;
                assignmentY = contentY;
                assignmentWidth = rightX + rightWidth - assignmentX;
                assignmentHeight = contentHeight;
            } else {
                previewX = rightX;
                previewY = contentY;
                previewWidth = rightWidth;
                previewHeight = Math.max(180, (contentHeight - gap) * 46 / 100);
                assignmentX = rightX;
                assignmentY = previewY + previewHeight + gap;
                assignmentWidth = rightWidth;
                assignmentHeight = contentY + contentHeight - assignmentY;
            }
            wheelSize = Math.max(150, Math.min(260, Math.min(assignmentWidth - 36, assignmentHeight - 168)));
            wheelX = assignmentX + (assignmentWidth - wheelSize) / 2;
            wheelY = assignmentY + 146 + Math.max(0, (assignmentHeight - 146 - wheelSize) / 2);
            keyWidth = Math.max(88, (assignmentWidth - 36) * 2 / 3);
            resetX = assignmentX + 14 + keyWidth + 8;
            resetWidth = Math.max(64, assignmentWidth - keyWidth - 36);
        }
    }
}
