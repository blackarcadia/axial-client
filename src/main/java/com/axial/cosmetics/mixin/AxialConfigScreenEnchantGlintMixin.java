package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CrosshairColorPickerScreen;
import com.axial.cosmetics.client.EnchantGlintConfig;
import com.axial.cosmetics.client.EnchantGlintSliderWidget;
import com.axial.cosmetics.client.ThirdPersonNameTagsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenEnchantGlintMixin extends Screen {
    @Unique private static final StyleSpriteSource.Font AXIAL_GLINT_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    @Unique private static final int AXIAL_GLINT_HEADER_HEIGHT = 20;
    @Shadow @Final private List<?> optionRows;
    @Shadow private int panelHeight;
    @Shadow private int panelWidth;
    @Shadow private int panelTargetY;
    @Shadow private int submenuContentHeight;
    @Shadow private int currentPanelBaseY() { throw new AssertionError(); }

    @Unique private EnchantGlintSliderWidget axial_cosmetics$glintSlider;
    @Unique private ButtonWidget axial_cosmetics$glintColor;
    @Unique private ButtonWidget axial_cosmetics$glintReset;
    @Unique private int axial_cosmetics$glintY;
    @Unique private boolean axial_cosmetics$glintOptions;
    @Unique private static Field axial_cosmetics$rowXField;
    @Unique private static Method axial_cosmetics$addOptionRowMethod;
    @Unique private static Class<?> axial_cosmetics$booleanSupplierClass;
    @Unique private static Class<?> axial_cosmetics$booleanConsumerClass;

    protected AxialConfigScreenEnchantGlintMixin(Text title) { super(title); }

    @Inject(method = "method_25426", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$initGlintControls(CallbackInfo ci) {
        axial_cosmetics$glintSlider = addSelectableChild(new EnchantGlintSliderWidget());
        axial_cosmetics$glintColor = addSelectableChild(ButtonWidget.builder(axial_cosmetics$glintText("ENCHANT GLINT COLOR"), button ->
                MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "ENCHANT GLINT",
                        EnchantGlintConfig.color(), EnchantGlintConfig::setColor, EnchantGlintConfig::save)))
                .dimensions(0, 0, 308, 20).build());
        axial_cosmetics$glintReset = addSelectableChild(ButtonWidget.builder(axial_cosmetics$glintText("DEFAULT"), button ->
                EnchantGlintConfig.resetColor()).dimensions(0, 0, 76, 20).build());
        axial_cosmetics$positionGlintControls();
    }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$extendOptions(CallbackInfo ci) {
        axial_cosmetics$glintOptions = false;
        for (Object row : optionRows) {
            try {
                Field label = row.getClass().getDeclaredField("label");
                label.setAccessible(true);
                if (!"PLAYER SHADOWS".equals(label.get(row))) continue;
                int rowX = axial_cosmetics$rowX(row);
                int rowY = axial_cosmetics$rowY(row);
                axial_cosmetics$addThirdPersonNameTagsRow(rowX, rowY + 28);
                axial_cosmetics$glintY = rowY + 76;
                axial_cosmetics$glintOptions = true;
                submenuContentHeight = axial_cosmetics$glintY + 62;
                panelHeight = Math.min(height - 32, submenuContentHeight);
                panelTargetY = (height - panelHeight) / 2;
                break;
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Could not place enchant glint options below Player Shadows", ex);
            }
        }
        axial_cosmetics$positionGlintControls();
    }

    @Unique
    private void axial_cosmetics$positionGlintControls() {
        if (axial_cosmetics$glintSlider == null) return;
        int x = (width - panelWidth) / 2 + 14;
        int y = currentPanelBaseY() + axial_cosmetics$glintY - axial_cosmetics$optionsScrollOffset();
        axial_cosmetics$glintSlider.setPosition(x, y);
        axial_cosmetics$glintColor.setPosition(x, y + 28);
        axial_cosmetics$glintReset.setPosition(x + 316, y + 28);
        axial_cosmetics$glintSlider.visible = axial_cosmetics$glintOptions;
        axial_cosmetics$glintColor.visible = axial_cosmetics$glintOptions;
        axial_cosmetics$glintReset.visible = axial_cosmetics$glintOptions;
    }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$drawGlintControls(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        axial_cosmetics$positionGlintControls();
        if (!axial_cosmetics$glintOptions || axial_cosmetics$glintSlider == null) return;
        int panelX = (width - panelWidth) / 2;
        int panelBaseY = currentPanelBaseY();
        context.enableScissor(panelX, panelBaseY + 26, panelX + panelWidth, panelBaseY + panelHeight - 14);
        try {
            int headerX = axial_cosmetics$glintSlider.getX();
            int headerY = axial_cosmetics$glintSlider.getY() - AXIAL_GLINT_HEADER_HEIGHT;
            Text header = axial_cosmetics$glintText("ENCHANT GLINT");
            context.drawTextWithShadow(textRenderer, header, headerX + 2, headerY + 2, 0xFFC6D0F3);
            context.fill(headerX + textRenderer.getWidth(header) + 10, headerY + 6,
                    headerX + panelWidth - 28, headerY + 7, 0x998F5DFF);
            axial_cosmetics$glintSlider.render(context, mouseX, mouseY, delta);
            axial_cosmetics$glintColor.render(context, mouseX, mouseY, delta);
            axial_cosmetics$glintReset.render(context, mouseX, mouseY, delta);
            int x = axial_cosmetics$glintColor.getX() + 8;
            int y = axial_cosmetics$glintColor.getY() + 6;
            context.fill(x, y, x + 8, y + 8, EnchantGlintConfig.color());
        } finally {
            context.disableScissor();
        }
    }

    @Unique
    private static Text axial_cosmetics$glintText(String value) {
        return Text.literal(value).styled(style -> style.withFont(AXIAL_GLINT_FONT));
    }

    @Unique
    private static int axial_cosmetics$rowX(Object row) throws ReflectiveOperationException {
        if (axial_cosmetics$rowXField == null) {
            axial_cosmetics$rowXField = row.getClass().getDeclaredField("x");
            axial_cosmetics$rowXField.setAccessible(true);
        }
        return axial_cosmetics$rowXField.getInt(row);
    }

    @Unique
    private static int axial_cosmetics$rowY(Object row) throws ReflectiveOperationException {
        Field y = row.getClass().getDeclaredField("y");
        y.setAccessible(true);
        return y.getInt(row);
    }

    @Unique
    private void axial_cosmetics$addThirdPersonNameTagsRow(int x, int y) throws ReflectiveOperationException {
        if (axial_cosmetics$booleanSupplierClass == null) {
            axial_cosmetics$booleanSupplierClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanSupplier");
            axial_cosmetics$booleanConsumerClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen$BooleanConsumer");
            axial_cosmetics$addOptionRowMethod = getClass().getDeclaredMethod("addOptionRow", int.class, int.class,
                    String.class, axial_cosmetics$booleanSupplierClass, axial_cosmetics$booleanConsumerClass);
            axial_cosmetics$addOptionRowMethod.setAccessible(true);
        }

        Object getter = java.lang.reflect.Proxy.newProxyInstance(
                axial_cosmetics$booleanSupplierClass.getClassLoader(),
                new Class<?>[]{axial_cosmetics$booleanSupplierClass},
                (proxy, method, args) -> "getAsBoolean".equals(method.getName()) && ThirdPersonNameTagsConfig.enabled());
        Object setter = java.lang.reflect.Proxy.newProxyInstance(
                axial_cosmetics$booleanConsumerClass.getClassLoader(),
                new Class<?>[]{axial_cosmetics$booleanConsumerClass},
                (proxy, method, args) -> {
                    if ("accept".equals(method.getName()) && args != null && args.length == 1) {
                        ThirdPersonNameTagsConfig.setEnabled((boolean) args[0]);
                    }
                    return null;
                });
        axial_cosmetics$addOptionRowMethod.invoke(this, x, y, "THIRD PERSON NAME TAGS", getter, setter);
    }

    @Unique
    private int axial_cosmetics$optionsScrollOffset() {
        try {
            Field scroll = getClass().getDeclaredField("submenuScrollOffset");
            scroll.setAccessible(true);
            return scroll.getInt(this);
        } catch (ReflectiveOperationException ex) {
            return 0;
        }
    }

}
