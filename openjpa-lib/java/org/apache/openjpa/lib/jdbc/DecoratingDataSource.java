/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.jdbc;


import java.sql.*;
import java.util.*;
import javax.sql.*;


/**
 *	<p>Delegating data source that maintains a list of 
 *	{@link ConnectionDecorator}s.</p>
 *
 *	@author		Abe White
 *	@nojavadoc
 */
public class DecoratingDataSource
	extends DelegatingDataSource
{
	private static final int INITIAL_SIZE = 7;

	private volatile List 	_decorators	= null;
	private boolean			_conf		= false;


	/**
	 *	Constructor.  Supply wrapped data source.
	 */
	public DecoratingDataSource (DataSource ds)
	{
		super (ds);
	}


	/**
	 *	Whether the datasource is in configuration mode.  Configuration mode
	 *	allows more efficient modification of the decorator chain.  No 
	 *	connections can be obtained while in configuration mode.
	 */
	public boolean isConfiguring ()
	{
		return _conf;
	}


	/**
	 *	Whether the datasource is in configuration mode.  Configuration mode
	 *	allows more efficient modification of the decorator chain.  No 
	 *	connections can be obtained while in configuration mode.
	 */
	public void setConfiguring (boolean conf)
	{
		_conf = conf;
	}


	/**
	 *	Return a read-only list of connection decorators in the order they were
	 *	added.
	 */
	public Collection getDecorators ()
	{
		return (_decorators == null) ? Collections.EMPTY_LIST
			: Collections.unmodifiableCollection (_decorators);
	}


	/**
	 *	Add a connection decorator.
	 */
	public synchronized void addDecorator (ConnectionDecorator decorator)
	{
		if (decorator == null)
			throw new NullPointerException ("decorator == null");

		if (_conf)
		{
			if (_decorators == null)
				_decorators = new ArrayList (INITIAL_SIZE);
			_decorators.add (decorator);
		}
		else
		{
			// copy so we don't have to synchronize iteration in decorate
			int size = (_decorators == null) ? 1 : _decorators.size () + 1;
			List copy = new ArrayList (Math.max (INITIAL_SIZE, size));
			if (_decorators != null)
				copy.addAll (_decorators);		
			copy.add (decorator);
			_decorators = copy;
		}
	}


	/**
	 *	Remove a connection decorator.
	 */
	public synchronized boolean removeDecorator (ConnectionDecorator decorator)
	{
		if (decorator == null || _decorators == null 
			|| !_decorators.contains (decorator))
			return false;

		if (_conf)
			_decorators.remove (decorator);
		else
		{
			// copy so we don't have to synchronize iteration in decorate
			List copy = new ArrayList (Math.max (INITIAL_SIZE, 
				_decorators.size () - 1));
			for (int i = 0; i < _decorators.size (); i++)
				if (_decorators.get (i) != decorator)
					copy.add (_decorators.get (i));
			_decorators = copy;
		}
		return true;
	}


	/**
	 *	Clear all decorators.
	 */
	public synchronized void clearDecorators ()
	{
		_decorators = null;
	}


	public Connection getConnection ()
		throws SQLException
	{
		Connection conn = super.getConnection ();
		return decorate (conn);
	}


	public Connection getConnection (String user, String pass)
		throws SQLException
	{
		Connection conn = super.getConnection (user, pass);
		return decorate (conn);
	}


	private Connection decorate (Connection conn)
		throws SQLException
	{
		if (_conf)
			throw new IllegalStateException ();

		// use local in case _decorators replaced during loop
		List dec = _decorators;
		if (dec != null)
			for (int i = 0; i < dec.size (); i++)
				conn = ((ConnectionDecorator) dec.get (i)).decorate (conn);	
		return conn;
	}
}
