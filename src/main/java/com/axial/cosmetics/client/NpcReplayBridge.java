package com.axial.cosmetics.client;

import java.lang.reflect.*;
import java.util.concurrent.Executor;

/** Reflection is confined to stable Replay Mod API names and its recording save queue. */
final class NpcReplayBridge {
    record Recording(Object listener,Object file,Executor executor,long time) {}
    record Playback(Object handler,Object file,long time) {}
    private static Object module(String name)throws ReflectiveOperationException {return Class.forName(name).getField("instance").get(null);}
    private static Object call(Object instance,String name)throws ReflectiveOperationException {return instance.getClass().getMethod(name).invoke(instance);}
    private static Object field(Object instance,String name)throws ReflectiveOperationException {
        for(Class<?> type=instance.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(instance);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    static Object open(java.nio.file.Path path)throws ReflectiveOperationException {
        var core=module("com.replaymod.core.ReplayMod");var files=core.getClass().getField("files").get(core);
        return files.getClass().getMethod("open",java.nio.file.Path.class).invoke(files,path);
    }
    static void fileCall(Object file,String method)throws ReflectiveOperationException {
        Class.forName("com.replaymod.replaystudio.replay.ReplayFile",true,file.getClass().getClassLoader()).getMethod(method).invoke(file);
    }
    static Recording recording()throws ReflectiveOperationException {
        var mod=module("com.replaymod.recording.ReplayModRecording");if(mod==null)return null;
        var connection=call(mod,"getConnectionEventHandler");if(connection==null)return null;
        var listener=call(connection,"getPacketListener");if(listener==null)return null;
        return new Recording(listener,field(listener,"replayFile"),(Executor)field(listener,"saveService"),((Number)call(listener,"getCurrentDuration")).longValue());
    }
    static Playback playback()throws ReflectiveOperationException {
        var mod=module("com.replaymod.replay.ReplayModReplay");if(mod==null)return null;
        var handler=call(mod,"getReplayHandler");if(handler==null)return null;
        var sender=call(handler,"getReplaySender");var api=Class.forName("com.replaymod.replay.ReplaySender");
        return new Playback(handler,call(handler,"getReplayFile"),((Number)api.getMethod("currentTimeStamp").invoke(sender)).longValue());
    }
}
