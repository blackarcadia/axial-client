package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.NpcReplayProcessing;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.replaymod.editor.gui.MarkerProcessor",remap=false)
public abstract class NpcReplayProcessingMixin {
    @WrapMethod(method="apply",remap=false)
    private static List<?> axial$preserveActorTrack(Path input,Consumer<Float> progress,Operation<List<?>> original) {
        // Capture before Replay Mod moves the source to raw/ and creates fresh archives.
        var track=NpcReplayProcessing.capture(input);
        var outputs=original.call(input,progress);
        if(track!=null)track.finish(outputs);
        return outputs;
    }
}
