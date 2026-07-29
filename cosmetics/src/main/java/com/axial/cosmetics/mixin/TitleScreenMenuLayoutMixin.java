package com.axial.cosmetics.mixin;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.util.Arrays;
import com.axial.cosmetics.client.MenuMusicConfig;
import com.axial.cosmetics.client.MenuMusicVolumeSliderWidget;
import com.axial.cosmetics.client.TitleScreenAccountsDropdown;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMenuLayoutMixin {
    private static final int AXIAL_TITLE_BUTTON_X = 20;
    private static final int AXIAL_TITLE_BUTTON_Y = 152;
    private static final int AXIAL_TITLE_QUIT_Y = 20;
    private static final int AXIAL_TITLE_BUTTON_RIGHT_MARGIN = 20;
    private static final int AXIAL_TITLE_BUTTON_SPACING = 28;
    private static final int AXIAL_TITLE_BUTTON_BASE_WIDTH = 208;
    private static final int AXIAL_TITLE_BUTTON_HEIGHT = 22;
    private static final int AXIAL_TITLE_QUIT_BUTTON_SIZE = 32;
    private static final int AXIAL_TITLE_MUSIC_SLIDER_WIDTH = 112;
    private static final int AXIAL_TITLE_MUSIC_SLIDER_HEIGHT = 18;
    private static final int AXIAL_TITLE_MUSIC_SLIDER_GAP = 8;
    private static final int AXIAL_TITLE_ACCOUNTS_Y = AXIAL_TITLE_BUTTON_Y + (AXIAL_TITLE_BUTTON_SPACING * 3);
    private static final String ACCOUNTS_LABEL = "Accounts";
    private static final List<String> SCREEN_CHILD_LIST_FIELDS = Arrays.asList("field_22786", "field_33816", "field_33815");

    @Inject(method = "init", at = @At("TAIL"))
    private void axial_cosmetics$layoutTitleMenu(CallbackInfo ci) {
        axial_cosmetics$ensureMusicSlider();
        axial_cosmetics$ensureAccountsButton();
        axial_cosmetics$applyLayout();
    }

    private void axial_cosmetics$applyLayout() {
        List<?> children = ((TitleScreen) (Object) this).children();
        List<ButtonWidget> buttons = new ArrayList<>();
        for (Object child : children) {
            if (!(child instanceof ClickableWidget widget)) {
                continue;
            }

            if (widget instanceof ButtonWidget button) {
                buttons.add(button);
            }
        }

        for (ButtonWidget button : buttons) {
            String label = button.getMessage().getString();
            String lower = label.toLowerCase(Locale.ROOT);
            if (lower.contains("quit")) {
                button.setWidth(AXIAL_TITLE_QUIT_BUTTON_SIZE);
                button.setHeight(AXIAL_TITLE_QUIT_BUTTON_SIZE);
                button.setPosition(
                        Math.max(AXIAL_TITLE_BUTTON_RIGHT_MARGIN, ((TitleScreen) (Object) this).width - AXIAL_TITLE_BUTTON_RIGHT_MARGIN - AXIAL_TITLE_QUIT_BUTTON_SIZE),
                        AXIAL_TITLE_QUIT_Y
                );
            } else if (lower.contains("single")) {
                button.setWidth(AXIAL_TITLE_BUTTON_BASE_WIDTH);
                button.setHeight(22);
                button.setPosition(AXIAL_TITLE_BUTTON_X, AXIAL_TITLE_BUTTON_Y);
            } else if (lower.contains("multi")) {
                button.setWidth(AXIAL_TITLE_BUTTON_BASE_WIDTH);
                button.setHeight(22);
                button.setPosition(AXIAL_TITLE_BUTTON_X, AXIAL_TITLE_BUTTON_Y + AXIAL_TITLE_BUTTON_SPACING);
            } else if (lower.contains("options")) {
                button.setWidth(AXIAL_TITLE_BUTTON_BASE_WIDTH);
                button.setHeight(AXIAL_TITLE_BUTTON_HEIGHT);
                button.setPosition(AXIAL_TITLE_BUTTON_X, AXIAL_TITLE_BUTTON_Y + (AXIAL_TITLE_BUTTON_SPACING * 2));
            } else if (lower.contains("accounts")) {
                button.setWidth(AXIAL_TITLE_BUTTON_BASE_WIDTH);
                button.setHeight(AXIAL_TITLE_BUTTON_HEIGHT);
                button.setPosition(AXIAL_TITLE_BUTTON_X, AXIAL_TITLE_ACCOUNTS_Y);
            } else {
                button.setWidth(AXIAL_TITLE_BUTTON_BASE_WIDTH);
                button.setHeight(AXIAL_TITLE_BUTTON_HEIGHT);
                button.setPosition(AXIAL_TITLE_BUTTON_X, button.getY());
            }
        }

        for (Object child : children) {
            if (child instanceof MenuMusicVolumeSliderWidget slider) {
                int x = Math.max(
                        AXIAL_TITLE_BUTTON_X,
                        ((TitleScreen) (Object) this).width - AXIAL_TITLE_BUTTON_RIGHT_MARGIN - AXIAL_TITLE_QUIT_BUTTON_SIZE - AXIAL_TITLE_MUSIC_SLIDER_GAP - AXIAL_TITLE_MUSIC_SLIDER_WIDTH
                );
                int y = AXIAL_TITLE_QUIT_Y + ((AXIAL_TITLE_QUIT_BUTTON_SIZE - AXIAL_TITLE_MUSIC_SLIDER_HEIGHT) / 2);
                slider.setDimensionsAndPosition(AXIAL_TITLE_MUSIC_SLIDER_WIDTH, AXIAL_TITLE_MUSIC_SLIDER_HEIGHT, x, y);
            }
        }

        removeRealmsWidgets();
    }

    private void axial_cosmetics$ensureMusicSlider() {
        List<?> children = ((TitleScreen) (Object) this).children();
        for (Object child : children) {
            if (child instanceof MenuMusicVolumeSliderWidget) {
                return;
            }
        }

        int x = Math.max(
                AXIAL_TITLE_BUTTON_X,
                ((TitleScreen) (Object) this).width - AXIAL_TITLE_BUTTON_RIGHT_MARGIN - AXIAL_TITLE_QUIT_BUTTON_SIZE - AXIAL_TITLE_MUSIC_SLIDER_GAP - AXIAL_TITLE_MUSIC_SLIDER_WIDTH
        );
        int y = AXIAL_TITLE_QUIT_Y + ((AXIAL_TITLE_QUIT_BUTTON_SIZE - AXIAL_TITLE_MUSIC_SLIDER_HEIGHT) / 2);
        MenuMusicVolumeSliderWidget slider = new MenuMusicVolumeSliderWidget(
                x,
                y,
                AXIAL_TITLE_MUSIC_SLIDER_WIDTH,
                AXIAL_TITLE_MUSIC_SLIDER_HEIGHT,
                MenuMusicConfig.volume()
        );
        axial_cosmetics$addToScreenLists(slider);
    }

    private void axial_cosmetics$ensureAccountsButton() {
        List<?> children = ((TitleScreen) (Object) this).children();
        for (Object child : children) {
            if (child instanceof ButtonWidget button && ACCOUNTS_LABEL.equals(button.getMessage().getString())) {
                return;
            }
        }

        ButtonWidget accountsButton = ButtonWidget.builder(Text.literal(ACCOUNTS_LABEL), button -> TitleScreenAccountsDropdown.toggle())
                .build();
        axial_cosmetics$addToScreenLists(accountsButton);
    }

    private void axial_cosmetics$addToScreenLists(MenuMusicVolumeSliderWidget slider) {
        axial_cosmetics$addToScreenLists0(slider);
    }

    private void axial_cosmetics$addToScreenLists(ButtonWidget button) {
        axial_cosmetics$addToScreenLists0(button);
    }

    private void axial_cosmetics$addToScreenLists0(Object widget) {
        Class<?> type = ((TitleScreen) (Object) this).getClass();
        while (type != null) {
            for (String fieldName : SCREEN_CHILD_LIST_FIELDS) {
                try {
                    Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value instanceof List<?> list && !list.contains(widget)) {
                        @SuppressWarnings("unchecked")
                        List<Object> mutableList = (List<Object>) list;
                        mutableList.add(widget);
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
