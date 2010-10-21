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
package org.apache.openjpa.instrumentation.jconsole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.openjpa.instrumentation.jmx.DataCacheJMXInstrumentMBean;

import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsolePlugin;

/**
 * DataCachePlugin
 */
public class DataCachePlugin extends JConsolePlugin {
    private Map<String, JPanel> _tabs;
    private Map<DataCacheJMXInstrumentMBean, DataCachePanel> _mbeanPanelMap;

    @Override
    public Map<String, JPanel> getTabs() {
        if (_tabs == null) {
            _tabs = buildPanels(getMbeans());
        }
        return _tabs;
    }

    @Override
    public SwingWorker<Map<DataCachePanel, DataCacheStatistic>, Object> newSwingWorker() {
        return new DataCacheWorker(_mbeanPanelMap);
    }

    /**
     * Private worker method that instantiates the CacheName -> DataCachePanel mapping.
     */
    private Map<String, JPanel> buildPanels(Map<String, DataCacheJMXInstrumentMBean> mbeans) {
        _mbeanPanelMap = new HashMap<DataCacheJMXInstrumentMBean, DataCachePanel>();
        Map<String, JPanel> res = new HashMap<String, JPanel>();
        for (Map.Entry<String, DataCacheJMXInstrumentMBean> entry : mbeans.entrySet()) {
            DataCacheJMXInstrumentMBean m = entry.getValue();
            String cfgId = entry.getKey();
            String name = m.getCacheName();

            // TODO -- should NLSize this tab.
            DataCachePanel panel = new DataCachePanel(m);
            String key = "DataCache-" + cfgId + "-" + name;
            // This 'shouldn't' ever happen... but it will if we have name collisions for one reason or another.
            while (res.containsKey(key) == true) {
                key = key + "_dup";
            }
            res.put(key, panel);
            _mbeanPanelMap.put(m, panel);
        }
        return res;
    }

    /**
     * Private worker method that returns all MBeans matching the query "org.apache.openjpa:type=DataCache,*"
     */
    private Map<String, DataCacheJMXInstrumentMBean> getMbeans() {
        Map<String, DataCacheJMXInstrumentMBean> mbeans = new HashMap<String, DataCacheJMXInstrumentMBean>();

        JConsoleContext ctx = getContext();
        Set<MBeanServerConnection> connections = new HashSet<MBeanServerConnection>();
        connections.add(ctx.getMBeanServerConnection());
        connections.addAll(MBeanServerFactory.findMBeanServer(null));
        if (connections == null || connections.size() == 0) {
            System.err
                .println("DataCachePlugin found zero from MBeanServerFactory.findMBeanServer(null) using default");
        }

        for (MBeanServerConnection server : connections) {
            try {
                ObjectName generic = new ObjectName("org.apache.openjpa:type=DataCache,*");
                ObjectName[] objects = server.queryNames(generic, null).toArray(new ObjectName[0]);
                if (objects == null || objects.length == 0) {
                    System.err
                        .println("No ObjectNames found matching 'org.apache.openjpa:type=DataCache,*' for MBeanServer "
                            + server);
                }
                for (ObjectName o : objects) {
                    DataCacheJMXInstrumentMBean bean = JMX.newMBeanProxy(server, o, DataCacheJMXInstrumentMBean.class);
                    String cfgId = o.getKeyProperty("cfgid");
                    // Handle config id collision
                    int i = 1;
                    String tempCfgId = cfgId;
                    while (mbeans.containsKey(tempCfgId)) {
                        tempCfgId = cfgId + "-" + i;
                        i++;
                    }
                    mbeans.put(tempCfgId, bean);
                }
            } catch (Exception e) {
                // Shouldn't happen. Allow to bubble up as runtime exception
                throw new RuntimeException(e);
            }
        }
        return mbeans;
    }
}
