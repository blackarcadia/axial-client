package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** Shares the client's charcoal frames, typography, purple accent and button sounds. */
public final class FeedbackScreen extends NavigationScreen {
    private String draft="",status="";
    private UUID requestId=UUID.randomUUID();
    private boolean sending,sent;
    private Rect panel,box;
    private EditBoxWidget message;
    private NavButton send;
    public FeedbackScreen(Screen parent){super("Feedback",parent);}
    @Override protected void init() {
        int w=Math.min(440,width-20),h=Math.min(300,height-16);
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        int top=panel.y()+(h<230?38:66);
        box=new Rect(panel.x()+16,top,w-32,Math.max(35,panel.bottom()-top-83));
        message=EditBoxWidget.builder().x(box.x()+6).y(box.y()+6).hasBackground(false).hasOverlay(false)
                .textColor(INK).cursorColor(BRAND).textShadow(false).placeholder(copy("What would make Axial better?",.85f,false))
                .build(textRenderer,box.width()-12,box.height()-12,copy("Your feedback",1,false));
        message.setMaxLength(2000);message.setText(draft);
        message.setChangeListener(value->{if(!value.equals(draft))requestId=UUID.randomUUID();draft=value;});
        addDrawableChild(message);
        send=button("Send Feedback",new Rect(panel.right()-146,panel.bottom()-34,130,24),this::submit);send.primary=true;
        button("Back",new Rect(panel.x()+16,panel.bottom()-34,70,24),this::close);
        setInitialFocus(message);
    }
    private void submit() {
        if(sending||sent||draft.strip().length()<10)return;
        sending=true;message.setFocused(false);error="";status="Sending securely…";
        FeedbackService.submit(draft,requestId).whenComplete((id,failure)->client.execute(()->{
            sending=false;
            if(failure==null){sent=true;status="Thanks! Feedback #"+id+" sent to the team.";}
            else {status="";var cause=failure.getCause();error=cause instanceof java.io.IOException?cause.getMessage():"Could not send feedback. Please retry.";}
        }));
    }
    @Override public void render(DrawContext c,int mx,int my,float dt) {
        heading(c,panel,"Feedback","Ideas, bugs or suggestions — straight to the Axial team.");
        frame(c,box,message.isFocused()?BRAND:LINE,FIELD);
        send.active=!sending&&!sent&&draft.strip().length()>=10;
        send.label(sending?"Sending…":sent?"Sent":"Send Feedback");
        message.active=!sending&&!sent;
        line(c,draft.length()+" / 2000",box.x(),box.bottom()+6,MUTED,.75f,box.width(),false);
        String hint=!error.isEmpty()?error:!status.isEmpty()?status:"Linked to your Minecraft account. Approval appears on login.";
        line(c,hint,panel.x()+16,panel.bottom()-49,!error.isEmpty()?RED:sent?GREEN:MUTED,.75f,panel.width()-32,false);
        for(var child:children())if(child instanceof net.minecraft.client.gui.Drawable drawable)drawable.render(c,mx,my,dt);
    }
}
