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
package org.apache.openjpa.lib.util;


import java.io.*;


/**
 *	<p>Lock implementation.  The lock is aware of the thread that owns it, and 
 *	allows that thread to {@link #lock} multiple times without blocking.  Only
 *	the owning thread can {@link #unlock}, and the lock will not be released to
 *	other threads until {@link #unlock} has been called the same number of 
 *	times as {@link #lock}.</p>
 *
 *	<p>Using this lock is similar to synchronizing on an object, but is more
 *	flexible (for example, the calls to {@link #lock} and {@link #unlock} can
 *	be surrounded by if statements).</p>
 *
 *	<p>Note that the lock resets on serialization.</p>
 *
 *	@author		Abe White
 *	@nojavadoc
 */
public class ThreadLock
	implements Serializable
{
	private transient int		_count	= 0;
	private transient Thread 	_owner 	= null;

	
	/**
	 *	Atomically lock.  Blocks until the lock is available.
	 */
	public void lock ()
	{
		Thread thread = Thread.currentThread ();
		synchronized (this)
		{
			if (thread == _owner)
				_count++;
			else
			{
				while (_owner != null)
					try { wait (); } catch (InterruptedException ie) {}
				_count = 1;
				_owner = thread;
			}
		}
	}


	/**
	 *	Atomically lock.  Blocks until the lock is available or a timeout
	 *	occurs.
	 *
	 *	@para	timeout		the number of milliseconds to wait before timing out
	 *	@return				true if the lock was obtained, false on timeout
	 */
	public boolean lock (long timeout)
	{
		// use version that doesn't need to check time; more efficient
		if (timeout == 0)
		{
			lock ();
			return true;
		}

		Thread thread = Thread.currentThread ();
		synchronized (this)
		{
			if (thread == _owner)
			{
				_count++;
				return true;
			}
			if (_owner == null)
			{
				_count = 1;
				_owner = thread;
				return true;
			}

			long time = System.currentTimeMillis ();
			long end = time + timeout;
			while (_owner != null && time < end)
			{
				try { wait (end - time); } catch (InterruptedException ie) {}
				time = System.currentTimeMillis ();
			}
			if (_owner == null)
			{
				_count = 1;
				_owner = thread;
				return true;
			}
			return false;
		}
	}


	/**
	 *	Releases the lock.  This method can only be called by the owning
	 *	thread.
	 *
	 *	@throws	IllegalStateException if current thread is not owner
	 */
	public void unlock ()
	{
		Thread thread = Thread.currentThread ();
		synchronized (this)
		{
			if (thread != _owner)
				throw new IllegalStateException ();
		
			_count--;
			if (_count == 0)
			{
				_owner = null;
				notify ();
			}
		}
	}


	/**
	 *	Return true if this lock is locked by the current thread.
	 */
	public boolean isLocked ()
	{
		Thread thread = Thread.currentThread ();
		synchronized (this)
		{
			if (thread != _owner)
				return false;
			return _count > 0;
		}
	}
}
