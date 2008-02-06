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
package org.apache.openjpa.slice.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.openjpa.jdbc.kernel.JDBCStoreManager;
import org.apache.openjpa.lib.jdbc.DelegatingDataSource;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.slice.Slice;
import org.apache.openjpa.util.InternalException;

/**
 * A specialized JDBCStoreManager for XA-complaint DataSource.
 * If the configured DataSource is not XA-complaint, behaves as the super 
 * implementation.
 * 
 * @author Pinaki Poddar 
 *
 */
public class SliceStoreManager extends JDBCStoreManager {
	private final Slice _slice;
	private Boolean isXAEnabled;
	private XAConnection xcon;
	
	private static final Localizer _loc = 
		Localizer.forPackage(SliceStoreManager.class);
	
	/**
	 * Construct with immutable logical name of the slice. 
	 */
	public SliceStoreManager(Slice slice) {
		_slice = slice;
	}
	
	/**
	 * Gets the slice for which this receiver is working.
	 */
	public Slice getSlice() {
	    return _slice;
	}
	
	public String getName() {
	    return _slice.getName();
	}
	
	/**
	 * Gets the connection via XAConnection if the datasource is XA-complaint.
	 * Otherwise, behaves exactly as the super implementation. 
	 */
	@Override
	protected RefCountConnection connectInternal() throws SQLException { 
		if (!isXAEnabled)
			return super.connectInternal();
		XADataSource xds = getXADataSource();
		xcon = xds.getXAConnection();
		Connection con = xcon.getConnection();
		return new RefCountConnection(con);
	}
	
	/**
	 * Gets the XAConnection if connected and XA-complaint. Otherwise null.
	 */
	public XAConnection getXAConnection() {
		return xcon;
	}
	
	private XADataSource getXADataSource() {
		if (!isXAEnabled())
			throw new InternalException(_loc.get("slice-not-xa", this));
		return (XADataSource)getInnerDataSource();
	}
	
	/**
	 * Affirms if the configured DataSource is XA-complaint.
	 * Can return null if the context has not been set yet.
	 */
	public boolean isXAEnabled() {
		if (isXAEnabled == null) {
			isXAEnabled = getInnerDataSource() instanceof XADataSource;
		}
		return isXAEnabled.booleanValue();
	}
	
	private DataSource getInnerDataSource() {
		DataSource parent = super.getDataSource();
		DataSource real = (parent instanceof DelegatingDataSource) ?
				((DelegatingDataSource)parent).getInnermostDelegate() 
				: parent;
		return real;
	}
}
