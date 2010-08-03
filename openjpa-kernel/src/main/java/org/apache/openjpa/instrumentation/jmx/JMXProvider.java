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
package org.apache.openjpa.instrumentation.jmx;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.openjpa.lib.instrumentation.AbstractInstrumentationProvider;
import org.apache.openjpa.lib.instrumentation.Instrument;
import org.apache.openjpa.util.UserException;

/**
 * A simple MBean JMX instrumentation provider
 */
public class JMXProvider
    extends AbstractInstrumentationProvider {
    
    // Aliases for built-in JMX Instrumentation
    public static final String[] JMX_INSTRUMENT_ALIASES = {
        "DataCache", "org.apache.openjpa.instrumentation.jmx.DataCacheJMXInstrument",
        "QueryCache", "org.apache.openjpa.insrumentation.jmx.QueryCacheJMXInstrument",
        "QuerySQLCache", "org.apache.openjpa.insrumentation.jmx.PreparedQueryCacheJMXInstrument"
    };
    
    /**
     * The standard mbean package for OpenJPA
     */
    public static final String MBEAN_PACKAGE = "org.apache.openjpa";
    
    private MBeanServer _mbs = null;

    /**
     * Register an MBean with the mbean server.
     * @param mBean
     */
    protected void registerMBean(JMXInstrument mBean) {
        MBeanServer mbs = getMBeanServer(); 
        try {
            mbs.registerMBean(mBean, mBean.getObjectName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the mbean server
     * @return
     */
    public MBeanServer getMBeanServer() {
        if (_mbs == null) {
            _mbs = ManagementFactory.getPlatformMBeanServer();
        }
        return _mbs;
    }

    @Override
    public void start() {
       try {
           MBeanServer mbs = getMBeanServer();
           if (mbs == null) {
               throw new UserException("jmx-server-failed-creation");
           }
       } catch (Throwable t) {
           throw new UserException("jmx-server-unavailable",t);
       }
    }

    @Override
    public void stop() {
        // no-op with the built in MBean server
    }

    /**
     * Creates an object name for the supplied instrument and key properties
     * @param instrument the instrument
     * @param props additional key properties
     * @return the JMX object name
     * @throws Exception a generic JMX-type exception
     */
    public static ObjectName createObjectName(JMXInstrument instrument, Map<String,String> props) 
        throws Exception {
        // Construct the base name
        StringBuilder sbName = new StringBuilder(MBEAN_PACKAGE);
        sbName.append("type=");
        sbName.append(instrument.getName());
        sbName.append(",cfgid=");
        sbName.append(instrument.getConfigId());
        sbName.append(",cfgref=");
        sbName.append(instrument.getContextRef());
        // Add any additional key properties that were provided
        if (props != null && !props.isEmpty()) {
            for (Entry<String,String> prop : props.entrySet()) {
               sbName.append(",");
               sbName.append(prop.getKey());
               sbName.append("=");
               sbName.append(prop.getValue());
            }
        }
        return new ObjectName(sbName.toString());
    }

    /**
     * Start an instrument.  Registers an mbean with the server.
     */
    public void startInstrument(Instrument instrument) {
        if (!instrument.isStarted()) {
            registerMBean((JMXInstrument)instrument);
        }
    }

    /**
     * Stop an instrument.  Unregisters an mbean with the server.
     */
    public void stopInstrument(Instrument instrument, boolean force) {
        if (instrument.isStarted() || force) {
            try {
                getMBeanServer().unregisterMBean(((JMXInstrument)instrument).getObjectName());
            } catch (Exception e) {
                // If force, swallow the exception since the bean may not even
                // be registered.
                if (!force) {
                    throw new UserException("cannot-stop-instrument",e);
                }
            }
        }
    }
    
    /**
     * Returns aliases for built-in instruments.
     */
    @Override
    public String[] getInstrumentAliases() {
        return JMX_INSTRUMENT_ALIASES;
    }
}
