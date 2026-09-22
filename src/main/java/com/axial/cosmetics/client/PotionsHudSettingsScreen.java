package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.axial.axialutils.client.AxialUiTheme;
import org.axial.axialutils.client.HudTitleRenamerScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class PotionsHudSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 452;
    private static final int PANEL_HEIGHT = 168;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private boolean colorsExpanded;
    private MenuControl titleColorButton;
    private final List<MenuControl> controls = new ArrayList<>();

    public PotionsHudSettingsScreen(Screen parent) {
        super(AxialUiTheme.uiText("POTIONS HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        controls.clear();
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        int x = panelX + 18;
        int y = panelY + 30;
        addControl(x, y, 132,
                () -> PotionsHudConfig.isEnabled() ? "ENABLED" : "DISABLED",
                PotionsHudConfig::toggle, () -> toggleAccent(PotionsHudConfig.isEnabled()));
        addControl(x + 142, y, 132, () -> "BOX",
                PotionsHudConfig::toggleBox, () -> toggleAccent(PotionsHudConfig.showBox()));
        addControl(x + 284, y, 132, () -> "TITLE", () ->
                MinecraftClient.getInstance().setScreen(new HudTitleRenamerScreen(this, "POTIONS HUD",
                        PotionsHudConfig.title(), "Potions", value -> {
                            PotionsHudConfig.setTitle(value);
                            PotionsHudConfig.save();
                        })), () -> AxialUiTheme.ACTION_ACCENT);
        addControl(x, y + 30, 132,
                () -> colorsExpanded ? "HUD COLORS -" : "HUD COLORS +", () -> {
                    colorsExpanded = !colorsExpanded;
                    titleColorButton.button.visible = colorsExpanded;
                }, () -> AxialUiTheme.ACTION_ACCENT);
        titleColorButton = addControl(x, y + 60, 203, () -> "TITLE COLOR", () ->
                MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "POTIONS TITLE",
                        PotionsHudConfig.titleColor(), PotionsHudConfig::setTitleColor, PotionsHudConfig::save)),
                PotionsHudConfig::titleColor);
        titleColorButton.button.visible = colorsExpanded;
        MenuControl back = addControl(x, panelY + 6, 24, () -> "BACK", () ->
                MinecraftClient.getInstance().setScreen(parent), () -> AxialUiTheme.ACTION_ACCENT);
        back.button.setHeight(18);
        back.back = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        AxialUiTheme.drawMenuBackdrop(context, width, height);
        AxialUiTheme.drawPanel(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 10, AxialUiTheme.TITLE_COLOR);
        super.render(context, mouseX, mouseY, deltaTicks);
        for (MenuControl control : controls) control.render(context, mouseX, mouseY);
    }

    @Override
    public void removed() {
        PotionsHudConfig.save();
        super.removed();
    }

    @Override
    public void close() {
        PotionsHudConfig.save();
        MinecraftClient.getInstance().setScreen(null);
    }

    private static int toggleAccent(boolean enabled) {
        return enabled ? AxialUiTheme.TOGGLE_ON : AxialUiTheme.TOGGLE_OFF;
    }

    // Keep standard keyboard/narration behavior, but draw using the exact
    // theme used by AxialConfigScreen.MenuTile instead of the general button skin.
    private MenuControl addControl(int x, int y, int width, Supplier<String> label, Runnable action, IntSupplier accent) {
        MenuControl control = new MenuControl(label, accent);
        control.button = addSelectableChild(ButtonWidget.builder(AxialUiTheme.uiText(label.get()), button -> {
            control.pressedUntil = System.currentTimeMillis() + 140;
            action.run();
            button.setMessage(AxialUiTheme.uiText(label.get()));
        }).dimensions(x, y, width, 20).build());
        controls.add(control);
        return control;
    }

    private class MenuControl {
        private final Supplier<String> label;
        private final IntSupplier accent;
        private ButtonWidget button;
        private long pressedUntil;
        private boolean back;

        private MenuControl(Supplier<String> label, IntSupplier accent) {
            this.label = label;
            this.accent = accent;
        }

        private void render(DrawContext context, int mouseX, int mouseY) {
            if (!button.visible) return;
            boolean hovered = button.isMouseOver(mouseX, mouseY) || button.isFocused();
            if (back) {
                ModMenuBackButton.draw(context, button.getX(), button.getY(), button.getWidth(), button.getHeight(), hovered);
            } else {
                AxialUiTheme.drawButton(context, textRenderer, button.getX(), button.getY(), button.getWidth(), button.getHeight(),
                        label.get(), "", hovered, System.currentTimeMillis() < pressedUntil, accent.getAsInt());
            }
        }
    }
}
