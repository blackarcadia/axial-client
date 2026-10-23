package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Same map square is used for painting, pointer projection and hit testing. */
final class NavigationLayout {
    final Rect panel,sidebar,list,map,search;
    NavigationLayout(int width,int height) {
        int w=Math.max(240,width-20),h=Math.max(140,height-20);
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        int side=Math.min(190,Math.max(116,w/3));
        sidebar=new Rect(panel.x()+10,panel.y()+60,side,h-94);
        search=new Rect(sidebar.x(),sidebar.y(),side,24);
        list=new Rect(sidebar.x(),search.bottom()+6,side,Math.max(1,sidebar.height()-30));
        int space=w-side-34,size=Math.max(1,Math.min(space,h-88));
        map=new Rect(sidebar.right()+12+(space-size)/2,panel.y()+60+(h-88-size)/2,size,size);
    }
    int maxScroll(int count){return Math.max(0,count*43-list.height());}
    Rect row(int index,int scroll){return new Rect(list.x(),list.y()+index*43-scroll,list.width(),39);}
}
