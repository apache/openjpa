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
import javax.persistence.criteria.CriteriaQuery;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * Parameter of a criteria query.
 * 
 * @author Pinaki Poddar
 *
 * @param <T> the type of value held by this parameter.
 */
public class ParameterImpl<T> extends ExpressionImpl<T> implements Parameter<T>{
	private String name;
	private int position;
	
    public ParameterImpl(Class<T> cls) {
        super(cls);
    }

	public final String getName() {
		return name;
	}
	
	public final ParameterImpl<T> setName(String name) {
		this.name = name;
		return this;
	}

	public final Integer getPosition() {
		return position;
	}

    @Override
    public Value toValue(ExpressionFactory factory, MetamodelImpl model,
        CriteriaQuery q) {
        boolean positional = false;
        LinkedMap parameterTypes = ((CriteriaQueryImpl)q).getParameterTypes();
        if (parameterTypes == null) {
            parameterTypes = new LinkedMap(6);
            ((CriteriaQueryImpl)q).setParameterTypes(parameterTypes);
        } 
        if (name == null) {
            position = parameterTypes.size() + 1;
            positional = true;
        }
        
        Object paramKey = name == null ? Integer.valueOf(position) : name;
        if (!parameterTypes.containsKey(paramKey))
            parameterTypes.put(paramKey, Object.class);

        ClassMetaData meta = null;
        Class clzz = getJavaType();
        int index;
        if (positional) 
            index = position - 1;
        else 
            // otherwise the index is just the current size of the params
            index = parameterTypes.indexOf(paramKey);
        
        boolean isCollectionValued  = Collection.class.isAssignableFrom(clzz);
        org.apache.openjpa.kernel.exps.Parameter param = isCollectionValued 
            ? factory.newCollectionValuedParameter(paramKey, Object.class) 
            : factory.newParameter(paramKey, Object.class);
        param.setMetaData(meta);
        param.setIndex(index);
        
        return param;
    }	
}
