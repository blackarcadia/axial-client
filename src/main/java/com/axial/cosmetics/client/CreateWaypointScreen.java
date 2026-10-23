package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.math.BlockPos;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** A draft editor: name, coordinates and colour are committed together on Save. */
public final class CreateWaypointScreen extends NavigationScreen {
    private final WaypointConfig.Entry existing;
    private final String world,dimension;
    private String draftName="",draftX,draftY,draftZ;
    private int color=BRAND;
    private TextFieldWidget name,x,y,z;
    private Rect panel,nameBox,xBox,yBox,zBox;
    private NavButton save,colour;
    public CreateWaypointScreen(){this(null,null,null);}
    public CreateWaypointScreen(Screen parent,BlockPos position){this(parent,position,null);}
    public CreateWaypointScreen(Screen parent,WaypointConfig.Entry existing){this(parent,null,existing);}
    private CreateWaypointScreen(Screen parent,BlockPos position,WaypointConfig.Entry existing) {
        super(existing==null?"New Waypoint":"Edit Waypoint",parent);this.existing=existing;
        var client=MinecraftClient.getInstance();
        world=existing==null?WaypointConfig.worldId(client):existing.world();
        dimension=existing==null?(client.world==null?"minecraft:overworld":client.world.getRegistryKey().getValue().toString()):existing.dimension();
        BlockPos p=existing!=null?new BlockPos(existing.x(),existing.y(),existing.z()):position!=null?position:client.player!=null?client.player.getBlockPos():BlockPos.ORIGIN;
        draftX=""+p.getX();draftY=""+p.getY();draftZ=""+p.getZ();
        if(existing!=null){draftName=existing.name();color=existing.color();}
    }
    @Override protected void init() {
        clearChildren();int w=Math.min(386,width-20),h=Math.min(258,height-12);panel=new Rect((width-w)/2,(height-h)/2,w,h);
        // Compact spacing keeps every field reachable at high GUI scales.
        int body=panel.y()+(h<190?35:h<230?39:62),gap=h<190?29:h<230?31:40;
        nameBox=new Rect(panel.x()+16,body,w-32,26);
        int cw=(w-44)/3;
        xBox=new Rect(panel.x()+16,body+gap,cw,26);yBox=new Rect(xBox.right()+6,body+gap,cw,26);zBox=new Rect(yBox.right()+6,body+gap,cw,26);
        name=field("Waypoint name",draftName,nameBox,48);name.setChangedListener(v->draftName=v);
        x=field("X",draftX,xBox,10);x.setChangedListener(v->draftX=v);
        y=field("Y",draftY,yBox,10);y.setChangedListener(v->draftY=v);
        z=field("Z",draftZ,zBox,10);z.setChangedListener(v->draftZ=v);
        colour=button("Choose marker colour",new Rect(panel.x()+16,body+gap*2,w-32,h<190?19:24),()->client.setScreen(new CrosshairColorPickerScreen(this,"WAYPOINT",color,v->color=v,()->{})));
        colour.selected=true;
        save=button(existing==null?"Create Waypoint":"Save Changes",new Rect(panel.right()-144,panel.bottom()-(h<190?27:34),128,h<190?19:24),this::save);save.primary=true;
        button("Cancel",new Rect(panel.x()+16,panel.bottom()-(h<190?27:34),78,h<190?19:24),this::close);
        setInitialFocus(name);
    }
    private BlockPos position() {
        try {
            int px=Integer.parseInt(draftX.strip()),py=Integer.parseInt(draftY.strip()),pz=Integer.parseInt(draftZ.strip());
            if(Math.abs((long)px)>30_000_000||Math.abs((long)pz)>30_000_000||py<-2048||py>2048)return null;
            return new BlockPos(px,py,pz);
        }catch(NumberFormatException ex){return null;}
    }
    private void save() {
        if(existing==null&&client.world==null){error="Join a world before creating a waypoint.";return;}
        var pos=position();if(draftName.isBlank()||pos==null){error="Enter a name and valid X, Y, Z coordinates.";return;}
        if(existing==null)WaypointConfig.create(draftName,world,dimension,pos,color);
        else WaypointConfig.update(existing,draftName,pos.getX(),pos.getY(),pos.getZ(),color);
        close();
    }
    @Override public void render(DrawContext c,int mx,int my,float dt) {
        boolean compact=panel.height()<230;
        heading(c,panel,title.getString(),compact?"":dimension.replace("minecraft:",""));
        inputFrame(c,name,nameBox);inputFrame(c,x,xBox);inputFrame(c,y,yBox);inputFrame(c,z,zBox);
        save.active=!draftName.isBlank()&&position()!=null&&(existing!=null||client.world!=null);
        for(var child:children())if(child instanceof net.minecraft.client.gui.Drawable d)d.render(c,mx,my,dt);
        rounded(c,colour.getX()+7,colour.getY()+(colour.getHeight()-9)/2,9,9,color);
        if(!compact) {
            line(c,"X",xBox.x(),xBox.y()-10,MUTED,.75f,30,false);line(c,"Y",yBox.x(),yBox.y()-10,MUTED,.75f,30,false);line(c,"Z",zBox.x(),zBox.y()-10,MUTED,.75f,30,false);
        }
        if(!error.isEmpty())line(c,error,panel.x()+16,panel.bottom()-48,RED,.75f,panel.width()-32,false);
    }
    @Override public boolean keyPressed(KeyInput key) {
        if(key.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER){attempt(this::save);return true;}
        return super.keyPressed(key);
    }
}
