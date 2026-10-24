package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import static com.axial.cosmetics.client.ModMenuUi.*;

public final class KeystrokesHud {
    public record Bounds(int x,int y,int width,int height){public boolean contains(double x,double y){return x>=this.x&&x<this.x+width&&y>=this.y&&y<this.y+height;}}
    private KeystrokesHud(){}
    public static Bounds bounds(MinecraftClient client){return new Bounds(KeystrokesConfig.x(client.getWindow().getScaledWidth()),KeystrokesConfig.y(client.getWindow().getScaledHeight()),KeystrokesConfig.width(),KeystrokesConfig.height());}
    public static void render(DrawContext context){
        var client=MinecraftClient.getInstance();
        if(!KeystrokesConfig.enabled()||client.player==null||client.options.hudHidden||client.currentScreen!=null||KeystrokesConfig.visibleMask()==0)return;
        int pressed=0;
        if(client.isWindowFocused()){
            var window=client.getWindow();
            if(KeystrokesConfig.visible(0)&&InputUtil.isKeyPressed(window,GLFW.GLFW_KEY_W))pressed|=1;
            if(KeystrokesConfig.visible(1)&&InputUtil.isKeyPressed(window,GLFW.GLFW_KEY_A))pressed|=2;
            if(KeystrokesConfig.visible(2)&&InputUtil.isKeyPressed(window,GLFW.GLFW_KEY_S))pressed|=4;
            if(KeystrokesConfig.visible(3)&&InputUtil.isKeyPressed(window,GLFW.GLFW_KEY_D))pressed|=8;
            if(KeystrokesConfig.mouse()){
                if(KeystrokesConfig.visible(4)&&GLFW.glfwGetMouseButton(window.getHandle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS)pressed|=16;
                if(KeystrokesConfig.visible(5)&&GLFW.glfwGetMouseButton(window.getHandle(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_PRESS)pressed|=32;
            }
        }
        var b=bounds(client);draw(context,b.x(),b.y(),pressed);
    }
    public static void preview(DrawContext c,MinecraftClient client){if(KeystrokesConfig.enabled()){var b=bounds(client);draw(c,b.x(),b.y(),17);}}
    public static void example(DrawContext c,int x,int y){draw(c,x,y,17);}
    private static void draw(DrawContext c,int x,int y,int pressed){
        var keys=KeystrokesConfig.layout().keys();
        var font=MinecraftClient.getInstance().textRenderer;
        c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(KeystrokesConfig.scale(),KeystrokesConfig.scale());
        try{for(var key:keys){
            boolean down=(pressed&(1<<key.bit()))!=0;
            int color=down?KeystrokesConfig.pressedColor():0xFF171B20;
            color=(Math.round(KeystrokesConfig.opacity()*255)<<24)|(color&0xFFFFFF);
            rounded(c,key.x(),key.y(),key.width(),key.height(),color);
            var label=UiTypography.text(key.label(),key.bit()<4?1:.75f,key.bit()<4);
            c.drawText(font,label,key.x()+(key.width()-font.getWidth(label))/2,key.y()+(key.height()-font.fontHeight)/2,KeystrokesConfig.textColor(key.bit()),false);
        }}finally{c.getMatrices().popMatrix();}
    }
}
