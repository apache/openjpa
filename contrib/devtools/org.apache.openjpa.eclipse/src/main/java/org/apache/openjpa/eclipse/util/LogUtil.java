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


import org.apache.openjpa.eclipse.Activator;
import org.eclipse.core.runtime.Status;

/**
 * Helper for Logging.
 * 
 * @author Michael Vorburger
 */
public final class LogUtil {
	private LogUtil() { };
	
	public static void logOK(String msg) {
		log(Status.OK, msg);
		
	}
	public static void logInfo(String msg) {
		log(Status.INFO, msg);
	}
	
	public static void logInfo(String msg, Throwable t) {
		log(Status.INFO, msg, t);
	}

	public static void logWarn(String msg, Throwable t) {
		log(Status.WARNING, msg, t);
	}
	
	public static void logError(Throwable t) {
		log(Status.ERROR, t.getMessage(), t);
	}

	public static void logError(String msg, Throwable t) {
		log(Status.ERROR, msg, t);
	}

	private static void log(int status, String msg) {
		Activator.getDefault().getLog().log(new Status(status, Activator.PLUGIN_ID, msg));
	}

	private static void log(int status, String msg, Throwable t) {
		Activator.getDefault().getLog().log(new Status(status, Activator.PLUGIN_ID, msg, t));
	}
}
