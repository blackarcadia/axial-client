package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import java.util.function.LongSupplier;

/** Samples the current bounds before redirecting, including during an unfinished resize. */
final class PanelBoundsMotion {
    private final LongSupplier clock;
    private Rect from,target;
    private long started;
    PanelBoundsMotion(LongSupplier clock){this.clock=clock;}
    void jump(Rect bounds){from=target=bounds;started=clock.getAsLong();}
    void redirect(Rect bounds){
        if(bounds.equals(target))return;
        from=target==null?bounds:value();target=bounds;started=clock.getAsLong();
    }
    Rect value(){
        double t=Math.clamp((clock.getAsLong()-started)/180_000_000.0,0,1);
        double eased=1-Math.pow(1-t,3);
        return new Rect(mix(from.x(),target.x(),eased),mix(from.y(),target.y(),eased),
                Math.max(1,mix(from.width(),target.width(),eased)),Math.max(1,mix(from.height(),target.height(),eased)));
    }
    static double inverseX(Rect shown,Rect target,double x){return target.x()+(x-shown.x())*target.width()/shown.width();}
    static double inverseY(Rect shown,Rect target,double y){return target.y()+(y-shown.y())*target.height()/shown.height();}
    private static int mix(int a,int b,double t){return (int)Math.round(a+(b-a)*t);}
}
