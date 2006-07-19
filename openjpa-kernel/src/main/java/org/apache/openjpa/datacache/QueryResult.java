/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.datacache;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A query result.
 *
 * @author Abe White
 */
public class QueryResult
    extends ArrayList {

    private final long _ex;

    /**
     * Constructor; supply corresponding query key and result data.
     */
    public QueryResult(QueryKey key, Collection data) {
        super(data);

        if (key.getTimeout() == -1)
            _ex = -1;
        else
            _ex = System.currentTimeMillis() + key.getTimeout();
    }

    /**
     * Constructor to set internal data from a serializer.
     */
    public QueryResult(Collection data, long ex) {
        super(data);
        _ex = ex;
    }

    /**
     * Expiration time, or -1 for no timeout.
     */
    public long getTimeoutTime() {
        return _ex;
    }

    /**
     * Whether this data is timed out.
     */
    public boolean isTimedOut() {
        return _ex != -1 && _ex < System.currentTimeMillis();
	}
}
