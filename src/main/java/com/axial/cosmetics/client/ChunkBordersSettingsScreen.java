package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ChunkBordersSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 404;
    private static final int PANEL_HEIGHT = 168;
    private static final int PANEL_PADDING = 18;
    private static final int BACK_BUTTON_WIDTH = 24;
    private static final int BACK_BUTTON_HEIGHT = 18;
    private static final int TOGGLE_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2;
    private static final int TOGGLE_HEIGHT = 20;
    private static final Identifier BACK_ARROW_ICON = AxialCosmetics.id("textures/gui/back-arrow.png");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private int panelX;
    private int panelY;

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
        context.drawCenteredTextWithShadow(textRenderer, uiText("TOGGLE MINECRAFT CHUNK BORDERS."), panelX + PANEL_WIDTH / 2, panelY + 22, 0xFFC6D0F3);
        drawBackButton(context, mouseX, mouseY);
        drawToggleButton(context, mouseX, mouseY);
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

        return super.mouseClicked(click, doubled);
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
        int buttonX = panelX + PANEL_PADDING;
        int buttonY = panelY + 6;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
        context.fill(buttonX, buttonY, buttonX + BACK_BUTTON_WIDTH, buttonY + BACK_BUTTON_HEIGHT, hovered ? 0xBC20283A : 0xA0181D2C);
        context.fill(buttonX + 1, buttonY + 1, buttonX + BACK_BUTTON_WIDTH - 1, buttonY + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, hovered ? 0xFFE7D9FF : 0xD08F5DFF);

        int iconWidth = 16;
        int iconHeight = 13;
        int iconX = buttonX + (BACK_BUTTON_WIDTH - iconWidth) / 2 - 4;
        int iconY = buttonY + (BACK_BUTTON_HEIGHT - iconHeight) / 2 - 1;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACK_ARROW_ICON, iconX, iconY, 0.0f, 0.0f, iconWidth, iconHeight, 64, 64, 64, 64);
    }

    private void drawToggleButton(DrawContext context, int mouseX, int mouseY) {
        int x = toggleX();
        int y = toggleY();
        boolean enabled = ChunkBordersConfig.enabled();
        boolean hovered = inside(mouseX, mouseY, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        context.fill(x, y, x + TOGGLE_WIDTH, y + TOGGLE_HEIGHT, hovered ? 0xBC20283A : 0xA0181D2C);
        context.fill(x + 1, y + 1, x + TOGGLE_WIDTH - 1, y + 2, 0x33FFFFFF);
        context.drawStrokedRectangle(x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, enabled ? 0xFF8AF0C2 : 0xD08F5DFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                uiText(enabled ? "ON" : "OFF"),
                x + TOGGLE_WIDTH / 2,
                y + 6,
                enabled ? 0xFFFFFFFF : 0xFFC6D0F3
        );
    }

    private int toggleX() {
        return panelX + PANEL_PADDING;
    }

    private int toggleY() {
        return panelY + 58;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
