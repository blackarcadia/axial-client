package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Keeps the Chroma row separate from hex entry and the footer on small windows. */
final class ColorPickerLayout {
    final Rect panel,shade,hue,hex,chroma,back,reset,done;
    ColorPickerLayout(int width,int height){
        int w=Math.min(360,width-20),h=Math.min(266,height-20);
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        shade=new Rect(panel.x()+14,panel.y()+38,w-62,Math.max(8,h-134));
        hue=new Rect(panel.right()-32,shade.y(),18,shade.height());
        hex=new Rect(panel.x()+14,shade.bottom()+9,w-28,22);
        chroma=new Rect(panel.x()+14,panel.bottom()-60,w-28,22);
        back=new Rect(panel.x()+10,panel.y()+10,44,20);
        reset=new Rect(panel.x()+14,panel.bottom()-28,66,20);
        done=new Rect(panel.right()-80,panel.bottom()-28,66,20);
    }
}
