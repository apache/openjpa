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

import javax.persistence.ResultItem;

/**
 * A single dimension of projection in query result.
 * 
 * @author Pinaki Poddar
 *
 * @param <X> type of the result
 */
public class ResultItemImpl<X> implements ResultItem<X> {
    protected String _alias;
    protected Class<X> _cls;
    
    protected ResultItemImpl(Class<X> cls) {
        _cls = cls;
    }
    
    public String getAlias() {
        return _alias;
    }

    public void setAlias(String alias) {
        _alias = alias;
    }

    public Class<X> getJavaType() {
        return _cls;
    }
}
