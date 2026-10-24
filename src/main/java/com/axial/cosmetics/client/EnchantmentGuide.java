package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import java.nio.ByteBuffer;

/** Poll only while the guide is open; keep no state across server connections. */
public final class EnchantmentGuide {
    static EnchantmentCatalogue catalogue=new EnchantmentCatalogue();
    static String status="Waiting for the server's enchantment catalogue...";
    private static long nextRequest,opened;
    public static void reset(){catalogue=new EnchantmentCatalogue();nextRequest=opened=0;status="Waiting for the server's enchantment catalogue...";}
    static void open(){nextRequest=0;opened=System.currentTimeMillis();}
    public static void tick(MinecraftClient client){
        if(!(client.currentScreen instanceof EnchantmentsScreen))return;
        long now=System.currentTimeMillis();if(now<nextRequest)return;nextRequest=now+5000;
        if(client.getNetworkHandler()==null){status="Join a server to view its custom enchantments.";return;}
        if(!ClientPlayNetworking.canSend(EnchantmentPayload.ID)){status="This server does not provide an enchantment catalogue.";return;}
        if(catalogue.revision==0&&now-opened>10000)status="No catalogue received. Check that PrisonsCore is updated on the server.";
        ClientPlayNetworking.send(new EnchantmentPayload(ByteBuffer.allocate(9).put((byte)1).putLong(catalogue.revision).array()));
    }
    public static void receive(byte[] data){
        try{catalogue.receive(data);if(catalogue.revision!=0)status=catalogue.entries.isEmpty()?"No custom enchantments configured.":"";}
        catch(java.io.IOException|RuntimeException ex){status="Could not read the server's enchantment catalogue. Retrying...";}
    }
}
