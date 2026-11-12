package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import java.util.List;

/** The countdown uses the server's remaining duration and the local monotonic clock. */
record ClientPoll(int id,String question,List<Option> options,Integer voted,long deadlineNanos) {
    record Option(int id,String label,long votes) {
        Option(int id,String label){this(id,label,-1);}
    }
    ClientPoll {options=List.copyOf(options);}
    static ClientPoll parse(JsonObject object,long serverTime,long receivedNanos) {
        int id=object.get("id").getAsInt();String question=object.get("question").getAsString();
        long seconds=Math.clamp(object.get("closes_at").getAsLong()-serverTime,0,30*86400);
        var options=parseOptions(object.getAsJsonArray("options"));
        Integer voted=object.get("voted").isJsonNull()?null:object.get("voted").getAsInt();
        if(id<1||question.isBlank()||question.length()>320||voted!=null&&options.stream().noneMatch(o->o.id()==voted))throw new IllegalArgumentException("Invalid poll");
        return new ClientPoll(id,question,options,voted,receivedNanos+seconds*1_000_000_000L);
    }
    private static List<Option> parseOptions(com.google.gson.JsonArray rows) {
        var options=new java.util.ArrayList<Option>();
        for(var value:rows){
            var option=value.getAsJsonObject();int optionId=option.get("id").getAsInt();String label=option.get("label").getAsString();
            if(optionId<1||optionId>6||label.isBlank()||label.length()>160||options.stream().anyMatch(o->o.id()==optionId))throw new IllegalArgumentException("Invalid poll option");
            long votes=option.has("votes")?option.get("votes").getAsLong():-1;
            if(option.has("votes")&&(votes<0||votes>1_000_000_000L))throw new IllegalArgumentException("Invalid vote count");
            options.add(new Option(optionId,label,votes));
        }
        if(options.size()<2||options.size()>6)throw new IllegalArgumentException("Invalid poll options");
        return List.copyOf(options);
    }
    ClientPoll recordVote(JsonObject receipt) {
        int accepted=receipt.get("optionId").getAsInt();
        if(options.stream().noneMatch(o->o.id()==accepted))throw new IllegalArgumentException("Invalid vote receipt");
        var results=receipt.has("options")?parseOptions(receipt.getAsJsonArray("options")):options;
        if(!results.stream().map(Option::id).toList().equals(options.stream().map(Option::id).toList()))throw new IllegalArgumentException("Invalid result options");
        return new ClientPoll(id,question,results,accepted,deadlineNanos);
    }
    boolean resultsAvailable(){return options.stream().allMatch(o->o.votes()>=0);}
    long totalVotes(){return resultsAvailable()?options.stream().mapToLong(Option::votes).sum():0;}
    /** Largest-remainder rounding keeps displayed result percentages at 100%. */
    int percentage(Option option) {
        long total=totalVotes();if(total==0)return 0;
        int unassigned=100-options.stream().mapToInt(o->(int)(o.votes()*100/total)).sum();
        var ranked=options.stream().sorted(java.util.Comparator.<Option>comparingLong(o->o.votes()*100%total).reversed().thenComparingInt(Option::id)).toList();
        return (int)(option.votes()*100/total)+(ranked.indexOf(option)<unassigned?1:0);
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
