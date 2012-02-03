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

import java.util.HashMap;
import java.util.Map;

/**
 * A thread-specific storage similar to {@link ThreadLocal} that 
 * <em>heuristically</em> relaxes the affinity of a value to a thread.
 * <br>
 * A thread <tt>t1</tt> can {@linkplain #set(Object) set} a value, while
 * a different thread <tt>t2</tt> can {@linkplain #get() access} the same
 * value, if <tt>t1</tt> and <tt>t2</tt> are <em>{@link #isEquivalent(Thread, Thread)
 * equivalent}</em>.
 *  
 * @author Pinaki Poddar
 * @since 2.2.0
 */
public class FlexibleThreadLocal<T>  {
	private final Map<Thread, T> _values = new HashMap<Thread, T>();
	
	/**
	 * Gets the value associated with the calling thread or its 
	 * {@link #isEquivalent(Thread, Thread) equivalent}.
	 * 
	 * @see #isEquivalent(Thread, Thread)
	 */
	public T get() {
		Thread current = Thread.currentThread();
		if (_values.containsKey(current)) {
			return _values.get(current);
		} else {
			if (_values.size() == 1) {
				return _values.values().iterator().next();
			} else {
				for (Map.Entry<Thread, T> e : _values.entrySet()) {
					if (isEquivalent(e.getKey(), current))
						return e.getValue();
				}
			}
			throw new RuntimeException(current + " is not a known thread. Known threads are " + _values);
		} 
	}
	
	/**
	 * Associates the value to the current thread.
	 */
	public T set(T t) {
		return _values.put(Thread.currentThread(), t);
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
	protected boolean isEquivalent(Thread a, Thread b) {
		if (a == b) return true;
		if (a.getThreadGroup() == b.getThreadGroup()) return true;
		if (a == null || b == null) return false;
		return a.equals(b) || b.equals(a);
	}

}
