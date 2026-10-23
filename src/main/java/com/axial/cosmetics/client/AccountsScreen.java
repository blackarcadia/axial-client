package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;

import java.net.Proxy;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class AccountsScreen extends Screen {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axial_cosmetics", "accounts"));
    private static final StyleSpriteSource.Font HEADING_FONT = new StyleSpriteSource.Font(Identifier.of("axial_cosmetics", "accounts_bold"));
    private static final int INK = 0xFFF4F0FA, MUTED = 0xFF9693AB, PURPLE = 0xFFC18AFF;
    private static final int GREEN = 0xFF69E5AA, AMBER = 0xFFFFC15C, PANEL = 0xFF101019;

    private final Screen parent;
    private final AccountStore store;
    private final Map<UUID, SkinTextures> skins = new ConcurrentHashMap<>();
    private final Set<UUID> reconnectAccounts = new HashSet<>();
    private List<AccountStore.Entry> entries = List.of();
    private String status = "";
    private String query = "";
    private boolean busy;
    private UUID switchingAccount, selectedAccount, menuAccount;
    private int page;
    private AccountsLayout layout;
    private TextFieldWidget search;
    private AccountButton menuButton;

    public AccountsScreen(Screen parent) {
        this(parent, AccountStore.shared());
    }

    AccountsScreen(Screen parent, AccountStore store) {
        super(uiText("Accounts"));
        this.parent = parent;
        this.store = store;
    }

    @Override protected void init() {
        try {
            entries = store.list().stream().sorted(java.util.Comparator.comparing((AccountStore.Entry entry) -> !isActive(entry))).toList();
        }
        catch (Exception ex) { entries = List.of(); status = "Could not read saved accounts."; }
        layout = new AccountsLayout(width, height);
        if (selectedEntry() == null) {
            selectedAccount = entries.stream().filter(this::isActive).findFirst()
                    .or(() -> entries.stream().findFirst()).map(AccountStore.Entry::uuid).orElse(null);
        }
        entries.forEach(this::requestSkin);
        rebuildWidgets();
    }

    private List<AccountStore.Entry> filteredEntries() {
        String filter = query.strip().toLowerCase(Locale.ROOT);
        return entries.stream().filter(e -> e.name().toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    private AccountStore.Entry selectedEntry() {
        return entries.stream().filter(e -> e.uuid().equals(selectedAccount)).findFirst().orElse(null);
    }

    private boolean isActive(AccountStore.Entry entry) {
        return AccountSessions.isSignedIn() && entry.uuid().equals(client.getSession().getUuidOrNull());
    }

    private void rebuildWidgets() {
        boolean restoreSearchFocus = search != null && search.isFocused();
        int cursor = search == null ? query.length() : search.getCursor();
        clearChildren();
        menuButton = null;
        var panel = layout.panel;
        button("Close", panel.right() - 36, panel.y() + 10, 24, 24, this::close, !busy, Tone.CLOSE);

        if (layout.preview != null && selectedEntry() != null) {
            var preview = layout.preview;
            int modelHeight = preview.height() - 100;
            var skin = new PlayerSkinWidget(preview.width() - 20, modelHeight, client.getLoadedEntityModels(),
                    () -> skinFor(selectedEntry()));
            skin.setPosition(preview.x() + 10, preview.y() + 8);
            skin.active = !busy;
            addDrawableChild(skin);
            var entry = selectedEntry();
            button(isActive(entry) ? "Log out" : reconnectAccounts.contains(entry.uuid()) ? "Reconnect" : "Switch account",
                    preview.x() + (preview.width() - 112) / 2, preview.bottom() - 32, 112, 24,
                    () -> { if (isActive(entry)) logoutActive(); else activate(entry); }, !busy, Tone.OUTLINE);
        }

        var box = layout.search;
        search = new TextFieldWidget(textRenderer, box.x() + 25, box.y() + 7, box.width() - 33, 16, uiText("Search accounts"));
        search.setMaxLength(64);
        search.setDrawsBackground(false);
        search.setTextShadow(false);
        search.setEditableColor(INK);
        search.setPlaceholder(uiText("Search accounts"));
        search.addFormatter((value, index) -> uiText(value).asOrderedText());
        search.setText(query);
        search.setCursor(Math.min(cursor, query.length()), false);
        search.setEditable(!busy);
        search.setChangedListener(value -> {
            query = value;
            page = 0;
            menuAccount = null;
            rebuildWidgets();
        });
        addDrawableChild(search);
        if (restoreSearchFocus) setFocused(search);

        List<AccountStore.Entry> filtered = filteredEntries();
        int pages = Math.max(1, (filtered.size() + layout.pageSize - 1) / layout.pageSize);
        page = Math.min(page, pages - 1);
        List<AccountStore.Entry> visible = filtered.stream().skip((long) page * layout.pageSize).limit(layout.pageSize).toList();
        for (int i = 0; i < visible.size(); i++) {
            var entry = visible.get(i);
            var row = layout.row(i);
            int actionWidth = layout.compact ? 56 : 72;
            // Row selection is separate from switching the signed-in session.
            var card = new AccountButton(row.x(), row.y(), row.width(), row.height(), uiText("Preview " + entry.name()),
                    () -> { selectedAccount = entry.uuid(); menuAccount = null; rebuildWidgets(); }, Tone.CARD, entry);
            card.active = !busy;
            addDrawableChild(card);
            if (!isActive(entry)) {
                String label = entry.uuid().equals(switchingAccount) ? "Working..."
                        : reconnectAccounts.contains(entry.uuid()) ? "Reconnect" : "Switch";
                button(label, row.right() - actionWidth - 30, row.y() + (row.height() - 24) / 2, actionWidth, 24,
                        () -> activate(entry), !busy, Tone.OUTLINE);
            }
            button("Account options for " + entry.name(), row.right() - 27, row.y() + (row.height() - 24) / 2,
                    22, 24, () -> { menuAccount = entry.uuid().equals(menuAccount) ? null : entry.uuid(); rebuildWidgets(); },
                    !busy, isActive(entry) ? Tone.CHECK : Tone.MORE);
        }

        if (pages > 1) {
            button("Previous page", layout.list.x() + 12, layout.list.bottom() - 19, 22, 16,
                    () -> { page--; menuAccount = null; rebuildWidgets(); }, !busy && page > 0, Tone.PREVIOUS);
            button("Next page", layout.list.right() - 34, layout.list.bottom() - 19, 22, 16,
                    () -> { page++; menuAccount = null; rebuildWidgets(); }, !busy && page + 1 < pages, Tone.NEXT);
        }
        int buttonHeight = layout.compact ? 26 : 30;
        int addWidth = Math.min(186, panel.width() - 108);
        button("Add Microsoft account", panel.x() + 14, layout.footerY + 8, addWidth, buttonHeight,
                () -> client.setScreen(new MicrosoftAccountLoginScreen(this)), !busy, Tone.PRIMARY);
        button("Done", panel.right() - 94, layout.footerY + 8, 80, buttonHeight, this::close, !busy, Tone.OUTLINE);

        // Draw last and handle first, so the popover cannot click through into another row.
        for (int i = 0; i < visible.size(); i++) {
            var entry = visible.get(i);
            if (!entry.uuid().equals(menuAccount)) continue;
            var row = layout.row(i);
            int y = Math.min(row.bottom() + 2, layout.list.bottom() - 32);
            menuButton = button(isActive(entry) ? "Log out" : "Remove account", row.right() - 120, y, 120, 28,
                    () -> { if (isActive(entry)) logoutActive(); else remove(entry); }, !busy, Tone.DANGER);
        }
    }

    private AccountButton button(String label, int x, int y, int width, int height, Runnable action, boolean enabled, Tone tone) {
        var button = new AccountButton(x, y, width, height, uiText(label), action, tone, null);
        button.active = enabled;
        return addDrawableChild(button);
    }

    private void activate(AccountStore.Entry entry) {
        if (busy) return;
        if (reconnectAccounts.contains(entry.uuid())) client.setScreen(new MicrosoftAccountLoginScreen(this));
        else switchAccount(entry);
    }

    private void remove(AccountStore.Entry entry) {
        if (busy) return;
        try { store.remove(entry); status = "Removed " + entry.name() + " from saved accounts."; }
        catch (Exception ex) { status = "Could not remove account."; }
        menuAccount = null;
        init();
    }

    private SkinTextures skinFor(AccountStore.Entry entry) {
        UUID uuid = entry == null ? new UUID(0, 0) : entry.uuid();
        return skins.getOrDefault(uuid, DefaultSkinHelper.getSkinTextures(uuid));
    }

    private void requestSkin(AccountStore.Entry entry) {
        if (skins.putIfAbsent(entry.uuid(), DefaultSkinHelper.getSkinTextures(entry.uuid())) != null) return;
        CompletableFuture.supplyAsync(() -> {
            try {
                var result = new YggdrasilAuthenticationService(Proxy.NO_PROXY).createMinecraftSessionService().fetchProfile(entry.uuid(), true);
                return result == null ? null : result.profile();
            } catch (Exception ignored) { return null; }
        }).thenAccept(profile -> {
            if (profile == null) return;
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().getSkinProvider().fetchSkinTextures(profile)
                    .thenAccept(skin -> skin.ifPresent(value -> skins.put(entry.uuid(), value))));
        });
    }

    // Intentionally no panorama, scenery, blur, or full-screen backdrop.
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (menuButton != null) {
            if (menuButton.mouseClicked(click, doubled)) return true;
            menuAccount = null;
            rebuildWidgets();
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public void close() { if (!busy) client.setScreen(parent); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var panel = layout.panel;
        framed(context, panel.x(), panel.y(), panel.width(), panel.height(), 0xFF57416D, PANEL);
        text(context, "AXIAL CLIENT", panel.x() + 16, panel.y() + 12, PURPLE, 0.85f);
        text(context, "Accounts", panel.x() + 16, panel.y() + (layout.compact ? 26 : 31), INK, layout.compact ? 1.55f : 2.3f);
        text(context, status.isBlank() ? "Choose who you play as." : status, panel.x() + 16,
                panel.y() + (layout.compact ? 43 : 62), MUTED, layout.compact ? 0.8f : 1.0f, panel.width() - 38);
        framed(context, layout.content.x(), layout.content.y(), layout.content.width(), layout.content.height(), 0xFF2B293A, 0xFF12121C);
        if (layout.preview != null) {
            context.fill(layout.preview.x(), layout.preview.y() + 1, layout.preview.x() + 1, layout.preview.bottom() - 1, 0xFF302C40);
            drawPreviewDetails(context);
        }
        if (!layout.compact) {
            text(context, "Saved accounts", layout.list.x() + 14, layout.list.y() + 13, INK, 1.15f);
            String count = entries.size() + (entries.size() == 1 ? " account" : " accounts");
            rightText(context, count, layout.list.right() - 14, layout.list.y() + 15, MUTED);
        }
        var box = layout.search;
        framed(context, box.x(), box.y(), box.width(), box.height(), search.isFocused() ? 0xFF9774D6 : 0xFF3C374D, 0xFF171620);
        context.drawStrokedRectangle(box.x() + 9, box.y() + 8, 6, 6, MUTED);
        context.fill(box.x() + 15, box.y() + 14, box.x() + 18, box.y() + 16, MUTED);
        List<AccountStore.Entry> filtered = filteredEntries();
        if (filtered.isEmpty()) {
            centeredText(context, entries.isEmpty() ? "No saved accounts yet" : "No matching accounts",
                    layout.list.x() + layout.list.width() / 2, layout.rowsY + 16, MUTED, 1.0f, layout.list.width() - 26);
        }
        int pages = Math.max(1, (filtered.size() + layout.pageSize - 1) / layout.pageSize);
        if (pages > 1) centeredText(context, (page + 1) + " / " + pages, layout.list.x() + layout.list.width() / 2,
                layout.list.bottom() - 14, MUTED, 0.8f, layout.list.width() - 80);
        if (!layout.compact) text(context, "Sign in securely with Microsoft.", panel.x() + 16, layout.footerY + 44, MUTED, 0.8f);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPreviewDetails(DrawContext context) {
        var preview = layout.preview;
        var entry = selectedEntry();
        int center = preview.x() + preview.width() / 2;
        if (entry == null) {
            centeredText(context, "Your next adventure", center, preview.y() + preview.height() / 2 - 8, INK, 1.1f, preview.width() - 20);
            centeredText(context, "starts with an account.", center, preview.y() + preview.height() / 2 + 10, MUTED, 0.9f, preview.width() - 20);
            return;
        }
        centeredText(context, entry.name(), center, preview.bottom() - 89, INK, 1.35f, preview.width() - 20);
        String label = isActive(entry) ? "Active account" : reconnectAccounts.contains(entry.uuid()) ? "Sign in again" : "Saved account";
        int color = isActive(entry) ? GREEN : reconnectAccounts.contains(entry.uuid()) ? AMBER : PURPLE;
        int badgeWidth = Math.min(preview.width() - 24, textRenderer.getWidth(uiText(label)) + 22);
        roundedRect(context, center - badgeWidth / 2, preview.bottom() - 67, badgeWidth, 17, isActive(entry) ? 0xFF142B25 : 0xFF262034);
        centeredText(context, label, center, preview.bottom() - 63, color, 0.9f, badgeWidth - 8);
        centeredText(context, "Microsoft account", center, preview.bottom() - 45, MUTED, 0.85f, preview.width() - 20);
    }

    private static Text uiText(String value) { return Text.literal(value).styled(style -> style.withFont(UI_FONT)); }

    private static Text displayText(String value, float scale) {
        return Text.literal(value).styled(style -> style.withFont(scale > 1.0f ? HEADING_FONT : UI_FONT));
    }

    private void text(DrawContext context, String value, int x, int y, int color, float scale) {
        text(context, value, x, y, color, scale, Integer.MAX_VALUE / 4);
    }

    private void text(DrawContext context, String value, int x, int y, int color, float scale, int maxWidth) {
        Text label = displayText(textRenderer.trimToWidth(displayText(value, scale), (int) (maxWidth / scale)).getString(), scale);
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        context.drawText(textRenderer, label, 0, 0, color, false);
        matrices.popMatrix();
    }

    private void centeredText(DrawContext context, String value, int center, int y, int color, float scale, int maxWidth) {
        String trimmed = textRenderer.trimToWidth(displayText(value, scale), (int) (maxWidth / scale)).getString();
        text(context, trimmed, center - Math.round(textRenderer.getWidth(displayText(trimmed, scale)) * scale / 2), y, color, scale);
    }

    private void rightText(DrawContext context, String value, int right, int y, int color) {
        text(context, value, right - textRenderer.getWidth(uiText(value)), y, color, 1.0f);
    }

    private static void roundedRect(DrawContext context, int x, int y, int w, int h, int color) {
        if (w <= 8 || h <= 8) { context.fill(x, y, x + w, y + h, color); return; }
        context.fill(x + 4, y, x + w - 4, y + h, color);
        context.fill(x + 2, y + 1, x + w - 2, y + h - 1, color);
        context.fill(x + 1, y + 2, x + w - 1, y + h - 2, color);
        context.fill(x, y + 4, x + w, y + h - 4, color);
    }

    private static void framed(DrawContext context, int x, int y, int w, int h, int border, int fill) {
        roundedRect(context, x, y, w, h, border);
        roundedRect(context, x + 1, y + 1, w - 2, h - 2, fill);
    }

    private void drawSkinHead(DrawContext context, AccountStore.Entry entry, int x, int y, int size) {
        var texture = skinFor(entry).body().texturePath();
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 8, 8, size, size, 8, 8, 64, 64);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 40, 8, size, size, 8, 8, 64, 64);
    }

    private enum Tone { OUTLINE, PRIMARY, DANGER, CLOSE, MORE, CHECK, CARD, PREVIOUS, NEXT }

    /** Keeps native focus, activation, and narration with screen-local rendering. */
    private final class AccountButton extends ButtonWidget {
        private final Tone tone;
        private final AccountStore.Entry entry;

        private AccountButton(int x, int y, int width, int height, net.minecraft.text.Text label, Runnable action, Tone tone, AccountStore.Entry entry) {
            super(x, y, width, height, label, ignored -> action.run(), DEFAULT_NARRATION_SUPPLIER);
            this.tone = tone;
            this.entry = entry;
        }

        @Override public boolean mouseClicked(Click click, boolean doubled) {
            int controlsWidth = entry == null || isActive(entry) ? 30 : (layout.compact ? 56 : 72) + 36;
            if (tone == Tone.CARD && click.x() >= getRight() - controlsWidth) return false;
            return super.mouseClicked(click, doubled);
        }

        @Override protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY();
            boolean hovered = active && (isHovered() || isFocused());
            if (tone == Tone.CARD) { drawCard(context, hovered); return; }
            if (tone == Tone.CLOSE) {
                int color = hovered ? INK : MUTED;
                for (int i = 0; i < 9; i++) {
                    context.fill(x + 7 + i, y + 7 + i, x + 8 + i, y + 8 + i, color);
                    context.fill(x + 15 - i, y + 7 + i, x + 16 - i, y + 8 + i, color);
                }
                return;
            }
            if (tone == Tone.MORE || tone == Tone.CHECK) {
                if (hovered) roundedRect(context, x, y, width, height, 0xFF30263F);
                if (tone == Tone.MORE) {
                    for (int i = 0; i < 3; i++) context.fill(x + 10, y + 6 + i * 5, x + 12, y + 8 + i * 5, hovered ? INK : MUTED);
                } else centeredText(context, "✓", x + width / 2, y + 7, GREEN, 1.0f, width);
                return;
            }
            if (tone == Tone.PREVIOUS || tone == Tone.NEXT) {
                centeredText(context, tone == Tone.PREVIOUS ? "‹" : "›", x + width / 2, y + 3,
                        active ? PURPLE : 0xFF494356, 1.0f, width);
                return;
            }
            int border = tone == Tone.PRIMARY ? PURPLE : tone == Tone.DANGER ? 0xFF9D536A : hovered ? PURPLE : 0xFF665079;
            int fill = tone == Tone.PRIMARY ? hovered ? 0xFFD1A1FF : 0xFFB985F0 : hovered ? 0xFF292036 : 0xFF18151F;
            framed(context, x, y, width, height, border, active ? fill : 0xFF1C1923);
            int foreground = !active ? 0xFF676173 : tone == Tone.PRIMARY ? 0xFF170D24 : tone == Tone.DANGER ? 0xFFFFB2C5 : PURPLE;
            int labelCenter = x + width / 2;
            if (tone == Tone.PRIMARY) {
                context.fill(x + 11, y + height / 2 - 4, x + 12, y + height / 2 + 5, foreground);
                context.fill(x + 7, y + height / 2, x + 16, y + height / 2 + 1, foreground);
                labelCenter += 7;
            }
            centeredText(context, getMessage().getString(), labelCenter, y + (height - textRenderer.fontHeight) / 2,
                    foreground, 1.0f, width - (tone == Tone.PRIMARY ? 28 : 12));
        }

        private void drawCard(DrawContext context, boolean hovered) {
            boolean selected = entry.uuid().equals(selectedAccount);
            boolean switching = entry.uuid().equals(switchingAccount);
            framed(context, getX(), getY(), width, height, selected ? 0xFFBD87F5 : hovered ? 0xFF665079 : 0xFF302B3D,
                    selected ? 0xFF272033 : hovered ? 0xFF201B2B : 0xFF191720);
            int headSize = Math.min(32, height - 14);
            drawSkinHead(context, entry, getX() + 10, getY() + (height - headSize) / 2, headSize);
            int labelX = getX() + headSize + 20;
            int available = width - headSize - 58 - (isActive(entry) ? 0 : layout.compact ? 56 : 72);
            text(context, entry.name(), labelX, getY() + height / 2 - 10, INK, 1.05f, available);
            String state = switching ? "Refreshing session..." : isActive(entry) ? "Currently playing"
                    : reconnectAccounts.contains(entry.uuid()) ? "Sign in again" : "Ready to switch";
            int color = switching ? PURPLE : isActive(entry) ? GREEN : reconnectAccounts.contains(entry.uuid()) ? AMBER : MUTED;
            roundedRect(context, labelX, getY() + height / 2 + 6, 5, 5, color);
            text(context, state, labelX + 10, getY() + height / 2 + 3, color, 0.85f, available - 10);
        }
    }
    private void switchAccount(AccountStore.Entry entry) {
        if (busy) return;
        if (client.world != null || client.getNetworkHandler() != null) {
            status = "Return to the main menu before switching accounts.";
            return;
        }
        selectedAccount = entry.uuid();
        busy = true;
        switchingAccount = entry.uuid();
        status = "Signing in as " + entry.name() + "...";
        rebuildWidgets();
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
                    reconnectAccounts.add(entry.uuid());
                    status = "Sign-in expired. Reconnect this Microsoft account.";
                } else {
                    prepared.apply();
                    reconnectAccounts.remove(entry.uuid());
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


}
