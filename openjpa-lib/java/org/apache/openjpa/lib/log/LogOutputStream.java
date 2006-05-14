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
package org.apache.openjpa.lib.log;


import java.io.*;
import java.util.*;


/**
 *	<p>Many standard components log to {@link OutputStream}s.
 *	This wrapper class extends the {@link ByteArrayOutputStream}
 *	class and writes all given data to an underlying {@link Log} instance
 *	whenever it is flushed and the internally held byte array contains a 
 *	newline.</p>
 *
 *	@author		Abe White, Marc Prud'hommeaux
 */
public class LogOutputStream
	extends ByteArrayOutputStream
{
	private static final String _sep = System.getProperty ("line.separator");

	private final int _level;
	private final Log _log;

	
	/**
	 *	Constructor.
	 *	
	 *	@param	log		the log to log to
	 *	@param	level	the level to log at
	 */
	public LogOutputStream (Log log, int level)
	{
		_log = log;
		_level = level;
	}
	
	
	public void flush ()
		throws IOException
	{
		super.flush ();

		String msg = new String (toByteArray ());
		if (msg != null && msg.length () > 0 && msg.indexOf (_sep) != -1)
		{
			// break up the message based on the line separator; this
			// may be because the flushed buffer contains mutliple lines
			for (StringTokenizer tok = new StringTokenizer (msg, _sep);
				tok.hasMoreTokens (); )
			{
				String next = tok.nextToken ();
				log (next);
			}

			// clear the internally held byte array
			reset ();
		}
	}


	private void log (String msg)
	{
		switch (_level)
		{
		case Log.TRACE:
			_log.trace (msg);
			break;
		case Log.DEBUG:
			_log.debug (msg);
			break;
		case Log.INFO:
			_log.info (msg);
			break;
		case Log.WARN:
			_log.warn (msg);
			break;
		case Log.ERROR:
			_log.error (msg);
			break;
		case Log.FATAL:
			_log.fatal (msg);
			break;
		}
	}
}

