package com.axial.worldmap;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Main-thread lifecycle for Paper's main-thread chunk futures and deferred surface sampling. */
final class ChunkLoadQueue<T,C> {
    private final LinkedHashMap<Long,Request<T,C>> requests=new LinkedHashMap<>();
    private long sequence;

    void submit(T task,long now,Supplier<CompletableFuture<C>> loader,Function<C,Runnable> retain) {
        var request=new Request<T,C>(++sequence,task,now);requests.put(request.id,request);
        try {
            loader.get().whenComplete((chunk,error)->{
                if(requests.get(request.id)!=request)return; // Timed-out or cancelled load completed late.
                try {
                    if(error==null&&chunk!=null)request.release=retain.apply(chunk);
                    request.chunk=chunk;request.error=error;
                } catch(Throwable failure){request.error=failure;}
                request.ready=true;
            });
        } catch(Throwable failure){request.error=failure;request.ready=true;}
    }
    boolean drainOne(BiConsumer<T,C> success,BiConsumer<T,Throwable> failure) {
        Request<T,C> ready=null;
        for(var request:requests.values())if(request.ready){ready=request;break;}
        if(ready==null)return false;
        requests.remove(ready.id);
        try {
            if(ready.error==null)success.accept(ready.task,ready.chunk);else failure.accept(ready.task,ready.error);
        } finally {release(ready);}
        return true;
    }
    List<T> expire(long now,long timeout) {
        var expired=new ArrayList<T>();var iterator=requests.values().iterator();
        while(iterator.hasNext()) {
            var request=iterator.next();
            if(!request.ready&&now-request.started>=timeout){iterator.remove();expired.add(request.task);release(request);}
        }
        return expired;
    }
    void cancel(Predicate<T> matches) {
        var removed=new ArrayList<Request<T,C>>();
        var iterator=requests.values().iterator();
        while(iterator.hasNext()){var request=iterator.next();if(matches.test(request.task)){iterator.remove();removed.add(request);}}
        removed.forEach(ChunkLoadQueue::release);
    }
    int size(){return requests.size();}
    Waiting<T> oldestWaiting(Predicate<T> matches,long now) {
        for(var request:requests.values())if(!request.ready&&matches.test(request.task))return new Waiting<>(request.task,Math.max(0,now-request.started));
        return null;
    }
    record Waiting<T>(T task,long ageNanos) { }
    private static void release(Request<?,?> request){if(request.release!=null){var release=request.release;request.release=null;release.run();}}
    private static final class Request<T,C> {
        final long id,started;final T task;
        C chunk;Throwable error;boolean ready;Runnable release;
        Request(long id,T task,long started){this.id=id;this.task=task;this.started=started;}
    }
}
