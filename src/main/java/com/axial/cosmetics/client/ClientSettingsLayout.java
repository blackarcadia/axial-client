package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Fixed navigation and footer around one scrollable settings column. */
final class ClientSettingsLayout {
    static final int CONTENT_HEIGHT = 640;
    final Rect panel, controls, back, settings, editHud, wardrobe, close, reset, done, scrollbar;
    final boolean compact;
    final int headerBottom;

    ClientSettingsLayout(int width, int height) {
        int w=Math.min(720,Math.max(1,(int)(width*.82))),h=Math.min(342,Math.max(1,height-20));
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        compact=h<260;
        int navWidth=Math.min(102,(w-68)/4);
        int navX=panel.x()+(w-navWidth*4)/2;
        int navY=panel.y()+10;
        back=new Rect(navX,navY,navWidth,24);
        settings=new Rect(back.right(),navY,navWidth,24);
        editHud=new Rect(settings.right(),navY,navWidth,24);
        wardrobe=new Rect(editHud.right(),navY,navWidth,24);
        close=new Rect(panel.right()-27,navY+2,20,20);
        headerBottom=panel.y()+42;
        int contentTop=panel.y()+(compact?49:91);
        int controlWidth=Math.min(480,w-40);
        controls=new Rect(panel.x()+(w-controlWidth)/2,contentTop,controlWidth,Math.max(40,panel.bottom()-40-contentTop));
        reset=new Rect(panel.x()+20,panel.bottom()-31,94,22);
        done=new Rect(panel.right()-88,panel.bottom()-31,68,22);
        scrollbar=new Rect(controls.right()+6,controls.y(),3,controls.height());
    }

    int maxScroll() { return Math.max(0,CONTENT_HEIGHT-controls.height()); }
    boolean fullyVisible(Rect r) { return r.y()>=controls.y()&&r.bottom()<=controls.bottom(); }
}
