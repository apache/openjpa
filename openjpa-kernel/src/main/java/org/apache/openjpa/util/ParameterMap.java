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
package org.apache.openjpa.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Structure for holding binding query parameters.
 * 
 * Allows either named or positional parameter keys. The parameter key type is
 * determined at the first insertion unless specified at construction.
 *
 * Maintains parameter ordering.
 * The parameter ordering for named keys is same as the insertion order.
 * Positional parameters can use an starting offset that defaults to 1. 
 * 
 * Allows to set expected types of values for each parameter.
 * 
 * Can validate itself.
 * 
 * Can convert a named parameter map to a positional parameter map.
 * 
 * @author Pinaki Poddar
 *
 */
public class ParameterMap implements Map {
	public static enum Type {POSITIONAL, NAMED};
	
	private static final Localizer _loc = 
		Localizer.forPackage(ParameterMap.class);
	
	private Map _delegate   = null;
	private final Map<Object,Class> _valueTypes = new HashMap<Object,Class>();
	private Type _type = null;
	private final int _offset;
	
	/**
	 * Construct a map with indefinite key type and positional offset of 1.
	 */
	public ParameterMap() {
		this(null, 1);
	}
	
	/**
	 * Construct a map with indefinite key type and given positional offset.
	 */
	public ParameterMap(int offset) {
		this(null, offset);
	}
	
	/**
	 * Construct a map with indefinite key type and positional offset of 1.
	 */
	public ParameterMap(Type type) {
		this(type, 1);
	}
	
	/**
	 * Construct a map with given key type and given positional offset.
	 * Even if this map is not positional then the offset is used when the
	 * map is converted to positional map.
	 */
	public ParameterMap(Type type, int offset) {
		setType(type);
		_offset = offset;
			
	}
	
	private void setType(Type type) {
		_type = type;
		if (isNamed())
			_delegate = new LinkedMap();
		if (isPositional())
			_delegate = new TreeMap<Integer, Object>();
	}
	
	/**
	 * Affirms if this receiver contains named parameter keys.
	 * 
	 * @return false also denotes that the parameter key type is not yet known.
	 * Map that are constructed with unspecified key type becomes known at 
	 * first insertion. 
	 * 
	 * @see #put(Object, Object)
	 */
	public boolean isNamed() {
		return Type.NAMED.equals(_type);
	}
	

	/**
	 * Affirms if this receiver contains positional parameter keys.
	 * 
	 * @return false also denotes that the parameter key type is not yet known.
	 * Map that are constructed with unspecified key type becomes known at 
	 * first insertion. 
	 * 
	 * @see #put(Object, Object)
	 */
	public boolean isPositional() {
		return Type.POSITIONAL.equals(_type);
	}
	
	/**
	 * Gets the enumerated key type allowed in this receiver.
	 */
	public Type getType() {
		return _type;
	}
	
	/**
	 * Clears this receiver for complete reuse. This includes clearing the
	 * bound values, their allowed types as well as the type of allowed keys.
	 */
	public void clear() {
		if (_delegate == null)
			return;
		_delegate = null;
		_valueTypes.clear();
		_type = null;
	}
	
	/**
	 * Clears all the bound keys by nullifying their values. The allowed value
	 * type for each key and the allowed key type remain.
	 */
	public void clearBinding() {
		if (_delegate == null)
			return;
		for (Object key : keySet())
			_delegate.put(key, null);
	}

	/**
	 * Affirms if this receiver contains the given key.
	 */
	public boolean containsKey(Object key) {
		return _delegate == null ? false : _delegate.containsKey(key);
	}

	/**
	 * Affirms if this receiver contains the given value.
	 */
	public boolean containsValue(Object value) {
		return _delegate == null ? null : _delegate.containsValue(value);
	}

	/**
	 * Gets the keys known to this receiver.
	 */
	public Set entrySet() {
		return _delegate == null ? Collections.EMPTY_SET : _delegate.entrySet();
	}

	/**
	 * Gets the value for the given key.
	 * 
	 * @return null if the key does not exist or if the key exists with null
	 * value.
	 */
	public Object get(Object key) {
		return _delegate == null ? null : _delegate.get(key);
	}

	/**
	 * Affirms if this receiver has no key. 
	 * 
	 */
	public boolean isEmpty() {
		return _delegate == null || _delegate.isEmpty();
	}

	/**
	 * Puts the given key-value pair. If this is the first key inserted and the
	 * key type is unspecified during construction, then the type of the given
	 * key determines whether this receiver will hold named or positional 
	 * parameters.
	 * 
	 * @param key must be either String or Integer and must match the allowed
	 * key type of this receiver unless this is a first key insertion and the
	 * allowed key type is unspecified at construction.
	 * 
	 * @param value is validated if an allowed value type for the given key is
	 * set.
	 * 
	 */
	public Object put(Object key, Object value) {
		if (key == null)
			newValidationException("param-null-key", new Object[]{});
		Type type =  determineKeyType(key);
		if (_type == null) {
			setType(type);
		} else {
			if (!_type.equals(type))
				newValidationException("param-mismatch-key-type", key, 
					key.getClass(), _type);
					
			if (isPositional() && ((Integer)key).intValue() < _offset)
				newValidationException("param-bad-key-index", key, _offset);
		}
		
		assertCompatiableValue(key, value);
		return _delegate.put(key, value);
	}
	
	public void putAll(Map t) {
		_delegate.putAll(t);
	}

	public Object remove(Object key) {
		return (_delegate == null) ? null : _delegate.remove(key);
	}

	public int size() {
		return (_delegate == null) ? 0 : _delegate.size();
	}

	/**
	 * Sets the type of value that the given key can be bound to. If a value
	 * exists for the key, then the value is validated against the given type. 
	 * 
	 * @param key the key may or may not have been bound to a value.
	 * @param type the type of the value that the given key can be bound to. 
	 */
	public void setValueType(Object key, Class expectedType) {
		_valueTypes.put(key, expectedType);
		if (containsKey(key)) {
			Object value = get(key);
			assertCompatiableValue(key, value);
		}
	}
	
	/**
	 * Gets the type of value the given key can bind to.
	 * 
	 * @return null denotes the value type for the given key is either not set 
	 * or set to null explicitly.
	 *  
	 */
	public Class getValueType(Object key) {
		return _valueTypes.get(key);
	}
	
	/**
	 * Gets the set of keys. The returned set has deterministic iteration order.
	 * For named parameter type, the resultant order is the order in which
	 * keys were inserted.
	 * For positional parameter type, the resultant order is the natural order 
	 * of the key indices. 
	 */
	public Set keySet() {
		return (_delegate == null) ? Collections.EMPTY_SET : _delegate.keySet();
	}

	/**
	 * Gets the values. The returned collection has deterministic iteration 
	 * order. 
	 * For named parameter type, the resultant order is the order in which
	 * key-value pairs were inserted.
	 * For positional parameter type, the resultant order is the natural order 
	 * of the key indices.
	 * 
	 */
	public Collection values() {
		return (_delegate == null) ? Collections.EMPTY_SET : _delegate.values();
	}
	
	/**
	 * Get the unmodifable map of key-value in deterministic iteration order.
	 * For named parameter type, the resultant order is the order in which
	 * key-value pairs were inserted.
	 * For positional parameter type, the resultant order is the natural order 
	 * of the key indices.
	 */
	public Map getMap() {
		return isEmpty() ? Collections.EMPTY_MAP : 
			Collections.unmodifiableMap(_delegate);
	}
	
	/**
	 * Create a positional map as a copy of this receiver.
	 * If this receiver is a named map, then the new positional map will have
	 * integer keys corresponding to insertion order of the original keys. 
	 * 
	 */
	public ParameterMap toPositional() {
		ParameterMap positional = new ParameterMap(Type.POSITIONAL, _offset);
		Set keys = keySet();
		int i = _offset;
		for (Object key : keys) {
			Object newKey = (isNamed()) ? new Integer(i++) : key;
			positional.put(newKey, get(key));
			Class expectedType = getValueType(key);
			if (_valueTypes.containsKey(key))
				setValueType(newKey, expectedType);
		}
		return positional;
	}
	
	/**
	 * Validate the parameters against a set of expected parameter types.
	 * If strict then also checks that each given expected key exists in
	 * this receiver.
	 */
	public boolean validate(LinkedMap expected, boolean strict) {
		if (expected.size() != size())
			newValidationException("param-invalid-size", 
			    expected.size(), expected, size(), getMap());
		if (!strict) return true;
		for (Object key : expected.keySet()) {
			key = isNamed() ? key.toString() : Integer.parseInt(key.toString());
			if (!containsKey(key))
				newValidationException("param-missing", key, expected, getMap());
		} 
		return true;
	}
	
	void assertCompatiableValue(Object key, Object value) {
		Class expected = _valueTypes.get(key);
		if (expected == null)
			return;
		if (value == null) {
			if (expected.isPrimitive()) 
				newValidationException("param-null-primitive", key, expected);
			return;
		}
		if (expected.isPrimitive()) {
			if (unwrap(value.getClass()) != expected) 
				newValidationException("param-mismatch-value-type", 
					key, value, value.getClass(), expected);
		} else if (!expected.isAssignableFrom(value.getClass())) {
			newValidationException("param-mismatch-value-type", 
					key, value, value.getClass(), expected);
		} 
	}
	
	private Type determineKeyType(Object key) {
		if (key instanceof Integer)
			return Type.POSITIONAL;
		else if (key instanceof String)
			return Type.NAMED;
		else
			newValidationException("param-bad-key-type", key, key.getClass());
		return null;//unreachable
	}

	
	Class unwrap(Class c) {
		if (c == null)
			return c;
		if (c == Boolean.class)   return boolean.class;
		if (c == Byte.class)      return byte.class;
		if (c == Character.class) return char.class;
		if (c == Double.class)    return double.class;
		if (c == Float.class)     return float.class;
		if (c == Integer.class)   return int.class;
		if (c == Long.class)      return long.class;
		if (c == Short.class)     return short.class;
		return c;
	}
	
	public String toString() {
		return _type + " values " + _delegate + " types " + _valueTypes;
	}
	
	void newValidationException(String msgKey, Object... args) {
		throw new IllegalArgumentException(_loc.get(msgKey, args).toString());
	}
}
