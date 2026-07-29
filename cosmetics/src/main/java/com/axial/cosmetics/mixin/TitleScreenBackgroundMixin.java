package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenBackgroundMixin {
    private static final Identifier AXIAL_TITLE_BACKGROUND = AxialCosmetics.id("textures/gui/title/main_menu_background.png");
    private static final int AXIAL_TITLE_BACKGROUND_WIDTH = 1717;
    private static final int AXIAL_TITLE_BACKGROUND_HEIGHT = 916;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean_large"));
    private static final String OFFICIAL_GAMEMODES = "OFFICIAL GAMEMODES";
    private static final int OFFICIAL_GAMEMODES_RIGHT_MARGIN = 24;
    private static final int OFFICIAL_GAMEMODES_TOP = 126;
    private static final int OFFICIAL_GAMEMODES_LABEL_HEIGHT = 12;
    private static final Identifier OFFICIAL_GAMEMODES_BUTTON = AxialCosmetics.id("textures/gui/title/official_gamemodes_button.png");
    private static final String AXIAL_SERVER_IP = "mc.axialprisons.com";
    private static final String AXIAL_SERVER_NAME = "Axial Prisons";
    private static final int OFFICIAL_GAMEMODES_BUTTON_WIDTH = 168;
    private static final int OFFICIAL_GAMEMODES_BUTTON_HEIGHT = 72;
    private static final int OFFICIAL_GAMEMODES_BUTTON_GAP = 18;
    private static final int OFFICIAL_GAMEMODES_COUNT_LEFT_PADDING = 14;
    private static final long OFFICIAL_GAMEMODES_REFRESH_INTERVAL_MS = 30_000L;
    private static final Pattern OFFICIAL_GAMEMODES_ONLINE_PATTERN = Pattern.compile("\"online\"\\s*:\\s*(\\d+)");
    private volatile String axial_cosmetics$serverCountText = "Loading";
    private volatile boolean axial_cosmetics$serverCountLoading;
    private volatile long axial_cosmetics$serverCountLastRefresh;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"
            )
    )
    private void axial_cosmetics$renderCustomBackground(TitleScreen screen, DrawContext context, float deltaTicks) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                AXIAL_TITLE_BACKGROUND,
                0,
                0,
                0.0f,
                0.0f,
                screen.width,
                screen.height,
                AXIAL_TITLE_BACKGROUND_WIDTH,
                AXIAL_TITLE_BACKGROUND_HEIGHT,
                AXIAL_TITLE_BACKGROUND_WIDTH,
                AXIAL_TITLE_BACKGROUND_HEIGHT
        );
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"
            )
    )
    private void axial_cosmetics$skipLogo(LogoDrawer logoDrawer, DrawContext context, int width, float alpha) {
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/SplashTextRenderer;render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/client/font/TextRenderer;F)V"
            )
    )
    private void axial_cosmetics$skipSplash(SplashTextRenderer splashTextRenderer, DrawContext context, int width, TextRenderer textRenderer, float alpha) {
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
            )
    )
    private void axial_cosmetics$moveCopyrightText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        if (text.contains("Copyright Mojang AB") || text.contains("Fabric") || text.contains("Mods")) {
            return;
        }
        TitleScreen screen = (TitleScreen) (Object) this;
        int rightX = Math.max(2, screen.width - textRenderer.getWidth(text) - 2);
        context.drawTextWithShadow(textRenderer, text, rightX, y, color);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void axial_cosmetics$drawOfficialGamemodes(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        axial_cosmetics$ensureServerCount();

        TitleScreen screen = (TitleScreen) (Object) this;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        Text label = Text.literal(OFFICIAL_GAMEMODES).styled(style -> style.withFont(UI_FONT));
        int textWidth = textRenderer.getWidth(label);
        int x = Math.max(OFFICIAL_GAMEMODES_RIGHT_MARGIN, screen.width - OFFICIAL_GAMEMODES_RIGHT_MARGIN - textWidth);
        int y = OFFICIAL_GAMEMODES_TOP;

        drawOutlinedText(context, textRenderer, label, x, y, 0xFFFFFFFF, 0xFF000000);

        int buttonX = Math.max(
                OFFICIAL_GAMEMODES_RIGHT_MARGIN,
                screen.width - OFFICIAL_GAMEMODES_RIGHT_MARGIN - OFFICIAL_GAMEMODES_BUTTON_WIDTH
        );
        int buttonY = y + OFFICIAL_GAMEMODES_LABEL_HEIGHT + OFFICIAL_GAMEMODES_BUTTON_GAP;
        boolean hovered = axial_cosmetics$inRect(mouseX, mouseY, buttonX, buttonY, OFFICIAL_GAMEMODES_BUTTON_WIDTH, OFFICIAL_GAMEMODES_BUTTON_HEIGHT);
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                OFFICIAL_GAMEMODES_BUTTON,
                buttonX,
                buttonY,
                0.0f,
                0.0f,
                OFFICIAL_GAMEMODES_BUTTON_WIDTH,
                OFFICIAL_GAMEMODES_BUTTON_HEIGHT,
                OFFICIAL_GAMEMODES_BUTTON_WIDTH,
                OFFICIAL_GAMEMODES_BUTTON_HEIGHT,
                OFFICIAL_GAMEMODES_BUTTON_WIDTH,
                OFFICIAL_GAMEMODES_BUTTON_HEIGHT
        );
        if (hovered) {
            context.fill(buttonX, buttonY, buttonX + OFFICIAL_GAMEMODES_BUTTON_WIDTH, buttonY + OFFICIAL_GAMEMODES_BUTTON_HEIGHT, 0x18000000);
        }

        Text count = Text.literal(axial_cosmetics$serverCountText).styled(style -> style.withFont(UI_FONT));
        int countX = buttonX + OFFICIAL_GAMEMODES_COUNT_LEFT_PADDING;
        int countY = buttonY + Math.max(0, (OFFICIAL_GAMEMODES_BUTTON_HEIGHT - textRenderer.fontHeight) / 2);
        drawOutlinedText(context, textRenderer, count, countX, countY, 0xFFFFFFFF, 0xFF000000);
    }

    private static void drawOutlinedText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int fillColor, int outlineColor) {
        context.drawText(textRenderer, text, x - 1, y, outlineColor, false);
        context.drawText(textRenderer, text, x + 1, y, outlineColor, false);
        context.drawText(textRenderer, text, x, y - 1, outlineColor, false);
        context.drawText(textRenderer, text, x, y + 1, outlineColor, false);
        context.drawText(textRenderer, text, x, y, fillColor, false);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$connectOfficialGamemodes(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        TitleScreen screen = (TitleScreen) (Object) this;
        int buttonX = Math.max(
                OFFICIAL_GAMEMODES_RIGHT_MARGIN,
                screen.width - OFFICIAL_GAMEMODES_RIGHT_MARGIN - OFFICIAL_GAMEMODES_BUTTON_WIDTH
        );
        int buttonY = OFFICIAL_GAMEMODES_TOP + OFFICIAL_GAMEMODES_LABEL_HEIGHT + OFFICIAL_GAMEMODES_BUTTON_GAP;
        if (!axial_cosmetics$inRect((int) Math.round(click.x()), (int) Math.round(click.y()), buttonX, buttonY, OFFICIAL_GAMEMODES_BUTTON_WIDTH, OFFICIAL_GAMEMODES_BUTTON_HEIGHT)) {
            return;
        }

        ConnectScreen.connect(
                screen,
                MinecraftClient.getInstance(),
                ServerAddress.parse(AXIAL_SERVER_IP),
                new ServerInfo(AXIAL_SERVER_NAME, AXIAL_SERVER_IP, ServerInfo.ServerType.OTHER),
                false,
                new CookieStorage(Map.of(), Map.of(), false)
        );
        cir.setReturnValue(true);
        cir.cancel();
    }

    private void axial_cosmetics$ensureServerCount() {
        long now = System.currentTimeMillis();
        if (axial_cosmetics$serverCountLoading) {
            return;
        }
        if (now - axial_cosmetics$serverCountLastRefresh < OFFICIAL_GAMEMODES_REFRESH_INTERVAL_MS && !"Loading".equals(axial_cosmetics$serverCountText)) {
            return;
        }

        axial_cosmetics$serverCountLoading = true;
        axial_cosmetics$serverCountLastRefresh = now;
        CompletableFuture.runAsync(() -> {
            String countText = "Offline";
            try {
                countText = axial_cosmetics$fetchServerCount();
            } catch (IOException ignored) {
                countText = "Offline";
            } finally {
                axial_cosmetics$serverCountText = countText;
                axial_cosmetics$serverCountLoading = false;
            }
        });
    }

    private String axial_cosmetics$fetchServerCount() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(AXIAL_SERVER_IP, 25565), 2500);
            socket.setSoTimeout(2500);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshakeOut = new DataOutputStream(handshakeBytes);
            axial_cosmetics$writeVarInt(handshakeOut, 0);
            axial_cosmetics$writeVarInt(handshakeOut, axial_cosmetics$getProtocolVersion());
            axial_cosmetics$writeString(handshakeOut, AXIAL_SERVER_IP);
            handshakeOut.writeShort(25565);
            axial_cosmetics$writeVarInt(handshakeOut, 1);
            axial_cosmetics$writePacket(out, handshakeBytes.toByteArray());
            axial_cosmetics$writePacket(out, new byte[] {0});
            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            int packetLength = axial_cosmetics$readVarInt(in);
            byte[] packet = in.readNBytes(packetLength);
            DataInputStream packetIn = new DataInputStream(new ByteArrayInputStream(packet));
            if (axial_cosmetics$readVarInt(packetIn) != 0) {
                return "Offline";
            }

            String response = axial_cosmetics$readString(packetIn);
            Matcher matcher = OFFICIAL_GAMEMODES_ONLINE_PATTERN.matcher(response);
            if (matcher.find()) {
                return matcher.group(1) + " online";
            }
            return "Online";
        }
    }

    private static void axial_cosmetics$writePacket(DataOutputStream out, byte[] payload) throws IOException {
        axial_cosmetics$writeVarInt(out, payload.length);
        out.write(payload);
    }

    private static void axial_cosmetics$writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        axial_cosmetics$writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static int axial_cosmetics$readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static void axial_cosmetics$writeVarInt(DataOutputStream out, int value) throws IOException {
        do {
            int temp = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                temp |= 0x80;
            }
            out.writeByte(temp);
        } while (value != 0);
    }

    private static String axial_cosmetics$readString(DataInputStream in) throws IOException {
        int length = axial_cosmetics$readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int axial_cosmetics$getProtocolVersion() {
        try {
            var versionMethod = SharedConstants.class.getDeclaredMethod("getCurrentVersion");
            versionMethod.setAccessible(true);
            Object version = versionMethod.invoke(null);
            if (version == null) {
                return 0;
            }

            var protocolMethod = version.getClass().getDeclaredMethod("getProtocolVersion");
            protocolMethod.setAccessible(true);
            Object value = protocolMethod.invoke(version);
            if (value instanceof Integer integer) {
                return integer;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return 0;
    }

    private static boolean axial_cosmetics$inRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < (x + width) && mouseY >= y && mouseY < (y + height);
    }
}
