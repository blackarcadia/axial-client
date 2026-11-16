package com.axial.cosmetics.client;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;

/** Uses ReplayStudio's extra-entry API; writes run on Replay Mod's own save queue. */
final class NpcReplayArchive {
    static final String ENTRY="axial/npc-emotes-v1.json";
    static byte[] read(Object file)throws ReflectiveOperationException,IOException {
        var fileApi=Class.forName("com.replaymod.replaystudio.replay.ReplayFile",true,file.getClass().getClassLoader());
        var optional=fileApi.getMethod("get",String.class).invoke(file,ENTRY);
        // Optional's concrete subclasses may be package-private. Read through its public base API.
        var api=Class.forName("com.replaymod.replaystudio.lib.guava.base.Optional",true,file.getClass().getClassLoader());
        if(!(boolean)api.getMethod("isPresent").invoke(optional))return new byte[0];
        try(var input=(InputStream)api.getMethod("get").invoke(optional)) {
            var data=input.readNBytes(NpcEmoteTimeline.MAX_BYTES+1);
            if(data.length>NpcEmoteTimeline.MAX_BYTES)throw new IOException("NPC emote timeline is too large");return data;
        }
    }
    static void write(Object file,byte[] data)throws ReflectiveOperationException,IOException {
        var api=Class.forName("com.replaymod.replaystudio.replay.ReplayFile",true,file.getClass().getClassLoader());
        try(var output=(OutputStream)api.getMethod("write",String.class).invoke(file,ENTRY)){output.write(data);}
    }
    static final class Writer {
        private final Executor executor;private final Object file;
        private byte[] pending;private boolean scheduled;private volatile Throwable failure;
        Writer(Executor executor,Object file){this.executor=executor;this.file=file;}
        Throwable failure(){return failure;}
        synchronized void submit(byte[] data) {
            if(data.length>NpcEmoteTimeline.MAX_BYTES)throw new IllegalArgumentException("Actor timeline is too large");
            pending=data.clone();if(scheduled)return;scheduled=true;
            try{executor.execute(this::drain);}catch(RuntimeException ex){scheduled=false;failure=ex;throw ex;}
        }
        private void drain() {
            while(true){byte[] next;synchronized(this){next=pending;pending=null;if(next==null){scheduled=false;return;}}
                try{write(file,next);}catch(Exception ex){failure=ex instanceof InvocationTargetException wrapped?wrapped.getCause():ex;synchronized(this){pending=null;scheduled=false;}return;}
            }
        }
    }
}
