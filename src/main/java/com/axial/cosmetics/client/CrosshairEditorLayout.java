package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

final class CrosshairEditorLayout {
    final Rect panel,controls,preview,presets,customize,back,enabled,close,reset,undo,done,scrollbar;
    CrosshairEditorLayout(int width,int height){
        int w=Math.min(760,Math.max(1,width-20)),h=Math.min(420,Math.max(1,height-20));
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        int top=panel.y()+42,bottom=panel.bottom()-34;
        boolean split=w>=560&&h>=240;
        int previewWidth=split?Math.min(224,w/3):0;
        int leftWidth=w-30-(split?previewWidth+14:0);
        if(split)preview=new Rect(panel.right()-12-previewWidth,top,previewWidth,bottom-top);
        else if(h>=270){preview=new Rect(panel.x()+12,top,leftWidth,64);top+=70;}
        else preview=null;
        presets=new Rect(panel.x()+12,top,(leftWidth-6)/2,22);
        customize=new Rect(presets.right()+6,top,leftWidth-presets.width()-6,22);
        controls=new Rect(panel.x()+12,top+28,leftWidth,Math.max(1,bottom-top-28));
        back=new Rect(panel.x()+8,panel.y()+10,48,22);
        enabled=new Rect(panel.right()-(w<400?96:111),panel.y()+10,w<400?64:79,22);
        close=new Rect(panel.right()-27,panel.y()+10,19,22);
        reset=new Rect(panel.x()+12,panel.bottom()-26,w<230?64:82,19);
        undo=new Rect(reset.right()+6,reset.y(),w<230?36:46,19);
        done=new Rect(panel.right()-(w<230?66:78),reset.y(),w<230?54:66,19);
        scrollbar=new Rect(controls.right()+3,controls.y(),3,controls.height());
    }
    int columns(){return Math.max(1,controls.width()/88);}
    int maxScroll(int contentHeight){return Math.max(0,contentHeight-controls.height());}
    boolean fullyVisible(Rect r){return r.y()>=controls.y()&&r.bottom()<=controls.bottom();}
}
