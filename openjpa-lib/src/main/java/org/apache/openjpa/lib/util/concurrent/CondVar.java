/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
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

import java.util.Collection;
import java.util.Date;

class CondVar implements Condition, java.io.Serializable {

    /**
     * The lock
     */
    protected final ExclusiveLock lock;

    /**
     * Create a new CondVar that relies on the given mutual exclusion lock.
     * @param lock A non-reentrant mutual exclusion lock.
     */
    CondVar(ExclusiveLock lock) {
        this.lock = lock;
    }

    public void awaitUninterruptibly() {
        int holdCount = lock.getHoldCount();
        if (holdCount == 0) {
            throw new IllegalMonitorStateException();
        }
        // avoid instant spurious wakeup if thread already interrupted
        boolean wasInterrupted = Thread.interrupted();
        try {
            synchronized (this) {
                for (int i = holdCount; i > 0; i--) lock.unlock();
                while (true) {
                    try {
                        wait();
                        break;
                    } catch (InterruptedException ex) {
                        wasInterrupted = true;
                        // may have masked the signal and there is no way
                        // to tell; defensively propagate the signal
                        notify();
                    }
                }
            }
        }
        finally {
            for (int i = holdCount; i > 0; i--) lock.lock();
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void await() throws InterruptedException {
        int holdCount = lock.getHoldCount();
        if (holdCount == 0) {
            throw new IllegalMonitorStateException();
        }
        if (Thread.interrupted()) throw new InterruptedException();
        try {
            synchronized (this) {
                for (int i = holdCount; i > 0; i--) lock.unlock();
                try {
                    wait();
                } catch (InterruptedException ex) {
                    notify();
                    throw ex;
                }
            }
        } finally {
            for (int i = holdCount; i > 0; i--) lock.lock();
        }
    }

    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        int holdCount = lock.getHoldCount();
        if (holdCount == 0) {
            throw new IllegalMonitorStateException();
        }
        if (Thread.interrupted()) throw new InterruptedException();
        long nanos = unit.toNanos(timeout);
        boolean success = false;
        try {
            synchronized (this) {
                for (int i = holdCount; i > 0; i--) lock.unlock();
                try {
                    if (nanos > 0) {
                        long start = Utils.nanoTime();
                        TimeUnit.NANOSECONDS.timedWait(this, nanos);
                        // DK: due to coarse-grained(millis) clock, it seems
                        // preferable to acknowledge timeout(success == false)
                        // when the equality holds(timing is exact)
                        success = Utils.nanoTime() - start < nanos;
                    }
                } catch (InterruptedException ex) {
                    notify();
                    throw ex;
                }
            }
        } finally {
            for (int i = holdCount; i > 0; i--) lock.lock();
        }
        return success;
    }

//    public long awaitNanos(long timeout) throws InterruptedException {
//        throw new UnsupportedOperationException();
//    }

    public boolean awaitUntil(Date deadline) throws InterruptedException {
        if (deadline == null) throw new NullPointerException();
        int holdCount = lock.getHoldCount();
        if (holdCount == 0) {
            throw new IllegalMonitorStateException();
        }
        long abstime = deadline.getTime();
        if (Thread.interrupted()) throw new InterruptedException();

        boolean success = false;
        try {
            synchronized (this) {
                for (int i = holdCount; i > 0; i--) lock.unlock();
                try {
                    long start = System.currentTimeMillis();
                    long msecs = abstime - start;
                    if (msecs > 0) {
                        wait(msecs);
                        // DK: due to coarse-grained(millis) clock, it seems
                        // preferable to acknowledge timeout(success == false)
                        // when the equality holds(timing is exact)
                        success = System.currentTimeMillis() - start < msecs;
                    }
                } catch (InterruptedException ex) {
                    notify();
                    throw ex;
                }
            }
        } finally {
            for (int i = holdCount; i > 0; i--) lock.lock();
        }
        return success;
    }

    public synchronized void signal() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        notify();
    }

    public synchronized void signalAll() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        notifyAll();
    }

    protected ExclusiveLock getLock() {
        return lock;
    }

    protected boolean hasWaiters() {
        throw new UnsupportedOperationException("Use FAIR version");
    }

    protected int getWaitQueueLength() {
        throw new UnsupportedOperationException("Use FAIR version");
    }

    protected Collection getWaitingThreads() {
        throw new UnsupportedOperationException("Use FAIR version");
    }

    static interface ExclusiveLock extends Lock {

        boolean isHeldByCurrentThread();

        int getHoldCount();
    }
}
