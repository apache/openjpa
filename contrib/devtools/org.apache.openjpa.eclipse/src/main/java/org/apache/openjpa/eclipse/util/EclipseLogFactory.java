/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openjpa.eclipse.util;

import org.apache.openjpa.lib.log.AbstractLog;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.log.LogFactory;

/**
 * An OpenJPA LogFactory which logs using the Eclipse logging stuff.
 * 
 * @see LogFactory
 * @see LogUtil
 * 
 * @author Michael Vorburger
 */
public class EclipseLogFactory implements LogFactory {

	/**
	 * @see http://openjpa.apache.org/builds/latest/docs/manual/ref_guide_logging_custom.html
	 * 
	 * @see org.apache.openjpa.lib.log.LogFactory#getLog(java.lang.String)
	 */
	public Log getLog(String channel) {
		return new AbstractLog() {

            protected boolean isEnabled(short logLevel) {
                // log all levels
                return true;
            }

            protected void log(short type, String message, Throwable t) {
            	if (type == Log.ERROR || type == Log.FATAL)
            		LogUtil.logError("OpenJPA Error: " + message, t);
            	if (type == Log.WARN)
            		LogUtil.logWarn("OpenJPA Warning: " + message, t);
            	
            	// Ignore other internal messages... could be made this configurable via Builder argument 
            	// (like include pattern), if really needed 
            	// else
            	// LogUtil.logInfo("OpenJPA Info: " + message, t);
            }
        };
	}

}
