package com.axial.cosmetics.client;

import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.SoundManager;
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
    private WardrobeItem focusedItem;
    private float rotation = 155;
    private float shownRotation=155;
    private final UiMotion previewRotation=new UiMotion(155);
    private final UiMotion categoryMarker=new UiMotion(0);
    private final SmoothScroll scrolling=new SmoothScroll();
    private final FilterGridMotion<WardrobeItem> filtering=new FilterGridMotion<>();
    private java.util.List<FilterGridMotion.Tile<WardrobeItem>> filteredTiles=java.util.List.of();
    private MenuAnimation animation=new MenuAnimation();
    private boolean reopen;
    private final java.util.Map<WardrobeItem,UiMotion> equipmentMotion=new java.util.HashMap<>();
    private final java.util.Map<WardrobeItem,UiMotion> cardMotion=new java.util.HashMap<>();
    private long equippedAt;
    private String query = "", error = "";
    private TextFieldWidget search;
    private int scroll;
    private net.minecraft.entity.player.SkinTextures menuSkin;
    private boolean skinRequested;
    private final java.util.List<CosmeticCard> cards=new java.util.ArrayList<>();
    private WardrobeScreen(Screen parent) { super(text("Wardrobe")); this.parent = parent; }

    public static boolean open(Screen parent) {
        if (!WardrobeAccess.allowed()) return false;
        MinecraftClient.getInstance().setScreen(new WardrobeScreen(parent));
        return true;
    }

    void refreshCosmetics() {
        if(!WardrobeAccess.allowed()){close();return;}
        if(focusedItem==null||!focusedItem.ownedBy(client.getSession().getUuidOrNull()))
            focusedItem=WardrobeItem.owned(client.getSession().getUuidOrNull(),WardrobeCategory.ALL).stream().findFirst().orElse(null);
        resetScroll();rebuild();
    }

    @Override protected void init() {
        if (!WardrobeAccess.allowed()) return;
        if(reopen){animation.open();reopen=false;}
        NavigationIndicator.mark(3);
        if(focusedItem==null) {
            focusedItem=WardrobeItem.of(WardrobePreferences.current());
            if(focusedItem==null)focusedItem=WardrobeItem.owned(client.getSession().getUuidOrNull(),WardrobeCategory.ALL).stream().findFirst().orElse(null);
            if(focusedItem!=null&&focusedItem.aura()!=null)category=WardrobeCategory.AURAS;
            if(focusedItem!=null&&focusedItem.accessory()!=null)category=WardrobeCategory.OUTFITS;
            rotation=focusedItem==null?155:WardrobeModelView.yaw(focusedItem);previewRotation.jump(rotation);
        }
        if(!skinRequested) {
            skinRequested=true;
            var uuid=client.getSession().getUuidOrNull();
            menuSkin=net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(uuid==null?new java.util.UUID(0,0):uuid);
            if(uuid!=null)client.getSkinProvider().fetchSkinTextures(client.getGameProfile())
                    .thenAccept(result->client.execute(()->result.ifPresent(skin->menuSkin=skin)));
        }
        layout = new WardrobeLayout(width,height);
        rebuild();
    }

    private void resetScroll(){filtering.offsetY(-scroll);scroll=0;}

    private void rebuild() {
        boolean focused = search != null && search.isFocused();
        int cursor = search == null ? query.length() : search.getCursor();
        clearChildren();
        cards.clear();
        button("X", layout.close, this::close);
        button("Done", layout.done, this::close);
        button("Redeem code",new Rect(layout.panel.x()+10,layout.panel.bottom()-26,84,19),()->client.setScreen(new CosmeticRedeemScreen(this)));
        for (var item:WardrobeCategory.values()) {
            button(item.label,layout.category(item.ordinal()),()->{category=item;query="";resetScroll();rebuild();});
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
        search.setChangedListener(value->{query=value;resetScroll();rebuild();});
        addDrawableChild(search);
        if(focused)setFocused(search);
        var matching=matchingItems();
        if(!matching.isEmpty()&&!matching.contains(focusedItem)){focusedItem=matching.stream().findFirst().orElse(null);rotation=WardrobeModelView.yaw(focusedItem);}
        int nextScroll=Math.clamp(scroll,0,layout.maxScroll(matching.size()));
        filtering.offsetY(nextScroll-scroll);scroll=nextScroll;
        filtering.update(matching,index->layout.card(index,0));
        scrolling.jump(scroll,layout.maxScroll(matching.size()));
        int index=0;
        for(var item:matchingItems())card(item,layout.card(index++,scroll));
        int arrowY=layout.preview.bottom()-21;
        button("<",new Rect(layout.preview.x()+4,arrowY,20,18),()->rotation-=45);
        button(">",new Rect(layout.preview.right()-24,arrowY,20,18),()->rotation+=45);

    }

    private boolean matches(String name) {return name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).trim());}
    private java.util.List<WardrobeItem> matchingItems() {
        return WardrobeItem.owned(client.getSession().getUuidOrNull(),category).stream().filter(item->matches(item.label())).toList();
    }
    private void save(WardrobeItem item) {
        if (!WardrobeAccess.allowed()) return;
        try {item.toggle();focusedItem=item;rotation=WardrobeModelView.yaw(item);equippedAt=System.nanoTime();error="";rebuild();}
        catch(IOException ex) {error="Could not save. Try again.";}
    }
    private void card(WardrobeItem item,Rect r) {
        var card=new CosmeticCard(item,r);
        card.visible=r.bottom()>layout.grid.y()&&r.y()<layout.grid.bottom();
        card.active=false;cards.add(addSelectableChild(card));
    }

    /** Cosmetic tiles draw independently of PressableWidget's generic button skin. */
    private final class CosmeticCard extends ClickableWidget {
        private final WardrobeItem item;
        CosmeticCard(WardrobeItem item,Rect r) {
            super(r.x(),r.y(),r.width(),r.height(),text(item.label()));this.item=item;
        }
        private void select() {save(item);}
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
            drawCosmeticCard(c,item,new Rect(getX(),getY(),width,height),isHovered()||isFocused());
        }
    }

    void drawRewardCard(DrawContext c,WardrobeItem item,Rect r){drawCosmeticCard(c,item,r,false,true);}
    private void drawCosmeticCard(DrawContext c,WardrobeItem item,Rect r,boolean hover) {drawCosmeticCard(c,item,r,hover,false);}
    private void drawCosmeticCard(DrawContext c,WardrobeItem item,Rect r,boolean hover,boolean reward) {
        boolean selected=!reward&&item.equipped();
        float equipProgress=(float)equipmentMotion.computeIfAbsent(item,key->new UiMotion(0)).toward(selected?1:0);
        frame(c,r,mixColor(hover?HOVER_LINE:LINE,0xFFAE55FF,equipProgress),mixColor(0xFF15191F,0xFF282033,equipProgress));
        Rect image=WardrobeLayout.capeImage(r);
        Rect stage=WardrobeLayout.modelStage(r);
        if(r.height()>=55) {
            rounded(c,stage.x(),stage.y(),stage.width(),stage.height(),hover?0xFF20232F:0xFF191D27);
            int floorY=stage.bottom()-5;
            rounded(c,stage.x()+stage.width()/5,floorY,Math.max(1,stage.width()*3/5),2,selected?0xFF6D448A:0xFF343A48);
            drawModel(c,item,stage,WardrobeModelView.cardYaw(item)+(float)cardMotion.computeIfAbsent(item,key->new UiMotion(0)).toward(hover?8:0),true);
            if(selected) {
                rounded(c,r.right()-15,r.y()+7,8,8,BRAND);
                label(c,textRenderer,"✓",r.right()-14,r.y()+7,INK,.65f,7,false);
            }
        } else if(item.cape()!=null)drawCape(c,item.cape(),image.x(),image.y(),image.width(),image.height());
        else if(item.accessory()!=null)drawAccessory(c,item.accessory(),image);
        else drawAura(c,item.aura(),r.height()<55?image:new Rect(r.x()+5,r.y()+4,r.width()-10,Math.max(8,r.height()-35)));
        if(r.height()<55) {
            label(c,textRenderer,item.label(),image.right()+5,r.y()+(r.height()-9)/2,
                    selected?BRAND:INK,.75f,r.right()-image.right()-9,false);
            return;
        }
        c.fill(r.x()+6,r.bottom()-29,r.right()-6,r.bottom()-28,tint(DIVIDER));
        label(c,textRenderer,item.label(),r.x()+7,r.bottom()-23,INK,.85f,r.width()-14,true);
        boolean equipped=item.equipped();
        label(c,textRenderer,reward?"ADDED TO YOUR WARDROBE":equipped?"EQUIPPED":"CLICK TO EQUIP",r.x()+7,r.bottom()-12,
                reward?0xFFB5F5CB:equipped?BRAND:MUTED,.7f,r.width()-14,false);
    }

    private WardrobeButton button(String title,Rect r,Runnable action) {
        return addDrawableChild(new WardrobeButton(title,r,action));
    }
    @Override public void tick() {if(!WardrobeAccess.allowed()||animation.finished())client.setScreen(parent);}
    @Override public void close() {animation.close();}
    @Override public void removed() {reopen=true;}
    @Override public boolean shouldPause() {return false;}
    @Override public void renderBackground(DrawContext c,int mx,int my,float delta) {renderInGameBackground(c);}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        if(layout==null)return;
        PanelResize.push(c,this,layout.panel,width,height);
        try {renderPanel(c,(int)PanelResize.x(this,layout.panel,mx),(int)PanelResize.y(this,layout.panel,my),delta);}
        finally {c.getMatrices().popMatrix();}
    }
    private void renderPanel(DrawContext c,int mx,int my,float delta) {
        if(!WardrobeAccess.allowed()||layout==null)return;
        var matching=matchingItems();
        scroll=scrolling.value(layout.maxScroll(matching.size()));
        filtering.update(matching,index->layout.card(index,0));filteredTiles=filtering.frame();
        for(var card:cards) {
            var tile=filteredTiles.stream().filter(value->value.present()&&value.key().equals(card.item)).findFirst().orElse(null);
            if(tile==null){card.visible=false;card.active=false;continue;}
            var r=tile.bounds();card.setPosition(r.x(),r.y()-scroll);
            card.active=tile.opacity()>.6f;card.visible=r.bottom()-scroll>layout.grid.y()&&r.y()-scroll<layout.grid.bottom();
        }
        shownRotation=(float)previewRotation.toward(rotation);
        frame(c,layout.panel,PANEL_BORDER,PANEL);
        float previousOpacity=opacity(animation.progress());
        c.getMatrices().pushMatrix();c.getMatrices().translate(0,animation.offset());
        try {renderContent(c,mx,(int)(my-animation.offset()),delta);}
        finally {c.getMatrices().popMatrix();opacity(previousOpacity);}
    }
    private void renderContent(DrawContext c,int mx,int my,float delta) {
        if(!WardrobeAccess.allowed()||layout==null)return;
        Rect p=layout.panel;
        MenuHeader.draw(c,textRenderer,new Rect(p.x()+8,p.y()+4,p.width()-46,32),"WARDROBE");
        c.fill(p.x()+1,p.y()+42,p.right()-1,p.y()+43,DIVIDER);
        frame(c,layout.search,search.isFocused()?BRAND:LINE,FIELD);
        Rect preview=layout.preview;
        rounded(c,preview.x(),preview.y(),preview.width(),preview.height(),0xA0101218);
        drawPlayer(c,delta);
        double equipAge=(System.nanoTime()-equippedAt)/220_000_000.0;
        if(equippedAt!=0&&equipAge<1) {
            int alpha=(int)(100*(1-equipAge));
            c.drawStrokedRectangle(preview.x(),preview.y(),preview.width(),preview.height(),(alpha<<24)|(BRAND&0xFFFFFF));
        }
        Rect d=layout.details;
        label(c,textRenderer,focusedItem==null?"Your wardrobe starts here":focusedItem.label(),d.x()+4,d.y()+3,INK,.95f,d.width()-8,true);
        if(d.height()>60&&focusedItem!=null) {
            label(c,textRenderer,focusedItem.badge(),d.x()+4,d.y()+18,0xFFE8BA47,.7f,d.width()-8,false);
            label(c,textRenderer,"Drag to rotate",d.x()+4,d.y()+32,MUTED,.7f,d.width()-8,false);
            label(c,textRenderer,focusedItem.equipped()?"Click to unequip":"Click to equip",d.x()+4,d.y()+43,MUTED,.65f,d.width()-8,false);
        }
        if(matchingItems().isEmpty()&&filteredTiles.isEmpty()) {
            label(c,textRenderer,"No cosmetics here yet",layout.grid.x()+3,layout.grid.y()+12,MUTED,.8f,layout.grid.width()-6,false);
        }
        c.fill(p.x()+8,p.bottom()-31,p.right()-8,p.bottom()-30,DIVIDER);
        label(c,textRenderer,error.isEmpty()?"Your collection • Click a cosmetic to equip":error,p.x()+101,p.bottom()-20,error.isEmpty()?MUTED:RED,.7f,p.width()-174,false);
        super.render(c,mx,my,delta);
        var categoryStart=layout.category(0);var categoryNext=layout.category(1);
        int categoryY=categoryStart.y()+(int)Math.round(categoryMarker.toward(category.ordinal())*(categoryNext.y()-categoryStart.y()));
        rounded(c,categoryStart.x(),categoryY+4,2,Math.max(1,categoryStart.height()-8),BRAND);

        c.enableScissor(layout.grid.x(),layout.grid.y(),layout.grid.right(),layout.grid.bottom());
        for(var tile:filteredTiles) {
            if(!tile.key().ownedBy(client.getSession().getUuidOrNull()))continue;
            var bounds=tile.bounds();var r=new Rect(bounds.x(),bounds.y()-scroll,bounds.width(),bounds.height());
            if(r.bottom()<=layout.grid.y()||r.y()>=layout.grid.bottom())continue;
            float parentOpacity=opacity(1);opacity(parentOpacity*tile.opacity());
            try {
                var card=cards.stream().filter(value->value.item.equals(tile.key())).findFirst().orElse(null);
                if(tile.present()&&card!=null)card.render(c,mx,my,delta);
                else drawCosmeticCard(c,tile.key(),r,false);
            } finally {opacity(parentOpacity);}
        }
        c.disableScissor();
        int maximum=layout.maxScroll(matchingItems().size());
        if(maximum>0) {
            int thumb=Math.max(10,layout.grid.height()*layout.grid.height()/(layout.grid.height()+maximum));
            int y=layout.grid.y()+(layout.grid.height()-thumb)*scroll/maximum;
            rounded(c,layout.grid.right()+2,layout.grid.y(),2,layout.grid.height(),DIVIDER);
            rounded(c,layout.grid.right()+2,y,2,thumb,MUTED);
        }
    }

    private void drawCape(DrawContext c,WardrobeCape cape,int x,int y,int w,int h) {
        var preview=cape.preview();
        if(preview!=null){
            float scale=Math.min(w/(float)preview.width(),h/(float)preview.height());
            int imageW=Math.max(1,Math.round(preview.width()*scale)),imageH=Math.max(1,Math.round(preview.height()*scale));
            c.drawTexture(RenderPipelines.GUI_TEXTURED,preview.texture(),x+(w-imageW)/2,y+(h-imageH)/2,0,0,imageW,imageH,
                    preview.width(),preview.height(),preview.width(),preview.height(),tint(0xFFFFFFFF));
            return;
        }
        float age=client.player==null?(System.currentTimeMillis()%4000)/50f:client.player.age;
        c.drawTexture(RenderPipelines.GUI_TEXTURED,cape.texture(age),x,y,32,32,w,h,320,512,2048,1024,tint(0xFFFFFFFF));
    }

    private void drawAccessory(DrawContext c,WardrobeAccessory accessory,Rect r){
        int size=Math.max(1,Math.min(r.width(),r.height()));
        c.drawTexture(RenderPipelines.GUI_TEXTURED,accessory.preview(),r.x()+(r.width()-size)/2,r.y()+(r.height()-size)/2,0,0,size,size,accessory.previewSize,accessory.previewSize,accessory.previewSize,accessory.previewSize,tint(0xFFFFFFFF));
    }
    private void drawAura(DrawContext c,WardrobeAura aura,Rect bounds) {
        int frame=(int)((System.currentTimeMillis()%6000)*64/6000);
        int size=Math.max(1,Math.min(bounds.width(),bounds.height()));
        c.drawTexture(RenderPipelines.GUI_TEXTURED,aura.preview(),bounds.x()+(bounds.width()-size)/2,bounds.y()+(bounds.height()-size)/2,
                (frame%8)*128,(frame/8)*128,size,size,128,128,1024,1024,tint(0xFFFFFFFF));
    }

    private void drawPlayer(DrawContext c,float delta) {
        Rect r=layout.preview;
        label(c,textRenderer,"YOUR LOADOUT",r.x()+8,r.y()+8,MUTED,.65f,r.width()-16,false);
        drawModel(c,focusedItem,new Rect(r.x()+3,r.y()+19,Math.max(1,r.width()-6),Math.max(1,r.height()-44)),shownRotation,false);
    }

    private void drawModel(DrawContext c,WardrobeItem item,Rect bounds,float yaw,boolean card) {
        var skin=card?WardrobePreviewState.CARD_SKIN:client.player==null?menuSkin:client.player.getSkin();
        if(skin==null||(card&&item==null))return;
        float age=(System.nanoTime()/50_000_000L)%1_000_000;
        var state=card?new WardrobePreviewState(item,skin,age,yaw):WardrobePreviewState.equipped(skin,age,yaw);
        var view=WardrobeModelView.fit(card?item:WardrobeItem.of(state.aura),bounds,card);
        c.addEntity(state,view.scale(),new Vector3f(0,view.centerY(),0),
                new Quaternionf().rotateZ((float)Math.PI),new Quaternionf(),
                bounds.x(),bounds.y(),bounds.right(),bounds.bottom());
    }

    @Override public boolean keyPressed(KeyInput input) {
        if(animation.closing()&&QuickMenuScreen.isMenuKey(input)){animation.open();return true;}
        if(animation.closing()&&input.key()!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)animation.open();
        if(QuickMenuScreen.isMenuKey(input)){close();return true;}
        return super.keyPressed(input);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(!WardrobeAccess.allowed())return true;
        if(animation.closing())animation.open();
        click=new Click(PanelResize.x(this,layout.panel,click.x()),PanelResize.y(this,layout.panel,click.y())-animation.offset(),click.buttonInfo());
        if(click.button()==0&&layout.preview.contains(click.x(),click.y())&&click.y()<layout.preview.bottom()-24){dragging=true;return true;}
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        if(dragging){rotation+=(float)dx*1.3f;previewRotation.jump(rotation);return true;}return super.mouseDragged(new Click(PanelResize.x(this,layout.panel,click.x()),PanelResize.y(this,layout.panel,click.y())-animation.offset(),click.buttonInfo()),dx,dy);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(!WardrobeAccess.allowed())return true;
        if(layout.grid.contains(PanelResize.x(this,layout.panel,x),PanelResize.y(this,layout.panel,y)-animation.offset())) {
            scrolling.to(scrolling.target()-(int)Math.round(vertical*28),layout.maxScroll(matchingItems().size()));
            return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean mouseReleased(Click click) {dragging=false;return super.mouseReleased(new Click(PanelResize.x(this,layout.panel,click.x()),PanelResize.y(this,layout.panel,click.y())-animation.offset(),click.buttonInfo()));}

    private class WardrobeButton extends AnimatedMenuButton {
        private float hoverProgress;
        private long lastFrame=System.nanoTime();
        WardrobeButton(String title,Rect r,Runnable action) {
            super(r.x(),r.y(),r.width(),r.height(),text(title),b->{if(WardrobeAccess.allowed())action.run();},DEFAULT_NARRATION_SUPPLIER);
        }
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawContents(DrawContext c,int mx,int my,float delta) {
            Rect r=new Rect(getX(),getY(),width,height);
            boolean selected=getMessage().getString().equalsIgnoreCase(category.label);
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
            for(var value:WardrobeCategory.values())if(value.label.equalsIgnoreCase(title))item=value;
            int textX=r.x()+7;
            if(item!=null&&r.width()>74) {
                icon(c,item.icon,r.x()+8,r.y()+(r.height()-12)/2,12,12,selected?BRAND:MUTED);
                textX=r.x()+26;
            }
            label(c,textRenderer,title,textX,r.y()+(r.height()-9)/2,active?INK:MUTED,.8f,r.right()-textX-6,false);
        }
    }
}
