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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.apache.openjpa.instrumentation.jmx.DataCacheJMXInstrumentMBean;

/**
 * <pre>
 * 
 * DataCachePanel Layout:
 *  -> parentTopPanel:JPanel
 *      -> actionsPanel:JPanel
 *          -> enable checkbox
 *          -> clear cache button
 *          -> reset stats button
 *      -> types panel
 *          -> [N] types
 *  -> statisticsPanel:JScrollPane
 *  
 * </pre>
 */
public class DataCachePanel extends JPanel {
    private static final long serialVersionUID = 8273595264174478456L;
    private DataCacheJMXInstrumentMBean _mbean;
    private DataCacheTable _model;
    private Map<String, JCheckBox> _typesPanelMap = new ConcurrentHashMap<String, JCheckBox>();
    private JPanel _typesPanel;

    public DataCachePanel(DataCacheJMXInstrumentMBean mbean) {
        super(new BorderLayout());
        _model = new DataCacheTable();
        _mbean = mbean;

        // setup parent panel
        JPanel parentTopPanel = new JPanel(new GridLayout(2, 0));
        parentTopPanel.setBorder(new TitledBorder(LineBorder.createGrayLineBorder(), ""));
        // Add parentTopPanel to DataCachePanel
        add(parentTopPanel, BorderLayout.PAGE_START);

        // Panel for action buttons
        JPanel actionsPanel = new JPanel(new GridLayout(1, 3));
        actionsPanel.setBorder(new TitledBorder(LineBorder.createGrayLineBorder(), "Actions"));
        parentTopPanel.add(actionsPanel, -1);

        // Create new panel for [N] children checkboxes
        // Don't add anything here yet. This will happen dynamically in updateTypesCached
        _typesPanel = new JPanel(new GridLayout());
        _typesPanel.setBorder(new TitledBorder(LineBorder.createGrayLineBorder(), "Currently known types"));
        parentTopPanel.add(_typesPanel, -1);

        // create enabled check box to parent
        JCheckBox enableStatisticsCheckBox = new JCheckBox("Statistics enabled", mbean.getStatisticsEnabled());
        enableStatisticsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean enable = (e.getStateChange() == ItemEvent.SELECTED);
                _mbean.collectStatistics(enable);
            }
        });

        // create clear cache button
        JButton clear = new JButton("Clear cache");
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _mbean.clear();
            }
        });

        // create clear cache button
        JButton reset = new JButton("Reset statistics");
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _mbean.reset();
            }
        });

        actionsPanel.add(enableStatisticsCheckBox, -1);
        actionsPanel.add(clear, -1);
        actionsPanel.add(reset, -1);

        // setup stats table
        JTable table = new JTable(_model);
        table.setPreferredScrollableViewportSize(new Dimension(500, 300));
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);
        add(new JScrollPane(table), BorderLayout.PAGE_END);
    }

    DataCacheTable getModel() {
        return _model;
    }

    /**
     * This method is responsible for taking the provided DataCacheStatistic and adding any a check box for any types
     * that we don't already know about.
     */
    void updateTypesCached(DataCacheStatistic stat) {
        List<String> masterList = stat.getAllTypes();
        List<String> enabled = stat.getEnabledTypes();

        for (String s : masterList) {
            JCheckBox box = _typesPanelMap.get(s);
            if (box == null) {
                // We don't know about this type, make a new check box.
                box = new JCheckBox(s, enabled.contains(s));
                box.setName(s);
                box.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        // When this item is selected / deselected, spin up a new thread to make MBean call
                        final ItemSelectable item = e.getItemSelectable();
                        if (item instanceof JCheckBox) {
                            try {
                                new Thread() {
                                    public void run() {
                                        JCheckBox jcb = (JCheckBox) item;
                                        _mbean.cache(jcb.getName(), jcb.isSelected());
                                    };
                                }.start();
                            } catch (Exception ex) {
                                // Unexpected
                                ex.printStackTrace();
                            }

                        }
                    }
                });

                _typesPanel.add(box);
                _typesPanelMap.put(s, box);
            } else {
                box.setSelected(enabled.contains(s));
            }
        }
    }
}
