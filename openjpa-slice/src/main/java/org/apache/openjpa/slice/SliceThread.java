/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.slice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A thread to execute operation against each database slice.
 * 
 * @author Pinaki Poddar
 *
 */
public class SliceThread extends Thread {
    private final Thread _parent;

    public SliceThread(String name, Thread parent, Runnable r) {
        super(r, name);
        _parent = parent;
    }
    
    public SliceThread(Thread parent, Runnable r) {
        super(r);
        _parent = parent;
    }
    
    /**
     * Gets the parent thread of this receiver.
     * 
     */
    public Thread getParent() {
        return _parent;
    }
    
    /** 
     * Create a pool of given size.
     * The thread factory is specialized to create SliceThread which gets
     * preferential treatment for locking.
     * 
     */

    public static ExecutorService newPool(int size) {
        return new ThreadPoolExecutor(size, size, 60L, TimeUnit.SECONDS, 
            new SynchronousQueue<Runnable>(), new SliceThreadFactory());
    }
    
    static class SliceThreadFactory implements ThreadFactory {
        int n = 0;
        public Thread newThread(Runnable r) {
            Thread parent = Thread.currentThread();
            return new SliceThread(parent.getName()+"-slice-"+n++, parent, r);
        }
    }

}
