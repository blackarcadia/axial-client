package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** A staged, animated feedback composer using the shared client controls. */
public final class FeedbackScreen extends NavigationScreen {
    private enum Page {CATEGORY,KIND,MESSAGE}
    private Page page=Page.CATEGORY,pending;
    private FeedbackCategory category;
    private FeedbackKind kind;
    private String draft="",status="";
    private UUID requestId=UUID.randomUUID();
    private boolean sending,exiting;
    private long acceptedAt;
    private MenuAnimation motion=new MenuAnimation();
    private Rect panel,box;
    private EditBoxWidget message;
    private FeedbackButton send,back;
    public FeedbackScreen(Screen parent){super("Feedback",parent);}
    @Override protected void init(){build();}
    private void build(){
        clearChildren();message=null;send=null;
        int w=Math.min(440,width-24),h=Math.min(280,height-62);
        panel=new Rect((width-w)/2,(height-h-34)/2,w,h);
        int top=panel.y()+(h<190?34:62);
        if(page==Page.MESSAGE){
            box=new Rect(panel.x()+16,top,w-32,Math.max(26,panel.bottom()-top-37));
            message=EditBoxWidget.builder().x(box.x()+6).y(box.y()+6).hasBackground(false).hasOverlay(false)
                    .textColor(INK).cursorColor(BRAND).textShadow(false).placeholder(copy(kind==FeedbackKind.BUG?"What happened? How can we reproduce it?":"What would you like us to add or improve?",.85f,false))
                    .build(textRenderer,box.width()-12,box.height()-12,copy("Your feedback",1,false));
            message.setMaxLength(2000);message.setText(draft);
            message.setChangeListener(value->{if(!value.equals(draft))requestId=UUID.randomUUID();draft=value;});
            addDrawableChild(message);setInitialFocus(message);
            send=control("Submit",new Rect(panel.right()-132,panel.bottom()+9,132,24),this::submit);send.primary=true;
        }else{
            int gap=10,row=Math.min(48,(panel.bottom()-top-16-gap)/2);
            if(page==Page.CATEGORY){
                control("PRISONS FEEDBACK",new Rect(panel.x()+16,top,w-32,row),()->choose(FeedbackCategory.PRISONS));
                control("CLIENT FEEDBACK",new Rect(panel.x()+16,top+row+gap,w-32,row),()->choose(FeedbackCategory.CLIENT));
            }else{
                control(category.label+" BUG",new Rect(panel.x()+16,top,w-32,row),()->choose(FeedbackKind.BUG));
                control(category.label+" SUGGESTION",new Rect(panel.x()+16,top+row+gap,w-32,row),()->choose(FeedbackKind.SUGGESTION));
            }
        }
        back=control("Back",new Rect(panel.x(),panel.bottom()+9,88,24),this::close);
    }
    private FeedbackButton control(String label,Rect bounds,Runnable action){return addDrawableChild(new FeedbackButton(label,bounds,action));}
    private void choose(FeedbackCategory value){if(category!=value){category=value;kind=null;requestId=UUID.randomUUID();}transition(Page.KIND);}
    private void choose(FeedbackKind value){if(kind!=value){kind=value;requestId=UUID.randomUUID();}transition(Page.MESSAGE);}
    private void transition(Page target){if(pending!=null||sending||acceptedAt!=0)return;pending=target;error="";status="";motion.close();}
    @Override public void close(){
        if(sending||acceptedAt!=0||pending!=null)return;
        if(page==Page.MESSAGE)transition(Page.KIND);
        else if(page==Page.KIND)transition(Page.CATEGORY);
        else {exiting=true;motion.close();}
    }
    @Override public void tick(){
        if(acceptedAt!=0&&FeedbackSendAnimation.finished(elapsed())){client.setScreen(parent);return;}
        if(motion.finished()){
            if(exiting){client.setScreen(parent);return;}
            if(pending!=null){page=pending;pending=null;build();motion=new MenuAnimation();}
        }
    }
    private boolean interactive(){return !exiting&&pending==null&&acceptedAt==0&&!sending&&motion.progress()>.995f;}
    @Override public boolean mouseClicked(Click click,boolean doubled){return interactive()&&super.mouseClicked(click,doubled);}
    @Override public boolean mouseDragged(Click click,double dx,double dy){return interactive()&&super.mouseDragged(click,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return interactive()&&super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyInput input){return !interactive()||super.keyPressed(input);}
    @Override public boolean charTyped(net.minecraft.client.input.CharInput input){return !interactive()||super.charTyped(input);}
    private void submit(){
        if(sending||acceptedAt!=0||page!=Page.MESSAGE||category==null||kind==null||draft.strip().length()<10)return;
        sending=true;message.setFocused(false);error="";status="Sending securely…";
        FeedbackService.submit(draft,requestId,category,kind).whenComplete((id,failure)->client.execute(()->{
            sending=false;
            if(failure==null){acceptedAt=System.nanoTime();message.setFocused(false);setFocused(null);}
            else{status="";Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();error=cause instanceof java.io.IOException?cause.getMessage():"Could not send feedback. Please retry.";}
        }));
    }
    private double elapsed(){return (System.nanoTime()-acceptedAt)/1_000_000_000.0;}
    @Override public void render(DrawContext c,int mx,int my,float dt){
        double sent=acceptedAt==0?-1:elapsed();
        float progress=motion.progress(),fold=sent<0?0:FeedbackSendAnimation.fold(sent);
        float scale=(.94f+.06f*progress)*(1-.95f*fold),alpha=progress*(1-fold);
        if(alpha>.005f){
            float previous=opacity(alpha);
            c.getMatrices().pushMatrix();
            try{
                c.getMatrices().translate(width/2f,height/2f+(1-progress)*10);c.getMatrices().scale(scale,scale);c.getMatrices().translate(-width/2f,-height/2f);
                drawForm(c,interactive()?mx:-1000,interactive()?my:-1000,dt);
            }finally{c.getMatrices().popMatrix();opacity(previous);}
        }
        if(sent>=0)drawEnvelope(c,sent);
    }
    private void drawForm(DrawContext c,int mx,int my,float dt){
        String title=page==Page.CATEGORY?"Feedback":page==Page.KIND?category.label+" FEEDBACK":category.label+" "+kind.label;
        String subtitle=page==Page.CATEGORY?"Choose where your feedback belongs.":page==Page.KIND?"Report a bug or share your next idea.":"Straight to the Axial development team.";
        heading(c,panel,title,subtitle);
        back.active=!sending&&acceptedAt==0;
        if(message!=null){
            frame(c,box,message.isFocused()?BRAND:LINE,FIELD);
            send.active=!sending&&acceptedAt==0&&draft.strip().length()>=10;send.label(sending?"Sending…":"Submit");message.active=!sending&&acceptedAt==0;
            line(c,draft.length()+" / 2000",box.x(),box.bottom()+5,tint(MUTED),.7f,box.width(),false);
            String hint=!error.isEmpty()?error:!status.isEmpty()?status:"Linked to your Minecraft account.";
            line(c,hint,panel.x()+16,panel.bottom()-13,tint(error.isEmpty()?MUTED:RED),.7f,panel.width()-32,false);
        }
        for(var child:children())if(child instanceof net.minecraft.client.gui.Drawable drawable)drawable.render(c,mx,my,dt);
    }
    private void drawEnvelope(DrawContext c,double seconds){
        float appear=FeedbackSendAnimation.appear(seconds),seal=FeedbackSendAnimation.seal(seconds),flight=FeedbackSendAnimation.flight(seconds);
        if(appear>0&&flight<1){
            float old=opacity(appear);
            c.getMatrices().pushMatrix();
            try{
                float x=width/2f+(float)Math.sin(flight*Math.PI)*22,y=height/2f-(height/2f+100)*flight;
                c.getMatrices().translate(x,y);c.getMatrices().rotate(-flight*.13f);c.getMatrices().scale(.65f+.35f*appear,.65f+.35f*appear);
                frame(c,new Rect(-47,-25,94,60),BRAND,0xFFF0E7FA);
                if(seal<.9){c.fill(-33,-46+(int)(seal*30),33,9,tint(0xFFF8F7FC));c.fill(-23,-34+(int)(seal*30),23,-32+(int)(seal*30),tint(0xFFB8A3CB));}
                triangle(c,-45,-23,45,-23,0,-55+seal*64,0xFFD2B6EB);
                triangle(c,-45,-21,-45,32,9,9,0xFFE4D3F1);
                triangle(c,45,-21,45,32,-9,9,0xFFE4D3F1);
                triangle(c,-45,32,45,32,0,-1,0xFFECDEF6);
                if(seal>.85f)rounded(c,-5,1,10,10,BRAND);
            }finally{c.getMatrices().popMatrix();opacity(old);}
        }
        float text=FeedbackSendAnimation.text(seconds);
        if(text>0){
            float size=width<260?1.35f:1.65f;
            var label=copy("FEEDBACK SENT",size,true);
            int textWidth=textRenderer.getWidth(label),w=Math.min(width-24,textWidth+44);
            int x=(width-w)/2,y=Math.min(height-52,height/2+46);
            float old=opacity(text);
            try{
                frame(c,new Rect(x,y,w,44),BRAND,0xF5101319);
                rounded(c,x+(w-28)/2,y+7,28,2,BRAND);
            }finally{opacity(old);}
            c.drawTextWithShadow(textRenderer,label,(width-textWidth)/2,y+17,(Math.max(4,Math.round(text*255))<<24)|(INK&0xFFFFFF));
        }
    }
    /** Scan-line triangles keep the envelope a code-native asset with smooth transformed motion. */
    private static void triangle(DrawContext c,float ax,float ay,float bx,float by,float cx,float cy,int color){
        float[] xs={ax,bx,cx},ys={ay,by,cy};
        for(int y=(int)Math.floor(Math.min(ay,Math.min(by,cy)));y<=Math.ceil(Math.max(ay,Math.max(by,cy)));y++){
            float left=Float.POSITIVE_INFINITY,right=Float.NEGATIVE_INFINITY;
            for(int i=0;i<3;i++){int j=(i+1)%3;if(ys[i]==ys[j]||y<Math.min(ys[i],ys[j])||y>=Math.max(ys[i],ys[j]))continue;float x=xs[i]+(y-ys[i])*(xs[j]-xs[i])/(ys[j]-ys[i]);left=Math.min(left,x);right=Math.max(right,x);}
            if(left<=right)c.fill((int)Math.floor(left),y,(int)Math.ceil(right),y+1,tint(color));
        }
    }
    private final class FeedbackButton extends NavButton {
        private float hover;
        private long lastFrame=System.nanoTime();
        FeedbackButton(String label,Rect bounds,Runnable action){super(label,bounds,action);}
        @Override protected void drawContents(DrawContext c,int mx,int my,float dt){
            long now=System.nanoTime();hover=MenuAnimation.hover(hover,active&&(isHovered()||isFocused()),(now-lastFrame)/1_000_000_000f);lastFrame=now;
            frame(c,new Rect(getX(),getY(),getWidth(),getHeight()),mixColor(primary?SELECTED_LINE:LINE,BRAND,hover),mixColor(primary?SELECTED:FIELD,HOVER,hover));
            int w=textRenderer.getWidth(copy(label,.85f,false));
            line(c,label,getX()+Math.max(6,(getWidth()-w)/2),getY()+(getHeight()-8)/2,tint(active?INK:MUTED),.85f,getWidth()-12,false);
        }
    }
}
