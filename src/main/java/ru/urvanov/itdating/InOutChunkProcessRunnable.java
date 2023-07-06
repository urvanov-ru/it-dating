package ru.urvanov.itdating;

import java.util.function.Function;

public class InOutChunkProcessRunnable<I, O, C> implements Runnable {

    private CanBeMoreQueue<I> inQueue;
    private CanBeMoreQueue<O> outQueue;
    private CanBeMoreQueue<C> chunkQueue;
    private Function<I, O> process;
    private Function<O, C> toChunk;
    
    public InOutChunkProcessRunnable(CanBeMoreQueue<I> inQueue, CanBeMoreQueue<O> outQueue, CanBeMoreQueue<C> chunkQueue, Function<I, O> process, Function<O, C> toChunk) {
        this.inQueue = inQueue;
        this.outQueue = outQueue;
        this.chunkQueue = chunkQueue;
        this.process = process;
        this.toChunk = toChunk;
    }
    
    @Override
    public void run() {
        try {
            I input;
            while (null != (input = inQueue.pollWithWait())) {
                
                O o = process.apply(input);
                C chunk = toChunk.apply(o);
                chunkQueue.add(chunk);
                outQueue.add(o);
            }
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }
    }

}
