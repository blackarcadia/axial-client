package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** The wordmark and supplied mark share a vertical centre at every GUI scale. */
final class MenuHeaderLayout {
    final Rect mark;
    final int textX,textWidth;
    MenuHeaderLayout(Rect area){
        int h=Math.min(18,Math.max(1,area.height()));
        int w=Math.min(Math.max(1,area.width()/3),(int)Math.round(h*512.0/302));
        h=Math.min(h,Math.max(1,(int)Math.round(w*302.0/512)));
        mark=new Rect(area.x(),area.y()+(area.height()-h)/2,w,h);
        textX=mark.right()+Math.min(6,Math.max(2,area.width()/24));textWidth=Math.max(0,area.right()-textX);
    }
    int textY(int fontHeight){return mark.y()+(mark.height()-fontHeight)/2;}
}
