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
package org.apache.openjpa.slice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.slice.jdbc.TargetFetchConfiguration;

/**
 * Extension with slice locking policy.
 *
 * @author Pinaki Poddar
 *
 */
public class DistributedQueryImpl extends QueryImpl {
    private static final long serialVersionUID = 1L;
    private final ReentrantSliceLock _lock;
    private DistributedConfiguration _conf;

    public DistributedQueryImpl(Broker broker, String language, StoreQuery storeQuery) {
        super(broker, language, storeQuery);
        _lock = new ReentrantSliceLock();
        _conf = (DistributedConfiguration)broker.getConfiguration();
    }

    /**
     * Overrides to set the query targets via policy, if user has not already set the targets via hint
     * explicitly on this query.
     */
    @Override
    public Object execute(Map params) {
        applyTargetPolicy(params);
        return super.execute(params);
    }

    /**
     * Overrides to run a bulk delete on the targets of the policy as well.
     */
    @Override
    public long deleteAll(Object[] params) {
        applyTargetPolicy(toMap(params));
        return super.deleteAll(params);
    }

    @Override
    public long deleteAll(Map params) {
        applyTargetPolicy(params);
        return super.deleteAll(params);
    }

    /**
     * Overrides to run a bulk update on the targets of the policy as well.
     */
    @Override
    public long updateAll(Object[] params) {
        applyTargetPolicy(toMap(params));
        return super.updateAll(params);
    }

    @Override
    public long updateAll(Map params) {
        applyTargetPolicy(params);
        return super.updateAll(params);
    }

    /**
     * Sets the targets given by the configured policy, unless the user has set
     * the targets via hint explicitly on this query.
     */
    private void applyTargetPolicy(Map params) {
        TargetFetchConfiguration fetch = (TargetFetchConfiguration)getFetchConfiguration();
        if (!fetch.isExplicitTarget()) {
            QueryTargetPolicy policy = _conf.getQueryTargetPolicyInstance();
            if (policy != null) {
                Map safe = (params == null) ? Collections.emptyMap() : Collections.unmodifiableMap(params);
                String[] targets = policy.getTargets(getQueryString(), safe,
                        getLanguage(), _conf.getActiveSliceNames(), this.getBroker());
                fetch.setTargets(targets);
            }
        }
    }

    /**
     * Positional parameters keyed by their position, starting with 1.
     */
    private static Map<Object, Object> toMap(Object[] params) {
        Map<Object, Object> map = new HashMap<>();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                map.put(i + 1, params[i]);
            }
        }
        return map;
    }

    /**
     * Always uses lock irrespective of super's multi-threaded settings.
     */
    @Override
    public void lock() {
        _lock.lock();
    }

    @Override
    public void unlock() {
        _lock.unlock();
    }
}
