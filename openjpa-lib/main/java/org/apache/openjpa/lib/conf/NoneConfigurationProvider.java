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
package org.apache.openjpa.lib.conf;


import java.io.*;
import java.util.*;

import org.apache.commons.collections.*;


/**
 *	<p>No-op configuration provider.</p>
 *
 *	@author		Abe White 
 *	@nojavadoc
 */
public class NoneConfigurationProvider
	implements ConfigurationProvider
{
	private static final NoneConfigurationProvider _instance = 
		new NoneConfigurationProvider ();


	/**
	 *	Singleton.
	 */
	public static NoneConfigurationProvider getInstance ()
	{
		return _instance;
	}


	public boolean loadDefaults (ClassLoader loader)
	{
		return false;
	}


	public boolean load (String resource, ClassLoader loader)
	{
		return false;
	}


	public boolean load (File file)
	{
		return false;
	}


	public Map getProperties ()
	{
		return MapUtils.EMPTY_MAP;
	}


	public void addProperties (Map props)
	{
		if (props != null && !props.isEmpty ())
			throw new UnsupportedOperationException ();
	}


	public void setInto (Configuration conf)
	{
	}
}
