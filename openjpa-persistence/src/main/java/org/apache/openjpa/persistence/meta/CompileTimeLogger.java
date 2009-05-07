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
package org.apache.openjpa.persistence.meta;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

/**
 * Simple logger sets log level from javac compilers annotation processing 
 * options <code>-Alog=TRACE|INFO|WARN|ERROR</code> and uses the processing
 * environment to determine the log output stream.
 * 
 * @author Pinaki Poddar
 *
 */
public class CompileTimeLogger {
    private static enum Level {TRACE, INFO, WARN, ERROR};
    private int logLevel;
    private Messager messager;
    public CompileTimeLogger(ProcessingEnvironment env) {
        String level = env.getOptions().get("log");
        if ("trace".equalsIgnoreCase(level))
            logLevel = Level.TRACE.ordinal();
        else if ("info".equalsIgnoreCase(level))
            logLevel = Level.INFO.ordinal();
        else if ("warn".equalsIgnoreCase(level))
            logLevel = Level.WARN.ordinal();
        else if ("error".equalsIgnoreCase(level))
            logLevel = Level.ERROR.ordinal();
        else {
            logLevel = Level.INFO.ordinal();
            warn("mmg-bad-log");
        }
        messager = env.getMessager();
        
    }
    
    public void info(String message) {
        log(Level.INFO, message, Diagnostic.Kind.NOTE);
    }
    
    public void trace(String message) {
        log(Level.TRACE, message, Diagnostic.Kind.NOTE);
    }
    
    public void warn(String message) {
        log(Level.WARN, message, Diagnostic.Kind.MANDATORY_WARNING);
    }
    
    public void error(String message) {
        log(Level.ERROR, message, Diagnostic.Kind.ERROR);
    }
    
    private void log(Level level, String message, Diagnostic.Kind kind) {
        if (logLevel <= level.ordinal()) {
            messager.printMessage(kind, message);
        }
    }
}
