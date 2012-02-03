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
package org.apache.openjpa.lib.conf;

/**
 * An immutable boolean value can be set only once and can never be modified to a different value.
 * 
 * @author Pinaki Poddar
 *
 */
public class ImmutableBooleanValue extends BooleanValue {
	private boolean _dirty;
	
	public ImmutableBooleanValue(String prop) {
		super(prop);
	}
	
	public ImmutableBooleanValue(String prop, boolean value) {
		super(prop);
		set(value);
	}

	
    public void set(boolean value) {
    	if (_dirty) {
    		if (value != get().booleanValue())
    			throw new IllegalStateException(this + " can not be changed from " + get() + " to " + value);
    	} else {
    		_dirty = true;
            super.set(value);
    	}
    }


}
