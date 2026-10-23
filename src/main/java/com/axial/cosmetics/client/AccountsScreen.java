package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
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
    private boolean renderingParent;

    public AccountsScreen(Screen parent) {
        this(parent, AccountStore.shared());
    }

    AccountsScreen(Screen parent, AccountStore store) {
        super(uiText("Accounts"));
        this.parent = parent;
        this.store = store;
    }

    @Override protected void init() {
        if (parent != null) {
            if (parent.width != width || parent.height != height) parent.resize(width, height);
            parent.setFocused(null);
        }
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
        button("Close", layout.close, this::close, !busy, Tone.CLOSE);

        if (layout.preview != null && selectedEntry() != null) {
            var skin = new PlayerSkinWidget(layout.skin.width(), layout.skin.height(), client.getLoadedEntityModels(),
                    () -> skinFor(selectedEntry()));
            skin.setPosition(layout.skin.x(), layout.skin.y());
            skin.active = !busy;
            addDrawableChild(skin);
            var entry = selectedEntry();
            button(isActive(entry) ? "Log out" : reconnectAccounts.contains(entry.uuid()) ? "Reconnect" : "Switch account",
                    layout.previewAction, () -> { if (isActive(entry)) logoutActive(); else activate(entry); }, !busy, Tone.OUTLINE);
        }

        var box = layout.search;
        search = new TextFieldWidget(textRenderer, box.x() + 23, box.y() + (box.height() - 9) / 2, box.width() - 30, 12, uiText("Search accounts"));
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
            // Row selection is separate from switching the signed-in session.
            var card = new AccountButton(row.x(), row.y(), row.width(), row.height(), uiText("Preview " + entry.name()),
                    () -> { selectedAccount = entry.uuid(); menuAccount = null; rebuildWidgets(); }, Tone.CARD, entry);
            card.setTooltip(Tooltip.of(uiText(entry.name())));
            card.active = !busy;
            addDrawableChild(card);
            if (!isActive(entry)) {
                String label = entry.uuid().equals(switchingAccount) ? "Working..."
                        : reconnectAccounts.contains(entry.uuid()) ? (layout.compact ? "Sign in" : "Reconnect") : "Switch";
                button(label, layout.rowAction(row), () -> activate(entry), !busy, Tone.OUTLINE);
            }
            button("Account options for " + entry.name(), layout.rowMenu(row), () -> { menuAccount = entry.uuid().equals(menuAccount) ? null : entry.uuid(); rebuildWidgets(); },
                    !busy, isActive(entry) ? Tone.CHECK : Tone.MORE);
        }

        if (pages > 1) {
            button("Previous page", layout.list.x() + 12, layout.list.bottom() - 19, 22, 16,
                    () -> { page--; menuAccount = null; rebuildWidgets(); }, !busy && page > 0, Tone.PREVIOUS);
            button("Next page", layout.list.right() - 34, layout.list.bottom() - 19, 22, 16,
                    () -> { page++; menuAccount = null; rebuildWidgets(); }, !busy && page + 1 < pages, Tone.NEXT);
        }
        button("Add Microsoft account", layout.addAccount,
                () -> client.setScreen(new MicrosoftAccountLoginScreen(this)), !busy, Tone.PRIMARY);
        button("Done", layout.done, this::close, !busy, Tone.OUTLINE);

        // Draw last and handle first, so the popover cannot click through into another row.
        for (int i = 0; i < visible.size(); i++) {
            var entry = visible.get(i);
            if (!entry.uuid().equals(menuAccount)) continue;
            var row = layout.row(i);
            menuButton = button(isActive(entry) ? "Log out" : "Remove account", layout.popover(row),
                    () -> { if (isActive(entry)) logoutActive(); else remove(entry); }, !busy, Tone.DANGER);
        }
    }

    private AccountButton button(String label, AccountsLayout.Rect bounds, Runnable action, boolean enabled, Tone tone) {
        return button(label, bounds.x(), bounds.y(), bounds.width(), bounds.height(), action, enabled, tone);
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

    /** The menu keeps its own button theme while being drawn beneath this modal. */
    public TitleScreen renderedTitleScreen() {
        return renderingParent && parent instanceof TitleScreen title ? title : null;
    }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (parent == null) return;
        renderingParent = true;
        try {
            // Only the account manager receives input; the menu has no hover or focus.
            parent.renderBackground(context, -1, -1, delta);
            context.createNewRootLayer();
            parent.render(context, -1, -1, delta);
        } finally {
            renderingParent = false;
        }
    }

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
        if (!layout.tight) text(context, "AXIAL CLIENT", panel.x() + 13, panel.y() + 9, PURPLE, 0.75f);
        int titleY = panel.y() + (layout.tight ? 7 : layout.compact ? 20 : 24);
        text(context, "Accounts", panel.x() + 13, titleY, INK, layout.compact ? 1.35f : 1.8f, panel.width() - 50);
        text(context, status.isBlank() ? "Choose who you play as." : status, panel.x() + 13,
                panel.y() + (layout.tight ? 23 : layout.compact ? 36 : 47), MUTED, layout.compact ? 0.7f : 0.85f, panel.width() - 26);
        framed(context, layout.content.x(), layout.content.y(), layout.content.width(), layout.content.height(), 0xFF2B293A, 0xFF12121C);
        if (layout.preview != null) {
            context.fill(layout.preview.x(), layout.preview.y() + 1, layout.preview.x() + 1, layout.preview.bottom() - 1, 0xFF302C40);
            drawPreviewDetails(context);
        }
        if (!layout.compact) {
            text(context, "Saved accounts", layout.list.x() + 14, layout.list.y() + 9, INK, 1.0f);
            String count = entries.size() + (entries.size() == 1 ? " account" : " accounts");
            rightText(context, count, layout.list.right() - 14, layout.list.y() + 9, MUTED);
        }
        var box = layout.search;
        framed(context, box.x(), box.y(), box.width(), box.height(), search.isFocused() ? 0xFF9774D6 : 0xFF3C374D, 0xFF171620);
        icon(context, Icon.SEARCH, box.x() + 7, box.y() + (box.height() - 11) / 2, 11, 11, MUTED);
        List<AccountStore.Entry> filtered = filteredEntries();
        if (filtered.isEmpty()) {
            centeredText(context, entries.isEmpty() ? "No saved accounts yet" : "No matching accounts",
                    layout.list.x() + layout.list.width() / 2, layout.rowsY + 16, MUTED, 1.0f, layout.list.width() - 26);
        }
        int pages = Math.max(1, (filtered.size() + layout.pageSize - 1) / layout.pageSize);
        if (pages > 1) centeredText(context, (page + 1) + " / " + pages, layout.list.x() + layout.list.width() / 2,
                layout.list.bottom() - 14, MUTED, 0.8f, layout.list.width() - 80);
        if (!layout.compact) text(context, "Sign in securely with Microsoft.", panel.x() + 16, layout.footerY + 35, MUTED, 0.7f);
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
        centeredText(context, entry.name(), center, preview.bottom() - 73, INK, 1.15f, preview.width() - 20);
        String label = isActive(entry) ? "Active account" : reconnectAccounts.contains(entry.uuid()) ? "Sign in again" : "Saved account";
        int color = isActive(entry) ? GREEN : reconnectAccounts.contains(entry.uuid()) ? AMBER : PURPLE;
        int badgeWidth = Math.min(preview.width() - 24, textRenderer.getWidth(uiText(label)) + 22);
        roundedRect(context, center - badgeWidth / 2, preview.bottom() - 56, badgeWidth, 15, isActive(entry) ? 0xFF142B25 : 0xFF262034);
        centeredText(context, label, center, preview.bottom() - 53, color, 0.8f, badgeWidth - 8);
        centeredText(context, "Microsoft account", center, preview.bottom() - 36, MUTED, 0.75f, preview.width() - 20);
    }

    private static Text uiText(String value) { return Text.literal(value).styled(style -> style.withFont(UI_FONT)); }

    private static Text displayText(String value, float scale) {
        return Text.literal(value).styled(style -> style.withFont(scale > 1.0f ? HEADING_FONT : UI_FONT));
    }

    private void text(DrawContext context, String value, int x, int y, int color, float scale) {
        text(context, value, x, y, color, scale, Integer.MAX_VALUE / 4);
    }

    private void text(DrawContext context, String value, int x, int y, int color, float scale, int maxWidth) {
        if (maxWidth <= 0) return;
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

    private enum Icon {
        ROUNDED, CIRCLE, SEARCH, CLOSE, PLUS, CHECK, MORE, PREVIOUS, NEXT;
        final Identifier sprite = Identifier.of("axial_cosmetics", "accounts/" + name().toLowerCase(Locale.ROOT));
    }

    private static void icon(DrawContext context, Icon icon, int x, int y, int w, int h, int color) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, icon.sprite, x, y, w, h, color);
    }

    private static void roundedRect(DrawContext context, int x, int y, int w, int h, int color) {
        icon(context, w <= 8 && h <= 8 ? Icon.CIRCLE : Icon.ROUNDED, x, y, w, h, color);
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

        @Override public boolean isMouseOver(double mouseX, double mouseY) {
            // ParentElement chooses one hovered child before dispatching a click.
            // Exclude the overlaid controls here so they receive clicks themselves.
            if (tone == Tone.CARD && mouseX >= layout.controlsStart(
                    new AccountsLayout.Rect(getX(), getY(), width, height), isActive(entry))) return false;
            return super.isMouseOver(mouseX, mouseY);
        }

        @Override protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY();
            boolean hovered = active && (isHovered() || isFocused());
            if (tone == Tone.CARD) { drawCard(context, hovered); return; }
            if (tone == Tone.CLOSE) {
                icon(context, Icon.CLOSE, x + (width - 10) / 2, y + (height - 10) / 2, 10, 10, hovered ? INK : MUTED);
                return;
            }
            if (tone == Tone.MORE || tone == Tone.CHECK) {
                if (hovered) roundedRect(context, x, y, width, height, 0xFF30263F);
                icon(context, tone == Tone.MORE ? Icon.MORE : Icon.CHECK,
                        x + (width - 11) / 2, y + (height - 11) / 2, 11, 11, tone == Tone.CHECK ? GREEN : hovered ? INK : MUTED);
                return;
            }
            if (tone == Tone.PREVIOUS || tone == Tone.NEXT) {
                icon(context, tone == Tone.PREVIOUS ? Icon.PREVIOUS : Icon.NEXT,
                        x + (width - 10) / 2, y + (height - 10) / 2, 10, 10, active ? PURPLE : 0xFF494356);
                return;
            }
            int border = tone == Tone.PRIMARY ? PURPLE : tone == Tone.DANGER ? 0xFF9D536A : hovered ? PURPLE : 0xFF665079;
            int fill = tone == Tone.PRIMARY ? hovered ? 0xFFD1A1FF : 0xFFB985F0 : hovered ? 0xFF292036 : 0xFF18151F;
            framed(context, x, y, width, height, border, active ? fill : 0xFF1C1923);
            int foreground = !active ? 0xFF676173 : tone == Tone.PRIMARY ? 0xFF170D24 : tone == Tone.DANGER ? 0xFFFFB2C5 : PURPLE;
            int labelCenter = x + width / 2;
            if (tone == Tone.PRIMARY) {
                icon(context, Icon.PLUS, x + 7, y + (height - 10) / 2, 10, 10, foreground);
                labelCenter += 7;
            }
            String label = tone == Tone.PRIMARY && width < 140 ? "Add account" : getMessage().getString();
            centeredText(context, label, labelCenter, y + (height - textRenderer.fontHeight) / 2,
                    foreground, 1.0f, width - (tone == Tone.PRIMARY ? 28 : 12));
        }

        private void drawCard(DrawContext context, boolean hovered) {
            boolean selected = entry.uuid().equals(selectedAccount);
            boolean switching = entry.uuid().equals(switchingAccount);
            framed(context, getX(), getY(), width, height, selected ? 0xFFBD87F5 : hovered ? 0xFF665079 : 0xFF302B3D,
                    selected ? 0xFF272033 : hovered ? 0xFF201B2B : 0xFF191720);
            int headSize = Math.max(8, Math.min(layout.compact ? 24 : 28, height - 10));
            drawSkinHead(context, entry, getX() + 8, getY() + (height - headSize) / 2, headSize);
            int labelX = getX() + headSize + 16;
            int available = layout.controlsStart(new AccountsLayout.Rect(getX(), getY(), width, height), isActive(entry)) - labelX - 5;
            boolean twoLines = height >= 24;
            text(context, entry.name(), labelX, getY() + (twoLines ? height / 2 - 10 : (height - 9) / 2), INK, 1.0f, available);
            if (twoLines) {
                String state = switching ? "Signing in..." : isActive(entry) ? "Currently playing"
                        : reconnectAccounts.contains(entry.uuid()) ? "Sign in again" : "Ready to switch";
                int color = switching ? PURPLE : isActive(entry) ? GREEN : reconnectAccounts.contains(entry.uuid()) ? AMBER : MUTED;
                roundedRect(context, labelX, getY() + height / 2 + 5, 5, 5, color);
                text(context, state, labelX + 9, getY() + height / 2 + 3, color, 0.75f, available - 9);
            }
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
