package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CreateWaypointScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 188;
    private static final int PADDING = 18;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private int panelX;
    private int panelY;
    private int color = 0xFF62B6FF;
    private TextFieldWidget nameField;

    public CreateWaypointScreen() {
        super(uiText("CREATE WAYPOINT"));
    }

    @Override
    protected void init() {
        rebuildLayout();
        nameField = new TextFieldWidget(textRenderer, panelX + PADDING, panelY + 57, PANEL_WIDTH - PADDING * 2, 20, uiText("WAYPOINT NAME"));
        nameField.setPlaceholder(uiText("WAYPOINT NAME"));
        nameField.setMaxLength(48);
        nameField.setFocused(true);
        addDrawableChild(nameField);
        setFocused(nameField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        rebuildLayout();
        nameField.setPosition(panelX + PADDING, panelY + 57);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 12, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("SAVES YOUR CURRENT LOCATION"), panelX + PANEL_WIDTH / 2, panelY + 27, 0xFFC6D0F3);
        context.drawTextWithShadow(textRenderer, uiText("NAME"), panelX + PADDING, panelY + 45, 0xFFC6D0F3);
        nameField.render(context, mouseX, mouseY, deltaTicks);
        drawColorButton(context, mouseX, mouseY);
        drawActionButton(context, mouseX, mouseY, createX(), panelY + 146, 160, "CREATE", !nameField.getText().isBlank());
        drawActionButton(context, mouseX, mouseY, cancelX(), panelY + 146, 160, "CANCEL", true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && inside(click.x(), click.y(), colorX(), panelY + 94, PANEL_WIDTH - PADDING * 2, 26)) {
            MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "WAYPOINT", color, value -> color = value, () -> { }));
            return true;
        }
        if (click.button() == 0 && inside(click.x(), click.y(), createX(), panelY + 146, 160, 20) && !nameField.getText().isBlank()) {
            createWaypoint();
            return true;
        }
        if (click.button() == 0 && inside(click.x(), click.y(), cancelX(), panelY + 146, 160, 20)) {
            close();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }

    private void createWaypoint() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        WaypointConfig.create(nameField.getText().trim(), client.world.getRegistryKey().getValue().toString(), client.player.getBlockPos(), color);
        close();
    }

    private void drawColorButton(DrawContext context, int mouseX, int mouseY) {
        int x = colorX();
        int y = panelY + 94;
        boolean hovered = inside(mouseX, mouseY, x, y, PANEL_WIDTH - PADDING * 2, 26);
        context.fill(x, y, x + PANEL_WIDTH - PADDING * 2, y + 26, hovered ? 0xBC20283A : 0xA0181D2C);
        context.drawStrokedRectangle(x, y, PANEL_WIDTH - PADDING * 2, 26, color);
        context.fill(x + 6, y + 6, x + 20, y + 20, color);
        context.drawTextWithShadow(textRenderer, uiText("COLOR — OPEN PICKER"), x + 29, y + 9, 0xFFF7F7FF);
    }

    private void drawActionButton(DrawContext context, int mouseX, int mouseY, int x, int y, int width, String label, boolean enabled) {
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, width, 20);
        context.fill(x, y, x + width, y + 20, enabled ? (hovered ? 0xBC20283A : 0xA0181D2C) : 0x70303035);
        context.drawStrokedRectangle(x, y, width, 20, enabled ? 0xD08F5DFF : 0x805A5A66);
        context.drawCenteredTextWithShadow(textRenderer, uiText(label), x + width / 2, y + 6, enabled ? 0xFFF7F7FF : 0xFF777783);
    }

    private void rebuildLayout() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = Math.max(16, (height - PANEL_HEIGHT) / 2);
    }

    private int colorX() { return panelX + PADDING; }
    private int createX() { return panelX + PADDING; }
    private int cancelX() { return panelX + PANEL_WIDTH - PADDING - 160; }
    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) { return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height; }
    private static Text uiText(String value) { return Text.literal(value).styled(style -> style.withFont(UI_FONT)); }
}
