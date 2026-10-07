package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces Emotecraft's textured page controls with compact text arrows. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.utils.PageButton", remap = false)
public abstract class EmoteRadialPageArrowMixin {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    @Inject(method = "renderContents", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$drawPageArrow(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null
                || !"io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen".equals(client.currentScreen.getClass().getName())) {
            return;
        }

        ClickableWidget button = (ClickableWidget) (Object) this;
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        boolean hovered = button.isHovered() || button.isFocused();
        context.fill(x, y, x + width, y + height, hovered ? 0xFF20202D : 0xFF11111B);
        context.drawStrokedRectangle(x, y, width, height, hovered ? 0xFFFFFFFF : 0xFFE7D9FF);

        String arrow = x < client.getWindow().getScaledWidth() / 2 ? "<" : ">";
        Text text = Text.literal(arrow).styled(style -> style.withFont(UI_FONT));
        context.drawCenteredTextWithShadow(client.textRenderer, text, x + width / 2,
                y + (height - client.textRenderer.fontHeight) / 2, button.active ? 0xFFF7F7FF : 0xFF807B89);
        ci.cancel();
    }
}
