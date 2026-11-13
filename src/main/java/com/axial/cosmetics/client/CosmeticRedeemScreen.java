package com.axial.cosmetics.client;

import java.util.UUID;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import com.axial.cosmetics.client.ModMenuLayout.Rect;
import static com.axial.cosmetics.client.ModMenuUi.*;

/** A private code entry followed by a model-card reveal and a green impact stamp. */
final class CosmeticRedeemScreen extends NavigationScreen {
    private final WardrobeScreen wardrobe;
    private final MenuAnimation opening=new MenuAnimation();
    private UUID requestId=UUID.randomUUID();
    private String draft="";
    private Rect panel,input;
    private TextFieldWidget code;
    private NavButton submit,back;
    private boolean busy;
    private WardrobeItem unlocked;
    private long unlockedAt;
    CosmeticRedeemScreen(WardrobeScreen parent){super("Redeem cosmetic",parent);wardrobe=parent;}
    @Override protected void init(){
        int w=Math.min(382,Math.max(160,width-24)),h=Math.min(182,Math.max(130,height-24));
        panel=new Rect((width-w)/2,(height-h)/2,w,h);
        input=new Rect(panel.x()+16,panel.y()+64,w-32,27);
        code=field("AXIAL-XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX",draft,input,96);
        code.setChangedListener(value->{if(!draft.equals(value))requestId=UUID.randomUUID();draft=value;error="";});
        back=button("Back",new Rect(panel.x()+16,panel.bottom()-36,75,23),this::close);
        submit=button("Redeem",new Rect(panel.right()-120,panel.bottom()-36,104,23),this::redeem);submit.primary=true;
        setInitialFocus(code);
        if(unlocked!=null)clearChildren();
    }
    private void redeem(){
        if(busy||draft.isBlank()||unlocked!=null)return;
        busy=true;error="";
        var account=client.getSession();
        CosmeticRedemptionService.redeem(draft,requestId).whenComplete((result,failure)->client.execute(()->{
            busy=false;
            // A switched account must never receive another player's grant snapshot or celebration.
            if(client.getSession()!=account)return;
            if(failure==null&&result.account().equals(account.getUuidOrNull())) {
                CosmeticAssignments.acceptRedemption(result.account(),result.owned(),result.revision());
                if(client.currentScreen==this){
                    unlocked=result.item();unlockedAt=System.nanoTime();draft="";code.setText("");clearChildren();setFocused(null);
                }
            }else if(client.currentScreen==this){
                Throwable cause=failure;while(cause!=null&&cause.getCause()!=null)cause=cause.getCause();
                error=cause instanceof java.io.IOException?cause.getMessage():"Could not redeem this code. Please retry.";
            }
        }));
    }
    private double elapsed(){return (System.nanoTime()-unlockedAt)/1_000_000_000.0;}
    @Override public void tick(){
        if(unlocked!=null&&CosmeticUnlockAnimation.finished(elapsed())){wardrobe.refreshCosmetics();client.setScreen(wardrobe);return;}
        if(submit!=null){submit.active=!busy&&!draft.isBlank();back.active=!busy;code.setEditable(!busy);}
    }
    @Override public void close(){if(!busy&&unlocked==null)super.close();}
    @Override public boolean shouldPause(){return false;}
    @Override public boolean keyPressed(KeyInput key){if(busy||unlocked!=null)return true;if(key.key()==257||key.key()==335){redeem();return true;}return super.keyPressed(key);}
    @Override public boolean charTyped(CharInput key){return !busy&&unlocked==null&&super.charTyped(key);}
    @Override public boolean mouseClicked(Click click,boolean twice){return !busy&&unlocked==null&&super.mouseClicked(click,twice);}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        if(unlocked!=null){renderUnlock(c);return;}
        float previous=opacity(opening.progress());
        try{
            heading(c,panel,"REDEEM COSMETIC","");
            label(c,textRenderer,"Unlock a new addition to your wardrobe.",panel.x()+16,panel.y()+43,MUTED,.8f,panel.width()-32,false);
            inputFrame(c,code,input);
            label(c,textRenderer,busy?"Verifying your account and code…":error,panel.x()+16,input.bottom()+10,busy?BRAND:RED,.75f,panel.width()-32,false);
            super.render(c,mx,my,delta);
        }finally{opacity(previous);}
    }
    private void renderUnlock(DrawContext c){
        double seconds=elapsed();float appear=CosmeticUnlockAnimation.appear(seconds),fade=CosmeticUnlockAnimation.fade(seconds);
        c.fill(0,0,width,height,0xF20C1016);
        int h=Math.min(248,height-48),w=Math.min(176,Math.max(90,h*3/4));
        int x=(width-w)/2,y=(height-h)/2;
        float shake=CosmeticUnlockAnimation.shake(seconds);
        c.getMatrices().pushMatrix();
        try{
            c.getMatrices().translate(width/2f+shake,height/2f+(1-appear)*30);
            float scale=.80f+.20f*appear;c.getMatrices().scale(scale,scale);c.getMatrices().translate(-width/2f,-height/2f);
            for(int i=4;i>=1;i--)frame(c,new Rect(x-i*5,y-i*5,w+i*10,h+i*10),(0x10<<24)|0x96F5B7,0x080E271A);
            wardrobe.drawRewardCard(c,unlocked,new Rect(x,y,w,h));
        }finally{c.getMatrices().popMatrix();}
        float stamp=CosmeticUnlockAnimation.stamp(seconds);
        if(stamp>0){
            float old=opacity(stamp);
            c.getMatrices().pushMatrix();
            try{
                c.getMatrices().translate(width/2f+shake,y+h*.67f-(1-stamp)*90);
                float scale=CosmeticUnlockAnimation.stampScale(seconds);c.getMatrices().scale(scale,scale);c.getMatrices().rotate(-.09f);
                var title=copy("UNLOCKED",1.7f,true);int tw=textRenderer.getWidth(title);
                frame(c,new Rect(-tw/2-12,-10,tw+24,31),0xFFB6F5C8,0xF0152C21);
                c.drawTextWithShadow(textRenderer,title,-tw/2,-2,tint(0xFFB6F5C8));
            }finally{c.getMatrices().popMatrix();opacity(old);}
        }
        if(fade>0)c.fill(0,0,width,height,(Math.round(fade*255)<<24)|0x0C1016);
    }
}
