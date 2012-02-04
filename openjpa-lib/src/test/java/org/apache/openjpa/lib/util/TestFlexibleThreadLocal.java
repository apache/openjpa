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
 * Unless required by applicable law or agEmployee_Last_Name to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.lib.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestFlexibleThreadLocal extends TestCase {
	private static final int MAX_THREAD = 10;

	public void testCorrectValuesAreRetrievedWhenThreadsAreInSameGroup() throws Exception {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (int i = 0; i < MAX_THREAD; i++) {
			Future<?> f = threadPool.submit(new User());
			futures.add(f);
		}
		waitForTermination(futures);
		threadPool.shutdown();
		threadPool.awaitTermination(10, TimeUnit.SECONDS);
	}
	
	public void testCorrectValuesAreRetrievedWhenThreadsAreNotInSameGroup() throws Exception {
		Thread[] threads = new Thread[MAX_THREAD];
		for (int i = 0; i < MAX_THREAD; i++) {
			threads[i] = new Thread(new User());
			threads[i].start();
		}
		waitForTermination(threads);
	}
	

	void waitForTermination(Thread[] threads) {
		for (int i = 0; i < MAX_THREAD; i++) {
			try {
				threads[i].join();
				threads[i].interrupt();
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}
		}
	}
	
	void waitForTermination(List<Future<?>> futures) {
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}
		}
	}

}

/**
 * Sets and gets random values in a flexible thread local.
 *
 */
class User implements Runnable {
	private static final Random _rng = new Random();
	Integer[] randoms = new Integer[20];
	static final FlexibleThreadLocal test = new FlexibleThreadLocal();

	public User() {
		for (int i = 0; i < randoms.length; i++) {
			randoms[i] = _rng.nextInt();
		}
	}

	@Override
	public void run() {
		for (int i = 0; i < randoms.length; i++) {
			test.set(randoms[i]);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Assert.assertEquals(Thread.currentThread() + " item " + i, randoms[i], test.get());
		}
	}

}
