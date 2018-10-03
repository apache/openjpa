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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.PluginListValue;

/**
 * Specialized instrumentation providers can extend this class to get basic
 * provider state and capabilities.  It implements Configurable so it can
 * be used within the configuration framework to participate in automatic
 * configuration.
 */
public abstract class AbstractInstrumentationProvider implements InstrumentationProvider, Configurable {

    private Map<String, Instrument> _instruments = new ConcurrentHashMap<>();

    private boolean _started = false;
    private PluginListValue _instrumentValues;
    private String _options;
    private Configuration _config;

    @Override
    public void setConfiguration(Configuration conf) {
        _config = conf;
    }

    @Override
    public Configuration getConfiguration() {
        return _config;
    }

    @Override
    public void startConfiguration() {
    }

    @Override
    public void endConfiguration() {
    }

    @Override
    public void setInstrument(String instrument) {
        _instrumentValues = new PluginListValue("Instrument");
        if (getInstrumentAliases() != null) {
            _instrumentValues.setAliases(getInstrumentAliases());
        }
        _instrumentValues.setString(instrument);

        Instrument[] instruments = (Instrument[])_instrumentValues.instantiate(Instrument.class, _config);
        for (Instrument inst : instruments) {
            inst.setProvider(this);
            _instruments.put(inst.getName(), inst);
        }
    }

    @Override
    public String getInstrument() {
        return _instrumentValues.getString();
    }

    @Override
    public void setOptions(String options) {
        _options = options;
    }

    @Override
    public String getOptions() {
        return _options;
    }

    @Override
    public void addInstrument(Instrument instrument) {
        if (instrument == null) {
            return;
        }
        instrument.setProvider(this);
        _instruments.put(instrument.getName(), instrument);
    }

    @Override
    public void initializeInstrument(Instrument instrument, Object context) {
        initializeInstrument(instrument, _options, context);
    }

    @Override
    public void initializeInstrument(Instrument instrument, String options, Object context) {
        instrument.setProvider(this);
        instrument.setOptions(options);
        instrument.setContext(context);
        instrument.initialize();
    }

    @Override
    public Instrument getInstrumentByName(String name) {
        return _instruments.get(name);
    }

    @Override
    public Set<Instrument> getInstruments() {
        return new HashSet<>(_instruments.values());
    }

    @Override
    public void stopInstruments(InstrumentationLevel level, Object context) {
        try {
            Set<Instrument> instruments = getInstruments();
            for (Instrument instrument : instruments) {
                if (instrument.getLevel() == level &&
                    contextEquals(instrument.getContext(),context)) {
                    stopInstrument(instrument);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startInstruments(InstrumentationLevel level, Object context) {
        Set<Instrument> instruments = getInstruments();
        for (Instrument instrument : instruments) {
            if (instrument.getLevel() == level) {
                initializeInstrument(instrument, context);
                startInstrument(instrument);
            }
        }
    }

    @Override
    public void stopInstrument(Instrument instrument) {
        stopInstrument(instrument, true);
    }

    @Override
    public void removeInstrumentByName(String name) {
        Instrument ins = _instruments.remove(name);
        if (ins != null) {
            ins.stop();
        }
    }

    @Override
    public boolean isStarted() {
        return _started;
    }

    protected void setStarted(boolean started) {
        _started = started;
    }

    @Override
    public String[] getInstrumentAliases() {
        return null;
    }

    @Override
    public abstract void start();

    @Override
    public abstract void stop();

    private static boolean contextEquals(Object ctx1, Object ctx2) {
        if (ctx1 == ctx2) {
            return true;
        }
        if (ctx1 == null) {
            return false;
        }
        return ctx1.equals(ctx2);
    }

}
