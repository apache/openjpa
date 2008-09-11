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
package org.apache.openjpa.persistence;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Records query execution statistics.
 * 
 * @since 1.3.0
 * 
 * @author Pinaki Poddar
 * 
 */
public interface QueryStatistics extends Serializable {
	
	/**
	 * Record that the given query has been executed.
	 */
	void recordExecution(String query);
		
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
	public long getExecutionCount(String query);

	/**
	 * Gets number of executions for the given query since start.
	 */
	public long getTotalExecutionCount(String query);

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
	public static class Default implements QueryStatistics {
		private long[] astat = new long[1];
		private long[] stat  = new long[1];
		private Map<String, long[]> stats  = new HashMap<String, long[]>();
		private Map<String, long[]> astats = new HashMap<String, long[]>();
		private Date start = new Date();
		private Date since = new Date();

		private static final int READ  = 0;

		public long getExecutionCount() {
			return stat[READ];
		}

		public long getTotalExecutionCount() {
			return astat[READ];
		}

		public long getExecutionCount(String query) {
			return getCount(stats, query, READ);
		}

		public long getTotalExecutionCount(String query) {
			return getCount(astats, query, READ);
		}

		private long getCount(Map<String, long[]> target, String query, int index) {
			long[] row = target.get(query);
			return (row == null) ? 0 : row[index];
		}

		public Date since() {
			return since;
		}

		public Date start() {
			return start;
		}

		public void reset() {
			stat = new long[1];
			stats.clear();
			since = new Date();
		}

		private void addSample(String query, int index) {
			stat[index]++;
			astat[index]++;
			addSample(stats, query, index);
			addSample(astats, query, index);
		}
		
		private void addSample(Map<String, long[]> target, String query, int index) {
			long[] row = target.get(query);
			if (row == null) {
				row = new long[1];
			}
			row[index]++;
			target.put(query, row);
		}
		
		public void recordExecution(String query) {
			addSample(query, READ);
		}
	}
}
