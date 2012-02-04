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
package org.apache.openjpa.lib.util;

import java.util.Map;

import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashMap;


/**
 * A thread-specific storage similar to {@link ThreadLocal} that 
 * <em>heuristically</em> relaxes the affinity of a value to a thread.
 * <br>
 * A thread <tt>t1</tt> can {@linkplain #set(Object) set} a value, while
 * a different thread <tt>t2</tt> can {@linkplain #get() access} the same
 * value, if <tt>t1</tt> and <tt>t2</tt> are <em>{@link #eq(Object, Object)
 * equivalent}</em>.
 *  
 * @author Pinaki Poddar
 * @since 2.2.0
 */
public class FlexibleThreadLocal  extends ConcurrentReferenceHashMap {
	
	/**
	 * Must not hold hard reference to the threads used as keys.
	 */
    public FlexibleThreadLocal() {
		super(ReferenceMap.WEAK, ReferenceMap.HARD);
	}
	
	/**
	 * Gets the value associated with the calling thread or its equivalent.
	 * 
	 * @see #eq(Object, Object)
	 */
	public Object get() {
		Thread current = Thread.currentThread();
		if (containsKey(current)) {
			return super.get(current);
		} else {
			if (size() == 1)
				return ((Map.Entry)entrySet().iterator().next()).getValue();
			throw new RuntimeException(current + " is not a known thread. Known threads are " + keySet());
		} 
	}
	
	/**
	 * Associates the value to the current thread.
	 */
	public void set(Object t) {
		super.put(Thread.currentThread(), t);
	}
	
	/**
	 * Affirms if the two given thread are equivalent.
	 * Two threads are equivalent if the they are identical (of course),
	 * or they belong to the same thread group or they are <em>equal</em>.
	 * The equality can be defined <em>asymmetrically</em> by the 
	 * thread implementation. For example, a child thread (as done in Slice)
	 * can equal its parent thread which is a native thread. But the parent
	 * (native) thread is not equal to the child thread.   
	 */
	@Override
	protected boolean eq(Object a, Object b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		if (a instanceof Thread && b instanceof Thread) 
			if (((Thread)a).getThreadGroup() == ((Thread)b).getThreadGroup()) 
				return true;
		return a.equals(b) || b.equals(a);
	}
	
}
