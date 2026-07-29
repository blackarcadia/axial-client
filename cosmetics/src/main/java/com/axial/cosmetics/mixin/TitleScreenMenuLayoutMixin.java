package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.MenuMusicConfig;
import com.axial.cosmetics.client.MenuMusicVolumeSliderWidget;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMenuLayoutMixin {
    private static final int AXIAL_TITLE_MUSIC_SLIDER_WIDTH = 112;
    private static final int AXIAL_TITLE_MUSIC_SLIDER_HEIGHT = 18;
    private static final List<String> SCREEN_CHILD_LIST_FIELDS = Arrays.asList("field_22786", "field_33816", "field_33815");

    @Inject(method = "init", at = @At("TAIL"))
    private void axial_cosmetics$layoutTitleMenu(CallbackInfo ci) {
        axial_cosmetics$ensureMusicSlider();
        axial_cosmetics$hideVanillaTitleButtons();
        removeRealmsWidgets();
    }

    private void axial_cosmetics$hideVanillaTitleButtons() {
        List<?> children = ((TitleScreen) (Object) this).children();
        for (Object child : children) {
            if (!(child instanceof ButtonWidget button)) {
                continue;
            }

            String lower = button.getMessage().getString().toLowerCase(Locale.ROOT);
            if (lower.contains("single") || lower.contains("multi") || lower.contains("options") || lower.contains("quit")) {
                button.visible = false;
                button.active = false;
            }
        }
    }

    private void axial_cosmetics$ensureMusicSlider() {
        List<?> children = ((TitleScreen) (Object) this).children();
        for (Object child : children) {
            if (child instanceof MenuMusicVolumeSliderWidget) {
                return;
            }
        }

        TitleScreen screen = (TitleScreen) (Object) this;
        int x = Math.max(20, screen.width - 20 - 32 - 8 - AXIAL_TITLE_MUSIC_SLIDER_WIDTH);
        int y = 20 + ((32 - AXIAL_TITLE_MUSIC_SLIDER_HEIGHT) / 2);
        MenuMusicVolumeSliderWidget slider = new MenuMusicVolumeSliderWidget(
                x,
                y,
                AXIAL_TITLE_MUSIC_SLIDER_WIDTH,
                AXIAL_TITLE_MUSIC_SLIDER_HEIGHT,
                MenuMusicConfig.volume()
        );
        axial_cosmetics$addToScreenLists(slider);
    }

    private void axial_cosmetics$addToScreenLists(MenuMusicVolumeSliderWidget slider) {
        Class<?> type = ((TitleScreen) (Object) this).getClass();
        while (type != null) {
            for (String fieldName : SCREEN_CHILD_LIST_FIELDS) {
                try {
                    Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value instanceof List<?> list && !list.contains(slider)) {
                        @SuppressWarnings("unchecked")
                        List<Object> mutableList = (List<Object>) list;
                        mutableList.add(slider);
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    private void removeRealmsWidgets() {
        Class<?> type = ((TitleScreen) (Object) this).getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value instanceof List<?> list) {
                        list.removeIf(TitleScreenMenuLayoutMixin::axial_cosmetics$shouldRemoveTitleWidget);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    private static boolean axial_cosmetics$shouldRemoveTitleWidget(Object child) {
        if (!(child instanceof ClickableWidget widget)) {
            return false;
        }

        String type = widget.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String label = widget instanceof ButtonWidget button
                ? button.getMessage().getString().toLowerCase(Locale.ROOT)
                : "";

        if (type.contains("realm")) {
            return true;
        }

        if (type.contains("access") || type.contains("narrat")) {
            return true;
        }

        if (type.contains("language")) {
            return true;
        }

        if (type.contains("spriteiconbutton") || type.contains("texturedbutton") || type.contains("imagebutton")) {
            return label.contains("realm") || label.contains("access") || label.contains("language");
        }

        return label.contains("realm") || label.contains("access") || label.contains("narrat") || label.contains("language");
    }
}
