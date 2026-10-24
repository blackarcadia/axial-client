package com.axial.cosmetics.client;

/** Visibility and zoom decisions shared by ticking, networking and drawing. */
final class MinimapWork {
    static final double DETAIL_SPAN=2048;
    private MinimapWork() { }
    static boolean recentPreview(int now,int last){return last!=Integer.MIN_VALUE&&now>=last&&now-last<=1;}
    static boolean visible(boolean full,boolean enabled,boolean hudHidden,boolean screenOpen,boolean preview) {
        return full||preview||enabled&&!hudHidden&&!screenOpen;
    }
    static boolean sample(boolean visible,boolean full,double span,boolean completeServerMap) {
        return visible&&(!full||span<=DETAIL_SPAN||!completeServerMap);
    }
}
