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
package org.apache.openjpa.persistence.criteria;

import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Selection;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.persistence.ResultItemImpl;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * An item selected in the projection clause of  Criteria query.
 * 
 * @author Pinaki Poddar
 *
 * @param <X>
 */
public class SelectionImpl<X> extends ResultItemImpl<X> 
    implements Selection<X> {
    
    private List<Selection<?>>  _sels;
    
    public SelectionImpl(Class<X> cls) {
        super(cls);
    }
    
    public SelectionImpl setSelections(Selection<?>... selections) {
        _sels = Arrays.asList(selections);
        return this;
    }
    
    public List<Selection<?>> getSelections() {
        return _sels;
    }
    
    Value toValue(ExpressionFactory factory, MetamodelImpl model,
        CriteriaQuery q) {
        ((CriteriaQueryImpl)q).setResultClass(getJavaType());
        Value[] result = new Value[_sels.size()];
        int i = 0;
        for (Selection<?> s : _sels) {
            result[i++] = ((ExpressionImpl<?>)s).toValue(factory, model, 
                q);
        }
        ((CriteriaQueryImpl)q).setProjections(result);
        return null;
    }
}
