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


/**
 * A binding parameter in a SQL statement.
 * <br>
 * A binding parameter is used in <tt>WHERE<tt> clause. The parameter is identified by an
 * immutable key and has a value. The parameter is further qualified by whether it is
 * user specified (e.g. from a query parameter) or internally generated (e.g. a discriminator
 * value for inheritance join). A database column can also be optionally associated with 
 * a binding parameter.
 * 
 * @see SQLBuffer#appendValue(Object, Column, org.apache.openjpa.kernel.exps.Parameter)
 * 
 * @author Pinaki Poddar
 *
 */
public class BindParameter {
	// Is this parameter specified by user or our own runtime
	private final boolean _user;
	// the column
	private final Column  _column;
	// key of this parameter
	private final Object  _key;
	private Object _value;
	
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
		_value  = value;
	}
	
	/**
	 * Gets the value bound to this parameter. Can be null.
	 */
	public Object getValue() {
		return _value;
	}
	
	/**
	 * Binds the given value to this parameter. Can be null.
	 */
	public void setValue(Object value) {
		_value = value;
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
}
