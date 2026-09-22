package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PotionsHudSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 204;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private final Screen parent;
    private int panelX;
    private int panelY;

    public PotionsHudSettingsScreen(Screen parent) {
        super(uiText("POTIONS HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = Math.max(8, (height - PANEL_HEIGHT) / 2);
        int x = panelX + 18;
        addDrawableChild(ButtonWidget.builder(toggleText("ENABLED", PotionsHudConfig.isEnabled()), button -> {
            PotionsHudConfig.toggle();
            button.setMessage(toggleText("ENABLED", PotionsHudConfig.isEnabled()));
        }).dimensions(x, panelY + 36, 158, 20).build());
        addDrawableChild(ButtonWidget.builder(toggleText("BOX", PotionsHudConfig.showBox()), button -> {
            PotionsHudConfig.toggleBox();
            button.setMessage(toggleText("BOX", PotionsHudConfig.showBox()));
        }).dimensions(x + 166, panelY + 36, 158, 20).build());
        var titleField = new TextFieldWidget(textRenderer, x, panelY + 80, 324, 20, uiText("TITLE NAME"));
        titleField.setMaxLength(48);
        titleField.setText(PotionsHudConfig.title());
        titleField.setChangedListener(PotionsHudConfig::setTitle);
        addDrawableChild(titleField);
        addDrawableChild(ButtonWidget.builder(uiText("TITLE COLOR").copy().styled(style -> style.withColor(PotionsHudConfig.titleColor())), button ->
                MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "POTIONS TITLE",
                        PotionsHudConfig.titleColor(), PotionsHudConfig::setTitleColor, PotionsHudConfig::save)))
                .dimensions(x, panelY + 112, 324, 20).build());
        addDrawableChild(ButtonWidget.builder(uiText("BACK"), button -> close())
                .dimensions(x, panelY + 166, 324, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 12, 0xFFF7F7FF);
        context.drawTextWithShadow(textRenderer, uiText("TITLE NAME"), panelX + 18, panelY + 67, 0xFFC6D0F3);
        context.drawCenteredTextWithShadow(textRenderer, uiText("POSITION IN THE MOVE ELEMENTS TAB"), panelX + PANEL_WIDTH / 2, panelY + 144, 0xFFC6D0F3);
        super.render(context, mouseX, mouseY, deltaTicks);
        context.fill(panelX + 27, panelY + 118, panelX + 35, panelY + 126, PotionsHudConfig.titleColor());
    }

    @Override
    public void removed() {
        PotionsHudConfig.save();
        super.removed();
    }

    @Override
    public void close() {
        PotionsHudConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private static Text toggleText(String label, boolean enabled) {
        return uiText(label + ": " + (enabled ? "ON" : "OFF"));
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
