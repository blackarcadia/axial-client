package com.axial.cosmetics.client;

import java.util.function.LongSupplier;

/** Wheel input accumulates against its destination; direct scrollbar dragging stays immediate. */
final class SmoothScroll {
    private final UiMotion motion;
    private int target;
    SmoothScroll() {this(System::nanoTime);}
    SmoothScroll(LongSupplier clock) {motion=new UiMotion(0,24,clock);}
    int target(){return target;}
    void to(int next,int max){target=Math.clamp(next,0,Math.max(0,max));}
    void jump(int next,int max){to(next,max);motion.jump(target);}
    int value(int max){to(target,max);return Math.clamp((int)Math.round(motion.toward(target)),0,Math.max(0,max));}
}
