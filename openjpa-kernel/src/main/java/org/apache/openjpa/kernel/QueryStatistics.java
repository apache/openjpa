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
package org.apache.openjpa.kernel;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Records query execution statistics.
 * 
 * Statistics can be reset.
 * 
 * Gathers both accumulated statistics since start as well as statistics since
 * last reset.
 *  
 * @since 1.3.0
 * 
 * @author Pinaki Poddar
 * 
 */
public interface QueryStatistics<T> extends Serializable {
	
	/**
	 * Record that the given query has been executed. The boolean parameter
	 * designates whether the executed query is a cached version.  
	 */
	void recordExecution(T query, boolean cached);
		
	/**
	 * Gets number of total query execution since last reset.
	 */
	public long getExecutionCount();

	/**
	 * Gets number of total query execution since start.
	 */
	public long getTotalExecutionCount();

	/**
	 * Gets number of executions for the given query since last reset.
	 */
	public long getExecutionCount(T query);

	/**
	 * Gets number of executions for the given query since start.
	 */
	public long getTotalExecutionCount(T query);

	/**
	 * Gets number of total query execution that are cached since last reset.
	 */
	public long getHitCount();

	/**
	 * Gets number of total query execution that are cached since start.
	 */
	public long getTotalHitCount();

	/**
	 * Gets number of executions for the given query that are cached since 
	 * last reset.
	 */
	public long getHitCount(T query);

	/**
	 * Gets number of executions for the given query that are cached since 
	 * start.
	 */
	public long getTotalHitCount(T query);

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
	 * Dumps on the given output stream.
	 */
	public void dump(PrintStream out);
	
	/**
	 * A default implementation.
	 *
	 */
	public static class Default<T> implements QueryStatistics<T> {
		private static final int ARRAY_SIZE = 2;
		private long[] astat = new long[ARRAY_SIZE];
		private long[] stat  = new long[ARRAY_SIZE];
		private Map<T, long[]> stats  = new HashMap<T, long[]>();
		private Map<T, long[]> astats = new HashMap<T, long[]>();
		private Date start = new Date();
		private Date since = start;

		private static final int READ  = 0;
		private static final int HIT   = 1;

		public long getExecutionCount() {
			return stat[READ];
		}

		public long getTotalExecutionCount() {
			return astat[READ];
		}

		public long getExecutionCount(T query) {
			return getCount(stats, query, READ);
		}

		public long getTotalExecutionCount(T query) {
			return getCount(astats, query, READ);
		}

		public long getHitCount() {
			return stat[HIT];
		}

		public long getTotalHitCount() {
			return astat[HIT];
		}

		public long getHitCount(T query) {
			return getCount(stats, query, HIT);
		}

		public long getTotalHitCount(T query) {
			return getCount(astats, query, HIT);
		}

		private long getCount(Map<T, long[]> target, T query, int i) {
			long[] row = target.get(query);
			return (row == null) ? 0 : row[i];
		}

		public Date since() {
			return since;
		}

		public Date start() {
			return start;
		}

		public void reset() {
			stat = new long[ARRAY_SIZE];
			stats.clear();
			since = new Date();
		}

		private void addSample(T query, int index) {
			stat[index]++;
			astat[index]++;
			addSample(stats, query, index);
			addSample(astats, query, index);
		}
		
		private void addSample(Map<T, long[]> target, T query, int i) {
			long[] row = target.get(query);
			if (row == null) {
				row = new long[ARRAY_SIZE];
			}
			row[i]++;
			target.put(query, row);
		}
		
		public void recordExecution(T query, boolean cached) {
			addSample(query, READ);
			if (cached)
				addSample(query, HIT);
		}
		
		public void dump(PrintStream out) {
			String header = "Query Statistics starting from " + start;
			out.print(header);
			if (since == start) {
				out.println();
				out.println("Total Query Execution: " + toString(astat)); 
				out.println("\tTotal \t\tQuery");
			} else {
				out.println(" last reset on " + since);
				out.println("Total Query Execution since start " 
					+ toString(astat)  + " since reset " +  toString(stat));
				out.println("\tSince Start \tSince Reset \t\tQuery");
			}
			int i = 0;
			for (T key : stats.keySet()) {
				i++;
				long[] arow = astats.get(key);
				if (since == start) {
					out.println(i + ". \t" + toString(arow) + " \t"	+ key);
				} else {
					long[] row  = stats.get(key);
					out.println(i + ". \t" + toString(arow) + " \t"  
					    + toString(row) + " \t\t" + key);
				}
			}
		}
		
		long pct(long per, long cent) {
			if (cent <= 0)
				return 0;
			return (100*per)/cent;
		}
		
		String toString(long[] row) {
			return row[READ] + ":" + row[HIT] + "(" + pct(row[HIT], row[READ]) 
			+ "%)";
		}
	}
}
