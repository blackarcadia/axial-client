package com.axial.cosmetics.client;

import java.util.List;
import static com.axial.cosmetics.client.CrosshairConfig.CrosshairStyle.*;

final class CrosshairPresets {
    record Template(String name,String description,CrosshairConfig.CrosshairStyle style,int length,int width,int gap,boolean outline,boolean dot) {
        CrosshairConfig preview(CrosshairConfig source){var c=source.copy();applyTemplate(c,this);c.enabled=true;return c;}
    }
    private static final List<Template> TEMPLATES=List.of(
        new Template("Classic","Balanced four-arm crosshair",LUNAR,4,2,2,true,false),
        new Template("Precision","Thin arms and a clear aiming gap",LUNAR,4,1,2,true,false),
        new Template("Compact","Small, unobtrusive cross",LUNAR,2,1,1,true,false),
        new Template("Plus","Connected one-pixel cross",PLUS,4,1,0,true,false),
        new Template("Dot","Simple two-pixel aiming point",DOT,0,2,0,false,false),
        new Template("Pinpoint","A single-pixel aiming point",SMALL_DOT,0,1,0,false,false),
        new Template("Bold Dot","Larger, easy-to-see aiming point",DOT,0,4,0,true,false),
        new Template("Tactical T","Open top for a clearer target view",T,4,1,2,true,false),
        new Template("Diagonal","Four separated diagonal arms",X,3,1,2,true,false),
        new Template("Ring","Round, hollow aiming ring",CIRCLE,5,1,0,true,false),
        new Template("Ring + Dot","Circular target with a center point",CIRCLE,6,1,0,true,true),
        new Template("Diamond","Angular hollow diamond",DIAMOND,6,1,0,true,false),
        new Template("Diamond Dot","Diamond with a precise center point",DIAMOND,7,1,0,true,true),
        new Template("Square","Thin, hollow square reticle",SQUARE,5,1,0,true,false),
        new Template("Chevron","Upward-pointing aiming chevron",CHEVRON,4,1,0,true,false),
        new Template("Brackets","Open center framed by two brackets",BRACKETS,4,1,3,true,false),
        new Template("Cross + Dot","Separated arms around a center point",LUNAR,4,1,3,true,true),
        new Template("Wide Cross","Longer arms and a wider center gap",LUNAR,7,2,4,true,false),
        new Template("Tiny Cross","Compact cross with short arms",SMALL_CROSS,2,1,0,true,false),
        new Template("Bold Plus","Thicker, connected cross",PLUS,5,3,0,true,false));
    private CrosshairPresets(){}
    static List<Template> templates(){return TEMPLATES;}
    static void applyTemplate(CrosshairConfig c,Template p){
        c.style=p.style();c.size=1;c.length=p.length();c.width=p.width();c.gap=p.gap();
        c.outlineEnabled=p.outline();c.outlineThickness=1;c.centerDot=p.dot();c.dotSize=2;
    }
    static boolean matches(CrosshairConfig c,Template p){
        return c.style==p.style()&&c.size==1&&c.length==p.length()&&c.width==p.width()&&c.gap==p.gap()
                &&c.outlineEnabled==p.outline()&&c.outlineThickness==1&&c.centerDot==p.dot()&&(!p.dot()||c.dotSize==2);
    }
    /** Compatibility for existing style previews; user color, scale and dynamics are retained. */
    static void apply(CrosshairConfig c,CrosshairConfig.CrosshairStyle style){
        c.style=style;
        c.length=switch(style){case DOT,SMALL_DOT->0;case CIRCLE->3;case SMALL_CROSS->2;case DIAMOND,SQUARE->5;default->4;};
        c.width=style==SMALL_DOT?1:style==CIRCLE?4:2;
        c.gap=switch(style){case CLASSIC,DOT,SMALL_DOT,PLUS,CIRCLE,DIAMOND,SQUARE,CHEVRON->0;default->2;};
        c.outlineEnabled=style!=DOT&&style!=SMALL_DOT;
    }
}
