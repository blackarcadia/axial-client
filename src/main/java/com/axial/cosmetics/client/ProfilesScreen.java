package com.axial.cosmetics.client;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Saved client setups, with explicit updates and a second click to confirm removal. */
public final class ProfilesScreen extends NavigationScreen {
    private final ProfileStore store=ClientProfiles.store();
    private List<ProfileStore.Entry> profiles=List.of();
    private String selected,active,pendingDelete,status="";
    private int page;
    private Rect panel,nameArea;
    private TextFieldWidget name;
    private int pageSize;
    private boolean compact;
    public ProfilesScreen(Screen parent){super("PROFILES",parent);}
    @Override protected void init(){rebuild("");}
    private void rebuild(String nameValue){
        clearChildren();
        try{profiles=store.list();active=store.active();}
        catch(IOException ex){error="COULD NOT READ PROFILES.";}
        int w=Math.min(500,width-16),h=Math.min(330,height-16);
        panel=new Rect((width-w)/2,(height-h)/2,w,h);compact=h<230;
        button("BACK",new Rect(panel.right()-57,panel.y()+(compact?4:7),46,compact?20:22),this::close);
        nameArea=new Rect(panel.x()+12,panel.y()+(compact?28:40),w-112,26);
        name=field("PROFILE NAME",nameValue,nameArea,32);
        button("SAVE NEW",new Rect(nameArea.right()+6,nameArea.y(),82,26),()->run(()->{
            ClientProfiles.saveCurrent();var entry=store.create(name.getText());selected=entry.id();pendingDelete=null;
            page=(profiles.size())/pageSize;status="PROFILE SAVED";rebuild(entry.name());
        })).primary=true;
        int listTop=panel.y()+(compact?58:76);
        pageSize=Math.max(1,(h-(compact?125:174))/28);
        int pages=Math.max(1,(profiles.size()+pageSize-1)/pageSize);page=Math.clamp(page,0,pages-1);
        for(int i=page*pageSize;i<Math.min(profiles.size(),(page+1)*pageSize);i++){
            var entry=profiles.get(i);
            var row=button(entry.name()+(entry.id().equals(active)?"  •  CURRENT":""),new Rect(panel.x()+12,listTop+(i%pageSize)*28,w-24,24),()->{
                selected=entry.id();pendingDelete=null;status="";rebuild(entry.name());
            });row.selected=entry.id().equals(selected);
        }
        int pagerY=panel.bottom()-(compact?61:92);
        button("PREV",new Rect(panel.x()+12,pagerY,48,20),()->{page--;pendingDelete=null;rebuild(name.getText());}).active=page>0;
        button("NEXT",new Rect(panel.right()-60,pagerY,48,20),()->{page++;pendingDelete=null;rebuild(name.getText());}).active=page+1<pages;
        int actionY=panel.bottom()-(compact?39:65),cell=(w-42)/4;
        button("LOAD",new Rect(panel.x()+12,actionY,cell,24),()->run(()->{
            ClientProfiles.load(selected);pendingDelete=null;status="PROFILE LOADED";rebuild(name.getText());
        })).active=selected!=null;
        button("UPDATE",new Rect(panel.x()+18+cell,actionY,cell,24),()->run(()->{
            ClientProfiles.saveCurrent();store.update(selected);pendingDelete=null;status="CURRENT SETUP SAVED";rebuild(name.getText());
        })).active=selected!=null;
        button("RENAME",new Rect(panel.x()+24+cell*2,actionY,cell,24),()->run(()->{
            store.rename(selected,name.getText());pendingDelete=null;status="PROFILE RENAMED";rebuild(name.getText());
        })).active=selected!=null;
        var delete=button(selected!=null&&selected.equals(pendingDelete)?"CONFIRM?":"DELETE",new Rect(panel.x()+30+cell*3,actionY,cell,24),()->run(()->{
            if(!selected.equals(pendingDelete)){pendingDelete=selected;status="CLICK CONFIRM TO DELETE THIS PROFILE";rebuild(name.getText());return;}
            store.delete(selected);selected=null;pendingDelete=null;status="PROFILE DELETED";rebuild("");
        }));delete.active=selected!=null;delete.danger=true;
    }
    private interface Operation{void run() throws IOException;}
    private void run(Operation operation){
        try{error="";operation.run();}
        catch(IOException|RuntimeException ex){error=ex.getMessage()==null?"COULD NOT SAVE PROFILE.":ex.getMessage();org.slf4j.LoggerFactory.getLogger(getClass()).warn("Profile operation failed",ex);}
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        frame(c,panel,PANEL_BORDER,PANEL);
        MenuHeader.draw(c,textRenderer,panel.x()+12,panel.y()+5,panel.width()-82,compact?20:26,"PROFILES");
        inputFrame(c,name,nameArea);
        if(profiles.isEmpty())line(c,"SAVE YOUR CURRENT SETUP TO CREATE A PROFILE",panel.x()+15,panel.y()+(compact?63:82),MUTED,.8f,panel.width()-30,false);
        int pages=Math.max(1,(profiles.size()+pageSize-1)/pageSize);
        line(c,"PAGE "+(page+1)+" / "+pages,panel.x()+73,panel.bottom()-(compact?55:86),MUTED,.8f,panel.width()-146,false);
        if(!compact)line(c,"LOAD REPLACES SETTINGS • UPDATE SAVES CURRENT SETUP",panel.x()+12,panel.bottom()-33,MUTED,.75f,panel.width()-24,false);
        line(c,error.isBlank()?status:error,panel.x()+12,panel.bottom()-(compact?11:18),error.isBlank()?INK:RED,.75f,panel.width()-24,false);
        super.render(c,mx,my,delta);
    }
}
