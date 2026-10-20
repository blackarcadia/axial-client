package com.axial.cosmetics.client;

import io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget;
import io.github.kosmx.emotes.arch.gui.widgets.PlayerPreview;
import io.github.kosmx.emotes.arch.screen.EmoteMenu;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A normal Emotecraft screen subclass, deliberately avoiding bytecode
 * injection into Emotecraft's lifecycle. It keeps Emotecraft's own widgets
 * and interaction model while arranging them into Axial's loadout layout.
 */
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
        list.position(layout.leftX, layout.top, layout.leftWidth, layout.contentHeight);
        preview.setDimensions(layout.centerWidth, layout.contentHeight);
        preview.setPosition(layout.centerX, layout.top);

        searchBox.setDimensions(layout.searchWidth, 20);
        searchBox.setPosition(layout.searchX, Math.max(22, layout.top - 42));

        fastChoose.setSize(layout.wheelSize);
        fastChoose.setPosition(layout.wheelX, layout.wheelY);
        arrangeButtons(layout);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        Layout layout = new Layout(width, height);
        drawPanel(context, layout.leftX, layout.top, layout.leftWidth, layout.contentHeight, 0xE8141622, 0xFFB74DFF);
        drawPanel(context, layout.centerX, layout.top, layout.centerWidth, layout.contentHeight, 0xE8101422, 0xFF46DDF4);
        drawPanel(context, layout.rightX, layout.top, layout.rightWidth, layout.contentHeight, 0xE8141622, 0xFFB74DFF);
        context.fill(layout.rightX + 1, layout.top + 1, layout.rightX + layout.rightWidth - 1, layout.top + 54, 0x7A161427);
        context.fill(layout.leftX, layout.top - 15, layout.leftX + 132, layout.top - 13, 0xFFCF4BFF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("EMOTE LOADOUT"), layout.leftX, layout.top - 34, 0xFFF4F0FF);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, uiText("ASSIGN SELECTED EMOTE"), layout.rightX + 12, layout.top + 10, 0xFFF4F0FF);
    }

    private void arrangeButtons(Layout layout) {
        int footerIndex = 0;
        for (Element element : children()) {
            if (!(element instanceof ClickableWidget button)) continue;
            String label = button.getMessage().getString();
            if (label.equalsIgnoreCase("Open Emotes Folder") || label.equalsIgnoreCase("Done") || label.equalsIgnoreCase("Mod Options")) {
                int footerWidth = Math.max(120, (layout.footerWidth - layout.gap * 2) / 3);
                button.setDimensions(footerWidth, 20);
                button.setPosition(layout.margin + footerIndex * (footerWidth + layout.gap), layout.footerY);
                footerIndex++;
            } else if (label.equalsIgnoreCase("Reset")) {
                button.setDimensions(layout.resetWidth, 20);
                button.setPosition(layout.resetX, layout.top + 27);
            } else if (button instanceof ButtonWidget) {
                button.setDimensions(layout.keyWidth, 20);
                button.setPosition(layout.rightX + 12, layout.top + 27);
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

    private record Layout(int margin, int gap, int top, int footerY, int contentHeight,
                          int leftX, int leftWidth, int centerX, int centerWidth,
                          int rightX, int rightWidth, int searchX, int searchWidth,
                          int wheelX, int wheelY, int wheelSize, int keyWidth,
                          int resetX, int resetWidth, int footerWidth) {
        private Layout(int width, int height) {
            this(
                    Math.max(24, width / 18),
                    Math.max(12, width / 110),
                    Math.max(76, height / 11),
                    height - Math.max(48, height / 19),
                    Math.max(210, height - Math.max(48, height / 19) - Math.max(76, height / 11) - 18),
                    Math.max(24, width / 18),
                    Math.max(270, width * 31 / 100),
                    Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110),
                    Math.max(250, width * 22 / 100),
                    Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110),
                    Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)),
                    Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + (Math.max(250, width * 22 / 100) + Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - Math.min(Math.max(250, width * 22 / 100) + Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)), Math.max(280, width * 30 / 100))) / 2,
                    Math.min(Math.max(250, width * 22 / 100) + Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)), Math.max(280, width * 30 / 100)),
                    Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110) + (Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - Math.min(Math.min(Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - 24, Math.max(210, height - Math.max(48, height / 19) - Math.max(76, height / 11) - 18) - 72), Math.max(220, width / 5))) / 2,
                    Math.max(76, height / 11) + 56 + (Math.max(210, height - Math.max(48, height / 19) - Math.max(76, height / 11) - 18) - 56 - Math.min(Math.min(Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - 24, Math.max(210, height - Math.max(48, height / 19) - Math.max(76, height / 11) - 18) - 72), Math.max(220, width / 5))) / 2,
                    Math.min(Math.min(Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - 24, Math.max(210, height - Math.max(48, height / 19) - Math.max(76, height / 11) - 18) - 72), Math.max(220, width / 5)),
                    Math.max(120, (Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - 26) * 2 / 3),
                    Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110) + Math.max(120, (Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - 26) * 2 / 3) + 14,
                    Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - Math.max(120, (Math.max(260, width - (Math.max(24, width / 18) + Math.max(270, width * 31 / 100) + Math.max(12, width / 110) + Math.max(250, width * 22 / 100) + Math.max(12, width / 110)) - Math.max(24, width / 18)) - 26) * 2 / 3) - 26,
                    width - Math.max(24, width / 18) * 2
            );
        }
    }
}
