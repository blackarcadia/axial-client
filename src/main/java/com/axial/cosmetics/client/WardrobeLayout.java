package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

final class WardrobeLayout {
    final Rect panel, close, categories, search, grid, preview, details, done;
    final int columns;
    WardrobeLayout(int width, int height) {
        int w = Math.min(760, Math.max(1, width - 20));
        int h = Math.min(410, Math.max(1, height - 20));
        panel = new Rect((width-w)/2, (height-h)/2, w, h);
        close = new Rect(panel.right()-27, panel.y()+9, 20, 20);
        int side = w >= 480 ? 108 : 64;
        int right = Math.max(82, w*35/100);
        int top = panel.y()+47, bottom = panel.bottom()-34;
        categories = new Rect(panel.x()+8, top, side-12, bottom-top);
        int centerX = panel.x()+side;
        int centerW = Math.max(38, w-side-right-12);
        search = new Rect(centerX, top+2, centerW, 20);
        grid = new Rect(centerX, search.bottom()+8, centerW, Math.max(24,bottom-search.bottom()-12));
        int rightX = grid.right()+8;
        int detailsHeight = h >= 240 ? 78 : 48;
        preview = new Rect(rightX, top, panel.right()-8-rightX, Math.max(16,bottom-top-detailsHeight));
        details = new Rect(preview.x(), preview.bottom()+2, preview.width(), detailsHeight-2);
        done = new Rect(panel.right()-63, panel.bottom()-26, 55, 19);
        columns = centerW >= 150 ? 2 : 1;
    }
    Rect category(int index) {
        int h = Math.min(27, Math.max(10,categories.height()/6));
        return new Rect(categories.x(),categories.y()+index*h,categories.width(),h-2);
    }
    Rect card(int index) {
        int gap = 6, w = (grid.width()-(columns-1)*gap)/columns;
        int h = Math.min(118,Math.max(12,(grid.height()-6)/(columns==1?2:1)));
        return new Rect(grid.x()+index%columns*(w+gap), grid.y()+index/columns*(h+gap), w,h);
    }

    /** Fit the visible cape face at its native 10:16 ratio, including compact rows. */
    static Rect capeImage(Rect card) {
        boolean compact=card.height()<55;
        int maxH=Math.max(1,card.height()-(compact?6:39));
        int maxW=Math.max(1,compact?card.width()/4:card.width()-16);
        int w=Math.max(1,Math.min(maxW,maxH*10/16));
        int h=Math.max(1,w*16/10);
        return new Rect(compact?card.x()+4:card.x()+(card.width()-w)/2,
                card.y()+3+(maxH-h)/2,w,h);
    }
}
