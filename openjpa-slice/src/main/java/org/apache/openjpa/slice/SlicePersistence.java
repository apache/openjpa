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
package org.apache.openjpa.slice;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.util.ImplHelper;

/**
 * A helper to determine the slice identifier of an instance.
 * 
 * @author Pinaki Poddar 
 *
 */
public class SlicePersistence {
	/**
	 * Get the slice identifier for the given instance if it is a managed
	 * instance and has been assigned to a slice.
	 * 
	 * @return name of the slice, if any. null otherwise.
	 */
	public static String getSlice(Object obj) {
		if (obj == null)
			return null;
		PersistenceCapable pc = ImplHelper.toPersistenceCapable(obj, null);
		if (pc == null)
			return null;
		StateManagerImpl sm = (StateManagerImpl)pc.pcGetStateManager();
		if (sm == null)
			return null;
		Object slice = sm.getImplData();
		return (slice instanceof String) ? (String)slice : null;
	}
}
