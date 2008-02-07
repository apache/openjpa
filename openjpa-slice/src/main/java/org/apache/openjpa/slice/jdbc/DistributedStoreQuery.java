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
package org.apache.openjpa.slice.jdbc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.JDBCStoreQuery;
import org.apache.openjpa.kernel.ExpressionStoreQuery;
import org.apache.openjpa.kernel.OrderingMergedResultObjectProvider;
import org.apache.openjpa.kernel.QueryContext;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.lib.rop.MergedResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.StoreException;

/**
 * A query for distributed databases.
 * 
 * @author Pinaki Poddar 
 *
 */
@SuppressWarnings("serial")
class DistributedStoreQuery extends JDBCStoreQuery {
	private List<StoreQuery> _queries = new ArrayList<StoreQuery>();
	private ExpressionParser _parser;
	
	public DistributedStoreQuery(JDBCStore store, ExpressionParser parser) {
		super(store, parser);
		_parser = parser;
		
	}
	
	void add(StoreQuery q) {
		_queries.add(q);
	}
	
    public Executor newDataStoreExecutor(ClassMetaData meta, boolean subs) {
    	ParallelExecutor ex = new ParallelExecutor(this, meta, subs, _parser, 
    			ctx.getCompilation());
        for (StoreQuery q:_queries) {
        	ex.addExecutor(q.newDataStoreExecutor(meta, subs));
        }
        return ex;
    }
    
    public void setContext(QueryContext ctx) {
    	super.setContext(ctx);
    	for (StoreQuery q:_queries) 
    		q.setContext(ctx); 
    }
    
    public ExecutorService getExecutorServiceInstance() {
        DistributedJDBCConfiguration conf = 
            ((DistributedJDBCConfiguration)getStore().getConfiguration());
        return conf.getExecutorServiceInstance();
    }

	/**
	 * Executes queries on multiple databases.
	 * 
	 * @author Pinaki Poddar 
	 *
	 */
	public static class ParallelExecutor extends 
		ExpressionStoreQuery.DataStoreExecutor {
		private List<Executor> executors = new ArrayList<Executor>();
		private DistributedStoreQuery owner = null;
		private ExecutorService threadPool = null;
		
		public void addExecutor(Executor ex) {
			executors.add(ex);
		}
		
        public ParallelExecutor(DistributedStoreQuery dsq, ClassMetaData meta, 
        		boolean subclasses, ExpressionParser parser, Object parsed) {
        	super(dsq, meta, subclasses, parser, parsed);
        	owner = dsq;
        	threadPool = ((DistributedJDBCConfiguration)dsq.getStore()
        	        .getConfiguration()).getExecutorServiceInstance();
        }
        
        /**
         * Each child query must be executed with slice context and not the 
         * given query context.
         */
        public ResultObjectProvider executeQuery(StoreQuery q,
                final Object[] params, final Range range) {
        	ResultObjectProvider[] tmp = new ResultObjectProvider[executors.size()];
        	final Iterator<StoreQuery> qs = owner._queries.iterator();
        	final List<Future<ResultObjectProvider>> futures = 
        		new ArrayList<Future<ResultObjectProvider>>();
        	int i = 0;
        	for (Executor ex:executors)  {
        		QueryExecutor call = new QueryExecutor();
        		call.executor = ex;
        		call.query    = qs.next();
        		call.params   = params;
        		call.range    = range;
        		futures.add(threadPool.submit(call)); 
        	}
        	for (Future<ResultObjectProvider> future:futures) {
        		try {
					tmp[i++] = future.get();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new StoreException(e.getCause());
				}
        	}
        	boolean[] ascending = getAscending(q);
        	boolean isAscending = ascending.length > 0;
        	boolean isUnique    = q.getContext().isUnique();
        	if (isUnique) {
        	    return new UniqueResultObjectProvider(tmp, q, 
        	            getQueryExpressions());
        	}
        	if (isAscending) {
        	    return new OrderingMergedResultObjectProvider(tmp, ascending, 
                  (Executor[])executors.toArray(new Executor[executors.size()]),
                  q, params);
        	}
        	return new MergedResultObjectProvider(tmp);
        }
        
        public Number executeDelete(StoreQuery q, Object[] params) {
        	Iterator<StoreQuery> qs = owner._queries.iterator();
        	final List<Future<Number>> futures = new ArrayList<Future<Number>>();
        	for (Executor ex:executors) {
        		DeleteExecutor call = new DeleteExecutor();
        		call.executor = ex;
        		call.query    = qs.next();
        		call.params   = params;
        		futures.add(threadPool.submit(call)); 
        	}
        	int N = 0;
        	for (Future<Number> future:futures) {
        		try {
            		Number n = future.get();
            		if (n != null) 
            			N += n.intValue();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new StoreException(e.getCause());
				}
        	}
        	return new Integer(N);
        }
        
        public Number executeUpdate(StoreQuery q, Object[] params) {
        	Iterator<StoreQuery> qs = owner._queries.iterator();
        	final List<Future<Number>> futures = new ArrayList<Future<Number>>();
        	for (Executor ex:executors) {
        		UpdateExecutor call = new UpdateExecutor();
        		call.executor = ex;
        		call.query    = qs.next();
        		call.params   = params;
        		futures.add(threadPool.submit(call)); 
        	}
        	int N = 0;
        	for (Future<Number> future:futures) {
        		try {
            		Number n = future.get();
            		if (n != null) 
            			N += n.intValue();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new StoreException(e.getCause());
				}
        	}
        	return new Integer(N);
        }

	}
	
	static  class QueryExecutor implements Callable<ResultObjectProvider> {
		StoreQuery query;
		Executor executor;
		Object[] params;
		Range range;
		public ResultObjectProvider call() throws Exception {
			return executor.executeQuery(query, params, range);
		}
	}
	
	static  class DeleteExecutor implements Callable<Number> {
		StoreQuery query;
		Executor executor;
		Object[] params;
		public Number call() throws Exception {
			return executor.executeDelete(query, params);
		}
	}
	
	static  class UpdateExecutor implements Callable<Number> {
		StoreQuery query;
		Executor executor;
		Object[] params;
		public Number call() throws Exception {
			return executor.executeDelete(query, params);
		}
	}
}

