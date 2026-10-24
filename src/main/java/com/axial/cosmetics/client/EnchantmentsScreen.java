package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Server-backed reference guide; this is an action, never a feature toggle. */
final class EnchantmentsScreen extends NavigationScreen {
    private Rect panel,searchBox,list;
    private TextFieldWidget search;
    private String query="",selectedKey;
    private int scroll,detailScroll,sort;
    private boolean descending;
    private final List<NavButton> sortButtons=new ArrayList<>();
    private static final String[] SORT_LABELS={"Name / max level","Applies to","Rarity"};
    private long revision=-1;
    private List<EnchantmentCatalogue.Entry> rows=List.of();
    private final Map<String,List<ItemStack>> icons=new HashMap<>();
    private static final int ROW=29;
    EnchantmentsScreen(Screen parent){super("Enchantments",parent);EnchantmentGuide.open();}
    @Override protected void init(){
        int w=Math.min(690,width-20),h=Math.min(430,height-20);
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        searchBox=new Rect(panel.x()+14,panel.y()+58,w-28,27);
        list=new Rect(panel.x()+14,panel.y()+116,w-28,Math.max(ROW,h-143));
        search=field("Search enchantments, items or rarity...",query,searchBox,100);
        search.setChangedListener(v->{query=v;scroll=0;refresh();});
        button("Back",new Rect(panel.right()-62,panel.y()+12,48,22),this::close);
        sortButtons.clear();
        int[] xs={list.x(),itemX(),rarityX()},ws={itemX()-list.x()-4,rarityX()-itemX()-4,list.right()-rarityX()};
        for(int i=0;i<3;i++){final int column=i;sortButtons.add(button(SORT_LABELS[i],new Rect(xs[i],panel.y()+90,ws[i],20),()->{
            if(sort==column)descending=!descending;else{sort=column;descending=false;}
            scroll=0;refresh();
        }));}
        refresh();
    }
    private int itemX(){return list.x()+list.width()*49/100;}
    private int rarityX(){return list.x()+list.width()*77/100;}
    private int visible(){return Math.max(1,list.height()/ROW);}
    private void refresh(){
        revision=EnchantmentGuide.catalogue.revision;
        for(int i=0;i<sortButtons.size();i++){sortButtons.get(i).selected=i==sort;sortButtons.get(i).label(SORT_LABELS[i]+(i==sort?(descending?" ↓":" ↑"):""));}
        String q=query.toLowerCase(Locale.ROOT).trim();
        Comparator<EnchantmentCatalogue.Entry> comparator=switch(sort){
            case 1->Comparator.comparing(e->targets(e).toLowerCase(Locale.ROOT));
            case 2->Comparator.comparingInt(EnchantmentCatalogue.Entry::rank);
            default->Comparator.comparing(e->e.name().toLowerCase(Locale.ROOT));
        };
        if(descending)comparator=comparator.reversed();
        rows=EnchantmentGuide.catalogue.entries.stream().filter(e->(e.name()+" "+targets(e)+" "+e.rarity()).toLowerCase(Locale.ROOT).contains(q))
                .sorted(comparator.thenComparing(EnchantmentCatalogue.Entry::key)).toList();
        scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-visible()));
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        if(revision!=EnchantmentGuide.catalogue.revision)refresh();
        heading(c,panel,"ENCHANTMENTS","Explore the server's custom enchantments");
        c.fill(panel.x()+14,panel.y()+51,panel.right()-14,panel.y()+52,BRAND);
        inputFrame(c,search,searchBox);
        super.render(c,mx,my,delta);
        var selected=EnchantmentGuide.catalogue.entries.stream().filter(e->e.key().equals(selectedKey)).findFirst().orElse(null);
        if(selectedKey!=null&&selected==null)selectedKey=null;
        if(selected!=null){details(c,selected);return;}
        for(int i=scroll;i<Math.min(rows.size(),scroll+visible());i++){
            var e=rows.get(i);int y=list.y()+(i-scroll)*ROW;
            boolean hover=mx>=list.x()&&mx<list.right()&&my>=y&&my<y+ROW-2;
            c.fill(list.x(),y,list.right()-5,y+ROW-2,hover?HOVER:i%2==0?SURFACE:FIELD);
            line(c,e.name(),list.x()+7,y+5,INK,.92f,itemX()-list.x()-15,true);
            line(c,"MAX "+e.maxLevel(),list.x()+7,y+17,BRAND,.7f,itemX()-list.x()-15,false);
            var items=items(e);int max=Math.max(1,(rarityX()-itemX()-8)/18);
            for(int j=0;j<Math.min(items.size(),max);j++)c.drawItem(items.get(j),itemX()+j*18,y+5);
            line(c,e.rarity(),rarityX()+3,y+10,color(e.rarity()),.8f,list.right()-rarityX()-12,true);
            if(hover&&mx>=itemX()&&mx<rarityX())c.drawTooltip(textRenderer,Text.literal(targets(e)),mx,my);
        }
        if(rows.isEmpty())line(c,EnchantmentGuide.catalogue.revision==0?EnchantmentGuide.status:EnchantmentGuide.catalogue.entries.isEmpty()?"No custom enchantments configured.":"No matching enchantments.",list.x()+8,list.y()+16,MUTED,.85f,list.width()-16,false);
        if(rows.size()>visible()){
            int track=visible()*ROW-2,thumb=Math.max(12,track*visible()/rows.size());
            int y=list.y()+(track-thumb)*scroll/(rows.size()-visible());
            c.fill(list.right()-3,list.y(),list.right(),list.y()+track,FIELD);
            c.fill(list.right()-3,y,list.right(),y+thumb,BRAND);
        }
        line(c,rows.size()+" enchantments  ·  Click a row for its description",list.x(),panel.bottom()-17,MUTED,.75f,list.width(),false);
    }
    private void details(DrawContext c,EnchantmentCatalogue.Entry e){
        // Cover the table and controls while retaining the surrounding guide frame.
        c.fill(panel.x()+10,panel.y()+56,panel.right()-10,panel.bottom()-9,PANEL);
        line(c,"‹ Back to enchantments",list.x(),panel.y()+64,BRAND,.85f,list.width(),false);
        int y=panel.y()+91;
        line(c,e.name()+"  "+e.maxLevel(),list.x()+3,y,INK,1.2f,list.width()-6,true);
        line(c,e.rarity()+"  ·  MAX LEVEL "+e.maxLevel(),list.x()+3,y+20,color(e.rarity()),.85f,list.width()-6,true);
        line(c,"APPLIES TO",list.x()+3,y+43,MUTED,.75f,list.width()-6,true);
        var applies=textRenderer.wrapLines(copy(targets(e),.85f,false),list.width()-8);
        int targetY=y+58;for(var text:applies){c.drawText(textRenderer,text,list.x()+3,targetY,INK,false);targetY+=12;}
        int top=targetY+19,bottom=panel.bottom()-23;
        c.fill(list.x(),targetY+8,list.right(),targetY+9,LINE);
        var description=textRenderer.wrapLines(copy(e.description().isBlank()?"No description provided by the server.":e.description(),1,false),list.width()-12);
        int capacity=Math.max(1,(bottom-top)/13);detailScroll=Math.clamp(detailScroll,0,Math.max(0,description.size()-capacity));
        for(int i=detailScroll;i<Math.min(description.size(),detailScroll+capacity);i++)c.drawText(textRenderer,description.get(i),list.x()+5,top+(i-detailScroll)*13,INK,false);
        if(description.size()>capacity)line(c,"Scroll to read more",list.x(),panel.bottom()-17,MUTED,.7f,list.width(),false);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled){
        if(selectedKey!=null){if(click.button()==0&&click.y()>=panel.y()+56&&click.y()<panel.y()+85){selectedKey=null;detailScroll=0;}else if(click.button()==0&&click.x()>=panel.right()-62&&click.y()<panel.y()+40)close();return true;}
        if(super.mouseClicked(click,doubled))return true;
        if(click.button()==0&&list.contains(click.x(),click.y())){
            int index=scroll+((int)click.y()-list.y())/ROW;
            if(index<rows.size()&&index<scroll+visible()){selectedKey=rows.get(index).key();detailScroll=0;search.setFocused(false);return true;}
        }
        return false;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(selectedKey!=null){detailScroll=Math.max(0,detailScroll-(int)Math.signum(vertical)*3);return true;}
        if(list.contains(x,y)){scroll=Math.clamp(scroll-(int)Math.signum(vertical)*3,0,Math.max(0,rows.size()-visible()));return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void close(){if(selectedKey!=null){selectedKey=null;detailScroll=0;}else super.close();}
    @Override public boolean shouldPause(){return false;}
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
