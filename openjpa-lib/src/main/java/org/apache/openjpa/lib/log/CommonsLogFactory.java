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

/**
 * {@link LogFactory} implementation that delegates to the commons logging
 * framework.
 *
 * @author Patrick Linskey
 */
public class CommonsLogFactory extends LogFactoryAdapter {

    private org.apache.commons.logging.LogFactory _factory;

    public CommonsLogFactory() {
        _factory = org.apache.commons.logging.LogFactory.getFactory();
    }

    @Override
    protected org.apache.openjpa.lib.log.Log newLogAdapter(String channel) {
        return new LogAdapter(_factory.getInstance(channel));
    }

    /**
     * Adapts a commons logging log to the
     * {@link org.apache.openjpa.lib.log.Log} interface.
     */
    public static class LogAdapter implements org.apache.openjpa.lib.log.Log {

        private org.apache.commons.logging.Log _log;

        private LogAdapter(org.apache.commons.logging.Log wrapee) {
            _log = wrapee;
        }

        public org.apache.commons.logging.Log getDelegate() {
            return _log;
        }

        @Override
        public boolean isErrorEnabled() {
            return _log.isErrorEnabled();
        }

        @Override
        public boolean isFatalEnabled() {
            return _log.isFatalEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return _log.isInfoEnabled();
        }

        @Override
        public boolean isTraceEnabled() {
            return _log.isTraceEnabled();
        }

        @Override
        public boolean isWarnEnabled() {
            return _log.isWarnEnabled();
        }

        @Override
        public void trace(Object o) {
            _log.trace(o);
        }

        @Override
        public void trace(Object o, Throwable t) {
            _log.trace(o, t);
        }

        @Override
        public void info(Object o) {
            _log.info(o);
        }

        @Override
        public void info(Object o, Throwable t) {
            _log.info(o, t);
        }

        @Override
        public void warn(Object o) {
            _log.warn(o);
        }

        @Override
        public void warn(Object o, Throwable t) {
            _log.warn(o, t);
        }

        @Override
        public void error(Object o) {
            _log.error(o);
        }

        @Override
        public void error(Object o, Throwable t) {
            _log.error(o, t);
        }

        @Override
        public void fatal(Object o) {
            _log.fatal(o);
        }

        @Override
        public void fatal(Object o, Throwable t) {
            _log.fatal(o, t);
        }
    }
}
