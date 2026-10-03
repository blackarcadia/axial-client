package com.axial.cosmetics.mixin;

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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Sectioned Armor HUD settings matching the compact Information HUD layout. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false, priority = 1500)
public abstract class AxialConfigScreenArmorMenuMixin extends Screen {
    @Unique private static final int WIDTH = 452, HEIGHT = 168, CONTENT_HEIGHT = 328;
    @Unique private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    @Unique private int axial_cosmetics$armorScroll;

    protected AxialConfigScreenArmorMenuMixin(Text title) { super(title); }

    @Inject(method = "rebuildLayout", at = @At("HEAD"), remap = false, order = 3000)
    private void rememberScroll(CallbackInfo ci) { if (isArmor()) axial_cosmetics$armorScroll = getInt("submenuScrollOffset"); }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 3000)
    private void buildArmor(CallbackInfo ci) {
        if (!isArmor()) return;
        try {
            List<Object> tiles = list("tiles"), rows = list("optionRows");
            tiles.clear(); rows.clear();
            int panelX = (width - WIDTH) / 2;
            addEnabledTile(tiles, panelX);
            addOption(panelX, 84, 0, "HELMET", () -> enabled(AxialConfigManager.get().armorHudShowHelmet), v -> set("armorHudShowHelmet", v));
            addOption(panelX, 84, 1, "CHEST", () -> enabled(AxialConfigManager.get().armorHudShowChestplate), v -> set("armorHudShowChestplate", v));
            addOption(panelX, 114, 0, "LEGS", () -> enabled(AxialConfigManager.get().armorHudShowLeggings), v -> set("armorHudShowLeggings", v));
            addOption(panelX, 114, 1, "BOOTS", () -> enabled(AxialConfigManager.get().armorHudShowBoots), v -> set("armorHudShowBoots", v));
            addOption(panelX, 144, 0, "HELD ITEM", () -> enabled(AxialConfigManager.get().armorHudShowHeldItem), v -> set("armorHudShowHeldItem", v));
            addOption(panelX, 144, 1, "DURABILITY", () -> enabled(AxialConfigManager.get().armorHudShowDurability), v -> set("armorHudShowDurability", v));
            addColor(panelX, 198, "TITLE COLOR", () -> AxialConfigManager.get().armorHudColor, v -> { AxialConfigManager.get().armorHudColor = v; AxialConfigManager.save(); });
            addColor(panelX, 228, "DURABILITY COLOR", () -> AxialConfigManager.get().armorHudDurabilityColor, v -> { AxialConfigManager.get().armorHudDurabilityColor = v; AxialConfigManager.save(); });
            addModeTile(panelX, 284);
            setInt("panelWidth", WIDTH); setInt("panelHeight", Math.min(height - 32, HEIGHT)); setInt("panelHeightAnimated", getInt("panelHeight"));
            setInt("submenuContentHeight", CONTENT_HEIGHT); setInt("panelTargetY", (height - getInt("panelHeight")) / 2);
            axial_cosmetics$armorScroll = clamp(axial_cosmetics$armorScroll, 0, maxScroll()); setInt("submenuScrollOffset", axial_cosmetics$armorScroll);
            shift(-axial_cosmetics$armorScroll);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not build Armor HUD settings", e); }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void scrollArmor(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (!isArmor() || vertical == 0 || maxScroll() == 0) return;
        int next = clamp(axial_cosmetics$armorScroll + (vertical < 0 ? 18 : -18), 0, maxScroll());
        try { shift(axial_cosmetics$armorScroll - next); axial_cosmetics$armorScroll = next; setInt("submenuScrollOffset", next); cir.setReturnValue(true); }
        catch (ReflectiveOperationException ignored) { }
    }

    @Inject(method = "drawScrollBar", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressSharedScrollBar(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) { if (isArmor()) ci.cancel(); }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false, order = 3000)
    private void drawArmor(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isArmor()) return;
        int panelX = (width - getInt("panelWidth")) / 2, panelY = (height - getInt("panelHeight")) / 2;
        context.enableScissor(panelX, panelY + 26, panelX + getInt("panelWidth"), panelY + getInt("panelHeight") - 14);
        try {
            renderRows(context, mouseX, mouseY, panelY);
            divider(context, panelX + 18, panelY + 58 - axial_cosmetics$armorScroll, "ARMOR OPTIONS");
            divider(context, panelX + 18, panelY + 174 - axial_cosmetics$armorScroll, "COLOR");
            divider(context, panelX + 18, panelY + 258 - axial_cosmetics$armorScroll, "OPTIONS");
            drawScrollBar(context, panelX, panelY);
        } finally { context.disableScissor(); }
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void clickRow(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!isArmor() || click.button() != 0) return;
        try {
            int panelY = (height - getInt("panelHeight")) / 2;
            for (Object row : list("optionRows")) {
                if (!insideControl(row, click.x(), click.y(), panelY)) continue;
                Method activate = row.getClass().getDeclaredMethod("activate"); activate.setAccessible(true); activate.invoke(row);
                cir.setReturnValue(true); return;
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    @Unique private void addEnabledTile(List<Object> tiles, int panelX) throws ReflectiveOperationException {
        Class<?> type = getClass(), supplier = Class.forName(type.getName() + "$BooleanSupplier"), consumer = Class.forName(type.getName() + "$BooleanConsumer");
        Object get = Proxy.newProxyInstance(supplier.getClassLoader(), new Class[]{supplier}, (p, m, a) -> enabled(AxialConfigManager.get().showArmorHud));
        Object set = Proxy.newProxyInstance(consumer.getClassLoader(), new Class[]{consumer}, (p, m, a) -> { AxialConfigManager.get().showArmorHud = (Boolean) a[0]; AxialConfigManager.save(); updateEnabledTileLabel((Boolean) a[0]); rebuild(); return null; });
        Method add = type.getDeclaredMethod("addToggleTile", int.class, int.class, String.class, supplier, consumer); add.setAccessible(true); add.invoke(this, panelX + 18, 30, enabled(AxialConfigManager.get().showArmorHud) ? "ENABLED" : "DISABLED", get, set);
        setField(tiles.getLast(), "width", 416);
    }

    @Unique private void addOption(int panelX, int y, int column, String label, BooleanSupplier get, Consumer<Boolean> set) throws ReflectiveOperationException {
        Class<?> type = getClass(), supplier = Class.forName(type.getName() + "$BooleanSupplier"), consumer = Class.forName(type.getName() + "$BooleanConsumer");
        Object g = Proxy.newProxyInstance(supplier.getClassLoader(), new Class[]{supplier}, (p, m, a) -> get.getAsBoolean());
        Object s = Proxy.newProxyInstance(consumer.getClassLoader(), new Class[]{consumer}, (p, m, a) -> { set.accept((Boolean) a[0]); return null; });
        Method add = type.getDeclaredMethod("addOptionRow", int.class, int.class, String.class, supplier, consumer); add.setAccessible(true); add.invoke(this, panelX + 28 + column * 212, y, label, g, s);
        setField(list("optionRows").getLast(), "width", 170);
    }

    @Unique private void addColor(int panelX, int y, String label, IntSupplier get, IntConsumer set) throws ReflectiveOperationException {
        Class<?> type = getClass(); String prefix = type.getName() + "$";
        Class<?> getter = Class.forName(prefix + "ColorGetter"), setter = Class.forName(prefix + "ColorSetter");
        Object g = Proxy.newProxyInstance(getter.getClassLoader(), new Class[]{getter}, (p, m, a) -> get.getAsInt());
        Object s = Proxy.newProxyInstance(setter.getClassLoader(), new Class[]{setter}, (p, m, a) -> { set.accept((Integer) a[0]); return null; });
        Method add = type.getDeclaredMethod("addColorOptionRow", int.class, int.class, String.class, getter, setter); add.setAccessible(true); add.invoke(this, panelX + 28, y, label, g, s);
        setField(list("optionRows").getLast(), "width", 370);
    }

    @Unique private void addModeTile(int panelX, int y) throws ReflectiveOperationException {
        Class<?> type = getClass(), supplier = Class.forName(type.getName() + "$BooleanSupplier");
        Runnable toggle = () -> { AxialConfigManager.get().armorHudDurabilityPercent = !enabled(AxialConfigManager.get().armorHudDurabilityPercent); AxialConfigManager.save(); rebuild(); };
        Method add = type.getDeclaredMethod("addActionTile", int.class, int.class, String.class, Runnable.class, supplier); add.setAccessible(true);
        add.invoke(this, panelX + 28, y, enabled(AxialConfigManager.get().armorHudDurabilityPercent) ? "PERCENT" : "NUMBER", toggle, null);
    }

    @Unique private void renderRows(DrawContext context, int mouseX, int mouseY, int panelY) {
        try {
            for (Object row : list("optionRows")) {
                Method render = row.getClass().getDeclaredMethod("render", DrawContext.class, int.class, int.class, net.minecraft.client.font.TextRenderer.class, int.class); render.setAccessible(true);
                int rowMouseX = insideControl(row, mouseX, mouseY, panelY) ? mouseX : Integer.MIN_VALUE;
                render.invoke(row, context, rowMouseX, mouseY, textRenderer, panelY);
                drawHex(context, row, panelY);
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    @Unique private void drawHex(DrawContext context, Object row, int panelY) throws ReflectiveOperationException {
        Object getter = field(row, "colorGetter").get(row); if (getter == null) return;
        Method get = getter.getClass().getDeclaredMethod("get"); get.setAccessible(true); int color = (Integer) get.invoke(getter);
        int x = integer(row, "x"), y = integer(row, "y") + panelY + 6, rowWidth = integer(row, "width"); Text hex = text(String.format("#%06X", color & 0xFFFFFF));
        context.drawTextWithShadow(textRenderer, hex, x + rowWidth - 30 - textRenderer.getWidth(hex), y, 0xFFC6D0F3);
    }
    @Unique private void updateEnabledTileLabel(boolean enabled) {
        try { for (Object tile : list("tiles")) { Field label = tile.getClass().getDeclaredField("label"); label.setAccessible(true); String value = (String) label.get(tile); if ("ENABLED".equals(value) || "DISABLED".equals(value)) { label.set(tile, enabled ? "ENABLED" : "DISABLED"); return; } } }
        catch (ReflectiveOperationException ignored) { }
    }

    @Unique private boolean insideControl(Object row, double x, double y, int offset) throws ReflectiveOperationException {
        int rowX = integer(row, "x"), rowY = integer(row, "y") + offset;
        if (y < rowY - 2 || y > rowY + 22) return false;
        Object colorGetter = field(row, "colorGetter").get(row);
        int rowWidth = integer(row, "width");
        return colorGetter != null ? x >= rowX + rowWidth - 24 && x <= rowX + rowWidth + 4 : x >= rowX - 4 && x <= rowX + 20;
    }

    @Unique private void divider(DrawContext context, int x, int y, String label) {
        Text text = text(label);
        context.drawTextWithShadow(textRenderer, text, x + 2, y, 0xFFC6D0F3);
        context.fill(x + textRenderer.getWidth(text) + 12, y + 5, x + getInt("panelWidth") - 28, y + 6, 0x998F5DFF);
    }
    @Unique private void drawScrollBar(DrawContext context, int panelX, int panelY) { int max = maxScroll(); if (max == 0) return; int top = panelY + 30, viewport = getInt("panelHeight") - 48, content = Math.max(viewport + 1, CONTENT_HEIGHT - 48), thumb = Math.max(18, Math.round(viewport * (viewport / (float) content))), travel = Math.max(1, viewport - thumb), thumbY = top + Math.round(axial_cosmetics$armorScroll / (float) max * travel), x = panelX + getInt("panelWidth") - 12; context.fill(x, top, x + 4, top + viewport, 0x2AFFFFFF); context.fill(x, thumbY, x + 4, thumbY + thumb, 0xFFB06AF3); }
    @Unique private void shift(int delta) throws ReflectiveOperationException { for (Object row : list("optionRows")) setField(row, "y", integer(row, "y") + delta); for (Object tile : list("tiles")) setField(tile, "y", integer(tile, "y") + delta); }
    @Unique private void rebuild() { try { Method method = getClass().getDeclaredMethod("rebuildLayout"); method.setAccessible(true); method.invoke(this); } catch (ReflectiveOperationException ignored) { } }
    @Unique private static boolean enabled(Boolean value) { return Boolean.TRUE.equals(value); }
    @Unique private static void set(String field, boolean value) { try { Field f = AxialConfigManager.get().getClass().getField(field); f.set(AxialConfigManager.get(), value); AxialConfigManager.save(); } catch (ReflectiveOperationException ignored) { } }
    @Unique private boolean isArmor() { try { return "ARMOR".equals(String.valueOf(field(this, "mode").get(this))); } catch (ReflectiveOperationException e) { return false; } }
    @Unique private int maxScroll() { return Math.max(0, CONTENT_HEIGHT - getInt("panelHeight")); }
    @Unique private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    @Unique private static Text text(String value) { return Text.literal(value).styled(style -> style.withFont(FONT)); }
    @SuppressWarnings("unchecked") @Unique private List<Object> list(String name) throws ReflectiveOperationException { return (List<Object>) field(this, name).get(this); }
    @Unique private Field field(Object object, String name) throws NoSuchFieldException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field; }
    @Unique private int getInt(String name) { try { return field(this, name).getInt(this); } catch (ReflectiveOperationException e) { return 0; } }
    @Unique private void setInt(String name, int value) throws ReflectiveOperationException { field(this, name).setInt(this, value); }
    @Unique private static int integer(Object object, String name) throws ReflectiveOperationException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.getInt(object); }
    @Unique private static void setField(Object object, String name, int value) throws ReflectiveOperationException { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.setInt(object, value); }
}
