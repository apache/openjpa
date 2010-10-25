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

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.openjpa.lib.util.Localizer;

/**
 * DataCacheTable
 */
public class DataCacheTable extends AbstractTableModel {
    private static final long serialVersionUID = -117710809875227870L;
    private static final Localizer _loc = Localizer.forPackage(DataCacheTable.class);
    // Column names -- key, reads, hits, writes
    private static final String[] _cols =
        { _loc.get("datacachetable.column.key").getMessage(), _loc.get("datacachetable.column.reads").getMessage(),
            _loc.get("datacachetable.column.hits").getMessage(), _loc.get("datacachetable.column.writes").getMessage(), };

    // row, col
    private Object[][] _tableData;

    public int getColumnCount() {
        return _cols.length;
    }

    public int getRowCount() {
        return (_tableData == null) ? 0 : _tableData.length;
    }

    public Object getValueAt(int row, int col) {
        return _tableData[row][col];
    }

    public String getColumnName(int col) {
        return _cols[col];
    }

    public void setDataCacheStatistics(DataCacheStatistic s) {
        // extract data from statistics into table format
        _tableData = new Object[s.getNumTypes()][_cols.length];
        List<String> types = s.getAllTypes();
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            // { "key", "reads", "hits", "writes" }
            _tableData[i][0] = type;
            _tableData[i][1] = s.getReads(type);
            _tableData[i][2] = s.getHits(type);
            _tableData[i][3] = s.getWrites(type);
        }
    }

}
