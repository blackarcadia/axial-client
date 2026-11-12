package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Two visible result rows; the full voting menu retains every option. */
final class PollHudLayout {
    static final int HEIGHT=90;
    static int width(int screen){return Math.min(228,Math.max(1,screen-12));}
    static Rect option(Rect box,int index){return new Rect(box.x()+9,box.y()+40+index*18,Math.max(1,box.width()-18),16);}
    static Rect bar(Rect row){return new Rect(row.x(),row.y()+10,row.width(),3);}
    private PollHudLayout(){}
}
