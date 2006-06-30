/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Originally written by Doug Lea and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 * Thanks for the assistance and support of Sun Microsystems Labs,
 * and everyone contributing, testing, and using this code.
 */
package org.apache.openjpa.lib.util.concurrent;

import java.util.*;

/**
 * Base class for internal queue classes for semaphores, etc.
 * Relies on subclasses to actually implement queue mechanics.
 * NOTE: this class is NOT present in java.util.concurrent.
 */
public abstract class WaitQueue {

    public abstract void insert(WaitNode w); // assumed not to block
    public abstract WaitNode extract(); // should return null if empty

    public abstract boolean hasNodes();
    public abstract int getLength();
    public abstract Collection getWaitingThreads();
    public abstract boolean isWaiting(Thread thread);

    public static interface QueuedSync {
        // invoked with sync on wait node, (atomically) just before enqueuing
        boolean recheck(WaitNode node);
        // invoked with sync on wait node, (atomically) just before signalling
        void takeOver(WaitNode node);
    }

    public static class WaitNode {
        boolean waiting = true;
        WaitNode next = null;
        final Thread owner;

        public WaitNode() {
            this.owner = Thread.currentThread();
        }

        public Thread getOwner() {
            return owner;
        }

        public synchronized boolean signal(QueuedSync sync) {
            boolean signalled = waiting;
            if (signalled) {
                waiting = false;
                notify();
                sync.takeOver(this);
            }
            return signalled;
        }

        public synchronized boolean doTimedWait(QueuedSync sync, long nanos)
            throws InterruptedException {
            if (sync.recheck(this) || !waiting)
                return true;
            else if (nanos <= 0) {
                waiting = false;
                return false;
            } else {
                long deadline = Utils.nanoTime() + nanos;
                try {
                    for (; ; ) {
                        TimeUnit.NANOSECONDS.timedWait(this, nanos);
                        if (!waiting) // definitely signalled
                            return true;
                        else {
                            nanos = deadline - Utils.nanoTime();
                            if (nanos <= 0) { //  timed out
                                waiting = false;
                                return false;
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    if (waiting) { // no notification
                        waiting = false; // invalidate for the signaller
                        throw ex;
                    } else { // thread was interrupted after it was notified
                        Thread.currentThread().interrupt();
                        return true;
                    }
                }
            }
        }

        public synchronized void doWait(QueuedSync sync)
            throws InterruptedException {
            if (!sync.recheck(this)) {
                try {
                    while (waiting) wait();
                } catch (InterruptedException ex) {
                    if (waiting) { // no notification
                        waiting = false; // invalidate for the signaller
                        throw ex;
                    } else { // thread was interrupted after it was notified
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        public synchronized void doWaitUninterruptibly(QueuedSync sync) {
            if (!sync.recheck(this)) {
                boolean wasInterrupted = Thread.interrupted();
                try {
                    while (waiting) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            wasInterrupted = true;
                            // no need to notify; if we were signalled, we
                            // must be not waiting, and we'll act like signalled
                        }
                    }
                }
                finally {
                    if (wasInterrupted) Thread.currentThread().interrupt();
                }
            }
        }
    }
}

