package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import java.nio.ByteBuffer;

/** Poll only while the guide is open; keep no state across server connections. */
public final class GangDirectoryGuide {
    static GangDirectory catalogue=new GangDirectory();
    static String status="Waiting for the server's gang directory...";
    private static long nextRequest,opened;
    public static void reset(){catalogue=new GangDirectory();GangPlayerHeads.reset();nextRequest=opened=0;status="Waiting for the server's gang directory...";}
    static void open(){nextRequest=0;opened=System.currentTimeMillis();}
    public static void tick(MinecraftClient client){
        if(!(client.currentScreen instanceof GangsScreen))return;
        long now=System.currentTimeMillis();if(now<nextRequest)return;nextRequest=now+5000;
        if(client.getNetworkHandler()==null){status="Join a server to view its gangs.";return;}
        if(!ClientPlayNetworking.canSend(GangDirectoryPayload.ID)){status="This server does not provide a gang directory.";return;}
        if(catalogue.revision==0&&now-opened>10000)status="The server is still preparing its gang directory...";
        ClientPlayNetworking.send(new GangDirectoryPayload(ByteBuffer.allocate(9).put((byte)1).putLong(catalogue.revision).array()));
    }
    public static void receive(byte[] data){
        try{catalogue.receive(data);if(catalogue.revision!=0)status=catalogue.gangs.isEmpty()?"No gangs on this server.":"";}
        catch(java.io.IOException|RuntimeException ex){status="Could not read the server's gang directory. Retrying...";}
    }
}
