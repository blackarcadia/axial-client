package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialConfigManager;
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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Compact CPS settings that match the Information HUD and Satchel Helper menus. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false, priority = 1500)
public abstract class AxialConfigScreenCpsMenuMixin extends Screen {
    @Unique private static final int WIDTH = 452, HEIGHT = 168;
    @Unique private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    protected AxialConfigScreenCpsMenuMixin(Text title) { super(title); }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 3000)
    private void buildCps(CallbackInfo ci) {
        if (!isCps()) return;
        try {
            List<Object> tiles = list("tiles");
            tiles.clear();
            list("optionRows").clear();
            int panelX = (width - WIDTH) / 2;
            addEnabledTile(tiles, panelX);
            addColor(panelX, 84, "TITLE COLOR", () -> AxialConfigManager.get().cpsHudTitleColor, value -> { AxialConfigManager.get().cpsHudTitleColor = value; AxialConfigManager.save(); });
            addColor(panelX, 114, "CPS COLOR", () -> AxialConfigManager.get().cpsHudColor, value -> { AxialConfigManager.get().cpsHudColor = value; AxialConfigManager.save(); });
            setInt("panelWidth", WIDTH); setInt("panelHeight", Math.min(height - 32, HEIGHT)); setInt("panelHeightAnimated", getInt("panelHeight"));
            setInt("submenuContentHeight", 144); setInt("panelTargetY", (height - getInt("panelHeight")) / 2);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not build CPS settings", e); }
    }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false, order = 3000)
    private void drawCps(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isCps()) return;
        int panelX = (width - getInt("panelWidth")) / 2, panelY = (height - getInt("panelHeight")) / 2;
        context.enableScissor(panelX, panelY + 26, panelX + getInt("panelWidth"), panelY + getInt("panelHeight") - 14);
        try {
            renderRows(context, mouseX, mouseY, panelY);
            divider(context, panelX + 18, panelY + 58, "COLOR");
        } finally { context.disableScissor(); }
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void clickColor(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!isCps() || click.button() != 0) return;
        try {
            int panelY = (height - getInt("panelHeight")) / 2;
            for (Object row : list("optionRows")) {
                if (!insideSwatch(row, click.x(), click.y(), panelY)) continue;
                Method activate = row.getClass().getDeclaredMethod("activate"); activate.setAccessible(true); activate.invoke(row);
                cir.setReturnValue(true); return;
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    @Unique private void addEnabledTile(List<Object> tiles, int panelX) throws ReflectiveOperationException {
        Class<?> type = getClass(), supplier = Class.forName(type.getName() + "$BooleanSupplier"), consumer = Class.forName(type.getName() + "$BooleanConsumer");
        Object get = Proxy.newProxyInstance(supplier.getClassLoader(), new Class[]{supplier}, (p,m,a) -> Boolean.TRUE.equals(AxialConfigManager.get().showCps));
        Object set = Proxy.newProxyInstance(consumer.getClassLoader(), new Class[]{consumer}, (p,m,a) -> { AxialConfigManager.get().showCps = (Boolean) a[0]; AxialConfigManager.save(); updateEnabledTileLabel((Boolean) a[0]); refreshLayout(); return null; });
        Method add = type.getDeclaredMethod("addToggleTile", int.class, int.class, String.class, supplier, consumer); add.setAccessible(true); add.invoke(this, panelX + 18, 30, Boolean.TRUE.equals(AxialConfigManager.get().showCps) ? "ENABLED" : "DISABLED", get, set);
        setField(tiles.getLast(), "width", 416);
    }

    @Unique private void addColor(int panelX, int y, String label, IntSupplier get, IntConsumer set) throws ReflectiveOperationException {
        Class<?> type = getClass(); String prefix = type.getName() + "$";
        Class<?> getter = Class.forName(prefix + "ColorGetter"), setter = Class.forName(prefix + "ColorSetter");
        Object g = Proxy.newProxyInstance(getter.getClassLoader(), new Class[]{getter}, (p,m,a) -> get.getAsInt());
        Object s = Proxy.newProxyInstance(setter.getClassLoader(), new Class[]{setter}, (p,m,a) -> { set.accept((Integer) a[0]); return null; });
        Method add = type.getDeclaredMethod("addColorOptionRow", int.class, int.class, String.class, getter, setter); add.setAccessible(true); add.invoke(this, panelX + 28, y, label, g, s);
        setField(list("optionRows").getLast(), "width", 370);
    }

    @Unique private void renderRows(DrawContext context, int mouseX, int mouseY, int panelY) {
        try { for (Object row : list("optionRows")) { Method render = row.getClass().getDeclaredMethod("render", DrawContext.class, int.class, int.class, net.minecraft.client.font.TextRenderer.class, int.class); render.setAccessible(true); render.invoke(row, context, mouseX, mouseY, textRenderer, panelY); drawHex(context, row, panelY); } }
        catch (ReflectiveOperationException ignored) { }
    }
    @Unique private void drawHex(DrawContext context, Object row, int panelY) throws ReflectiveOperationException {
        Object getter = field(row, "colorGetter").get(row); if (getter == null) return;
        Method get = getter.getClass().getDeclaredMethod("get"); get.setAccessible(true); int color = (Integer) get.invoke(getter);
        int x = integer(row, "x"), y = integer(row, "y") + panelY + 6, rowWidth = integer(row, "width");
        Text hex = text(String.format("#%06X", color & 0xFFFFFF));
        context.drawTextWithShadow(textRenderer, hex, x + rowWidth - 30 - textRenderer.getWidth(hex), y, 0xFFC6D0F3);
    }
    @Unique private void updateEnabledTileLabel(boolean enabled) {
        try { for (Object tile : list("tiles")) { Field label = tile.getClass().getDeclaredField("label"); label.setAccessible(true); String value = (String) label.get(tile); if ("ENABLED".equals(value) || "DISABLED".equals(value)) { label.set(tile, enabled ? "ENABLED" : "DISABLED"); return; } } }
        catch (ReflectiveOperationException ignored) { }
    }
    @Unique private void refreshLayout() { try { Method method = getClass().getDeclaredMethod("rebuildLayout"); method.setAccessible(true); method.invoke(this); } catch (ReflectiveOperationException ignored) { } }
    @Unique private void divider(DrawContext context, int x, int y, String label) { Text text = text(label); context.drawTextWithShadow(textRenderer, text, x + 2, y, 0xFFC6D0F3); context.fill(x + textRenderer.getWidth(text) + 12, y + 5, x + getInt("panelWidth") - 28, y + 6, 0x998F5DFF); }
    @Unique private boolean insideSwatch(Object row, double x, double y, int offset) throws ReflectiveOperationException { int rowX = integer(row, "x"), rowY = integer(row, "y") + offset, width = integer(row, "width"); return x >= rowX + width - 24 && x <= rowX + width + 4 && y >= rowY - 2 && y <= rowY + 22; }
    @Unique private boolean isCps() { try { return "CPS".equals(String.valueOf(field(this, "mode").get(this))); } catch (ReflectiveOperationException e) { return false; } }
    @Unique private static Text text(String value) { return Text.literal(value).styled(style -> style.withFont(FONT)); }
    @SuppressWarnings("unchecked") @Unique private List<Object> list(String name) throws ReflectiveOperationException { return (List<Object>) field(this, name).get(this); }
    @Unique private Field field(Object object, String name) throws NoSuchFieldException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field; }
    @Unique private int getInt(String name) { try { return field(this, name).getInt(this); } catch (ReflectiveOperationException e) { return 0; } }
    @Unique private void setInt(String name, int value) throws ReflectiveOperationException { field(this, name).setInt(this, value); }
    @Unique private static int integer(Object object, String name) throws ReflectiveOperationException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.getInt(object); }
    @Unique private static void setField(Object object, String name, int value) throws ReflectiveOperationException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.setInt(object, value); }
}
