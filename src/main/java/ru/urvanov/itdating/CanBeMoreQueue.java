package ru.urvanov.itdating;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CanBeMoreQueue<T> {

    private Queue<T> queue = new ConcurrentLinkedQueue<>();
    private boolean canBeMore = true;
    
    
    public synchronized void noMore() {
        canBeMore = false;
        this.notifyAll();
    }
    
    public synchronized boolean isFinished() {
        return !canBeMore && queue.isEmpty();
    }
    
    public synchronized boolean canBeMore() {
        return canBeMore || !queue.isEmpty();
    }
    
    public T pollWithWait() throws InterruptedException {
        T t = queue.poll();
        if (t == null) {
            synchronized(this) {
                t = queue.poll();
                while ((t == null) && (canBeMore())) {
                    t = queue.poll();
                    if (t == null) {
                        if (canBeMore()) {
                            this.wait();
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
        return t;
    }
    
    public void add(T t) {
        queue.add(t);
        synchronized(this) {
            this.notifyAll();
        }
    }
    public void addAll(Collection<T> collection) {
        queue.addAll(collection);
        synchronized(this) {
            this.notifyAll();
        }
    }
}
