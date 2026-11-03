package com.axial.cosmetics.client;

import net.minecraft.client.gui.DrawContext;

/** Carries the selected navigation position between the client menu screens. */
final class NavigationIndicator {
    private static int previousTab;
    private final int tab;
    private final UiMotion position;
    NavigationIndicator(int tab) {this.tab=tab;position=new UiMotion(previousTab);previousTab=tab;}
    static void mark(int tab) {previousTab=tab;}
    void draw(DrawContext c,int x,int y,int tabWidth) {
        int left=x+(int)Math.round(position.toward(tab)*tabWidth);
        ModMenuUi.rounded(c,left+5,y,Math.max(1,tabWidth-10),2,ModMenuUi.BRAND);
    }
}
