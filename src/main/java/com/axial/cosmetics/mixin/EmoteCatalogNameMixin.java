package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmoteCatalogNames;
import com.zigythebird.playeranimcore.animation.Animation;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Display aliases preserve the original animation bytes, audio, author and UUID. */
@Mixin(targets="io.github.kosmx.emotes.main.EmoteHolder",remap=false)
public abstract class EmoteCatalogNameMixin {
    @Shadow @Final public Animation emote;
    @Shadow @Final @Mutable public Text name;
    @Inject(method="<init>",at=@At("TAIL"),remap=false)
    private void axial$useCatalogName(CallbackInfo ci) {
        String label=EmoteCatalogNames.label(emote.uuid());if(label!=null)name=Text.literal(label);
    }
}
