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
package org.apache.openjpa.lib.instrumentation;

/**
 * Provides a base for creating instruments.  Specialized instruments can
 * extend this class to get base instrument capabilities and then add their
 * own specialized functionality.
 */
public abstract class AbstractInstrument implements Instrument {

    private boolean _started = false;
    private InstrumentationProvider _provider;
    private Object _context;
    private String _options;

    @Override
    public Object getContext() {
        return _context;
    }

    @Override
    public void setContext(Object context) {
        _context = context;
    }

    @Override
    public String getOptions() {
        return _options;
    }

    @Override
    public void setOptions(String options) {
        _options = options;
    }

    @Override
    public boolean isStarted() {
        return _started;
    }

    @Override
    public void setStarted(boolean started) {
        _started = started;
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public void setProvider(InstrumentationProvider provider) {
        _provider = provider;
    }

    @Override
    public InstrumentationProvider getProvider() {
        return _provider;
    }

    @Override
    public InstrumentationLevel getLevel() {
        return InstrumentationLevel.MANUAL;
    }

    @Override
    public abstract String getName();

    @Override
    public abstract void initialize();
}
