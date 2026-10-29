package com.axial.cosmetics.client;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.EntityPose;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static com.axial.cosmetics.client.ModMenuUi.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Owner-only native wardrobe. All rendering uses real bundled cosmetics. */
public final class WardrobeScreen extends Screen {
    private static final String[] CATEGORIES = {"All cosmetics", "Capes", "Headwear", "Outfits", "Backpacks", "Emotes"};
    private final Screen parent;
    private WardrobeLayout layout;
    private int category = 1;
    private boolean selectedCape = true, dragging;
    private float rotation = 25;
    private String query = "", error = "";
    private TextFieldWidget search;
    private WardrobeScreen(Screen parent) { super(text("Wardrobe")); this.parent = parent; }

    public static boolean open(Screen parent) {
        if (!WardrobeAccess.allowed()) return false;
        MinecraftClient.getInstance().setScreen(new WardrobeScreen(parent));
        return true;
    }

    public static void applyVisibility(UUID player, PlayerEntityRenderState state) {
        if (CreatorCosmetics.OWNER.equals(player) && WardrobeAccess.allowed() && !WardrobePreferences.showCape())
            state.capeVisible = false;
    }

    @Override protected void init() {
        if (!WardrobeAccess.allowed()) return;
        layout = new WardrobeLayout(width,height);
        rebuild();
    }

    private void rebuild() {
        boolean focused = search != null && search.isFocused();
        int cursor = search == null ? query.length() : search.getCursor();
        clearChildren();
        button("X", layout.close, this::close);
        button("Done", layout.done, this::close);
        for (int i=0;i<CATEGORIES.length;i++) {
            int index=i;
            button(CATEGORIES[i],layout.category(i),()->{category=index;query="";rebuild();});
        }
        Rect s=layout.search;
        search=new TextFieldWidget(textRenderer,s.x()+7,s.y()+6,Math.max(1,s.width()-14),12,text("Search cosmetics"));
        search.setDrawsBackground(false);
        search.setEditableColor(INK);
        search.setPlaceholder(text("Search capes..."));
        search.setMaxLength(64);
        search.addFormatter((value,index)->text(value).asOrderedText());
        search.setText(query);
        search.setCursor(Math.min(cursor,query.length()),false);
        search.setChangedListener(value->{query=value;rebuild();});
        addDrawableChild(search);
        if(focused)setFocused(search);
        if(category<=1) {
            int index=0;
            if(matches("Gold Owner"))card("Gold Owner",layout.card(index++),true);
            if(matches("None"))card("None",layout.card(index),false);
        }
        Rect d=layout.details;
        int buttonY=d.bottom()-20;
        int arrowY=layout.preview.bottom()-21;
        button("<",new Rect(layout.preview.x()+4,arrowY,20,18),()->rotation-=45);
        button(">",new Rect(layout.preview.right()-24,arrowY,20,18),()->rotation+=45);
        button(selectedCape?(WardrobePreferences.showCape()?"Unequip":"Equip"):"Remove cape",
                new Rect(d.x()+4,buttonY,Math.max(28,d.width()-8),18),()->save(selectedCape&&!WardrobePreferences.showCape()));
    }

    private boolean matches(String name) {return name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).trim());}
    private void save(boolean visible) {
        if (!WardrobeAccess.allowed()) return;
        try {WardrobePreferences.setShowCape(visible);error="";rebuild();}
        catch(IOException ex) {error="Could not save. Try again.";}
    }
    private void card(String name,Rect r,boolean cape) {
        addDrawableChild(new CosmeticCard(name,r,cape));
    }

    /** Cosmetic tiles draw independently of PressableWidget's generic button skin. */
    private final class CosmeticCard extends ClickableWidget {
        private final boolean cape;
        CosmeticCard(String name,Rect r,boolean cape) {
            super(r.x(),r.y(),r.width(),r.height(),text(name));this.cape=cape;
        }
        private void select() {if(WardrobeAccess.allowed()){selectedCape=cape;rebuild();}}
        @Override public void onClick(Click click,boolean doubled) {select();}
        @Override public boolean keyPressed(KeyInput input) {
            if(active&&visible&&isFocused()&&(input.key()==257||input.key()==335||input.key()==32)) {
                playDownSound(client.getSoundManager());select();return true;
            }
            return super.keyPressed(input);
        }
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void appendClickableNarrations(NarrationMessageBuilder builder){appendDefaultNarrations(builder);}
        @Override protected void renderWidget(DrawContext c,int mx,int my,float delta) {
            Rect r=new Rect(getX(),getY(),width,height);
            boolean selected=selectedCape==cape;
            frame(c,r,selected?0xFFAE55FF:isHovered()||isFocused()?HOVER_LINE:LINE,0xEE15191F);
            Rect image=WardrobeLayout.capeImage(r);
            if(cape)drawCape(c,image.x(),image.y(),image.width(),image.height());
            else {
                frame(c,image,LINE,FIELD);
                // A muted empty slot, distinct from the real cape's artwork.
                c.fill(image.x()+image.width()/2,image.y()+2,image.x()+image.width()/2+1,image.bottom()-2,MUTED);
            }
            if(r.height()<55) {
                label(c,textRenderer,getMessage().getString(),image.right()+5,r.y()+(r.height()-9)/2,
                        selected?BRAND:INK,.75f,r.right()-image.right()-9,false);
                return;
            }
            c.fill(r.x()+6,r.bottom()-29,r.right()-6,r.bottom()-28,DIVIDER);
            label(c,textRenderer,getMessage().getString(),r.x()+7,r.bottom()-23,INK,.85f,r.width()-14,true);
            boolean equipped=cape==WardrobePreferences.showCape();
            label(c,textRenderer,equipped?"Equipped":cape?"Animated cape":"No cape",r.x()+7,r.bottom()-12,
                    equipped?BRAND:MUTED,.7f,r.width()-14,false);
        }
    }

    private WardrobeButton button(String title,Rect r,Runnable action) {
        return addDrawableChild(new WardrobeButton(title,r,action));
    }
    @Override public void tick() {if(!WardrobeAccess.allowed())close();}
    @Override public void close() {client.setScreen(parent);}
    @Override public boolean shouldPause() {return false;}
    @Override public void renderBackground(DrawContext c,int mx,int my,float delta) {renderInGameBackground(c);}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        if(!WardrobeAccess.allowed()||layout==null)return;
        Rect p=layout.panel;
        frame(c,p,PANEL_BORDER,PANEL);
        c.drawTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("axialutils","textures/gui/menu_logo.png"),
                p.x()+9,p.y()+6,0,0,64,31,240,117,240,117);
        label(c,textRenderer,"Wardrobe",p.x()+81,p.y()+10,INK,1.25f,p.width()-118,true);
        label(c,textRenderer,"Make it yours",p.x()+81,p.y()+27,MUTED,.8f,p.width()-118,false);
        c.fill(p.x()+1,p.y()+42,p.right()-1,p.y()+43,DIVIDER);
        frame(c,layout.search,search.isFocused()?BRAND:LINE,FIELD);
        Rect preview=layout.preview;
        rounded(c,preview.x(),preview.y(),preview.width(),preview.height(),0xA0101218);
        drawPlayer(c,delta);
        Rect d=layout.details;
        label(c,textRenderer,selectedCape?"Gold Owner":"None",d.x()+4,d.y()+3,INK,.95f,d.width()-8,true);
        if(d.height()>60) {
            label(c,textRenderer,selectedCape?"OWNER EXCLUSIVE":"No custom cape",d.x()+4,d.y()+18,0xFFE8BA47,.7f,d.width()-8,false);
            label(c,textRenderer,"Drag to rotate",d.x()+4,d.y()+32,MUTED,.7f,d.width()-8,false);
            label(c,textRenderer,"Visibility on this client",d.x()+4,d.y()+43,MUTED,.65f,d.width()-8,false);
        }
        if(category>1||(!matches("Gold Owner")&&!matches("None"))) {
            label(c,textRenderer,"No cosmetics here yet",layout.grid.x()+3,layout.grid.y()+12,MUTED,.8f,layout.grid.width()-6,false);
        }
        c.fill(p.x()+8,p.bottom()-31,p.right()-8,p.bottom()-30,DIVIDER);
        label(c,textRenderer,error.isEmpty()?"Saves automatically • Local preview":error,p.x()+10,p.bottom()-20,error.isEmpty()?MUTED:RED,.7f,p.width()-82,false);
        super.render(c,mx,my,delta);
    }

    private void drawCape(DrawContext c,int x,int y,int w,int h) {
        float age=client.player==null?(System.currentTimeMillis()%4000)/50f:client.player.age;
        c.drawTexture(RenderPipelines.GUI_TEXTURED,CreatorCosmetics.capeTexture(age),x,y,32,32,w,h,320,512,2048,1024);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void drawPlayer(DrawContext c,float delta) {
        Rect r=layout.preview;
        if(client.player==null) {
            int h=Math.max(12,r.height()-32),w=Math.min(r.width()-14,h*10/16);
            if(selectedCape)drawCape(c,r.x()+(r.width()-w)/2,r.y()+8,w,h);
            label(c,textRenderer,"Join a world to preview",r.x()+4,r.bottom()-32,MUTED,.65f,r.width()-8,false);
            return;
        }
        PlayerEntityRenderer renderer=(PlayerEntityRenderer)client.getEntityRenderDispatcher().getRenderer(client.player);
        PlayerEntityRenderState state=renderer.createRenderState();
        renderer.updateRenderState(client.player,state,delta);
        state.bodyYaw=rotation;state.relativeHeadYaw=0;state.pitch=0;
        state.pose=EntityPose.STANDING;state.sneaking=false;state.invisible=false;state.spectator=false;
        state.displayName=null;state.outlineColor=0;state.deathTime=0;
        state.limbSwingAmplitude=0;state.limbSwingAnimationProgress=0;
        state.equippedHeadStack=ItemStack.EMPTY;state.equippedChestStack=ItemStack.EMPTY;
        state.equippedLegsStack=ItemStack.EMPTY;state.equippedFeetStack=ItemStack.EMPTY;
        state.rightHandItem=ItemStack.EMPTY;state.leftHandItem=ItemStack.EMPTY;
        state.skinTextures=CreatorCosmetics.applyCape(CreatorCosmetics.OWNER,state.skinTextures,state.age);
        state.capeVisible=selectedCape;
        state.width/=state.baseScale;state.height/=state.baseScale;state.baseScale=1;
        c.addEntity(state,Math.max(8,(r.height()-25)/2.15f),new Vector3f(0,state.height/2,0),
                new Quaternionf().rotateZ((float)Math.PI),new Quaternionf(),r.x()+2,r.y()+2,r.right()-2,r.bottom()-24);
    }

    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(!WardrobeAccess.allowed())return true;
        if(click.button()==0&&layout.preview.contains(click.x(),click.y())&&click.y()<layout.preview.bottom()-24){dragging=true;return true;}
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        if(dragging){rotation+=(float)dx*1.3f;return true;}return super.mouseDragged(click,dx,dy);
    }
    @Override public boolean mouseReleased(Click click) {dragging=false;return super.mouseReleased(click);}

    private class WardrobeButton extends ButtonWidget {
        private float hoverProgress;
        private long lastFrame=System.nanoTime();
        WardrobeButton(String title,Rect r,Runnable action) {
            super(r.x(),r.y(),r.width(),r.height(),text(title),b->{if(WardrobeAccess.allowed())action.run();},DEFAULT_NARRATION_SUPPLIER);
        }
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float delta) {
            Rect r=new Rect(getX(),getY(),width,height);
            boolean selected=getMessage().getString().equals(CATEGORIES[category]);
            long now=System.nanoTime();
            hoverProgress=MenuAnimation.hover(hoverProgress,active&&(isHovered()||isFocused()),(now-lastFrame)/1_000_000_000f);
            lastFrame=now;
            frame(c,r,selected?SELECTED_LINE:mixColor(LINE,HOVER_LINE,hoverProgress),selected?SELECTED:mixColor(FIELD,HOVER,hoverProgress));
            String title=getMessage().getString();
            if(title.equals("X")) {
                icon(c,"close",r.x()+(r.width()-10)/2,r.y()+(r.height()-10)/2,10,10,INK);
                return;
            }
            int categoryIndex=java.util.Arrays.asList(CATEGORIES).indexOf(title);
            int textX=r.x()+7;
            if(categoryIndex>=0&&r.width()>74) {
                String[] icons={"grid","shirt","account","shirt","backpack","move"};
                icon(c,icons[categoryIndex],r.x()+8,r.y()+(r.height()-12)/2,12,12,selected?BRAND:MUTED);
                textX=r.x()+26;
            }
            label(c,textRenderer,title,textX,r.y()+(r.height()-9)/2,active?INK:MUTED,.8f,r.right()-textX-6,false);
        }
    }
}
