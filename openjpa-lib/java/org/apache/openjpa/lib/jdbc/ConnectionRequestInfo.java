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


import org.apache.commons.lang.*;


/**
 *	<p>Information about a JDBC connection request.</p>
 *
 *	@author		Marc Prud'hommeaux
 *	@nojavadoc
 */
public class ConnectionRequestInfo
{
	public String user = null;
	public String pass = null;


	public ConnectionRequestInfo ()
	{
	}


	public ConnectionRequestInfo (String user, String pass)
	{
		this.user = user;
		this.pass = pass;
	}


	public int hashCode ()
	{
		return (((user == null) ? 0 : user.hashCode ())
			+ ((pass == null) ? 0 : pass.hashCode ()))
			% Integer.MAX_VALUE;
	}


	public boolean equals (Object other)
	{
		if (other == this)
			return true;
		if (!(other instanceof ConnectionRequestInfo))
			return false;

		ConnectionRequestInfo cri = (ConnectionRequestInfo) other;
		return StringUtils.equals (user, cri.user)
			&& StringUtils.equals (pass, cri.pass);
	}
}
