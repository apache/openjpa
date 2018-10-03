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
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashMap;

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
     *  Gets all the identifier keys for the cached queries.
     */
    Set<T> keys();

	/**
	 * Record that the given query has been executed.
	 */
	void recordExecution(T query);

    /**
     * Record that the given query has been evicted.
     */
    void recordEviction(T query);

	/**
	 * Gets number of total query execution since last reset.
	 */
	long getExecutionCount();

	/**
	 * Gets number of total query execution since start.
	 */
	long getTotalExecutionCount();

	/**
	 * Gets number of executions for the given query since last reset.
	 */
	long getExecutionCount(T query);

	/**
	 * Gets number of executions for the given query since start.
	 */
	long getTotalExecutionCount(T query);

	/**
     * Gets number of total query execution that are cached since last reset.
	 */
	long getHitCount();

	/**
	 * Gets number of total query execution that are cached since start.
	 */
	long getTotalHitCount();

	/**
	 * Gets number of executions for the given query that are cached since
	 * last reset.
	 */
	long getHitCount(T query);

	/**
	 * Gets number of executions for the given query that are cached since
	 * start.
	 */
	long getTotalHitCount(T query);

	 /**
     * Gets number of total query evictions since last reset.
     */
    long getEvictionCount();

    /**
     * Gets number of total query evictions since start.
     */
    long getTotalEvictionCount();

	/**
	 * Gets the time of last reset.
	 */
	Date since();

	/**
	 * Gets the time of start.
	 */
	Date start();

	/**
	 * Clears all  statistics accumulated since last reset.
	 */
	void reset();

	/**
	 * Clears all statistics accumulated since start.
	 */
	void clear();

	/**
	 * Dumps on the given output stream.
	 */
	void dump(PrintStream out);

	/**
	 * A default implementation.
	 *
	 * Maintains statistics for only a fixed number of queries.
	 * Statistical counts are approximate and not exact (to keep thread synchorization overhead low).
	 *
	 */
	public static class Default<T> implements QueryStatistics<T> {
	    
        private static final long serialVersionUID = 1L;
        private static final int FIXED_SIZE = 1000;
	    private static final float LOAD_FACTOR = 0.75f;
	    private static final int CONCURRENCY = 16;

		private static final int ARRAY_SIZE = 3;
        private static final int READ  = 0;
        private static final int HIT   = 1;
        private static final int EVICT = 2;

		private long[] astat = new long[ARRAY_SIZE];
		private long[] stat  = new long[ARRAY_SIZE];
		private Map<T, long[]> stats;
		private Map<T, long[]> astats;
		private Date start = new Date();
		private Date since = start;

		public Default() {
            initializeMaps();
        }

        private void initializeMaps() {
            ConcurrentReferenceHashMap statsMap =
                new ConcurrentReferenceHashMap(ReferenceStrength.HARD, ReferenceStrength.HARD, CONCURRENCY, LOAD_FACTOR);
            statsMap.setMaxSize(FIXED_SIZE);
            stats = statsMap;

            ConcurrentReferenceHashMap aStatsMap =
                new ConcurrentReferenceHashMap(ReferenceStrength.HARD, ReferenceStrength.HARD, CONCURRENCY, LOAD_FACTOR);
            aStatsMap.setMaxSize(FIXED_SIZE);
            astats = aStatsMap;
        }

		@Override
        public Set<T> keys() {
		    return stats.keySet();
		}

		@Override
        public long getExecutionCount() {
			return stat[READ];
		}

		@Override
        public long getTotalExecutionCount() {
			return astat[READ];
		}

		@Override
        public long getExecutionCount(T query) {
			return getCount(stats, query, READ);
		}

		@Override
        public long getTotalExecutionCount(T query) {
			return getCount(astats, query, READ);
		}

		@Override
        public long getHitCount() {
			return stat[HIT];
		}

		@Override
        public long getTotalHitCount() {
			return astat[HIT];
		}

		@Override
        public long getHitCount(T query) {
			return getCount(stats, query, HIT);
		}

		@Override
        public long getTotalHitCount(T query) {
			return getCount(astats, query, HIT);
		}

		private long getCount(Map<T, long[]> target, T query, int i) {
			long[] row = target.get(query);
			return (row == null) ? 0 : row[i];
		}

		@Override
        public Date since() {
			return since;
		}

		@Override
        public Date start() {
			return start;
		}

		@Override
        public synchronized void reset() {
			stat = new long[ARRAY_SIZE];
			stats.clear();
			since = new Date();
		}

	    @Override
        public synchronized void clear() {
	       astat = new long[ARRAY_SIZE];
	       stat  = new long[ARRAY_SIZE];
	       initializeMaps();
	       start  = new Date();
	       since  = start;
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

		@Override
        public void recordExecution(T query) {
		    if (query == null)
		        return;
		    boolean cached = astats.containsKey(query);
			addSample(query, READ);
			if (cached)
				addSample(query, HIT);
		}

        @Override
        public void recordEviction(T query) {
            if (query == null) {
                return;
            }
            addSample(query, EVICT);
        }

		@Override
        public void dump(PrintStream out) {
            String header = "Query Statistics starting from " + start;
			out.print(header);
			if (since == start) {
				out.println();
                out.println("Total Query Execution: " + toString(astat));
				out.println("\tTotal \t\tQuery");
			} else {
				out.println(" last reset on " + since);
                out.println("Total Query Execution since start " +
                        toString(astat)  + " since reset " + toString(stat));
                out.println("\tSince Start \tSince Reset \t\tQuery");
			}
			int i = 0;
			for (T key : stats.keySet()) {
				i++;
				long[] arow = astats.get(key);
				if (since == start) {
                    out.println(i + ". \t" + toString(arow) + " \t" + key);
				} else {
					long[] row  = stats.get(key);
                    out.println(i + ". \t" + toString(arow) + " \t"  + toString(row) + " \t\t" + key);
				}
			}
		}

		long pct(long per, long cent) {
			if (cent <= 0)
				return 0;
			return (100*per)/cent;
		}

		String toString(long[] row) {
            return row[READ] + ":" + row[HIT] + "(" + pct(row[HIT], row[READ]) + "%)";
		}

        @Override
        public long getEvictionCount() {
            return stat[EVICT];
        }

        @Override
        public long getTotalEvictionCount() {
            return astat[EVICT];
        }
	}

	/**
	 * A do-nothing implementation.
	 *
	 * @author Pinaki Poddar
	 *
	 * @param <T>
	 */
	public static class None<T> implements QueryStatistics<T> {
        
        private static final long serialVersionUID = 1L;
        private Date start = new Date();
        private Date since = start;

        @Override
        public void clear() {
        }

        @Override
        public void dump(PrintStream out) {
        }

        @Override
        public long getExecutionCount() {
            return 0;
        }

        @Override
        public long getExecutionCount(T query) {
            return 0;
        }

        @Override
        public long getHitCount() {
            return 0;
        }

        @Override
        public long getHitCount(T query) {
            return 0;
        }

        @Override
        public long getTotalExecutionCount() {
            return 0;
        }

        @Override
        public long getTotalExecutionCount(T query) {
            return 0;
        }

        @Override
        public long getTotalHitCount() {
            return 0;
        }

        @Override
        public long getTotalHitCount(T query) {
            return 0;
        }

        @Override
        public long getEvictionCount() {
            return 0;
        }

        @Override
        public long getTotalEvictionCount() {
            return 0;
        }

        @Override
        public Set<T> keys() {
            return Collections.emptySet();
        }

        @Override
        public void recordExecution(T query) {
        }

        @Override
        public void reset() {
            start  = new Date();
            since  = start;
        }

        @Override
        public Date since() {
            return since;
        }

        @Override
        public Date start() {
            return start;
        }

        @Override
        public void recordEviction(T query) {
        }
	}
}

