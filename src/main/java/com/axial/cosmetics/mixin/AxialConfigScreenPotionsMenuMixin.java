package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PotionsHudConfig;
import com.axial.cosmetics.client.PotionsHudSettingsScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/** Gives the Potions HUD the same compact state and colour layout as the other HUDs. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false, priority = 1600)
public abstract class AxialConfigScreenPotionsMenuMixin extends Screen {
    @Unique private static final int WIDTH = 452, HEIGHT = 168;
    @Unique private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    protected AxialConfigScreenPotionsMenuMixin(Text title) { super(title); }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 3100)
    private void axial_cosmetics$buildPotions(CallbackInfo ci) {
        if (!PotionsHudSettingsScreen.isPotions((Screen) (Object) this)) return;
        try {
            List<Object> tiles = list("tiles");
            tiles.clear();
            list("optionRows").clear();
            int panelX = (width - WIDTH) / 2;
            addEnabledTile(tiles, panelX);
            addColorRow(panelX, 84);
            setInt("panelWidth", WIDTH);
            setInt("panelHeight", Math.min(height - 32, HEIGHT));
            setInt("panelHeightAnimated", getInt("panelHeight"));
            setInt("submenuContentHeight", 114);
            setInt("panelTargetY", (height - getInt("panelHeight")) / 2);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not build Potions HUD settings", ex);
        }
    }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false, order = 3100)
    private void axial_cosmetics$drawPotions(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!PotionsHudSettingsScreen.isPotions((Screen) (Object) this)) return;
        int panelX = (width - getInt("panelWidth")) / 2;
        int panelY = (height - getInt("panelHeight")) / 2;
        context.enableScissor(panelX, panelY + 26, panelX + getInt("panelWidth"), panelY + getInt("panelHeight") - 14);
        try {
            for (Object row : list("optionRows")) renderColorRow(context, mouseX, mouseY, row, panelY);
            Text title = text("COLOR");
            context.drawTextWithShadow(textRenderer, title, panelX + 20, panelY + 58, 0xFFC6D0F3);
            context.fill(panelX + 18 + textRenderer.getWidth(title) + 12, panelY + 63, panelX + WIDTH - 28, panelY + 64, 0x998F5DFF);
        } catch (ReflectiveOperationException ignored) {
        } finally {
            context.disableScissor();
        }
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$openPotionsColor(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!PotionsHudSettingsScreen.isPotions((Screen) (Object) this) || click.button() != 0) return;
        try {
            int panelY = (height - getInt("panelHeight")) / 2;
            for (Object row : list("optionRows")) {
                int x = integer(row, "x"), y = integer(row, "y") + panelY, width = integer(row, "width");
                if (click.x() < x + width - 24 || click.x() > x + width + 4 || click.y() < y - 2 || click.y() > y + 22) continue;
                Method activate = row.getClass().getDeclaredMethod("activate");
                activate.setAccessible(true);
                activate.invoke(row);
                cir.setReturnValue(true);
                return;
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    @Unique private void addEnabledTile(List<Object> tiles, int panelX) throws ReflectiveOperationException {
        Class<?> type = getClass();
        Class<?> supplier = Class.forName(type.getName() + "$BooleanSupplier");
        Class<?> consumer = Class.forName(type.getName() + "$BooleanConsumer");
        Object get = Proxy.newProxyInstance(supplier.getClassLoader(), new Class[]{supplier}, (p, m, a) -> PotionsHudConfig.isEnabled());
        Object set = Proxy.newProxyInstance(consumer.getClassLoader(), new Class[]{consumer}, (p, m, a) -> {
            if (PotionsHudConfig.isEnabled() != (Boolean) a[0]) PotionsHudConfig.toggle();
            refreshLayout();
            return null;
        });
        Method add = type.getDeclaredMethod("addToggleTile", int.class, int.class, String.class, supplier, consumer);
        add.setAccessible(true);
        add.invoke(this, panelX + 18, 30, PotionsHudConfig.isEnabled() ? "ENABLED" : "DISABLED", get, set);
        setField(tiles.getLast(), "width", 416);
    }

    @Unique private void addColorRow(int panelX, int y) throws ReflectiveOperationException {
        Class<?> type = getClass();
        String prefix = type.getName() + "$";
        Class<?> getterType = Class.forName(prefix + "ColorGetter");
        Class<?> setterType = Class.forName(prefix + "ColorSetter");
        Object getter = Proxy.newProxyInstance(getterType.getClassLoader(), new Class[]{getterType}, (p, m, a) -> PotionsHudConfig.titleColor());
        Object setter = Proxy.newProxyInstance(setterType.getClassLoader(), new Class[]{setterType}, (p, m, a) -> { PotionsHudConfig.setTitleColor((Integer) a[0]); PotionsHudConfig.save(); return null; });
        Method add = type.getDeclaredMethod("addColorOptionRow", int.class, int.class, String.class, getterType, setterType);
        add.setAccessible(true);
        add.invoke(this, panelX + 28, y, "TITLE COLOR", getter, setter);
        setField(list("optionRows").getLast(), "width", 370);
    }

    @Unique private void renderColorRow(DrawContext context, int mouseX, int mouseY, Object row, int panelY) throws ReflectiveOperationException {
        Method render = row.getClass().getDeclaredMethod("render", DrawContext.class, int.class, int.class, net.minecraft.client.font.TextRenderer.class, int.class);
        render.setAccessible(true);
        render.invoke(row, context, mouseX, mouseY, textRenderer, panelY);
        Object getter = field(row, "colorGetter").get(row);
        Method get = getter.getClass().getDeclaredMethod("get");
        get.setAccessible(true);
        int color = (Integer) get.invoke(getter);
        int x = integer(row, "x"), y = integer(row, "y") + panelY + 6, width = integer(row, "width");
        Text hex = text(String.format("#%06X", color & 0xFFFFFF));
        context.drawTextWithShadow(textRenderer, hex, x + width - 30 - textRenderer.getWidth(hex), y, 0xFFC6D0F3);
    }

    @Unique private void refreshLayout() { try { Method method = getClass().getDeclaredMethod("rebuildLayout"); method.setAccessible(true); method.invoke(this); } catch (ReflectiveOperationException ignored) { } }
    @SuppressWarnings("unchecked") @Unique private List<Object> list(String name) throws ReflectiveOperationException { return (List<Object>) field(this, name).get(this); }
    @Unique private Field field(Object object, String name) throws NoSuchFieldException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field; }
    @Unique private int getInt(String name) { try { return field(this, name).getInt(this); } catch (ReflectiveOperationException ex) { return 0; } }
    @Unique private void setInt(String name, int value) throws ReflectiveOperationException { field(this, name).setInt(this, value); }
    @Unique private static int integer(Object object, String name) throws ReflectiveOperationException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.getInt(object); }
    @Unique private static void setField(Object object, String name, int value) throws ReflectiveOperationException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.setInt(object, value); }
    @Unique private static Text text(String value) { return Text.literal(value).styled(style -> style.withFont(FONT)); }
}
