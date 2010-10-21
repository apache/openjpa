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
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingWorker;

import org.apache.openjpa.datacache.CacheStatistics;
import org.apache.openjpa.instrumentation.jmx.DataCacheJMXInstrumentMBean;

/**
 * This worker class is driven by the JConsole. On a specified interval(default 4 sec, or -interval=n) this worker will
 * be created and doInBackground() will be called. This method is responsible for performing any long running tasks that
 * will collect data to be displayed by the UI.
 * 
 * It keeps a mapping of MBean -> JConsole panel. On each run it needs to collect data which corresponds to each known
 * panel.
 */
public class DataCacheWorker extends SwingWorker<Map<DataCachePanel, DataCacheStatistic>, Object> {
    Map<DataCacheJMXInstrumentMBean, DataCachePanel> _map;

    DataCacheWorker(Map<DataCacheJMXInstrumentMBean, DataCachePanel> map) {
        _map = map;
    }

    @Override
    public Map<DataCachePanel, DataCacheStatistic> doInBackground() {
        Map<DataCachePanel, DataCacheStatistic> res = new HashMap<DataCachePanel, DataCacheStatistic>();
        // Loop over each known panel and build DataCacheStatistic that will be consumed by the UI.
        for (Entry<DataCacheJMXInstrumentMBean, DataCachePanel> entry : _map.entrySet()) {
            DataCacheJMXInstrumentMBean mbean = entry.getKey();
            DataCachePanel panel = entry.getValue();
            // Make MBean calls
            CacheStatistics stats = mbean.getCacheStatistics();
            Map<String, Boolean> types = mbean.listKnownTypes();

            DataCacheStatistic stat = new DataCacheStatistic(stats, types);

            res.put(panel, stat);
        }

        return res;
    }

    /**
     * This method is called by JConsole. It needs to pass data which was obtained in the background on to the UI for
     * updating.
     * 
     * Note : The method get() returns the values that were returned by the doInBackGround() method.
     */
    @Override
    protected void done() {
        try {
            Map<DataCachePanel, DataCacheStatistic> res = get();
            for (Entry<DataCachePanel, DataCacheStatistic> entry : res.entrySet()) {
                DataCachePanel panel = entry.getKey();
                DataCacheStatistic stat = entry.getValue();

                // Give the types panel a chance to update known types.
                panel.updateTypesCached(stat);

                // Update statistics
                DataCacheTable model = panel.getModel();
                model.setDataCacheStatistics(stat);
                model.fireTableDataChanged();
            }
        } catch (Exception e) {
            // Unexpected
            e.printStackTrace();
        }
    }
}
