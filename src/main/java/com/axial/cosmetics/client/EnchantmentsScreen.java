package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialUiTheme;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** A server-backed guide using the same shell and navigation as feature settings. */
public final class EnchantmentsScreen extends NavigationScreen {
    private FeatureSettingsLayout layout;
    private Rect searchBox,list;
    private TextFieldWidget search;
    private String query="",selectedKey;
    private int scroll,detailScroll,sort,detailHeight;
    private boolean descending,draggingScrollbar,reopen,compact;
    private MenuAnimation animation=new MenuAnimation();
    private final List<GuideButton> sortButtons=new ArrayList<>();
    private final List<EnchantButton> rowButtons=new ArrayList<>();
    private static final String[] SORT_LABELS={"NAME / MAX LEVEL","APPLIES TO","RARITY"};
    private long revision=-1;
    private List<EnchantmentCatalogue.Entry> rows=List.of();
    private final Map<String,List<ItemStack>> icons=new HashMap<>();
    private EnchantmentCatalogue.Entry selected;
    private List<OrderedText> description=List.of(),applicability=List.of();
    private static final int ROW=32;

    EnchantmentsScreen(Screen parent){super("Enchantments",parent);EnchantmentGuide.open();}
    @Override protected void init(){
        if(reopen){animation=new MenuAnimation();reopen=false;}
        layout=new FeatureSettingsLayout(width,height,false);
        compact=layout.controls.height()<90;
        searchBox=new Rect(layout.controls.x(),layout.controls.y(),layout.controls.width()-(compact?96:0),22);
        int toolbarHeight=compact?27:51;
        list=new Rect(layout.controls.x(),layout.controls.y()+toolbarHeight,layout.controls.width(),Math.max(1,layout.controls.height()-toolbarHeight));
        refresh();rebuild();
    }
    private int itemX(){return list.x()+list.width()*49/100;}
    private int rarityX(){return list.x()+list.width()*77/100;}
    private void rebuild(){
        clearChildren();sortButtons.clear();rowButtons.clear();search=null;draggingScrollbar=false;
        control(selected!=null?"BACK":parent instanceof QuickMenuScreen?"BACK":"MODS",layout.back,this::close);
        control("CLOSE",layout.close,this::close);
        control("DONE",layout.done,this::close);
        if(selected!=null){
            applicability=textRenderer.wrapLines(UiTypography.text(targets(selected),.8f,false),Math.max(1,list.width()-18));
            description=textRenderer.wrapLines(UiTypography.text(selected.description().isBlank()?"No description provided by the server.":selected.description(),.85f,false),Math.max(1,list.width()-18));
            detailHeight=105+applicability.size()*12+description.size()*13;
            detailScroll=Math.clamp(detailScroll,0,maxScroll());return;
        }
        search=field("Search enchantments, items or rarity...",query,searchBox,100);
        search.setPlaceholder(UiTypography.text("Search enchantments, items or rarity...",.85f,false));
        search.addFormatter((value,index)->UiTypography.text(value,.85f,false).asOrderedText());
        search.setChangedListener(v->{query=v;scroll=0;refresh();rebuildRows();});
        int[] xs={list.x(),itemX(),rarityX()},ws={itemX()-list.x()-4,rarityX()-itemX()-4,list.right()-rarityX()};
        if(compact)sortButtons.add(control("SORT: NAME",new Rect(searchBox.right()+6,searchBox.y(),90,22),()->{
            sort=(sort+1)%3;descending=false;scroll=0;refresh();rebuildRows();
        }));
        else for(int i=0;i<3;i++){
            final int column=i;
            sortButtons.add(control(SORT_LABELS[i],new Rect(xs[i],searchBox.bottom()+5,ws[i],20),()->{
                if(sort==column)descending=!descending;else{sort=column;descending=false;}
                scroll=0;refresh();rebuildRows();
            }));
        }
        updateSortLabels();rebuildRows();
    }
    private GuideButton control(String label,Rect r,Runnable action){return addDrawableChild(new GuideButton(label,r,action));}
    private void updateSortLabels(){
        for(int i=0;i<sortButtons.size();i++){
            var b=sortButtons.get(i);
            if(compact){b.selected=false;b.setMessage(text("SORT: "+new String[]{"NAME","ITEM","RARITY"}[sort]));continue;}
            b.selected=i==sort;
            b.setMessage(text(SORT_LABELS[i]+(i==sort?(descending?" ↓":" ↑"):"")));
        }
    }
    private void refresh(){
        revision=EnchantmentGuide.catalogue.revision;
        selected=EnchantmentGuide.catalogue.entries.stream().filter(e->e.key().equals(selectedKey)).findFirst().orElse(null);
        if(selected==null)selectedKey=null;
        String q=query.toLowerCase(Locale.ROOT).trim();
        Comparator<EnchantmentCatalogue.Entry> comparator=switch(sort){
            case 1->Comparator.comparing(e->targets(e).toLowerCase(Locale.ROOT));
            case 2->Comparator.comparingInt(EnchantmentCatalogue.Entry::rank);
            default->Comparator.comparing(e->e.name().toLowerCase(Locale.ROOT));
        };
        if(descending)comparator=comparator.reversed();
        rows=EnchantmentGuide.catalogue.entries.stream().filter(e->(e.name()+" "+targets(e)+" "+e.rarity()).toLowerCase(Locale.ROOT).contains(q))
                .sorted(comparator.thenComparing(EnchantmentCatalogue.Entry::key)).toList();
        scroll=Math.clamp(scroll,0,Math.max(0,rows.size()*ROW-list.height()));updateSortLabels();
    }
    private void rebuildRows(){
        for(var b:rowButtons){if(b.isFocused())setFocused(null);remove(b);}rowButtons.clear();
        for(int i=0;i<rows.size();i++)rowButtons.add(addSelectableChild(new EnchantButton(rows.get(i),i*ROW)));
        positionRows();
    }
    private void positionRows(){
        for(var b:rowButtons){
            b.setY(list.y()+b.contentY-scroll);
            b.visible=b.getY()>=list.y()&&b.getBottom()<=list.bottom();
            if(!b.visible&&b.isFocused())setFocused(null);
        }
    }
    private void open(EnchantmentCatalogue.Entry entry){
        selectedKey=entry.key();selected=entry;detailScroll=0;setFocused(null);rebuild();
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        if(revision!=EnchantmentGuide.catalogue.revision){refresh();rebuild();}
        c.getMatrices().pushMatrix();c.getMatrices().translate(0,animation.offset());
        try{drawContent(c,mx,(int)(my-animation.offset()),delta);}finally{c.getMatrices().popMatrix();}
    }
    private void drawContent(DrawContext c,int mx,int my,float delta){
        frame(c,layout.panel,PANEL_BORDER,PANEL);
        int titleWidth=layout.panel.width()-100,titleCenter=layout.panel.x()+65+titleWidth/2;
        centered(c,"AXIAL CLIENT",titleCenter,layout.panel.y()+8,BRAND,.75f,titleWidth,false);
        centered(c,selected==null?"Enchantments":selected.name(),titleCenter,layout.panel.y()+21,INK,1f,titleWidth,true);
        c.fill(layout.panel.x()+12,layout.controls.y()-5,layout.panel.right()-12,layout.controls.y()-4,DIVIDER);
        if(selected==null){
            frame(c,searchBox,search.isFocused()?FOCUS:LINE,FIELD);
            c.enableScissor(list.x(),list.y(),list.right(),list.bottom());
            for(var b:rowButtons)if(b.getBottom()>list.y()&&b.getY()<list.bottom())b.drawRow(c,mx,my);
            if(rows.isEmpty()){
                String status=EnchantmentGuide.catalogue.revision==0?EnchantmentGuide.status:EnchantmentGuide.catalogue.entries.isEmpty()?"No custom enchantments configured.":"No matching enchantments.";
                var lines=textRenderer.wrapLines(UiTypography.text(status,.8f,false),Math.max(1,list.width()-16));
                int y=list.y()+9;for(var value:lines){c.drawText(textRenderer,value,list.x()+7,y,MUTED,false);y+=12;}
            }
            c.disableScissor();
        }else drawDetails(c);
        c.fill(layout.panel.x()+12,layout.panel.bottom()-32,layout.panel.right()-12,layout.panel.bottom()-31,DIVIDER);
        line(c,selected==null?rows.size()+" ENCHANTMENTS · SELECT TO VIEW DETAILS":"ENCHANTMENT DETAILS",layout.controls.x(),layout.done.y()+6,MUTED,.75f,layout.done.x()-layout.controls.x()-12,false);
        if(maxScroll()>0){
            Rect bar=scrollbar();int thumb=thumbHeight();
            rounded(c,bar.x(),bar.y(),bar.width(),bar.height(),DIVIDER);
            rounded(c,bar.x(),bar.y()+(bar.height()-thumb)*currentScroll()/maxScroll(),bar.width(),thumb,MUTED);
        }
        super.render(c,mx,my,delta);
    }
    private void drawDetails(DrawContext c){
        Rect area=layout.controls;
        c.enableScissor(area.x(),area.y(),area.right(),area.bottom());
        int y=area.y()-detailScroll;
        frame(c,new Rect(area.x(),y,area.width(),36),LINE,FIELD);
        line(c,"MAX LEVEL "+selected.maxLevel(),area.x()+8,y+7,INK,.85f,area.width()/2-12,true);
        line(c,selected.rarity(),area.x()+8,y+21,color(selected.rarity()),.8f,area.width()/2-12,false);
        var items=items(selected);int max=Math.max(1,(area.width()/2-12)/18);
        for(int i=0;i<Math.min(items.size(),max);i++)c.drawItem(items.get(i),area.x()+area.width()/2+i*18,y+10);
        line(c,"APPLIES TO",area.x()+1,y+46,MUTED,.78f,area.width(),true);
        int textY=y+63;for(var value:applicability){c.drawText(textRenderer,value,area.x()+8,textY,INK,false);textY+=12;}
        line(c,"DESCRIPTION",area.x()+1,textY+11,MUTED,.78f,area.width(),true);
        textY+=28;
        for(var value:description){c.drawText(textRenderer,value,area.x()+8,textY,INK,false);textY+=13;}
        c.disableScissor();
    }
    private Rect scrollbar(){return selected==null?new Rect(layout.scrollbar.x(),list.y(),3,list.height()):layout.scrollbar;}
    private int maxScroll(){return selected==null?Math.max(0,rows.size()*ROW-list.height()):Math.max(0,detailHeight-layout.controls.height());}
    private int currentScroll(){return selected==null?scroll:detailScroll;}
    private int thumbHeight(){Rect bar=scrollbar();return Math.min(bar.height(),Math.max(12,bar.height()*bar.height()/(bar.height()+maxScroll())));}
    private void scrollTo(int value){
        int next=Math.clamp(value,0,maxScroll());
        if(selected==null){scroll=next;positionRows();}else detailScroll=next;
    }
    private void dragScroll(double y){
        Rect bar=scrollbar();int thumb=thumbHeight();
        scrollTo((int)Math.round((y-bar.y()-thumb/2.0)/Math.max(1,bar.height()-thumb)*maxScroll()));
    }
    private Click transformed(Click click){return new Click(click.x(),click.y()-animation.offset(),click.buttonInfo());}
    @Override public boolean mouseClicked(Click click,boolean doubled){
        if(animation.closing())return true;
        click=transformed(click);Rect bar=scrollbar();
        if(click.button()==0&&maxScroll()>0&&click.x()>=bar.x()-2&&click.x()<bar.right()+3&&click.y()>=bar.y()&&click.y()<bar.bottom()){
            draggingScrollbar=true;dragScroll(click.y());return true;
        }
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy){
        if(animation.closing())return true;
        if(draggingScrollbar){dragScroll(click.y()-animation.offset());return true;}
        return super.mouseDragged(transformed(click),dx,dy);
    }
    @Override public boolean mouseReleased(Click click){draggingScrollbar=false;return super.mouseReleased(transformed(click));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(animation.closing())return true;
        if(layout.controls.contains(x,y-animation.offset())){scrollTo(currentScroll()-(int)Math.round(vertical*26));return true;}
        return false;
    }
    @Override public boolean keyPressed(KeyInput input){
        if(animation.closing())return true;
        if(!(getFocused() instanceof TextFieldWidget)){
            int page=selected==null?list.height():layout.controls.height();
            switch(input.key()){
                case GLFW.GLFW_KEY_PAGE_DOWN->{scrollTo(currentScroll()+page);return true;}
                case GLFW.GLFW_KEY_PAGE_UP->{scrollTo(currentScroll()-page);return true;}
                case GLFW.GLFW_KEY_HOME->{scrollTo(0);return true;}
                case GLFW.GLFW_KEY_END->{scrollTo(maxScroll());return true;}
            }
            if(QuickMenuScreen.isMenuKey(input)){close();return true;}
        }
        return super.keyPressed(input);
    }
    @Override public void close(){
        if(selected!=null){selectedKey=null;selected=null;detailScroll=0;setFocused(null);rebuild();}
        else animation.close();
    }
    @Override public void tick(){if(animation.finished())client.setScreen(parent);}
    @Override public void removed(){reopen=true;}
    @Override public boolean shouldPause(){return false;}
    @Override protected void line(DrawContext c,String value,int x,int y,int color,float scale,int maxWidth,boolean bold){
        label(c,textRenderer,value,x,y,color,scale,maxWidth,bold);
    }
    private void centered(DrawContext c,String value,int center,int y,int color,float scale,int maxWidth,boolean bold){
        String trimmed=textRenderer.trimToWidth(UiTypography.text(value,scale,bold),Math.max(0,maxWidth)).getString();
        label(c,textRenderer,trimmed,center-textRenderer.getWidth(UiTypography.text(trimmed,scale,bold))/2,y,color,scale,maxWidth,bold);
    }
    private class GuideButton extends ButtonWidget {
        boolean selected;
        GuideButton(String label,Rect r,Runnable action){super(r.x(),r.y(),r.width(),r.height(),text(label),b->action.run(),DEFAULT_NARRATION_SUPPLIER);}
        @Override public void playDownSound(SoundManager sounds){AxialUiTheme.playButtonClickSound();}
        @Override protected void drawIcon(DrawContext c,int mx,int my,float dt){
            boolean hover=isHovered()||isFocused();Rect r=new Rect(getX(),getY(),width,height);
            if(getMessage().getString().equals("CLOSE")){icon(c,"close",getX()+4,getY()+5,11,11,hover?INK:MUTED);return;}
            frame(c,r,hover?HOVER_LINE:selected?SELECTED_LINE:LINE,selected?SELECTED:hover?HOVER:FIELD);
            centered(c,getMessage().getString(),r.x()+r.width()/2,r.y()+6,INK,.8f,r.width()-14,false);
        }
    }
    private final class EnchantButton extends GuideButton {
        final EnchantmentCatalogue.Entry entry;
        final int contentY;
        EnchantButton(EnchantmentCatalogue.Entry entry,int contentY){
            super(entry.name()+", max level "+entry.maxLevel()+", "+targets(entry)+", "+entry.rarity(),new Rect(list.x(),0,list.width(),ROW-3),()->open(entry));
            this.entry=entry;this.contentY=contentY;
        }
        void drawRow(DrawContext c,int mx,int my){
            boolean hover=visible&&(isMouseOver(mx,my)||isFocused());
            Rect r=new Rect(getX(),getY(),width,height);
            frame(c,r,hover?HOVER_LINE:LINE,hover?HOVER:FIELD);
            line(c,entry.name(),r.x()+7,r.y()+5,INK,.85f,itemX()-r.x()-14,true);
            line(c,"MAX LEVEL "+entry.maxLevel(),r.x()+7,r.y()+18,MUTED,.72f,itemX()-r.x()-14,false);
            var items=items(entry);int max=Math.max(1,(rarityX()-itemX()-8)/18);
            for(int i=0;i<Math.min(items.size(),max);i++)c.drawItem(items.get(i),itemX()+i*18,r.y()+6);
            line(c,entry.rarity(),rarityX()+3,r.y()+11,color(entry.rarity()),.8f,r.right()-rarityX()-8,false);
        }
    }
    private static String targets(EnchantmentCatalogue.Entry e){
        if(e.targets().isEmpty())return "All items";
        return String.join(", ",e.targets().stream().map(s->s.equals("ARMOR")?"Armor (helmet, chestplate, leggings, boots)":s.equals("WEAPON")?"Weapon (sword, axe, trident)":s.equals("CELL_BUSTER")?"Cell Buster":s.equals("ALL")||s.equals("ANY")?"All items":s.replace('_',' ').toLowerCase(Locale.ROOT)).toList());
    }
    private List<ItemStack> items(EnchantmentCatalogue.Entry e){return icons.computeIfAbsent(e.key()+e.targets(),key->{
        var names=new LinkedHashSet<String>();
        for(String token:e.targets())switch(token){
            case "ARMOR"->names.addAll(List.of("diamond_helmet","diamond_chestplate","diamond_leggings","diamond_boots"));
            case "WEAPON"->names.addAll(List.of("diamond_sword","diamond_axe","trident"));
            case "CELL_BUSTER"->names.add("wooden_shovel");
            case "SPEAR"->names.add("diamond_spear");
            case "PICKAXE","SWORD","AXE","HOE","SHOVEL","HELMET","CHESTPLATE","LEGGINGS","BOOTS"->names.add("diamond_"+token.toLowerCase(Locale.ROOT));
            default->names.add(token.toLowerCase(Locale.ROOT));
        }
        var result=new ArrayList<ItemStack>();for(String name:names){var id=Identifier.tryParse(name);if(id!=null&&Registries.ITEM.containsId(id))result.add(new ItemStack(Registries.ITEM.get(id)));}
        return result.isEmpty()?List.of(new ItemStack(Items.ENCHANTED_BOOK)):List.copyOf(result);
    });}
    private static int color(String rarity){return switch(rarity){case "UNCOMMON"->0xFF55FFFF;case "UNIQUE","COSMIC"->0xFF35B9C7;case "RARE"->0xFF55FF55;case "ELITE"->0xFFFFFF55;case "LEGENDARY"->0xFFFFAA00;default->0xFFFFFFFF;};}
}
