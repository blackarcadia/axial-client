package com.axial.cosmetics.client;

import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.animation.*;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

/** Detached GUI animation stack: no world, network events, player changes or emote audio. */
final class WardrobeEmotePreview extends AvatarAnimManager {
    private final PreviewController controller=new PreviewController();
    private final long started=System.nanoTime();
    private Animation animation;
    WardrobeEmotePreview(){super(null);addAnimLayer(1000,controller);}
    void attach(PlayerEntityRenderState state,WardrobeEmote emote) {
        var holder=emote.holder();if(holder==null)return;
        if(animation!=holder.emote){animation=holder.emote;controller.triggerAnimation(animation,0);}
        float time=(float)(((System.nanoTime()-started)/50_000_000d)%animation.length());
        controller.sample(time);setTickDelta(0);
        ((IAvatarAnimationState)state).playerAnimLib$setAnimManager(this);
    }
    @Override public void handleAnimations(float partial,boolean tick,boolean firstPerson) {
        // The detached stack is sampled explicitly; never dereference a world avatar.
    }
    static final class PreviewController extends HumanoidAnimationController {
        PreviewController(){super((c,d,s)->PlayState.CONTINUE,MolangLoader::createNewEngine);}
        void sample(float time){tick=0;startAnimFrom=time;setupAnim(new AnimationData(0,0,false));}
    }
}
