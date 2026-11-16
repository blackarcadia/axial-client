package com.axial.cosmetics.config;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.*;

/** Recording switch only; replay playback and saved recordings remain available. */
public final class ReplayRecordingPreference {
    private ReplayRecordingPreference() {}
    public static boolean read(Path file) {
        if(!Files.isRegularFile(file))return true;
        try {if(Files.size(file)>16384)return false;var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();var value=root.get("enabled");return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean()&&value.getAsBoolean();}
        catch(Exception ex){return false;}
    }
    public static void write(Path file,boolean enabled)throws IOException {
        Files.createDirectories(file.getParent());var temporary=Files.createTempFile(file.getParent(),"axial-replay-",".tmp");
        try {Files.writeString(temporary,"{\"enabled\":"+enabled+"}\n");
            try{Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
}
