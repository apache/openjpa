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

import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.util.InternalException;

public class AggregateResultObjectProvider implements ResultObjectProvider {
    private final ResultObjectProvider[] _rops;
    private final StoreQuery _query;
    private final QueryExpressions[] _exps;
    private Object _single;
    private boolean _opened;

    public AggregateResultObjectProvider(ResultObjectProvider[] rops, 
            StoreQuery q, QueryExpressions[] exps) {
        _rops = rops;
        _query = q;
        _exps = exps;
    }
    
    public boolean absolute(int pos) throws Exception {
        return false;
    }

    public void close() throws Exception {
        _opened = false;
        for (ResultObjectProvider rop:_rops)
            rop.close();
    }

    public Object getResultObject() throws Exception {
        if (!_opened)
            throw new InternalException(this + " not-open");
        return _single;
    }

    public void handleCheckedException(Exception e) {
        _rops[0].handleCheckedException(e);
    }

    public boolean next() throws Exception {
        if (!_opened) {
            open();
        }
            
        if (_single != null)
            return false;
        
        Value[] values = _exps[0].projections;
        Object[] single = new Object[values.length]; 
        for (int i=0; i<values.length; i++) {
            Value v = values[i];
            boolean isAggregate = v.isAggregate();
            int op = decideOperationType(v);
            for (ResultObjectProvider rop:_rops) {
                rop.next();
                Object[] row = (Object[]) rop.getResultObject();
                switch (op) {
                case 2: single[i] = count(single[i],row[i]);
                break;
                default : single[i] = row[i];
                }
            } 
        }
        
        _single = single;
        return true;
    }
    
    int decideOperationType(Value v) {
        String cls = v.getClass().getName();
        if (cls.equals("org.apache.openjpa.jdbc.kernel.exps.Sum"))
            return 1;
        if (cls.equals("org.apache.openjpa.jdbc.kernel.exps.Count"))
            return 2;
        return 0;
    }
    long count(Object current, Object other) {
        if (current == null)
            return (Long) other;
        return (Long)current + (Long)other;
    }

    public void open() throws Exception {
        for (ResultObjectProvider rop:_rops)
            rop.open();
        _opened = true;
    }

    public void reset() throws Exception {
    }

    public int size() throws Exception {
        return 1;
    }

    public boolean supportsRandomAccess() {
         return false;
    }
}
