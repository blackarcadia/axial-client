package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.ChatOptionsScreen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.FontOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.MouseOptionsScreen;
import net.minecraft.client.gui.screen.option.OnlineOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.CustomizeBuffetLevelScreen;
import net.minecraft.client.gui.screen.world.CustomizeFlatLevelScreen;
import net.minecraft.client.gui.screen.world.EditGameRulesScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.DataPackFailureScreen;
import net.minecraft.client.gui.screen.world.ExperimentsScreen;
import net.minecraft.client.gui.screen.world.OptimizeWorldScreen;
import net.minecraft.client.gui.screen.world.PresetsScreen;
import net.minecraft.client.gui.screen.world.RecoverWorldScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.BackupPromptScreen;
import net.minecraft.client.gui.screen.world.SymlinkWarningScreen;
import net.minecraft.client.realms.gui.screen.RealmsCreateWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsLongRunningMcoTaskScreen;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screen.Screen")
public abstract class MenuSubscreenBackgroundMixin {
    private static final Identifier AXIAL_MENU_BACKGROUND = AxialCosmetics.id("textures/gui/title/sub_menu_background.png");
    private static final int AXIAL_MENU_BACKGROUND_WIDTH = 1717;
    private static final int AXIAL_MENU_BACKGROUND_HEIGHT = 916;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$renderCustomSubmenuBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!axial_cosmetics$shouldUseCustomBackground(screen)) {
            return;
        }

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                AXIAL_MENU_BACKGROUND,
                0,
                0,
                0.0f,
                0.0f,
                screen.width,
                screen.height,
                AXIAL_MENU_BACKGROUND_WIDTH,
                AXIAL_MENU_BACKGROUND_HEIGHT,
                AXIAL_MENU_BACKGROUND_WIDTH,
                AXIAL_MENU_BACKGROUND_HEIGHT
        );
        ci.cancel();
    }

    private static boolean axial_cosmetics$shouldUseCustomBackground(Screen screen) {
        if (MinecraftClient.getInstance().world != null) {
            return false;
        }

        return screen instanceof SelectWorldScreen
                || screen instanceof MultiplayerScreen
                || screen instanceof OptionsScreen
                || screen instanceof CreateWorldScreen
                || screen instanceof CustomizeFlatLevelScreen
                || screen instanceof CustomizeBuffetLevelScreen
                || screen instanceof PresetsScreen
                || screen instanceof EditGameRulesScreen
                || screen instanceof ExperimentsScreen
                || screen instanceof OptimizeWorldScreen
                || screen instanceof RecoverWorldScreen
                || screen instanceof BackupPromptScreen
                || screen instanceof SymlinkWarningScreen
                || screen instanceof DataPackFailureScreen
                || screen instanceof EditWorldScreen
                || screen instanceof LevelLoadingScreen
                || screen instanceof ProgressScreen
                || screen instanceof AddServerScreen
                || screen instanceof ConnectScreen
                || screen instanceof DirectConnectScreen
                || screen instanceof DisconnectedScreen
                || screen instanceof MultiplayerWarningScreen
                || screen instanceof PackScreen
                || screen instanceof GameOptionsScreen
                || screen instanceof VideoOptionsScreen
                || screen instanceof SoundOptionsScreen
                || screen instanceof ControlsOptionsScreen
                || screen instanceof ChatOptionsScreen
                || screen instanceof AccessibilityOptionsScreen
                || screen instanceof LanguageOptionsScreen
                || screen instanceof SkinOptionsScreen
                || screen instanceof MouseOptionsScreen
                || screen instanceof OnlineOptionsScreen
                || screen instanceof FontOptionsScreen
                || screen instanceof RealmsScreen
                || screen instanceof RealmsCreateWorldScreen
                || screen instanceof RealmsLongRunningMcoTaskScreen;
    }
}
