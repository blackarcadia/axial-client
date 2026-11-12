package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import java.util.List;

/** The countdown uses the server's remaining duration and the local monotonic clock. */
record ClientPoll(int id,String question,List<Option> options,Integer voted,long deadlineNanos) {
    record Option(int id,String label) {}
    ClientPoll {options=List.copyOf(options);}
    static ClientPoll parse(JsonObject object,long serverTime,long receivedNanos) {
        int id=object.get("id").getAsInt();String question=object.get("question").getAsString();
        long seconds=Math.clamp(object.get("closes_at").getAsLong()-serverTime,0,30*86400);
        var options=new java.util.ArrayList<Option>();
        for(var value:object.getAsJsonArray("options")){
            var option=value.getAsJsonObject();int optionId=option.get("id").getAsInt();String label=option.get("label").getAsString();
            if(optionId<1||optionId>6||label.isBlank()||label.length()>160||options.stream().anyMatch(o->o.id()==optionId))throw new IllegalArgumentException("Invalid poll option");
            options.add(new Option(optionId,label));
        }
        Integer voted=object.get("voted").isJsonNull()?null:object.get("voted").getAsInt();
        if(id<1||question.isBlank()||question.length()>320||options.size()<2||options.size()>6||voted!=null&&options.stream().noneMatch(o->o.id()==voted))throw new IllegalArgumentException("Invalid poll");
        return new ClientPoll(id,question,options,voted,receivedNanos+seconds*1_000_000_000L);
    }
    long remaining(long now){return Math.max(0,(deadlineNanos-now+999_999_999L)/1_000_000_000L);}
    String timeLeft(){return formatTime(remaining(System.nanoTime()));}
    static String formatTime(long seconds){
        if(seconds<=0)return "Ended";
        long days=seconds/86400,hours=seconds/3600%24,minutes=seconds/60%60;
        if(days>0)return days+"d "+hours+"h left";
        if(hours>0)return hours+"h "+minutes+"m left";
        return seconds>=60?minutes+"m "+seconds%60+"s left":seconds+"s left";
    }
}
