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
     * depending on whether the given instance is {@link DistributedConfiguration#isReplicated(Class) replicated}.
	 */
	public static SliceInfo getSlicesByPolicy(Object pc, 
			DistributedConfiguration conf, Object ctx) {
		List<String> actives = conf.getActiveSliceNames();
		Object policy = null;
		String[] targets = null;
		boolean replicated = isReplicated(pc, conf);
		if (replicated) {
			policy = conf.getReplicationPolicyInstance();
            targets = ((ReplicationPolicy)policy).replicate(pc, actives, ctx);
		} else {
			policy = conf.getDistributionPolicyInstance();
            targets = new String[]{((DistributionPolicy)policy).distribute
				(pc, actives, ctx)};
		}
		assertSlices(targets, pc, conf.getActiveSliceNames(), policy);
		return new SliceInfo(replicated, targets);
	}
	
	private static void assertSlices(String[] targets, Object pc, 
	    List<String> actives, Object policy) {
	    if (targets == null || targets.length == 0)
            throw new UserException(_loc.get("no-policy-slice", new Object[] {
                policy.getClass().getName(), pc, actives}));
        for (String target : targets) 
            if (!actives.contains(target))
                throw new UserException(_loc.get("bad-policy-slice", 
                   new Object[] {policy.getClass().getName(), target, pc, 
                    actives}));
	}
	
    /**
     * Gets the target slices for the given StateManager.
     */
    public static SliceInfo getSlicesByPolicy(OpenJPAStateManager sm, 
        DistributedConfiguration conf, Object ctx) {
        return getSlicesByPolicy(sm.getPersistenceCapable(), conf, ctx);
    }
    
    
	/**
	 * Affirms if the given instance be replicated to multiple slices.
	 */
    public static boolean isReplicated(Object pc, DistributedConfiguration conf) {
        return pc == null ? false : conf.isReplicated(pc.getClass());
	}

	/**
	 * Affirms if the given instance be replicated to multiple slices.
	 */
//	public static boolean isReplicated(OpenJPAStateManager sm) {
//	    return sm == null ? false : 
//		if (sm == null)
//			return false;
//		return sm.getMetaData().isReplicated();
//	}
	
	/**
	 * Affirms if the given StateManager has an assigned slice.
	 */
	public static boolean isSliceAssigned(OpenJPAStateManager sm) {
	     return sm != null && sm.getImplData() != null 
	         && sm.getImplData() instanceof SliceInfo;
	}

    /**
     * Gets the assigned slice information, if any, from the given StateManager.
     */
    public static SliceInfo getSliceInfo(OpenJPAStateManager sm) {
        return isSliceAssigned(sm) ? (SliceInfo) sm.getImplData() : null;
    }
}
