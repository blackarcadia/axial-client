package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.InformationHudExtrasConfig;
import net.minecraft.client.MinecraftClient;
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

/** Sectioned, scrollable Information HUD settings. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false, priority = 1500)
public abstract class AxialConfigScreenInformationHudOptionsMixin {
    @Unique private static final int WIDTH = 452, HEIGHT = 168, COLORS_Y = 174, CONTENT_HEIGHT = 294;
    @Unique private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    @Unique private int axial_cosmetics$scroll;

    @Inject(method = "rebuildLayout", at = @At("HEAD"), remap = false, order = 3000)
    private void rememberScroll(CallbackInfo ci) { if (isHud()) axial_cosmetics$scroll = getInt("submenuScrollOffset"); }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 3000)
    private void buildHud(CallbackInfo ci) {
        if (!isHud()) { removeBox(); return; }
        try {
            List<Object> tiles = list("tiles");
            tiles.clear(); list("optionRows").clear();
            int panelX = (((Screen) (Object) this).width - WIDTH) / 2;
            addToggle(panelX, 30, 0, "ENABLED", () -> AxialConfigManager.get().hudEnabled, v -> { AxialConfigManager.get().hudEnabled = v; AxialConfigManager.save(); });
            addToggle(panelX, 84, 0, "PING", InformationHudExtrasConfig::showPing, InformationHudExtrasConfig::setPing);
            addToggle(panelX, 84, 1, "COORDINATES", InformationHudExtrasConfig::showCoordinates, InformationHudExtrasConfig::setCoordinates);
            addToggle(panelX, 114, 0, "CHARGE PER MIN", () -> AxialConfigManager.get().showPickaxeChargePerMinute, v -> { AxialConfigManager.get().showPickaxeChargePerMinute = v; AxialConfigManager.save(); });
            addToggle(panelX, 114, 1, "XP PER MIN", () -> AxialConfigManager.get().showMiningXpPerMinute, v -> { AxialConfigManager.get().showMiningXpPerMinute = v; AxialConfigManager.save(); });
            addToggle(panelX, 144, 0, "SERVER", InformationHudExtrasConfig::showServer, InformationHudExtrasConfig::setServer);
            addToggle(panelX, 144, 1, "FACING", InformationHudExtrasConfig::showFacing, InformationHudExtrasConfig::setFacing);
            addColor(tiles, panelX, COLORS_Y, 0, "TITLE", () -> AxialConfigManager.get().informationHudTitleColor, v -> { AxialConfigManager.get().informationHudTitleColor = v; AxialConfigManager.save(); });
            addColor(tiles, panelX, COLORS_Y, 1, "PING", InformationHudExtrasConfig::pingColor, InformationHudExtrasConfig::setPingColor);
            addColor(tiles, panelX, COLORS_Y + 30, 0, "COORDINATES", InformationHudExtrasConfig::coordinatesColor, InformationHudExtrasConfig::setCoordinatesColor);
            addColor(tiles, panelX, COLORS_Y + 30, 1, "CHARGE / MIN", () -> AxialConfigManager.get().informationHudChargeColor, v -> { AxialConfigManager.get().informationHudChargeColor = v; AxialConfigManager.save(); });
            addColor(tiles, panelX, COLORS_Y + 60, 0, "XP / MIN", () -> AxialConfigManager.get().informationHudXpColor, v -> { AxialConfigManager.get().informationHudXpColor = v; AxialConfigManager.save(); });
            addColor(tiles, panelX, COLORS_Y + 60, 1, "SERVER", InformationHudExtrasConfig::serverColor, InformationHudExtrasConfig::setServerColor);
            addColor(tiles, panelX, COLORS_Y + 90, 0, "FACING", InformationHudExtrasConfig::facingColor, InformationHudExtrasConfig::setFacingColor);
            setInt("panelWidth", WIDTH); setInt("panelHeight", Math.min(((Screen) (Object) this).height - 32, HEIGHT));
            setInt("panelHeightAnimated", getInt("panelHeight")); setInt("submenuContentHeight", CONTENT_HEIGHT);
            setInt("panelTargetY", (((Screen) (Object) this).height - getInt("panelHeight")) / 2);
            axial_cosmetics$scroll = clamp(axial_cosmetics$scroll, 0, maxScroll()); setInt("submenuScrollOffset", axial_cosmetics$scroll);
            shift(-axial_cosmetics$scroll);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not build Information HUD settings", e); }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void scrollHud(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (!isHud() || vertical == 0 || maxScroll() == 0) return;
        int next = clamp(axial_cosmetics$scroll + (vertical < 0 ? 18 : -18), 0, maxScroll());
        try { shift(axial_cosmetics$scroll - next); axial_cosmetics$scroll = next; setInt("submenuScrollOffset", next); cir.setReturnValue(true); }
        catch (ReflectiveOperationException ignored) { }
    }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false, order = 3000)
    private void sections(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isHud()) return;
        int panelX = (((Screen) (Object) this).width - getInt("panelWidth")) / 2, panelY = (((Screen) (Object) this).height - getInt("panelHeight")) / 2;
        context.enableScissor(panelX, panelY + 26, panelX + getInt("panelWidth"), panelY + getInt("panelHeight") - 14);
        try { divider(context, panelX + 18, panelY + 58 - axial_cosmetics$scroll, "HUD OPTIONS"); }
        finally { context.disableScissor(); }
    }

    @Unique private void addToggle(int panelX, int y, int column, String label, BooleanSupplier get, Consumer<Boolean> set) throws ReflectiveOperationException {
        Class<?> s = getClass(), a = Class.forName(s.getName() + "$BooleanSupplier"), b = Class.forName(s.getName() + "$BooleanConsumer");
        Object getter = Proxy.newProxyInstance(a.getClassLoader(), new Class[]{a}, (p,m,args) -> get.getAsBoolean());
        Object setter = Proxy.newProxyInstance(b.getClassLoader(), new Class[]{b}, (p,m,args) -> { set.accept((Boolean) args[0]); return null; });
        Method add = s.getDeclaredMethod("addToggleTile", int.class, int.class, String.class, a, b); add.setAccessible(true); add.invoke(this, panelX + 18 + column * 154, y, label, getter, setter);
    }

    @Unique private void addColor(List<Object> tiles, int panelX, int y, int column, String label, IntSupplier get, IntConsumer set) throws ReflectiveOperationException {
        Class<?> s = getClass(); String p = s.getName() + "$";
        Class<?> group = Class.forName(p + "ColorGroup"), target = Class.forName(p + "ColorTarget"), getterType = Class.forName(p + "ColorGetter"), setterType = Class.forName(p + "ColorSetter");
        Object getter = Proxy.newProxyInstance(getterType.getClassLoader(), new Class[]{getterType}, (q,m,args) -> get.getAsInt());
        Object setter = Proxy.newProxyInstance(setterType.getClassLoader(), new Class[]{setterType}, (q,m,args) -> { set.accept((Integer) args[0]); return null; });
        Method add = s.getDeclaredMethod("addColorTile", int.class, int.class, int.class, String.class, group, target, getterType, setterType); add.setAccessible(true);
        add.invoke(this, panelX + 18 + column * 213, y, 203, label, null, null, getter, setter);
    }

    @Unique private void divider(DrawContext c, int x, int y, String label) { Text t = Text.literal(label).styled(s -> s.withFont(FONT)); var renderer = MinecraftClient.getInstance().textRenderer; c.drawTextWithShadow(renderer, t, x + 2, y, 0xFFC6D0F3); c.fill(x + renderer.getWidth(t) + 12, y + 5, x + getInt("panelWidth") - 28, y + 6, 0x998F5DFF); }
    @Unique private void shift(int delta) throws ReflectiveOperationException { for (Object r : list("optionRows")) shiftY(r, delta); for (Object t : list("tiles")) shiftY(t, delta); }
    @Unique private void removeBox() { try { list("tiles").removeIf(t -> "BOX".equals(text(t, "label"))); } catch (ReflectiveOperationException ignored) { } }
    @Unique private boolean isHud() { try { return "HUD".equals(String.valueOf(field("mode").get(this))); } catch (ReflectiveOperationException ignored) { return false; } }
    @Unique private int maxScroll() { return Math.max(0, CONTENT_HEIGHT - getInt("panelHeight")); }
    @Unique private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    @SuppressWarnings("unchecked") @Unique private List<Object> list(String n) throws ReflectiveOperationException { return (List<Object>) field(n).get(this); }
    @Unique private Field field(String n) throws NoSuchFieldException { Field f = getClass().getDeclaredField(n); f.setAccessible(true); return f; }
    @Unique private int getInt(String n) { try { return field(n).getInt(this); } catch (ReflectiveOperationException e) { return 0; } }
    @Unique private void setInt(String n, int v) throws ReflectiveOperationException { field(n).setInt(this, v); }
    @Unique private static void shiftY(Object o, int d) throws ReflectiveOperationException { Field f = o.getClass().getDeclaredField("y"); f.setAccessible(true); f.setInt(o, f.getInt(o) + d); }
    @Unique private static String text(Object o, String n) { try { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return (String)f.get(o); } catch (ReflectiveOperationException e) { return ""; } }
}
