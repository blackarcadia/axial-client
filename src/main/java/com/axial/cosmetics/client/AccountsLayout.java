package com.axial.cosmetics.client;

/** Geometry shared by rendering and hit targets, in Minecraft's scaled GUI coordinates. */
final class AccountsLayout {
    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }

    final Rect panel, content, list, preview, search, close, addAccount, done, skin, previewAction;
    final boolean compact, tight;
    final int rowsY, rowHeight, pageSize, footerY, actionWidth;

    AccountsLayout(int width, int height) {
        int panelWidth = Math.min(560, (int) (width * 0.82));
        int panelHeight = Math.min(360, (int) (height * 0.80));
        panel = new Rect((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        compact = panelHeight < 300;
        tight = panelHeight < 185;
        int header = tight ? 32 : compact ? 46 : 64;
        int footer = tight ? 30 : compact ? 36 : 46;
        content = new Rect(panel.x() + 10, panel.y() + header, panelWidth - 20, panelHeight - header - footer);
        boolean showPreview = panelWidth >= 480 && content.height() >= 176;
        int listWidth = showPreview ? content.width() * 60 / 100 : content.width();
        list = new Rect(content.x(), content.y(), listWidth, content.height());
        preview = showPreview ? new Rect(list.right(), content.y(), content.width() - listWidth, content.height()) : null;
        search = new Rect(list.x() + 8, list.y() + (compact ? 5 : 28), list.width() - 16, tight ? 20 : 22);
        rowsY = list.y() + (tight ? 28 : compact ? 32 : 58);
        int available = list.bottom() - rowsY - 16;
        rowHeight = Math.min(tight ? 34 : compact ? 42 : 50, available);
        pageSize = Math.max(1, Math.min(3, available / rowHeight));
        actionWidth = compact ? 50 : 58;
        footerY = content.bottom();
        close = new Rect(panel.right() - 30, panel.y() + 6, 22, 22);
        int footerButtonHeight = tight ? 22 : compact ? 24 : 26;
        int doneWidth = panelWidth < 240 ? 50 : 64;
        done = new Rect(panel.right() - 12 - doneWidth, footerY + 6, doneWidth, footerButtonHeight);
        addAccount = new Rect(panel.x() + 12, footerY + 6,
                Math.min(166, done.x() - panel.x() - 20), footerButtonHeight);
        skin = preview == null ? null : new Rect(preview.x() + 8, preview.y() + 6, preview.width() - 16, preview.height() - 88);
        previewAction = preview == null ? null : new Rect(preview.x() + (preview.width() - 108) / 2,
                preview.bottom() - 24, 108, 20);
    }

    Rect row(int index) {
        return new Rect(list.x() + 7, rowsY + index * rowHeight, list.width() - 14, rowHeight - 4);
    }

    Rect rowMenu(Rect row) {
        int size = Math.min(22, row.height());
        return new Rect(row.right() - 24, row.y() + (row.height() - size) / 2, 20, size);
    }

    Rect rowAction(Rect row) {
        Rect menu = rowMenu(row);
        return new Rect(menu.x() - 4 - actionWidth, menu.y(), actionWidth, menu.height());
    }

    int controlsStart(Rect row, boolean active) {
        return (active ? rowMenu(row).x() : rowAction(row).x()) - 4;
    }

    Rect popover(Rect row) {
        return new Rect(row.right() - 114, Math.min(row.bottom() + 2, list.bottom() - 26), 114, 24);
    }
}
