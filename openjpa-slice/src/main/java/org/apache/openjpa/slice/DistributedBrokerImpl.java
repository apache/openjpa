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

import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.OpCallbacks;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * A specialized Broker to associate slice identifiers with the StateManagers as
 * they are persisted in a cascade. This intervention helps the user to define
 * distribution policy only for root instances i.e. the instances that are
 * explicit argument to persist() call. The cascaded instances are assigned the
 * same slice to honor collocation constraint.
 * 
 * @author Pinaki Poddar
 * 
 */
@SuppressWarnings("serial")
public class DistributedBrokerImpl extends BrokerImpl {
	private transient String slice;

	private static final Localizer _loc =
			Localizer.forPackage(DistributedBrokerImpl.class);

	/**
	 * Assigns slice identifier to the resultant StateManager as initialized by
	 * the super class implementation. The slice identifier is decided by
	 * {@link DistributionPolicy} for given <code>pc</code> if it is a root
	 * instance i.e. the argument of the user application's persist() call. The
	 * cascaded instances are detected by non-empty status of the current
	 * operating set. The slice is assigned only if a StateManager has never
	 * been assigned before.
	 */
	public OpenJPAStateManager persist(Object pc, Object id, boolean explicit,
			OpCallbacks call) {
		OpenJPAStateManager sm = getStateManager(pc);
		if (getOperatingSet().isEmpty()
				&& (sm == null || sm.getImplData() == null)) {
			slice = getSlice(pc);
		}
		sm = super.persist(pc, id, explicit, call);
		if (sm.getImplData() == null)
			sm.setImplData(slice, true);

		return sm;
	}

	/**
	 * Gets the slice by the user-defined distribution policy.
	 */
	String getSlice(Object pc) {
		DistributedConfiguration conf =
				(DistributedConfiguration) getConfiguration();
		String slice =
				(conf.getDistributionPolicyInstance().distribute(pc, conf
						.getActiveSliceNames(), this));
		if (!conf.getActiveSliceNames().contains(slice))
			throw new UserException(_loc.get("bad-policy-slice", new Object[] {
					conf.getDistributionPolicyInstance().getClass().getName(),
					slice, pc, conf.getActiveSliceNames() }));
		return slice;
	}
}
