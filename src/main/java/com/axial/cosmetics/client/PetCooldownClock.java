package com.axial.cosmetics.client;

import java.util.HashMap;
import java.util.Map;

/** Remember native server timing after its entry expires, even with clock skew. */
final class PetCooldownClock {
    private record Timer(long serverUntil,long localUntil){}
    private final Map<String,Timer> timers=new HashMap<>();
    void reset(){timers.clear();}
    long remaining(PetCooldown.Pet pet,long now,Long nativeRemaining){
        Timer timer=timers.get(pet.instance());
        if(nativeRemaining!=null){
            long remaining=Math.max(0L,nativeRemaining);
            if(timers.size()>=512)timers.clear();
            timers.put(pet.instance(),new Timer(pet.until(),now+remaining));
            return remaining;
        }
        return Math.max(0L,(timer!=null&&timer.serverUntil()==pet.until()?timer.localUntil():pet.until())-now);
    }
}
