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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public final class ItemScalerSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 452;
    private static final int PANEL_HEIGHT = 168;
    private static final int PANEL_PADDING = 18;
    private static final int BACK_BUTTON_WIDTH = 24;
    private static final int BACK_BUTTON_HEIGHT = 18;
    private static final Identifier BACK_ARROW_ICON = AxialCosmetics.id("textures/gui/back-arrow.png");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private final ScaleSliderWidget mainHandSlider;
    private final ScaleSliderWidget offHandSlider;
    private int panelX;
    private int panelY;
    private static Method playButtonClickSoundMethod;

    public ItemScalerSettingsScreen(Screen parent) {
        super(uiText("ITEM SCALER"));
        this.parent = parent;
        this.mainHandSlider = new ScaleSliderWidget(0, 0, 0, 20, HandTarget.MAIN_HAND);
        this.offHandSlider = new ScaleSliderWidget(0, 0, 0, 20, HandTarget.OFF_HAND);
    }

    @Override
    protected void init() {
        rebuildLayout();
        addDrawableChild(mainHandSlider);
        addDrawableChild(offHandSlider);
        mainHandSlider.updateFromConfig();
        offHandSlider.updateFromConfig();
    }

    @Override
    public void close() {
        ItemScalerConfig.save();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        rebuildLayout();
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("HELD ITEM RENDER SCALE"), panelX + PANEL_WIDTH / 2, panelY + 23, 0xFFC6D0F3);

        drawBackButton(context, mouseX, mouseY);
        drawSliderLabel(context, "HELD ITEM", mainHandSlider.getX(), mainHandSlider.getY());
        drawSliderLabel(context, "OFF HAND", offHandSlider.getX(), offHandSlider.getY());
        drawSliderFrame(context, mainHandSlider.getX(), mainHandSlider.getY(), mainHandSlider.getWidth(), mainHandSlider.getHeight(), mainHandSlider.scale());
        drawSliderFrame(context, offHandSlider.getX(), offHandSlider.getY(), offHandSlider.getWidth(), offHandSlider.getHeight(), offHandSlider.scale());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && inside(click.x(), click.y(), panelX + PANEL_PADDING, panelY + 6, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)) {
            playButtonClickSound();
            ItemScalerConfig.save();
            returnToModsMenu();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void rebuildLayout() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = Math.max(16, (height - PANEL_HEIGHT) / 2);
        int sliderWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        int firstSliderY = panelY + 58;
        mainHandSlider.setPosition(panelX + PANEL_PADDING, firstSliderY);
        mainHandSlider.setWidth(sliderWidth);
        offHandSlider.setPosition(panelX + PANEL_PADDING, firstSliderY + 42);
        offHandSlider.setWidth(sliderWidth);
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);
    }

    private void drawSliderLabel(DrawContext context, String label, int sliderX, int sliderY) {
        context.drawTextWithShadow(textRenderer, uiText(label), sliderX + 2, sliderY - 12, 0xFFC6D0F3);
    }

    private void drawSliderFrame(DrawContext context, int x, int y, int sliderWidth, int sliderHeight, float scale) {
        float progress = (scale - ItemScalerConfig.MIN_SCALE) / (ItemScalerConfig.MAX_SCALE - ItemScalerConfig.MIN_SCALE);
        int handleX = x + Math.round(progress * (sliderWidth - 8));
        context.fill(x, y, x + sliderWidth, y + sliderHeight, 0xF00E1018);
        context.fill(x + 1, y + 1, x + sliderWidth - 1, y + 2, 0x66FFFFFF);
        context.fill(x + 2, y + sliderHeight / 2 - 2, x + sliderWidth - 2, y + sliderHeight / 2 + 2, 0xCC2A2F3C);
        context.fill(x + 2, y + sliderHeight / 2 - 2, handleX + 4, y + sliderHeight / 2 + 2, 0xFF8AF0C2);
        context.fill(handleX, y + 2, handleX + 8, y + sliderHeight - 2, 0xFFE9D9FF);
        context.drawStrokedRectangle(handleX, y + 2, 8, sliderHeight - 4, 0xFF8F5DFF);
        context.drawStrokedRectangle(x, y, sliderWidth, sliderHeight, 0xFF8F5DFF);

        String label = "SCALE: " + String.format(Locale.ROOT, "%.2fx", scale);
        context.drawCenteredTextWithShadow(textRenderer, uiText(label), x + sliderWidth / 2, y + 6, 0xFFFFFFFF);
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

    private void returnToModsMenu() {
        try {
            Object rootParent = parent;
            if (rootParent != null && "org.axial.axialutils.client.AxialConfigScreen".equals(rootParent.getClass().getName())) {
                rootParent = getParentScreen(rootParent);
            }

            Class<?> screenClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen");
            Class<?> modeClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$Mode");
            Object mainMode = Enum.valueOf(modeClass.asSubclass(Enum.class), "MAIN");
            Object screen = screenClass.getConstructor(Screen.class, modeClass).newInstance(rootParent, mainMode);
            if (screen instanceof Screen modsScreen) {
                MinecraftClient.getInstance().setScreen(modsScreen);
                return;
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }

        MinecraftClient.getInstance().setScreen(parent);
    }

    private static Object getParentScreen(Object screen) throws ReflectiveOperationException {
        Field parentField = screen.getClass().getDeclaredField("parent");
        parentField.setAccessible(true);
        return parentField.get(screen);
    }

    private static void playButtonClickSound() {
        try {
            if (playButtonClickSoundMethod == null) {
                Class<?> themeClass = Class.forName("org.axial.axialutils.client.AxialUiTheme");
                playButtonClickSoundMethod = themeClass.getDeclaredMethod("playButtonClickSound");
                playButtonClickSoundMethod.setAccessible(true);
            }
            playButtonClickSoundMethod.invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private enum HandTarget {
        MAIN_HAND("MAIN HAND") {
            @Override
            float scale() {
                return ItemScalerConfig.mainHandScale();
            }

            @Override
            void setScale(float scale) {
                ItemScalerConfig.setMainHandScale(scale);
            }
        },
        OFF_HAND("OFF HAND") {
            @Override
            float scale() {
                return ItemScalerConfig.offHandScale();
            }

            @Override
            void setScale(float scale) {
                ItemScalerConfig.setOffHandScale(scale);
            }
        };

        private final String label;

        HandTarget(String label) {
            this.label = label;
        }

        abstract float scale();

        abstract void setScale(float scale);
    }

    private final class ScaleSliderWidget extends SliderWidget {
        private final HandTarget target;

        private ScaleSliderWidget(int x, int y, int width, int height, HandTarget target) {
            super(x, y, width, height, Text.empty(), 0.0);
            this.target = target;
            updateFromConfig();
        }

        private void updateFromConfig() {
            setValue(scaleToSliderValue(target.scale()));
            updateMessage();
        }

        private float scale() {
            return target.scale();
        }

        @Override
        protected void updateMessage() {
            setMessage(uiText("SCALE: " + String.format(Locale.ROOT, "%.2fx", sliderValueToScale((float) value))));
        }

        @Override
        protected void applyValue() {
            target.setScale(sliderValueToScale((float) value));
            updateMessage();
        }

    }

    private static float sliderValueToScale(float sliderValue) {
        float progress = Math.max(0.0f, Math.min(1.0f, sliderValue));
        return ItemScalerConfig.MIN_SCALE + progress * (ItemScalerConfig.MAX_SCALE - ItemScalerConfig.MIN_SCALE);
    }

    private static float scaleToSliderValue(float scaleValue) {
        float clamped = Math.max(ItemScalerConfig.MIN_SCALE, Math.min(ItemScalerConfig.MAX_SCALE, scaleValue));
        return (clamped - ItemScalerConfig.MIN_SCALE) / (ItemScalerConfig.MAX_SCALE - ItemScalerConfig.MIN_SCALE);
    }
}
