package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;

import java.nio.file.Files;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public final class AccountsScreen extends Screen {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));

    private final Screen parent;
    private final AccountStore store = AccountStore.shared();
    private List<AccountStore.Entry> entries = List.of();
    private final Map<UUID, Identifier> skinTextures = new ConcurrentHashMap<>();
    private String status = "Add Microsoft accounts, then select one to play.";
    private boolean busy;
    private UUID switchingAccount;
    private int page;
    private int pageSize;
    private int panelX, panelY, panelWidth, panelHeight;

    public AccountsScreen(Screen parent) {
        super(uiText("ACCOUNTS"));
        this.parent = parent;
    }

    @Override protected void init() {
        try { entries = store.list(); }
        catch (Exception ex) { status = "Could not read saved accounts."; }
        panelWidth = Math.min(420, width - 24);
        pageSize = Math.max(1, (height - 192) / 70);
        panelHeight = Math.min(height - 16, 176 + pageSize * 70);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        page = Math.min(page, Math.max(0, (entries.size() - 1) / pageSize));
        entries.forEach(this::requestSkin);
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearChildren();
        button("Log out", panelX + panelWidth - 82, panelY + 19, 70,
                this::logoutActive, !busy && AccountSessions.isSignedIn(), ButtonTone.DANGER);
        int y = panelY + 66;
        for (var entry : entries.stream().skip((long) page * pageSize).limit(pageSize).toList()) {
            boolean active = entry.uuid().equals(MinecraftClient.getInstance().getSession().getUuidOrNull());
            boolean switching = entry.uuid().equals(switchingAccount);
            button(switching ? "WORKING" : active ? "ACTIVE" : "SWITCH", panelX + panelWidth - 160, y + 21, 84,
                    () -> switchAccount(entry), !busy && !active, active ? ButtonTone.SELECTED : ButtonTone.DEFAULT);
            button(active ? "LOG OUT" : "REMOVE", panelX + panelWidth - 70, y + 21, 58, () -> {
                if (active) { logoutActive(); return; }
                try { store.remove(entry); status = "Removed " + entry.name() + " from saved accounts."; init(); }
                catch (Exception ex) { status = "Could not remove account."; }
            }, !busy, ButtonTone.DANGER);
            y += 70;
        }
        int footer = panelY + panelHeight - 64;
        button("<", panelX + 12, footer, 28, () -> { page--; rebuildButtons(); }, !busy && page > 0);
        button(">", panelX + panelWidth - 40, footer, 28, () -> { page++; rebuildButtons(); }, !busy && (page + 1) * pageSize < entries.size());
        button("Add Microsoft account", panelX + 12, footer + 30, panelWidth - 112,
                () -> client.setScreen(new MicrosoftAccountLoginScreen(this)), !busy, ButtonTone.PRIMARY);
        button("Done", panelX + panelWidth - 88, footer + 30, 76, this::close, !busy);
    }

    private void button(String label, int x, int y, int w, Runnable action, boolean enabled) {
        button(label, x, y, w, action, enabled, ButtonTone.DEFAULT);
    }

    private void button(String label, int x, int y, int w, Runnable action, boolean enabled, ButtonTone tone) {
        var button = new AccountButton(x, y, w, uiText(label), action, tone);
        button.active = enabled;
        addDrawableChild(button);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private void requestSkin(AccountStore.Entry entry) {
        if (skinTextures.containsKey(entry.uuid())) return;
        skinTextures.put(entry.uuid(), DefaultSkinHelper.getSkinTextures(entry.uuid()).body().texturePath());
        CompletableFuture.supplyAsync(() -> {
            try {
                return new YggdrasilAuthenticationService(Proxy.NO_PROXY)
                        .createMinecraftSessionService()
                        .fetchProfile(entry.uuid(), true)
                        .profile();
            } catch (Exception ignored) {
                return null;
            }
        }).thenAccept(profile -> {
            if (profile == null) return;
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().getSkinProvider()
                    .fetchSkinTextures(profile)
                    .thenAccept(skin -> skin.ifPresent(value -> skinTextures.put(entry.uuid(), value.body().texturePath()))));
        });
    }

    private static void roundedRect(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x + 4, y, x + w - 4, y + h, color);
        context.fill(x + 2, y + 1, x + 4, y + h - 1, color);
        context.fill(x + w - 4, y + 1, x + w - 2, y + h - 1, color);
        context.fill(x + 1, y + 2, x + 2, y + h - 2, color);
        context.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, color);
        context.fill(x, y + 4, x + 1, y + h - 4, color);
        context.fill(x + w - 1, y + 4, x + w, y + h - 4, color);
    }

    private enum ButtonTone { DEFAULT, PRIMARY, DANGER, SELECTED }

    /** Screen-local styling; retains vanilla activation, focus, and narration behavior. */
    private static final class AccountButton extends ButtonWidget {
        private final ButtonTone tone;

        private AccountButton(int x, int y, int width, net.minecraft.text.Text label, Runnable action, ButtonTone tone) {
            super(x, y, width, 24, label, ignored -> action.run(), DEFAULT_NARRATION_SUPPLIER);
            this.tone = tone;
        }

        private float hoverProgress;
        private long lastFrameNanos;

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            long now = System.nanoTime();
            float elapsed = lastFrameNanos == 0 ? 0 : Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000f);
            lastFrameNanos = now;
            float target = active && (isHovered() || isFocused()) ? 1 : 0;
            hoverProgress += (target - hoverProgress) * (1 - (float) Math.exp(-elapsed * 18));

            int accent = switch (tone) {
                case PRIMARY -> 0xFFB99AFF;
                case DANGER -> 0xFFF393AD;
                case SELECTED -> 0xFF8FE0C6;
                default -> 0xFFB6ABEC;
            };
            int top = tone == ButtonTone.PRIMARY ? 0xFF7252B8 : 0xFF292836;
            int bottom = tone == ButtonTone.PRIMARY ? 0xFF493078 : 0xFF171720;
            int foreground = tone == ButtonTone.SELECTED ? 0xFFBCF3DF : 0xFFF3EFFA;
            int border = tone == ButtonTone.PRIMARY ? 0xFF9774D6 : 0xFF464252;
            if (tone == ButtonTone.SELECTED) {
                top = 0xFF243C38;
                bottom = 0xFF182825;
                border = 0xFF42695D;
            }
            if (!active && tone != ButtonTone.SELECTED) {
                top = 0xFF20202A;
                bottom = 0xFF181820;
                border = 0xFF302E3B;
                foreground = 0xFF767180;
                accent = foreground;
            }
            top = blend(top, accent, hoverProgress * 0.22f);
            bottom = blend(bottom, accent, hoverProgress * 0.12f);
            border = blend(border, accent, hoverProgress * 0.85f);

            int x = getX(), y = getY();
            // Draw the entire capsule here: no shared button textures or vanilla skin.
            capsule(context, x, y + 2, width, height, 0x50000000, 0x50000000);
            capsule(context, x, y, width, height, border, blend(border, bottom, 0.45f));
            capsule(context, x + 1, y + 1, width - 2, height - 2, top, bottom);
            context.fill(x + 10, y + 1, x + width - 10, y + 2,
                    blend(top, accent, active ? 0.45f : 0.12f));

            // A small illuminated rail makes each action's color visible at rest.
            if (width > 40) {
                capsule(context, x + 7, y + 8, 3, 8, accent, blend(accent, bottom, 0.3f));
            }
            if (active && isFocused()) {
                context.fill(x + 12, y + height - 3, x + width - 12, y + height - 2, accent);
            }
            var renderer = MinecraftClient.getInstance().textRenderer;
            int padding = width > 40 ? 28 : 12;
            var label = uiText(renderer.trimToWidth(getMessage(), Math.max(0, width - padding)).getString());
            context.drawText(renderer, label, x + (width - renderer.getWidth(label)) / 2,
                    y + (height - renderer.fontHeight) / 2, foreground, false);
        }

        private static void capsule(DrawContext context, int x, int y, int w, int h, int top, int bottom) {
            double radius = Math.min(w, h) / 2.0;
            for (int row = 0; row < h; row++) {
                double distance = Math.max(0, Math.abs(row + 0.5 - h / 2.0) - (h / 2.0 - radius));
                int inset = (int) Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - distance * distance)));
                context.fill(x + inset, y + row, x + w - inset, y + row + 1,
                        blend(top, bottom, row / (float) Math.max(1, h - 1)));
            }
        }

        private static int blend(int from, int to, float amount) {
            int result = 0;
            for (int shift = 0; shift <= 24; shift += 8) {
                int a = (from >>> shift) & 255;
                int b = (to >>> shift) & 255;
                result |= Math.round(a + (b - a) * amount) << shift;
            }
            return result;
        }
    }

    private void switchAccount(AccountStore.Entry entry) {
        if (busy) return;
        busy = true;
        switchingAccount = entry.uuid();
        status = "Signing in as " + entry.name() + "...";
        rebuildButtons();
        CompletableFuture.supplyAsync(() -> {
            try {
                var auth = JavaAuthManager.fromJson(MinecraftAuth.createHttpClient("AxialLauncher/1.0"),
                        JsonParser.parseString(Files.readString(store.file(entry.fileName()))).getAsJsonObject());
                auth.getChangeListeners().add(() -> {
                    try { AccountStore.write(store.file(entry.fileName()), JavaAuthManager.toJson(auth).toString()); }
                    catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
                });
                var prepared = AccountSessions.prepare(auth);
                store.save(auth);
                return prepared;
            } catch (Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        }).whenComplete((prepared, error) -> MinecraftClient.getInstance().execute(() -> {
            try {
                if (error != null) {
                    status = "Sign-in failed. Add this Microsoft account again to reconnect.";
                } else {
                    prepared.apply();
                    status = "Playing as " + entry.name() + ".";
                    try { store.select(entry.fileName()); }
                    catch (Exception ex) { status = "Switched, but could not save the startup account."; }
                }
            } catch (Exception ex) { status = "Could not switch. Return to the main menu and retry."; }
            finally {
                busy = false;
                switchingAccount = null;
                init();
            }
        }));
    }

    private void logoutActive() {
        if (busy) return;
        var minecraft = MinecraftClient.getInstance();
        if (minecraft.world != null || minecraft.getNetworkHandler() != null) {
            status = "Return to the main menu before logging out.";
            return;
        }
        try {
            store.logout(minecraft.getSession().getUuidOrNull());
            AccountSessions.signedOut().apply();
            status = "Logged out. Select a saved account or add another.";
        } catch (Exception ex) { status = "Could not finish logging out. Please try again."; }
        init();
    }

    @Override public void close() { if (!busy) MinecraftClient.getInstance().setScreen(parent); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MenuBackgroundRenderer.draw(context, this);
        roundedRect(context, panelX - 3, panelY + 3, panelWidth + 6, panelHeight + 3, 0x60000000);
        roundedRect(context, panelX, panelY, panelWidth, panelHeight, 0xFF514065);
        roundedRect(context, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, 0xF510101A);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 12, 0xFFFFFFFF);
        String accountLabel = AccountSessions.isSignedIn()
                ? "Playing as " + MinecraftClient.getInstance().getSession().getUsername() : "Not signed in";
        context.drawTextWithShadow(textRenderer, uiText(textRenderer.trimToWidth(uiText(accountLabel), panelWidth - 108).getString()), panelX + 12, panelY + 28, 0xFFBFA0FF);
        var statusText = uiText(textRenderer.trimToWidth(uiText(status), panelWidth - 24).getString());
        context.drawTextWithShadow(textRenderer, statusText, (width - textRenderer.getWidth(statusText)) / 2, panelY + 46, 0xFFCCD0DD);
        if (entries.isEmpty()) context.drawCenteredTextWithShadow(textRenderer, uiText("No saved accounts. Add one below."), width / 2, panelY + 78, 0xFFFFFFFF);
        else drawProfileCards(context);
        context.drawCenteredTextWithShadow(textRenderer, uiText((page + 1) + " / " + Math.max(1, (entries.size() + pageSize - 1) / pageSize)), width / 2, panelY + panelHeight - 58, 0xFFCCD0DD);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawProfileCards(DrawContext context) {
        int y = panelY + 66;
        for (AccountStore.Entry entry : entries.stream().skip((long) page * pageSize).limit(pageSize).toList()) {
            boolean active = entry.uuid().equals(MinecraftClient.getInstance().getSession().getUuidOrNull());
            boolean switching = entry.uuid().equals(switchingAccount);
            int border = switching ? 0xFF9774D6 : active ? 0xFF42695D : 0xFF454054;
            int background = switching ? 0xFF252038 : active ? 0xFF182825 : 0xFF191823;
            roundedRect(context, panelX + 12, y, panelWidth - 24, 64, border);
            roundedRect(context, panelX + 13, y + 1, panelWidth - 26, 62, background);
            drawSkinHead(context, entry, panelX + 20, y + 8, 48);
            int nameY = switching ? y + 19 : y + 27;
            context.drawTextWithShadow(textRenderer, uiText(entry.name()), panelX + 80, nameY, active ? 0xFFBCF3DF : 0xFFF3EFFA);
            if (switching) {
                context.drawTextWithShadow(textRenderer, uiText("REFRESHING SESSION…"), panelX + 80, y + 37, 0xFFD1BEFF);
            }
            y += 70;
        }
    }

    private void drawSkinHead(DrawContext context, AccountStore.Entry entry, int x, int y, int size) {
        Identifier texture = skinTextures.getOrDefault(entry.uuid(), DefaultSkinHelper.getSkinTextures(entry.uuid()).body().texturePath());
        context.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xFF514065);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 8, 8, size, size, 64, 64);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 40, 8, size, size, 64, 64);
    }
}
