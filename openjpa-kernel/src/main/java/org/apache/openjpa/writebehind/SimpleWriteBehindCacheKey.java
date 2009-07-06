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
package org.apache.openjpa.writebehind;

/**
 * Simple key implementation (concatenates entity classname with primary key
 * value).
 */
public class SimpleWriteBehindCacheKey implements WriteBehindCacheKey {
    public String _className;
    public Object _pk;

    public SimpleWriteBehindCacheKey(String className, Object pk) {
        _className = className;
        _pk = pk;
    }

    public Object getPk() {
        return _pk;
    }

    public void setPk(Object pk) {
        this._pk = pk;
    }

    public String getClassName() {
        return _className;
    }

    public void setClassName(String className) {
        _className = className;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result =
            prime * result + ((_className == null) ? 0 : _className.hashCode());
        result = prime * result + ((_pk == null) ? 0 : _pk.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleWriteBehindCacheKey other = (SimpleWriteBehindCacheKey) obj;
        if (_className == null) {
            if (other._className != null)
                return false;
        } else if (!_className.equals(other._className))
            return false;
        if (_pk == null) {
            if (other._pk != null)
                return false;
        } else if (!_pk.equals(other._pk))
            return false;
        return true;
    }

}
