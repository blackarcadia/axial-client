package com.axial.cosmetics.client;

public final class CrosshairConfig {
    public boolean enabled=true;
    public CrosshairStyle style=CrosshairStyle.LUNAR;
    public int color=0xFFFFFFFF,outlineColor=0xFF000000;
    public float size=1,length=4,width=2,gap=2;
    public boolean outlineEnabled=true,dynamicEnabled=false,centerDot=false;
    public int dotSize=2,outlineThickness=1;
    public float opacity=1;

    public enum CrosshairStyle {
        CLASSIC,LUNAR,DOT,SMALL_DOT,PLUS,T,X,CIRCLE,SMALL_CROSS,
        DIAMOND,SQUARE,CHEVRON,BRACKETS
    }
    public CrosshairConfig copy(){var copy=new CrosshairConfig();copy.copyFrom(this);return copy;}
    public void copyFrom(CrosshairConfig c){
        enabled=c.enabled;style=c.style;color=c.color;outlineColor=c.outlineColor;
        size=c.size;length=c.length;width=c.width;gap=c.gap;
        outlineEnabled=c.outlineEnabled;dynamicEnabled=c.dynamicEnabled;centerDot=c.centerDot;
        dotSize=c.dotSize;outlineThickness=c.outlineThickness;opacity=c.opacity;
    }
    void normalize(){
        if(style==null)style=CrosshairStyle.LUNAR;
        size=bounded(size,.1f,4,1);length=Math.round(bounded(length,0,32,4));
        width=Math.round(bounded(width,1,12,2));gap=Math.round(bounded(gap,0,20,2));
        opacity=bounded(opacity,.1f,1,1);dotSize=Math.clamp(dotSize,1,8);outlineThickness=Math.clamp(outlineThickness,1,4);
        color=(color==0?0xFFFFFF:color)|0xFF000000;outlineColor|=0xFF000000;
    }
    private static float bounded(float value,float min,float max,float fallback){return Float.isFinite(value)?Math.clamp(value,min,max):fallback;}
}
