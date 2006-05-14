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
package org.apache.openjpa.lib.xml;

import java.text.*;
import org.xml.sax.*;

import org.apache.openjpa.lib.util.*;

import serp.util.*;


/**
 *	@author		Stephen Kim
 *	@nojavadoc
 */
public class Location
{
	private static final Localizer _loc = Localizer.forPackage (Location.class);
	
	private boolean _nullOnNoLocator = false;
	private Locator	_locator	= null;

	public Location ()
	{
		this (false);
	}
	
	public Location (boolean nullOnNoLocator)
	{
		_nullOnNoLocator = nullOnNoLocator;
	}

	/**
	 * for customized responses
	 */ 
	public String getLocation (String format)
	{
		if (_locator == null)
		{
			if (_nullOnNoLocator)
				return null;
			return _loc.get ("no-locator");
		}
		String forma = MessageFormat.format (format, new Object [] {
			Numbers.valueOf (_locator.getLineNumber ()),
			Numbers.valueOf (_locator.getColumnNumber ()),
			_locator.getPublicId (),
			_locator.getSystemId ()});
		return forma;
	}

	public String getLocation ()
	{
		return getLocation (_loc.get ("location-format"));
	}

	public void setLocator (Locator locator)
	{
		_locator = locator;
	}

	public Locator getLocator ()
	{
		return _locator;
	}

	public void setNullOnNoLocator (boolean val)
	{
		_nullOnNoLocator = val;
	}

	public boolean isNullOnNoLocator ()
	{
		return _nullOnNoLocator;
	}
}
