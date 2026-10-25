package com.axial.cosmetics.client;

/** Shared render/hit geometry for the two official-server notice boards. */
public final class ServerBoardLayout {
    public record Area(int x,int y,int width,int height){
        public int right(){return x+width;}
        public int bottom(){return y+height;}
        public boolean contains(double px,double py){return px>=x&&px<right()&&py>=y&&py<bottom();}
    }
    public final Area prisons,sky;
    public final int headingX,headingY;
    public ServerBoardLayout(int width,int height){
        if(width<400){
            int gap=10,w=Math.max(1,(width-34)/2),h=Math.max(32,Math.min(80,height-80));
            headingX=12;headingY=54;prisons=new Area(12,68,w,h);sky=new Area(prisons.right()+gap,68,w,h);
        }else{
            int margin=Math.clamp(width/36,12,24),w=Math.min(244,width-264);
            headingX=width-margin-w;headingY=Math.min(126,Math.max(14,height/4-12));
            int top=headingY+18,h=Math.max(32,Math.min(164,(height-12-top-10)/2));
            prisons=new Area(headingX,top,w,h);sky=new Area(headingX,prisons.bottom()+10,w,h);
        }
    }
    static Area notice(Area board){
        int header=board.height()<120?18:32,footer=board.height()<120?24:30;
        return new Area(board.x()+7,board.y()+header,board.width()-14,Math.max(1,board.height()-header-footer));
    }
    static Area prisonsNotice(Area board){
        int header=board.height()<152?26:56,footer=board.height()<120?24:30;
        return new Area(board.x()+7,board.y()+header,board.width()-14,Math.max(1,board.height()-header-footer));
    }
    static Area prisonsLogo(Area board){
        int width=Math.min(board.height()<152?66:114,board.width()-18);
        return new Area(board.x()+(board.width()-width)/2,board.y()+2,width,width/3);
    }
    static Area join(Area board){
        int height=board.height()<120?18:22;
        return new Area(board.x()+7,board.bottom()-height-4,board.width()-14,height);
    }
}
