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
package org.apache.openjpa.jdbc.sql;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.lib.util.FlexibleThreadLocal;


/**
 * A binding parameter in a SQL statement.
 * <br>
 * A binding parameter is used in <tt>WHERE</tt> clause. The parameter is identified by an
 * immutable key and has a value. The value of a parameter is bound to a thread and hence
 * different thread may see different value. These parameters are associated with a {@link Select}
 * and they are the only mutable components of an otherwise {@link SelectExecutor#isReadOnly() immutable}  
 * select. 
 * <br><b>NOTE:</b>
 * The primary assumption of usage is that the binding parameter values to a cached select and
 * executing it are carried out in the same (or <em>equivalent</em>) thread. 
 * <br> 
 * The parameter is further qualified by whether it is user specified (e.g. from a query parameter) 
 * or internally generated (e.g. a discriminator value for inheritance join). A database column can 
 * also be optionally associated with a binding parameter. Currently {@link SQLBuffer#bind(Object, Column)
 * rebinding} a parameter to a value is only possible if the parameter is associated with a column.
 * 
 * @see SQLBuffer#appendValue(Object, Column, org.apache.openjpa.kernel.exps.Parameter)
 * @see SQLBuffer#bind(Object, Column)
 * 
 * @author Pinaki Poddar
 * @since 2.2.0
 */
public class BindParameter {
	// Is this parameter specified by user or our own runtime
	private final boolean _user;
	// the column
	private final Column  _column;
	// key of this parameter
	private final Object  _key;
	private FlexibleThreadLocal<Object> _values = new FlexibleThreadLocal<Object>();
	
	/**
	 * Constructs a parameter with given key, column and user flag.
	 * 
	 * @param key the identifier. Can be null only if not a user-defined parameter.
	 * @param user flags if this key is originally specified by the user. 
	 * @param column a column represented by this parameter. Can be null.
	 */
	public BindParameter(Object key, boolean user, Column column, Object value) {
		super();
		if (user && key == null)
			throw new IllegalArgumentException("User-defined parameter can not have null key");
		_key = key;
		_user = user;
		_column = column;
		_values.set(value);
	}
	
	/**
	 * Gets the value bound to this parameter for the calling thread. Can be null.
	 * 
	 * @exception if the current thread or its equivalent never bound a value
	 * to this parameter.
	 */
	public Object getValue() {
		return _values.get();
	}
	
	/**
	 * Binds the given value to this parameter. Can be null.
	 */
	public void setValue(Object value) {
		_values.set(value);
	}

	/**
	 * Affirms if this parameter is specified by the user.
	 */
	public boolean isUser() {
		return _user;
	}

	/**
	 * Gets the column associated with this parameter, if any.
	 */
	public Column getColumn() {
		return _column;
	}

	/**
	 * Gets the key associated with this parameter.
	 * The user-defined parameter must have a non-null key.
	 */
	public Object getKey() {
		return _key;
	}	
	
	public String toString() {
		return _key + ":" + getValue();
	}
}
