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

import java.util.List;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.UserException;

/**
 * Utility methods to determine the target slices for a persistence capable
 * instance by calling back to user-specified distribution policy.
 * 
 * @author Pinaki Poddar
 *
 */
public class SliceImplHelper {
	private static final Localizer _loc =
		Localizer.forPackage(SliceImplHelper.class);
	
	/**
	 * Gets the target slices by calling user-specified 
	 * {@link DistributionPolicy} or {@link ReplicationPolicy} 
	 * depending on whether the given instance is {@link Replicated replicated}.
	 */
	public static String[] getSlicesByPolicy(Object pc, 
			DistributedConfiguration conf, Object ctx) {
		List<String> availables = conf.getActiveSliceNames();
		Object policy = null;
		String[] targets = null;
		if (isReplicated(pc, conf)) {
			policy = conf.getReplicationPolicyInstance();
			targets = ((ReplicationPolicy)policy).replicate
				(pc, availables, ctx);
			if (targets == null || targets.length == 0)
				targets = availables.toArray(new String[availables.size()]);
		} else {
			policy = conf.getDistributionPolicyInstance();
			String slice = ((DistributionPolicy)policy).distribute 
				(pc, availables, ctx);
			targets = new String[]{slice};
		}
		for (String target : targets) 
			if (!availables.contains(target))
			throw new UserException(_loc.get("bad-policy-slice", new Object[] {
					policy.getClass().getName(), target, pc, availables}));
		return targets;
	}
	
	/**
	 * Affirms if the given instance be replicated to multiple slices.
	 */
	public static boolean isReplicated(Object pc, OpenJPAConfiguration conf) {
		if (pc == null)
			return false;
		ClassMetaData meta = conf.getMetaDataRepositoryInstance()
			.getMetaData(pc.getClass(), null, false);
		return (meta == null) ? false : meta.isReplicated();
	}

	/**
	 * Affirms if the given instance be replicated to multiple slices.
	 */
	public static boolean isReplicated(OpenJPAStateManager sm) {
		if (sm == null)
			return false;
		return sm.getMetaData().isReplicated();
	}
}
