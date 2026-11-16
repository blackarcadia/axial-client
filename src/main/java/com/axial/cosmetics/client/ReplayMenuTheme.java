package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

/** Replay Mod uses its own widget library rather than vanilla PressableWidget. */
public final class ReplayMenuTheme {
    private static final Identifier BUTTON=AxialCosmetics.id("textures/gui/buttons/packbutton2.png");
    private static final StyleSpriteSource.Font FONT=new StyleSpriteSource.Font(Identifier.of("axialutils","ui_clean"));
    private static final String GUI="com.replaymod.lib.de.johni0702.minecraft.gui.";
    private ReplayMenuTheme() {}
    public static boolean replayScreen(Screen screen){return screen!=null&&screen.getClass().getName().equals(GUI+"container.AbstractGuiScreen$MinecraftGuiScreen");}
    public static boolean background(Screen screen,DrawContext context) {
        if(!replayScreen(screen))return false;
        MenuBackgroundRenderer.draw(context,screen);return true;
    }
    public static boolean button(Object button,Object renderer,Object size,Object info) {
        var client=MinecraftClient.getInstance();if(!replayScreen(client.currentScreen))return false;
        try {
            var api=Class.forName(GUI+"GuiRenderer");var dimensions=Class.forName(GUI+"utils.lwjgl.ReadableDimension");var points=Class.forName(GUI+"utils.lwjgl.ReadablePoint");
            var buttons=Class.forName(GUI+"element.AbstractGuiButton");var renderInfo=Class.forName(GUI+"RenderInfo");
            // Texture/icon buttons (play/pause, folders, etc.) retain their original symbols.
            if(buttons.getMethod("getTexture").invoke(button)!=null)return false;
            var label=(String)buttons.getMethod("getLabel").invoke(button);if(label==null||label.isEmpty())return false;
            int w=(int)dimensions.getMethod("getWidth").invoke(size),h=(int)dimensions.getMethod("getHeight").invoke(size);
            var offset=api.getMethod("getOpenGlOffset").invoke(renderer);int x=(int)points.getMethod("getX").invoke(offset),y=(int)points.getMethod("getY").invoke(offset);
            var context=(DrawContext)api.getMethod("getContext").invoke(renderer);
            boolean enabled=(boolean)buttons.getMethod("isEnabled").invoke(button);
            int mx=renderInfo.getField("mouseX").getInt(info),my=renderInfo.getField("mouseY").getInt(info);
            boolean hovered=enabled&&mx>=0&&my>=0&&mx<w&&my<h;
            context.drawTexture(RenderPipelines.GUI_TEXTURED,BUTTON,x,y,0f,0f,w,h,200,20,200,20,enabled?0xFFFFFFFF:0xFF808080);
            if(hovered)context.fill(x,y,x+w,y+h,0x22000000);
            Text text=Text.literal(label).styled(style->style.withFont(FONT));
            context.drawCenteredTextWithShadow(client.textRenderer,text,x+w/2,y+(h-8)/2,enabled?0xFFFFFFFF:0xFFA0A0A0);
            return true;
        }catch(ReflectiveOperationException ex){throw new IllegalStateException("Replay Mod button API changed",ex);}
    }
}
