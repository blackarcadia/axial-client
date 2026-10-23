package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Edge-anchored title navigation; image aspect ratios never change with GUI scale. */
final class TitleMenuLayout {
    final Rect navigation,gamemodes,account,quit,music,prisonsImage,skyImage,prisonsConnect,skyConnect,pages;
    final boolean compact;
    private final int rowHeight,playY,clientY;

    TitleMenuLayout(int width,int height) {
        compact=width<560||height<290;
        int margin=compact?8:Math.max(12,Math.min(20,width/48));
        int panelWidth=compact?Math.min(176,(width-margin*3-24)/2):Math.min(200,(int)(width*.245));
        int top=Math.max(52,Math.min((int)(height*.24),height-180));
        int footer=32,bottom=height-footer-8;
        int imageW=panelWidth-16,imageH=imageW*941/1672;
        int actionH=compact?20:23,pageH=compact?14:17;
        int groupH=22+imageH*2+actionH*2+pageH+8+8;
        if(groupH>bottom-top){
            imageH=Math.max(24,(bottom-top-22-actionH*2-pageH-16)/2);
            groupH=22+imageH*2+actionH*2+pageH+16;
        }
        gamemodes=new Rect(width-margin-panelWidth,top,panelWidth,groupH);
        rowHeight=Math.min(compact?25:31,Math.max(18,(groupH-66)/4));
        int navHeight=59+4*rowHeight;
        navigation=new Rect(margin,top,panelWidth,navHeight);
        playY=navigation.y()+25;clientY=playY+rowHeight*2+23;
        prisonsImage=new Rect(gamemodes.x()+8,top+22,imageW,imageH);
        prisonsConnect=new Rect(gamemodes.x()+8,prisonsImage.bottom(),imageW,actionH);
        pages=new Rect(gamemodes.x()+8,prisonsConnect.bottom(),imageW,pageH);
        skyImage=new Rect(gamemodes.x()+8,pages.bottom()+6,imageW,imageH);
        skyConnect=new Rect(skyImage.x(),skyImage.bottom(),imageW,actionH);
        quit=new Rect(width-margin-24,10,24,24);
        int accountW=compact?90:Math.min(156,Math.max(110,width/4));
        account=new Rect(quit.x()-7-accountW,10,accountW,24);
        music=new Rect(margin,height-27,Math.min(184,width-2*margin),19);
    }
    Rect nav(int index) {return new Rect(navigation.x()+8,index<2?playY+index*(rowHeight+3):clientY+(index-2)*(rowHeight+3),navigation.width()-16,rowHeight);}
    int playHeading(){return navigation.y()+10;}
    int clientHeading(){return clientY-13;}
    Rect dot(int index){int total=3*15;return new Rect(pages.x()+(pages.width()-total)/2+index*15,pages.y(),15,pages.height());}
    Rect previous(){return new Rect(pages.x(),pages.y(),20,pages.height());}
    Rect next(){return new Rect(pages.right()-20,pages.y(),20,pages.height());}
    static Rect contain(Rect area,int imageWidth,int imageHeight) {
        double scale=Math.min(area.width()/(double)imageWidth,area.height()/(double)imageHeight);
        int w=Math.max(1,(int)Math.round(imageWidth*scale)),h=Math.max(1,(int)Math.round(imageHeight*scale));
        return new Rect(area.x()+(area.width()-w)/2,area.y()+(area.height()-h)/2,w,h);
    }
}
