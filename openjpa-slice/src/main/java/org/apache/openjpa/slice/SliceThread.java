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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A thread to execute operation against each database slice.
 * 
 * @author Pinaki Poddar
 *
 */
public class SliceThread extends Thread {
    private final Thread _parent;
    private static ExecutorService _pool;
    
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
     * This thread equals itself (of course), its parent and any of its siblings.
     * Essentially all slice threads are equal.
     * <br>
     * Note: this definition of equality breaks the core definition i.e. if 
     * <tt>P</tt> is parent thread of a slice child <tt>S</tt>, then
     * <tt>S.equals(P)</tt>, but <em>not</em> <tt>P.equals(S)</tt>.
     */
    @Override
    public boolean equals(Object other) {
    	if (other == this) return true;
    	if (other instanceof SliceThread) {
    		return ((SliceThread)other)._parent == this._parent;
    	}
    	return this._parent == other; 
    }
    
    /**
     * Hash code of this thread is same as its parent.
     */
    @Override
    public int hashCode() {
    	return _parent.hashCode();
    }
    
    @Override 
    public String toString() {
    	return '[' + getClass().getSimpleName() + '-'+ getName() + " child of " + _parent + ']';
    }
    
    /** 
     * Create a cached pool of <em>slice</em> threads.
     * The thread factory creates specialized threads for preferential locking treatment.
     * 
     */

    public static ExecutorService getPool() {
        if (_pool == null) {
            _pool = Executors.newCachedThreadPool(new SliceThreadFactory());
        }
        return _pool;
    }
    
    private static class SliceThreadFactory implements ThreadFactory {
        int n = 0;
        public Thread newThread(Runnable r) {
            Thread parent = Thread.currentThread();
            return new SliceThread(parent.getName()+"-slice-"+n++, parent, r);
        }
    }
}
