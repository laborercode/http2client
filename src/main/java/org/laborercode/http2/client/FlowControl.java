package org.laborercode.http2.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class FlowControl {

    private Map<Integer, Integer> windowMap = new HashMap<Integer, Integer>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition cond = lock.newCondition();
    private Policy policy;

    public FlowControl(Policy policy) {
        this.policy = policy;
    }

    public void refill(int streamId, int window) {
        lock.lock();
        try {
            int currentWindow = windowMap.get(streamId);
            currentWindow += window;
            windowMap.put(streamId, currentWindow);
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int consume(int streamId, int window) {
        lock.lock();
        try {
            while(true) {
                int streamWindow = windowMap.get(streamId);
                int connectionWindow = windowMap.get(0);

                try {
                    cond.await();
                } catch (InterruptedException e) {
                    continue;
                }
                return window;
            }
        } finally {
            lock.unlock();
        }
    }

    public class Policy {
        
    }
}
