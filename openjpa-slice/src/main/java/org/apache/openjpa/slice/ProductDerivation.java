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

import java.util.Map;

import org.apache.openjpa.conf.OpenJPAProductDerivation;
import org.apache.openjpa.lib.conf.AbstractProductDerivation;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.slice.jdbc.DistributedJDBCBrokerFactory;
import org.apache.openjpa.slice.jdbc.DistributedJDBCConfigurationImpl;

/**
 * Derives configuration for Slice.
 * Introduces a specialized BrokerFactory aliased as <code>slice</code>.
 * All Slice specific configuration is prefixed as 
 * <code>openjpa.slice.*.*</code>
 * 
 * @author Pinaki Poddar 
 *
 */
public class ProductDerivation extends AbstractProductDerivation implements
		OpenJPAProductDerivation {
    /**
     * Prefix for all Slice-specific configuration properties. 
     */
    public static final String PREFIX_SLICE = "openjpa.slice";
    
    /**
     * Hint key <code>openjpa.hint.slice.Target </code> to specify a subset of 
     * slices for query. The value corresponding to the key is comma-separated
     * list of slice identifiers.
     *  
     */
    public static final String HINT_TARGET  = "openjpa.hint.slice.Target";
    
	@SuppressWarnings("unchecked")
	public void putBrokerFactoryAliases(Map m) {
		m.put("slice", DistributedJDBCBrokerFactory.class.getName());
	}

	public String getConfigurationPrefix() {
		return PREFIX_SLICE;
	}

	public int getType() {
		return TYPE_STORE;
	}
	
	/**
	 * Sets the {@link DistributionPolicy} and {@link ReplicationPolicy} to
	 * their respective defaults if not set by the user.
	 */
    @Override
    public boolean afterSpecificationSet(Configuration c) {
        if (!(c instanceof DistributedJDBCConfigurationImpl))
            return false;
        DistributedJDBCConfigurationImpl conf = 
        	(DistributedJDBCConfigurationImpl)c;
        boolean modified = false;
        if (conf.getDistributionPolicyInstance() == null) {
        	conf.distributionPolicyPlugin.setString("random");
        	modified = true;
        }
        if (conf.getReplicationPolicyInstance() == null) {
        	conf.replicationPolicyPlugin.setString("all");
        	modified = true;
        }
        return modified;
    }
}
