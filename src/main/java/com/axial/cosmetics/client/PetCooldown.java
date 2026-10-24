package com.axial.cosmetics.client;

import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

/** PrisonsCore's persistent pet item fields; never infer cooldowns from lore. */
public final class PetCooldown {
    public record Pet(String instance,long until){}
    private PetCooldown(){}
    public static Pet read(NbtCompound data){
        Pet pet=fields(data);
        return pet!=null?pet:fields(data.getCompoundOrEmpty("PublicBukkitValues"));
    }
    private static Pet fields(NbtCompound data){
        if(data.getString("prisonscore:pet_id","").isBlank())return null;
        String instance=data.getString("prisonscore:pet_instance","");
        long until=data.getLong("prisonscore:pet_cooldown_until",0L);
        if(until<=0)return null;
        try{return new Pet(UUID.fromString(instance).toString(),until);}catch(IllegalArgumentException ex){return null;}
    }
    public static String label(long remainingMillis){
        if(remainingMillis<=0)return "";
        long seconds=1+(remainingMillis-1)/1000;
        if(remainingMillis<60000)return seconds+"s";
        if(remainingMillis<3600000)return (1+(seconds-1)/60)+"m";
        if(remainingMillis<86400000)return (1+(seconds-1)/3600)+"h";
        return (1+(seconds-1)/86400)+"d";
    }
}
