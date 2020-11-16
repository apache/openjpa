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
package org.apache.openjpa.lib.log;

import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Value;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JULLogFactory implements LogFactory, Configurable {
    private ClassLoader classLoader;

    public JULLogFactory() {
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Log getLog(final String channel) {
        final Thread thread;
        final ClassLoader oldLoader;
        if (classLoader != null) {
            thread = Thread.currentThread();
            oldLoader = thread.getContextClassLoader();
        } else {
            thread = null;
            oldLoader = null;
        }
        try {
            return new JULLog(Logger.getLogger(channel));
        } finally {
            if (thread != null) {
                thread.setContextClassLoader(oldLoader);
            }
        }
    }

    @Override
    public void setConfiguration(final Configuration conf) {
        final Value value = conf.getValue("Log.JULFactory");
        if (value != null) {
            final String string = value.getString();
            if (string != null && string.contains("SkipStartClassLoader=true")) {
                classLoader = null;
            }
        }
    }

    @Override
    public void startConfiguration() {
        // no-op
    }

    @Override
    public void endConfiguration() {
        // no-op
    }

    private static class JULLog implements Log {
        private final Logger logger;

        public JULLog(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isLoggable(Level.FINEST);
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isLoggable(Level.INFO);
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isLoggable(Level.WARNING);
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isLoggable(Level.SEVERE);
        }

        @Override
        public boolean isFatalEnabled() {
            return logger.isLoggable(Level.SEVERE);
        }

        @Override
        public void trace(final Object o) {
            logger.finest(String.valueOf(o));
        }

        @Override
        public void trace(final Object o, final Throwable t) {
            logger.log(Level.FINEST, String.valueOf(o), t);
        }

        @Override
        public void info(final Object o) {
            logger.info(String.valueOf(o));
        }

        @Override
        public void info(final Object o, final Throwable t) {
            logger.log(Level.INFO, String.valueOf(o), t);
        }

        @Override
        public void warn(final Object o) {
            logger.warning(String.valueOf(o));
        }

        @Override
        public void warn(final Object o, final Throwable t) {
            logger.log(Level.WARNING, String.valueOf(o), t);
        }

        @Override
        public void error(final Object o) {
            logger.severe(String.valueOf(o));
        }

        @Override
        public void error(final Object o, final Throwable t) {
            logger.log(Level.SEVERE, String.valueOf(o), t);
        }

        @Override
        public void fatal(final Object o) {
            error(o);
        }

        @Override
        public void fatal(final Object o, final Throwable t) {
            error(o, t);
        }
    }
}
