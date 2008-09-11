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
package org.apache.openjpa.datacache;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts number of read/write requests and hit ratio for a cache in total and
 * per-class basis.
 * 
 * All methods with Class as input argument treats null as 
 * <code>java.lang.Object</code>. All per-class statistics  depends on 
 * determining the runtime type of the instance being cached. If it is not
 * possible to determine the runtime type from the given context, the statistics
 * is registered under generic <code>java.lang.Object</code>. 
 * 
 * @since 1.3.0
 * 
 * @author Pinaki Poddar
 * 
 */
public interface CacheStatistics extends Serializable {
	/**
	 * Gets number of total read requests since last reset.
	 */
	public long getReadCount();

	/**
	 * Gets number of total read requests that has been found in cache since
	 * last reset.
	 */
	public long getHitCount();

	/**
	 * Gets number of total write requests since last reset.
	 */
	public long getWriteCount();

	/**
	 * Gets number of total read requests since start.
	 */
	public long getTotalReadCount();

	/**
	 * Gets number of total read requests that has been found in cache since
	 * start.
	 */
	public long getTotalHitCount();

	/**
	 * Gets number of total write requests since start.
	 */
	public long getTotalWriteCount();

	/**
	 * Gets number of total read requests for the given class since last reset.
	 */
	public long getReadCount(Class c);

	/**
	 * Gets number of total read requests that has been found in cache for the
	 * given class since last reset.
	 */
	public long getHitCount(Class c);

	/**
	 * Gets number of total write requests for the given class since last reset.
	 */
	public long getWriteCount(Class c);

	/**
	 * Gets number of total read requests for the given class since start.
	 */
	public long getTotalReadCount(Class c);

	/**
	 * Gets number of total read requests that has been found in cache for the
	 * given class since start.
	 */
	public long getTotalHitCount(Class c);

	/**
	 * Gets number of total write requests for the given class since start.
	 */
	public long getTotalWriteCount(Class c);

	/**
	 * Gets the time of last reset.
	 */
	public Date since();

	/**
	 * Gets the time of start.
	 */
	public Date start();

	/**
	 * Clears all accumulated statistics.
	 */
	public void reset();
	
	/**
	 * A default implementation.
	 *
	 */
	public static class Default implements CacheStatistics {
		private long[] astat = new long[3];
		private long[] stat  = new long[3];
		private Map<Class, long[]> stats  = new HashMap<Class, long[]>();
		private Map<Class, long[]> astats = new HashMap<Class, long[]>();
		private Date start = new Date();
		private Date since = new Date();

		private static final int READ  = 0;
		private static final int HIT   = 1;
		private static final int WRITE = 2;

		public long getReadCount() {
			return stat[READ];
		}

		public long getHitCount() {
			return stat[HIT];
		}

		public long getWriteCount() {
			return stat[WRITE];
		}

		public long getTotalReadCount() {
			return astat[READ];
		}

		public long getTotalHitCount() {
			return astat[HIT];
		}

		public long getTotalWriteCount() {
			return astat[WRITE];
		}

		public long getReadCount(Class c) {
			return getCount(stats, c, READ);
		}

		public long getHitCount(Class c) {
			return getCount(stats, c, HIT);
		}

		public long getWriteCount(Class c) {
			return getCount(stats, c, WRITE);
		}

		public long getTotalReadCount(Class c) {
			return getCount(astats, c, READ);
		}

		public long getTotalHitCount(Class c) {
			return getCount(astats, c, HIT);
		}

		public long getTotalWriteCount(Class c) {
			return getCount(astats, c, WRITE);
		}
		
		private long getCount(Map<Class, long[]> target, Class c, int index) {
			long[] row = target.get(c);
			return (row == null) ? 0 : row[index];
		}

		public Date since() {
			return since;
		}

		public Date start() {
			return start;
		}

		public void reset() {
			stat = new long[3];
			stats.clear();
			since = new Date();
		}

		void newGet(Class cls, boolean hit) {
			cls = (cls == null) ? Object.class : cls;
			addSample(cls, READ);
			if (hit) {
				addSample(cls, HIT);
			}
		}

		void newPut(Class cls) {
			cls = (cls == null) ? Object.class : cls;
			addSample(cls, WRITE);
		}
		
		private void addSample(Class c, int index) {
			stat[index]++;
			astat[index]++;
			addSample(stats, c, index);
			addSample(astats, c, index);
		}
		
		private void addSample(Map<Class, long[]> target, Class c, int index) {
			long[] row = target.get(c);
			if (row == null) {
				row = new long[3];
			}
			row[index]++;
			target.put(c, row);
		}
	}
}
