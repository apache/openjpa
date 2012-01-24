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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.apache.openjpa.lib.util.ThreadGate;

/**
 * Tests a thread gate.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestGate extends TestCase {
	public static enum Op {ENTER, EXIT};
	public static int MAX_THREAD  = 10;
	public static int THREAD_TIME = 10;
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	
	public void testAllThreadsBlockUntilFirstAccessIsComplete() throws Exception {
		final Info info = new Info();
		final ThreadGate gate = new ThreadGate();
		final AtomicBoolean first = new AtomicBoolean(true);
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (int i = 0; i < MAX_THREAD; i++) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					gate.lock();
					try {
						info.enter();
                        Thread.sleep(first.compareAndSet(true, false) 
                        		? THREAD_TIME*10 : THREAD_TIME);
						info.exit();
					} catch (InterruptedException e) {
						fail();
					} finally {
						gate.unlock();
					}
				}
			};
			futures.add(threadPool.submit(r));
		}
		Thread.sleep(THREAD_TIME*10);
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				fail();
			}
		}
		assertEquals(2*MAX_THREAD, info.msg.size());
		Token enter = info.msg.get(0);
		Token exit  = info.msg.get(1);
		assertEquals(Op.ENTER, enter.op);
		assertEquals(Op.EXIT, exit.op);
		assertSame(enter.thread, exit.thread);
		
		for (int i = 2; i < info.msg.size(); i++) {
			Token token = info.msg.get(i);
			if (token.op == Op.ENTER) {
				Token pair = info.find(i+1, token);
				assertNotNull("No pair for " + token.thread, pair);
			}
		}
	}
	
	/**
	 * Records starting and stopping of threads.
	 *
	 */
	public class Info {
		List<Token> msg = new ArrayList<Token>();
		
		/**
		 * Record entry of the current thread.
		 */
		public void enter() {
			msg.add(new Token(Thread.currentThread(), Op.ENTER));
		}
		
		/**
		 * Record exit of the current thread.
		 */
		public void exit() {
			msg.add(new Token(Thread.currentThread(), Op.EXIT));
		}

		/**
		 * Find the exit record corresponding to a given entry record.
		 */
		
		public Token find(int start, Token t) {
			for (int i = start; i < msg.size(); i++) {
				if (msg.get(i).thread.getName().equals(t.thread.getName()))
						return msg.get(i);
			}
			return null;
		}
		
		public void print(PrintStream out) {
			for (Token t : msg) {
				out.println(t);
			}
		}
	}
	
	/**
	 * Info about a thread activity.
	 */
	public class Token {
		final Thread thread;
		final Op     op;
		final long   time;
		
		public Token(Thread t, Op op) {
			super();
			this.thread = t;
			this.op = op;
			this.time = System.currentTimeMillis();
		}
		
		public String toString() {
			return time + " " + op + " " + thread.getName();
		}
	}
}
