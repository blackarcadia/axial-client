package com.axial.cosmetics.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.LongSupplier;

/** Monotonic countdown corrected by authoritative server tag and clear events. */
final class CombatCooldownState {
    private final LongSupplier clock;
    private long expires;
    private int duration;
    CombatCooldownState(){this(System::nanoTime);}
    CombatCooldownState(LongSupplier clock){this.clock=clock;}
    void receive(byte[] packet)throws IOException{
        if(packet.length!=9||packet[0]!=1)throw new IOException("Invalid combat cooldown packet");
        var in=ByteBuffer.wrap(packet);in.get();int remaining=in.getInt(),total=in.getInt();
        if(total<1||total>86_400_000||remaining<0||remaining>total)throw new IOException("Invalid combat cooldown duration");
        duration=total;expires=clock.getAsLong()+remaining*1_000_000L;
    }
    void reset(){expires=0;duration=0;}
    long remainingMillis(){return duration==0?0:Math.max(0,(expires-clock.getAsLong()+999_999)/1_000_000);}
    float fraction(){return duration==0?0:Math.clamp(remainingMillis()/(float)duration,0,1);}
}
