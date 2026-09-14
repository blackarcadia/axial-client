package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AccountsScreen extends Screen {
    private final Screen parent;
    private final AccountStore store = AccountStore.shared();
    private List<AccountStore.Entry> entries = List.of();
    private String status = "Add Microsoft accounts, then select one to play.";
    private boolean busy;
    private int page;
    private int pageSize;
    private int panelX, panelY, panelWidth, panelHeight;

    public AccountsScreen(Screen parent) {
        super(Text.literal("ACCOUNTS"));
        this.parent = parent;
    }

    @Override protected void init() {
        try { entries = store.list(); }
        catch (Exception ex) { status = "Could not read saved accounts."; }
        panelWidth = Math.min(420, width - 24);
        pageSize = Math.max(1, (height - 160) / 30);
        panelHeight = Math.min(height - 16, 144 + pageSize * 30);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        page = Math.min(page, Math.max(0, (entries.size() - 1) / pageSize));
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearChildren();
        button("Log out", panelX + panelWidth - 82, panelY + 23, 70,
                this::logoutActive, !busy && AccountSessions.isSignedIn());
        int y = panelY + 58;
        for (var entry : entries.stream().skip((long) page * pageSize).limit(pageSize).toList()) {
            boolean active = entry.uuid().equals(MinecraftClient.getInstance().getSession().getUuidOrNull());
            button(entry.name() + (active ? "  [ACTIVE]" : "  • Switch"), panelX + 12, y, panelWidth - 100,
                    () -> switchAccount(entry), !busy && !active);
            button(active ? "Log out" : "Remove", panelX + panelWidth - 82, y, 70, () -> {
                if (active) { logoutActive(); return; }
                try { store.remove(entry); status = "Removed " + entry.name() + " from saved accounts."; init(); }
                catch (Exception ex) { status = "Could not remove account."; }
            }, !busy);
            y += 30;
        }
        int footer = panelY + panelHeight - 64;
        button("<", panelX + 12, footer, 28, () -> { page--; rebuildButtons(); }, !busy && page > 0);
        button(">", panelX + panelWidth - 40, footer, 28, () -> { page++; rebuildButtons(); }, !busy && (page + 1) * pageSize < entries.size());
        button("Add Microsoft account", panelX + 12, footer + 30, panelWidth - 112,
                () -> client.setScreen(new MicrosoftAccountLoginScreen(this)), !busy);
        button("Done", panelX + panelWidth - 88, footer + 30, 76, this::close, !busy);
    }

    private void button(String label, int x, int y, int w, Runnable action, boolean enabled) {
        var button = ButtonWidget.builder(Text.literal(label), ignored -> action.run()).dimensions(x, y, w, 20).build();
        button.active = enabled;
        addDrawableChild(button);
    }

    private void switchAccount(AccountStore.Entry entry) {
        if (busy) return;
        busy = true;
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
            finally { busy = false; init(); }
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
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xED101018);
        context.drawStrokedRectangle(panelX, panelY, panelWidth, panelHeight, 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 12, 0xFFFFFFFF);
        String accountLabel = AccountSessions.isSignedIn()
                ? "Playing as " + MinecraftClient.getInstance().getSession().getUsername() : "Not signed in";
        context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(accountLabel, panelWidth - 108)), panelX + 12, panelY + 28, 0xFFBFA0FF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(status, panelWidth - 20)), width / 2, panelY + 42, 0xFFCCD0DD);
        if (entries.isEmpty()) context.drawCenteredTextWithShadow(textRenderer, Text.literal("No saved accounts. Add one below."), width / 2, panelY + 70, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal((page + 1) + " / " + Math.max(1, (entries.size() + pageSize - 1) / pageSize)), width / 2, panelY + panelHeight - 58, 0xFFCCD0DD);
        super.render(context, mouseX, mouseY, delta);
    }
}
