package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

/** Small filmstrip action beside the music slider; does not enter the main menu's text-button stack. */
public final class ReplayViewerButton extends ClickableWidget {
    private final UiMotion hover=new UiMotion(0);
    public ReplayViewerButton(int screenWidth) {
        super(x(screenWidth),24,24,24,ClientMenuText.literal("Replay Viewer"));
        setTooltip(Tooltip.of(ClientMenuText.literal("Replay Viewer")));
    }
    static int x(int screenWidth){return Math.max(20,screenWidth-204);}
    public void layout(int screenWidth){setDimensionsAndPosition(24,24,x(screenWidth),24);}
    @Override public void onClick(Click click,boolean doubled){if(click.button()==0)open();}
    private void open() {
        var client=MinecraftClient.getInstance();
        try{ReplayMenu.open(client.currentScreen);}
        catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(ReplayViewerButton.class).warn("Could not open replay viewer",ex);setTooltip(Tooltip.of(ClientMenuText.literal("Replay viewer unavailable. Please restart the client.")));}
    }
    @Override public boolean keyPressed(KeyInput input) {
        if(active&&isFocused()&&(input.key()==GLFW.GLFW_KEY_ENTER||input.key()==GLFW.GLFW_KEY_KP_ENTER||input.key()==GLFW.GLFW_KEY_SPACE)) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());open();return true;
        }
        return super.keyPressed(input);
    }
    @Override protected void renderWidget(DrawContext context,int mouseX,int mouseY,float delta) {
        float t=(float)hover.toward(isHovered()||isFocused()?1:0);
        ModMenuUi.rounded(context,getX(),getY(),width,height,ModMenuUi.mixColor(0xFF262A32,0xFF3E344A,t));
        ModMenuUi.rounded(context,getX()+1,getY()+1,width-2,height-2,ModMenuUi.mixColor(0xFF15191F,0xFF25232E,t));
        ModMenuUi.icon(context,"replay",getX()+4,getY()+4,16,16,ModMenuUi.mixColor(ModMenuUi.MUTED,ModMenuUi.BRAND,t));
    }
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder){appendDefaultNarrations(builder);}
}
