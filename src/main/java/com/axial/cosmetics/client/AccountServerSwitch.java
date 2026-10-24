package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Rejoins the remembered server as a fresh login, without the old account's cookies. */
final class AccountServerSwitch {
    private AccountServerSwitch(){}
    static Screen serverList(){return new MultiplayerScreen(new TitleScreen());}
    static AccountSwitchFlow capture(MinecraftClient client){
        if(client.isInSingleplayer())throw new IllegalStateException("Leave the singleplayer world before switching accounts.");
        var connection=client.getNetworkHandler();
        ServerInfo source=client.getCurrentServerEntry();
        if(source==null&&connection!=null)source=connection.getServerInfo();
        if((connection!=null||client.world!=null)&&source==null)throw new IllegalStateException("The current server address is unavailable. Please reconnect from the server list.");
        ServerInfo target;
        if(source!=null){
            target=new ServerInfo(source.name,source.address,source.getServerType());target.copyWithSettingsFrom(source);
        }else target=null;
        return new AccountSwitchFlow(new AccountSwitchFlow.Host(){
            public Object account(){return client.getSession();}
            public Object connection(){return client.getNetworkHandler();}
            public boolean hasWorld(){return client.world!=null;}
            public void disconnect(){
                if(client.world!=null)client.world.disconnect(Text.literal("Switching accounts"));
                client.disconnect(new ProgressScreen(true),false,true);
            }
            public void reconnect(){
                ConnectScreen.connect(serverList(),client,ServerAddress.parse(target.address),target,false,null);
            }
        },connection!=null||client.world!=null);
    }
}
