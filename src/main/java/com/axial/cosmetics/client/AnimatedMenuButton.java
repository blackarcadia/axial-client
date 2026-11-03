package com.axial.cosmetics.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.AbstractInput;

/** A short visual press pulse; actions and hit boxes respond immediately. */
abstract class AnimatedMenuButton extends ButtonWidget {
    private long pressed;
    protected AnimatedMenuButton(int x,int y,int w,int h,net.minecraft.text.Text text,PressAction action,NarrationSupplier narration) {
        super(x,y,w,h,text,action,narration);
    }
    @Override public void onPress(AbstractInput input) {pressed=System.nanoTime();super.onPress(input);}
    @Override protected final void drawIcon(DrawContext c,int mx,int my,float delta) {
        double t=(System.nanoTime()-pressed)/140_000_000.0;
        float scale=1-(float)(t>=0&&t<1?.035*Math.sin(Math.PI*t):0);
        c.getMatrices().pushMatrix();
        try {
            float cx=getX()+getWidth()/2f,cy=getY()+getHeight()/2f;
            c.getMatrices().translate(cx,cy);c.getMatrices().scale(scale,scale);c.getMatrices().translate(-cx,-cy);
            drawContents(c,mx,my,delta);
        } finally {c.getMatrices().popMatrix();}
    }
    protected abstract void drawContents(DrawContext c,int mx,int my,float delta);
}
