package com.axial.cosmetics.client;

import java.io.IOException;
import java.util.Locale;
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

/** UUID-granted native wardrobe. All rendering uses real bundled cosmetics. */
public final class WardrobeScreen extends Screen {
    private final Screen parent;
    private WardrobeLayout layout;
    private WardrobeCategory category = WardrobeCategory.CAPES;
    private boolean dragging;
    private WardrobeCape focusedCape;
    private float rotation = 25;
    private String query = "", error = "";
    private TextFieldWidget search;
    private int scroll;
    private final java.util.List<CosmeticCard> cards=new java.util.ArrayList<>();
    private WardrobeScreen(Screen parent) { super(text("Wardrobe")); this.parent = parent; }

    public static boolean open(Screen parent) {
        if (!WardrobeAccess.allowed()) return false;
        MinecraftClient.getInstance().setScreen(new WardrobeScreen(parent));
        return true;
    }

    @Override protected void init() {
        if (!WardrobeAccess.allowed()) return;
        if(focusedCape==null) {
            focusedCape=WardrobePreferences.current();
            if(focusedCape==null)focusedCape=WardrobeCape.defaultFor(client.getSession().getUuidOrNull());
        }
        layout = new WardrobeLayout(width,height);
        rebuild();
    }

    private void rebuild() {
        boolean focused = search != null && search.isFocused();
        int cursor = search == null ? query.length() : search.getCursor();
        clearChildren();
        cards.clear();
        button("X", layout.close, this::close);
        button("Done", layout.done, this::close);
        for (var item:WardrobeCategory.values()) {
            button(item.label,layout.category(item.ordinal()),()->{category=item;query="";scroll=0;rebuild();});
        }
        Rect s=layout.search;
        search=new TextFieldWidget(textRenderer,s.x()+7,s.y()+6,Math.max(1,s.width()-14),12,text(category.search));
        search.setDrawsBackground(false);
        search.setEditableColor(INK);
        search.setPlaceholder(text(category.search));
        search.setMaxLength(64);
        search.addFormatter((value,index)->text(value).asOrderedText());
        search.setText(query);
        search.setCursor(Math.min(cursor,query.length()),false);
        search.setChangedListener(value->{query=value;scroll=0;rebuild();});
        addDrawableChild(search);
        if(focused)setFocused(search);
        if(category.includesCapes()) {
            scroll=Math.clamp(scroll,0,layout.maxScroll(matchingCapes().size()));
            int index=0;
            for(var cape:matchingCapes())card(cape,layout.card(index++,scroll));
        }
        int arrowY=layout.preview.bottom()-21;
        button("<",new Rect(layout.preview.x()+4,arrowY,20,18),()->rotation-=45);
        button(">",new Rect(layout.preview.right()-24,arrowY,20,18),()->rotation+=45);

    }

    private boolean matches(String name) {return name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).trim());}
    private java.util.List<WardrobeCape> matchingCapes() {
        return category.includesCapes()?WardrobeCape.owned(client.getSession().getUuidOrNull()).stream().filter(cape->matches(cape.label)).toList():java.util.List.of();
    }
    private void save(WardrobeCape cape) {
        if (!WardrobeAccess.allowed()) return;
        try {WardrobePreferences.toggle(cape);focusedCape=cape;error="";rebuild();}
        catch(IOException ex) {error="Could not save. Try again.";}
    }
    private void card(WardrobeCape cape,Rect r) {
        var card=new CosmeticCard(cape,r);
        card.visible=r.bottom()>layout.grid.y()&&r.y()<layout.grid.bottom();
        cards.add(addSelectableChild(card));
    }

    /** Cosmetic tiles draw independently of PressableWidget's generic button skin. */
    private final class CosmeticCard extends ClickableWidget {
        private final WardrobeCape cape;
        CosmeticCard(WardrobeCape cape,Rect r) {
            super(r.x(),r.y(),r.width(),r.height(),text(cape.label));this.cape=cape;
        }
        private void select() {save(cape);}
        @Override public void onClick(Click click,boolean doubled) {select();}
        @Override public boolean mouseClicked(Click click,boolean doubled) {
            return layout.grid.contains(click.x(),click.y())&&super.mouseClicked(click,doubled);
        }
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
            boolean selected=WardrobePreferences.current()==cape;
            frame(c,r,selected?0xFFAE55FF:isHovered()||isFocused()?HOVER_LINE:LINE,0xEE15191F);
            Rect image=WardrobeLayout.capeImage(r);
            drawCape(c,cape,image.x(),image.y(),image.width(),image.height());
            if(r.height()<55) {
                label(c,textRenderer,getMessage().getString(),image.right()+5,r.y()+(r.height()-9)/2,
                        selected?BRAND:INK,.75f,r.right()-image.right()-9,false);
                return;
            }
            c.fill(r.x()+6,r.bottom()-29,r.right()-6,r.bottom()-28,DIVIDER);
            label(c,textRenderer,getMessage().getString(),r.x()+7,r.bottom()-23,INK,.85f,r.width()-14,true);
            boolean equipped=WardrobePreferences.current()==cape;
            label(c,textRenderer,equipped?"Equipped":"Click to equip",r.x()+7,r.bottom()-12,
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
        label(c,textRenderer,focusedCape.label,d.x()+4,d.y()+3,INK,.95f,d.width()-8,true);
        if(d.height()>60) {
            label(c,textRenderer,focusedCape.badge,d.x()+4,d.y()+18,0xFFE8BA47,.7f,d.width()-8,false);
            label(c,textRenderer,"Drag to rotate",d.x()+4,d.y()+32,MUTED,.7f,d.width()-8,false);
            label(c,textRenderer,WardrobePreferences.current()==focusedCape?"Click cape to unequip":"Click cape to equip",d.x()+4,d.y()+43,MUTED,.65f,d.width()-8,false);
        }
        if(!category.includesCapes()||WardrobeCape.owned(client.getSession().getUuidOrNull()).stream().noneMatch(cape->matches(cape.label))) {
            label(c,textRenderer,"No cosmetics here yet",layout.grid.x()+3,layout.grid.y()+12,MUTED,.8f,layout.grid.width()-6,false);
        }
        c.fill(p.x()+8,p.bottom()-31,p.right()-8,p.bottom()-30,DIVIDER);
        label(c,textRenderer,error.isEmpty()?"Saves automatically • Local preview":error,p.x()+10,p.bottom()-20,error.isEmpty()?MUTED:RED,.7f,p.width()-82,false);
        super.render(c,mx,my,delta);
        c.enableScissor(layout.grid.x(),layout.grid.y(),layout.grid.right(),layout.grid.bottom());
        for(var card:cards)card.render(c,mx,my,delta);
        c.disableScissor();
        int maximum=layout.maxScroll(matchingCapes().size());
        if(maximum>0) {
            int thumb=Math.max(10,layout.grid.height()*layout.grid.height()/(layout.grid.height()+maximum));
            int y=layout.grid.y()+(layout.grid.height()-thumb)*scroll/maximum;
            rounded(c,layout.grid.right()+2,layout.grid.y(),2,layout.grid.height(),DIVIDER);
            rounded(c,layout.grid.right()+2,y,2,thumb,MUTED);
        }
    }

    private void drawCape(DrawContext c,WardrobeCape cape,int x,int y,int w,int h) {
        float age=client.player==null?(System.currentTimeMillis()%4000)/50f:client.player.age;
        c.drawTexture(RenderPipelines.GUI_TEXTURED,cape.texture(age),x,y,32,32,w,h,320,512,2048,1024);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void drawPlayer(DrawContext c,float delta) {
        Rect r=layout.preview;
        if(client.player==null) {
            int h=Math.max(12,r.height()-32),w=Math.min(r.width()-14,h*10/16);
            var equipped=WardrobePreferences.current();
            if(equipped!=null)drawCape(c,equipped,r.x()+(r.width()-w)/2,r.y()+8,w,h);
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
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(!WardrobeAccess.allowed())return true;
        if(layout.grid.contains(x,y)) {
            scroll=Math.clamp(scroll-(int)Math.round(vertical*28),0,layout.maxScroll(matchingCapes().size()));
            rebuild();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
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
            boolean selected=getMessage().getString().equals(category.label);
            long now=System.nanoTime();
            hoverProgress=MenuAnimation.hover(hoverProgress,active&&(isHovered()||isFocused()),(now-lastFrame)/1_000_000_000f);
            lastFrame=now;
            frame(c,r,selected?SELECTED_LINE:mixColor(LINE,HOVER_LINE,hoverProgress),selected?SELECTED:mixColor(FIELD,HOVER,hoverProgress));
            String title=getMessage().getString();
            if(title.equals("X")) {
                icon(c,"close",r.x()+(r.width()-10)/2,r.y()+(r.height()-10)/2,10,10,INK);
                return;
            }
            WardrobeCategory item=null;
            for(var value:WardrobeCategory.values())if(value.label.equals(title))item=value;
            int textX=r.x()+7;
            if(item!=null&&r.width()>74) {
                icon(c,item.icon,r.x()+8,r.y()+(r.height()-12)/2,12,12,selected?BRAND:MUTED);
                textX=r.x()+26;
            }
            label(c,textRenderer,title,textX,r.y()+(r.height()-9)/2,active?INK:MUTED,.8f,r.right()-textX-6,false);
        }
    }
}
