package com.axial.worldmap;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ChunkLoadQueueTest {
    private static final class Chunk {
        boolean loaded=true;int tickets;
        Runnable retain(){tickets++;return ()->tickets--;}
        void attemptUnload(){if(tickets==0)loaded=false;}
    }
    @Test void loadedChunkIsRetainedBeforeDeferredSamplingSoItCannotEnterTheUnloadRetryLoop() {
        var queue=new ChunkLoadQueue<String,Chunk>();var future=new CompletableFuture<Chunk>();var chunk=new Chunk();
        queue.submit("last chunk",0,()->future,Chunk::retain);
        future.complete(chunk);
        // Simulate the server unloading idle chunks before the next budgeted map tick.
        chunk.attemptUnload();assertTrue(chunk.loaded);assertEquals(1,chunk.tickets);
        var sampled=new ArrayList<String>();
        assertTrue(queue.drainOne((task,value)->{assertTrue(value.loaded);assertEquals(1,value.tickets);sampled.add(task);},(task,error)->fail(error)));
        assertEquals(List.of("last chunk"),sampled);assertEquals(0,chunk.tickets);assertEquals(0,queue.size());
        chunk.attemptUnload();assertFalse(chunk.loaded);
    }
    @Test void stalledLoadsReleaseTheirSlotsAndLateCompletionCannotDuplicateTheRetry() {
        var queue=new ChunkLoadQueue<String,Chunk>();var stalled=new CompletableFuture<Chunk>();var retry=new CompletableFuture<Chunk>();
        queue.submit("last chunk",0,()->stalled,Chunk::retain);
        assertTrue(queue.expire(59,60).isEmpty());assertEquals(1,queue.size());
        assertEquals(List.of("last chunk"),queue.expire(60,60));assertEquals(0,queue.size());
        assertTrue(queue.expire(120,60).isEmpty());
        queue.submit("last chunk",60,()->retry,Chunk::retain);
        var late=new Chunk();stalled.complete(late);assertEquals(0,late.tickets);assertEquals(1,queue.size());
        var actual=new Chunk();retry.complete(actual);var count=new AtomicInteger();
        queue.drainOne((task,value)->{assertSame(actual,value);count.incrementAndGet();},(task,error)->fail(error));
        assertEquals(1,count.get());assertEquals(0,actual.tickets);assertEquals(0,queue.size());
    }
    @Test void readyChunksAreNotExpiredWhileWaitingForTheirSamplingBudget() {
        var queue=new ChunkLoadQueue<String,Chunk>();var chunk=new Chunk();
        queue.submit("ready",0,()->CompletableFuture.completedFuture(chunk),Chunk::retain);
        assertTrue(queue.expire(1000,60).isEmpty());assertNull(queue.oldestWaiting(task->true,1000));
        assertEquals(1,queue.size());assertEquals(1,chunk.tickets);
        queue.cancel(task->true);assertEquals(0,chunk.tickets);
    }
    @Test void cancellationReleasesReadyTicketsAndIgnoresOutstandingWorldLoads() {
        var queue=new ChunkLoadQueue<String,Chunk>();var first=new Chunk();var other=new Chunk();var pending=new CompletableFuture<Chunk>();
        queue.submit("world-a",0,()->CompletableFuture.completedFuture(first),Chunk::retain);
        queue.submit("world-a",0,()->pending,Chunk::retain);
        queue.submit("world-b",0,()->CompletableFuture.completedFuture(other),Chunk::retain);
        queue.cancel(task->task.equals("world-a"));assertEquals(0,first.tickets);assertEquals(1,other.tickets);assertEquals(1,queue.size());
        var late=new Chunk();pending.complete(late);assertEquals(0,late.tickets);
        queue.cancel(task->true);assertEquals(0,other.tickets);assertEquals(0,queue.size());
    }
    @Test void samplingFailureStillReleasesThePinnedChunk() {
        var queue=new ChunkLoadQueue<String,Chunk>();var chunk=new Chunk();
        queue.submit("last",0,()->CompletableFuture.completedFuture(chunk),Chunk::retain);
        assertThrows(IllegalStateException.class,()->queue.drainOne((task,value)->{throw new IllegalStateException("sample failed");},(task,error)->fail(error)));
        assertEquals(0,chunk.tickets);assertEquals(0,queue.size());
    }
    @Test void failedAndMissingLoadsAreDeliveredOnceWithoutRetainingAChunk() {
        var queue=new ChunkLoadQueue<String,Chunk>();var error=new IllegalStateException("load failed");var retained=new AtomicInteger();
        queue.submit("failed",0,()->CompletableFuture.failedFuture(error),chunk->{retained.incrementAndGet();return ()->{};});
        assertTrue(queue.drainOne((task,value)->fail("Unexpected success"),(task,failure)->assertSame(error,failure)));
        queue.submit("missing",0,()->CompletableFuture.completedFuture(null),chunk->{retained.incrementAndGet();return ()->{};});
        assertTrue(queue.drainOne((task,value)->assertNull(value),(task,failure)->fail(failure)));
        assertFalse(queue.drainOne((task,value)->fail(),(task,failure)->fail()));assertEquals(0,retained.get());assertEquals(0,queue.size());
    }
    @Test void synchronousLoadFailureAndWaitingDiagnosticsDoNotLeakRequests() {
        var queue=new ChunkLoadQueue<String,Chunk>();
        queue.submit("throws",0,()->{throw new IllegalStateException("load failed");},Chunk::retain);
        queue.drainOne((task,value)->fail(),(task,error)->assertEquals("load failed",error.getMessage()));
        queue.submit("pending",100,CompletableFuture::new,Chunk::retain);
        var waiting=queue.oldestWaiting(task->true,150);assertNotNull(waiting);assertEquals("pending",waiting.task());assertEquals(50,waiting.ageNanos());
        queue.cancel(task->true);assertEquals(0,queue.size());
    }
}
