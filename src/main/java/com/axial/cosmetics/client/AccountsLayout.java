package com.axial.cosmetics.client;

/** Geometry in Minecraft's scaled GUI coordinates, shared by drawing and hit targets. */
final class AccountsLayout {
    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }

    final Rect panel, content, list, preview, search;
    final boolean compact;
    final int rowsY, rowHeight, pageSize, footerY;

    AccountsLayout(int width, int height) {
        int panelWidth = Math.min(760, width - 24);
        int panelHeight = Math.min(480, height - 24);
        panel = new Rect((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        compact = panelHeight < 340;
        int header = compact ? 54 : 84;
        int footer = compact ? 42 : 60;
        content = new Rect(panel.x() + 12, panel.y() + header, panelWidth - 24, panelHeight - header - footer);
        boolean showPreview = panelWidth >= 460 && content.height() >= 170;
        int listWidth = showPreview ? content.width() * 61 / 100 : content.width();
        list = new Rect(content.x(), content.y(), listWidth, content.height());
        preview = showPreview ? new Rect(list.right(), content.y(), content.width() - listWidth, content.height()) : null;
        search = new Rect(list.x() + 12, list.y() + (compact ? 6 : 36), list.width() - 24, 24);
        rowsY = list.y() + (compact ? 38 : 70);
        rowHeight = compact ? 46 : 62;
        pageSize = Math.max(1, Math.min(4, (list.bottom() - rowsY - 18) / rowHeight));
        footerY = content.bottom();
    }

    Rect row(int index) {
        return new Rect(list.x() + 10, rowsY + index * rowHeight, list.width() - 20, rowHeight - 6);
    }
}
