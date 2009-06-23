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

import java.util.Collection;

import javax.persistence.Parameter;
import javax.persistence.criteria.ParameterExpression;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * Parameter of a criteria query.
 * 
 * @author Pinaki Poddar
 * @author Fay wang
 * 
 * @param <T> the type of value held by this parameter.
 */
public class ParameterImpl<T> extends ExpressionImpl<T> implements ParameterExpression<T>{
	private String name;
	private Integer position;
	
    public ParameterImpl(Class<T> cls, String name) {
        super(cls);
        this.name = name;
    }

	public final String getName() {
		return name;
	}
	
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(int p) {
        position = p;
    }
	
    @Override
    public Value toValue(ExpressionFactory factory, MetamodelImpl model,
        CriteriaQueryImpl q) {
        q.registerParameter(this);
        

        ClassMetaData meta = null;
        Class<?> clzz = getJavaType();
        Object paramKey = getKey();
        int index = getIndex();
        boolean isCollectionValued  = Collection.class.isAssignableFrom(clzz);
        org.apache.openjpa.kernel.exps.Parameter param = isCollectionValued 
            ? factory.newCollectionValuedParameter(paramKey, clzz) 
            : factory.newParameter(paramKey, clzz);
        param.setMetaData(meta);
        param.setIndex(index);
        
        return param;
    }	
    
    Object getKey() {
        if (name == null && position == null)
            throw new IllegalStateException(this + " key is not set");
        return name != null ? name : position;
    }
    
    int getIndex() {
        if (position == null)
            throw new IllegalStateException(this + " index is not set");
        return position-1;
    }
}
