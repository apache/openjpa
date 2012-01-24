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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread gate that guards on first invocation and remains open after the first invocation.
 * Used when a structure (such as Select) is constructed and populated first-time in a
 * thread-safe manner and can be used later with minimal thread synchronization overhead.
 * 
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
public class ThreadGate extends ReentrantLock {
	private AtomicBoolean _once = new AtomicBoolean(false);
	
	/**
	 * Lock the gate only if it has never been locked.
	 */
	@Override
	public void lock() {
		synchronized (_once) {
			if (!_once.get()) {
				super.lock();
			}
		}
	}
	
	/**
	 * Unlock the gate and keep it open for ever.
	 */
	@Override
	public void unlock() {
		if (_once.compareAndSet(false, true)) {
			super.unlock();
			synchronized (_once) {
				_once.notifyAll();
			}
		}
	}
}
