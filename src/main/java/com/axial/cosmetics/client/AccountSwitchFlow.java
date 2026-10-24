package com.axial.cosmetics.client;

/** Enforces identity/connection ordering independently of the authentication UI. */
final class AccountSwitchFlow {
    interface Host {
        Object account();
        Object connection();
        boolean hasWorld();
        void disconnect();
        void reconnect();
    }
    private final Host host;
    private final Object account,connection;
    private final boolean reconnect;
    private boolean finished,disconnected;
    AccountSwitchFlow(Host host,boolean reconnect){
        this.host=host;this.account=host.account();this.connection=host.connection();this.reconnect=reconnect;
    }
    boolean reconnects(){return reconnect;}
    boolean disconnected(){return disconnected;}
    void finish(Runnable apply,Runnable selected){
        if(finished)throw new IllegalStateException("This account switch has already finished.");
        finished=true;
        if(host.account()!=account||host.connection()!=connection)throw new IllegalStateException("The account or server changed during sign-in. Please try again.");
        if(reconnect){disconnected=true;host.disconnect();}
        if(host.hasWorld()||host.connection()!=null)throw new IllegalStateException("Could not finish disconnecting from the current world.");
        apply.run();selected.run();
        if(reconnect)host.reconnect();
    }
}
