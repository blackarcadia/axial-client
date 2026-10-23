package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Configure screens keep their header and footer outside the scrolling controls. */
final class FeatureSettingsLayout {
    final Rect panel, controls, preview, back, close, enabled, reset, done, scrollbar;

    FeatureSettingsLayout(int width, int height, boolean hasPreview) {
        int w = Math.min(624, Math.max(1, width - 20));
        int h = Math.min(304, Math.max(1, height - 20));
        panel = new Rect((width-w)/2, (height-h)/2, w, h);
        int top = panel.y()+42, bottom = panel.bottom()-34;
        boolean split = hasPreview && w >= 480 && h >= 210;
        int previewWidth = split ? Math.min(186, w/3) : 0;
        controls = new Rect(panel.x()+12, top, w-30-(split ? previewWidth+14 : 0), Math.max(1,bottom-top));
        preview = split ? new Rect(panel.right()-12-previewWidth, top, previewWidth, controls.height()) : null;
        back = new Rect(panel.x()+8,panel.y()+10,48,22);
        close = new Rect(panel.right()-27,panel.y()+10,19,22);
        enabled = new Rect(panel.right()-(w<400?96:111),panel.y()+10,w<400?64:79,22);
        reset = new Rect(panel.x()+12,panel.bottom()-26,106,19);
        done = new Rect(panel.right()-78,panel.bottom()-26,66,19);
        scrollbar = new Rect(controls.right()+3,controls.y(),3,controls.height());
    }

    int maxScroll(int contentHeight) { return Math.max(0,contentHeight-controls.height()); }
    boolean fullyVisible(Rect r) { return r.y() >= controls.y() && r.bottom() <= controls.bottom(); }
}
