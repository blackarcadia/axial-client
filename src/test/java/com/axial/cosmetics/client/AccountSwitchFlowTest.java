package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountSwitchFlowTest {
    private static final class Host implements AccountSwitchFlow.Host {
        Object account=new Object(),connection=new Object();
        boolean world=true,unload=true;
        Object next=new Object();
        final List<String> events=new ArrayList<>();
        public Object account(){return account;}
        public Object connection(){return connection;}
        public boolean hasWorld(){return world;}
        public void disconnect(){events.add("disconnect");if(unload){connection=null;world=false;}}
        public void reconnect(){assertSame(next,account,"New login must use the selected account");events.add("connect");}
        void apply(){assertNull(connection);assertFalse(world);events.add("apply");account=next;}
    }
    @Test void validatedAccountDisconnectsOldSessionBeforeSwitchAndRejoinsOnlyAfterNewSessionIsApplied(){
        var host=new Host();var flow=new AccountSwitchFlow(host,true);
        assertTrue(host.events.isEmpty(),"Preparing a sign-in must leave the old account connected");
        flow.finish(host::apply,()->host.events.add("save selection"));
        assertEquals(List.of("disconnect","apply","save selection","connect"),host.events);assertTrue(flow.disconnected());
    }
    @Test void mainMenuSwitchesDoNotAttemptAReconnection(){
        var host=new Host();host.connection=null;host.world=false;var flow=new AccountSwitchFlow(host,false);
        flow.finish(host::apply,()->host.events.add("save selection"));
        assertEquals(List.of("apply","save selection"),host.events);assertFalse(flow.disconnected());
    }
    @Test void changingServersDuringAuthenticationCannotDisconnectOrRejoinTheWrongServer(){
        var host=new Host();var flow=new AccountSwitchFlow(host,true);host.connection=new Object();
        assertThrows(IllegalStateException.class,()->flow.finish(host::apply,()->{}));assertTrue(host.events.isEmpty());assertFalse(flow.disconnected());
    }
    @Test void aConcurrentAccountSwitchInvalidatesTheOlderRequest(){
        var host=new Host();var flow=new AccountSwitchFlow(host,true);host.account=new Object();
        assertThrows(IllegalStateException.class,()->flow.finish(host::apply,()->{}));assertTrue(host.events.isEmpty());
    }
    @Test void failedDisconnectNeverMutatesTheLiveAccount(){
        var host=new Host();var old=host.account;host.unload=false;var flow=new AccountSwitchFlow(host,true);
        assertThrows(IllegalStateException.class,()->flow.finish(host::apply,()->{}));assertSame(old,host.account);assertEquals(List.of("disconnect"),host.events);
    }
    @Test void failedSessionApplicationDoesNotReconnectWithThePreviousIdentity(){
        var host=new Host();var flow=new AccountSwitchFlow(host,true);
        assertThrows(IllegalStateException.class,()->flow.finish(()->{throw new IllegalStateException("test failure");},()->host.events.add("save")));
        assertEquals(List.of("disconnect"),host.events);
    }
    @Test void completedRequestCannotStartADuplicateConnection(){
        var host=new Host();var flow=new AccountSwitchFlow(host,true);flow.finish(host::apply,()->{});
        assertThrows(IllegalStateException.class,()->flow.finish(host::apply,()->{}));assertEquals(List.of("disconnect","apply","connect"),host.events);
    }
}
