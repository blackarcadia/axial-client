package com.axial.cosmetics.client;

/** Shared geometry for the mod library, including clipped scrolling and hit targets. */
final class ModMenuLayout {
    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
    }
    final Rect panel, close, navigation, search, enabledFilter, categories, grid, scrollbar;
    final boolean sidebar, tight;
    final int columns, cardHeight, cardWidth, gap = 7;

    ModMenuLayout(int width, int height) {
        int w = Math.min(720, (int)(width * .82));
        int h = Math.min(304, height < 220 ? height-12 : Math.max(196,(int)(height * .68)));
        panel = new Rect((width-w)/2, (height-h)/2, w, h);
        tight = h < 190;
        sidebar = w >= 520 && h >= 260;
        int header = tight ? 26 : h < 320 ? 38 : 48;
        int navHeight = tight ? 20 : 24;
        close = new Rect(panel.right()-27, panel.y()+5, 20, 20);
        navigation = new Rect(panel.x()+10, panel.y()+header, w-20, navHeight);
        int toolbarY = navigation.bottom()+7;
        int sidebarWidth = sidebar ? 104 : 0;
        int contentX = panel.x()+10+sidebarWidth;
        int contentW = w-20-sidebarWidth;
        int filterW = sidebar ? 106 : tight ? 55 : 64;
        search = new Rect(contentX, toolbarY, contentW-filterW-6, tight ? 20 : 22);
        enabledFilter = new Rect(search.right()+6, toolbarY, filterW, search.height());
        categories = sidebar ? new Rect(panel.x()+10, toolbarY, 94, 144)
                : new Rect(contentX, search.bottom()+5, contentW, 20);
        int gridY = (sidebar || tight ? search.bottom() : categories.bottom())+7;
        grid = new Rect(contentX, gridY, contentW-8, panel.bottom()-19-gridY);
        columns = Math.max(1, Math.min(3, (grid.width()+gap)/137));
        cardWidth = (grid.width()-(columns-1)*gap)/columns;
        cardHeight = Math.min(sidebar ? Math.max(60,(grid.height()-gap)/2) : 68, grid.height());
        scrollbar = new Rect(grid.right()+4, grid.y(), 3, grid.height());
    }

    int contentHeight(int count) { return ((count+columns-1)/columns)*(cardHeight+gap)- (count == 0 ? 0 : gap); }
    int maxScroll(int count) { return Math.max(0, contentHeight(count)-grid.height()); }
    Rect card(int index, int scroll) {
        return new Rect(grid.x()+(index%columns)*(cardWidth+gap), grid.y()+(index/columns)*(cardHeight+gap)-scroll, cardWidth, cardHeight);
    }
    Rect toggle(Rect card) { return new Rect(card.x()+8, card.bottom()-22, card.width()-42, 17); }
    Rect settings(Rect card) { return new Rect(card.right()-27, card.bottom()-24, 20, 20); }
    Rect category(int index) {
        return sidebar ? new Rect(categories.x(), categories.y()+index*28, categories.width(), 24)
                : new Rect(categories.x()+index*(categories.width()/5), categories.y(), categories.width()/5-3, 20);
    }
}
