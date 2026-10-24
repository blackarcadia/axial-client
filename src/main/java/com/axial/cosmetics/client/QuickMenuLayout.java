package com.axial.cosmetics.client;

final class QuickMenuLayout {
    static final int ACTION_COUNT=7;
    final ModMenuLayout.Rect bar,logo;
    final int tileWidth,hintY;
    final boolean labels;

    QuickMenuLayout(int width,int height) {
        int barWidth=Math.min(628,width-24);
        tileWidth=(barWidth-12)/ACTION_COUNT;
        barWidth=tileWidth*ACTION_COUNT+12;
        int barHeight=height<220?48:62;
        int logoWidth=Math.min(180,(int)(width*.34));
        int logoHeight=Math.min(logoWidth*117/240,(int)(height*.30));
        logoWidth=logoHeight*240/117;
        int top=(height-(logoHeight+12+barHeight+20))/2;
        logo=new ModMenuLayout.Rect((width-logoWidth)/2,top,logoWidth,logoHeight);
        bar=new ModMenuLayout.Rect((width-barWidth)/2,logo.bottom()+12,barWidth,barHeight);
        hintY=bar.bottom()+8;
        labels=tileWidth>=43;
    }

    ModMenuLayout.Rect button(int index) {
        return new ModMenuLayout.Rect(bar.x()+6+index*tileWidth,bar.y()+5,tileWidth,bar.height()-10);
    }
}
