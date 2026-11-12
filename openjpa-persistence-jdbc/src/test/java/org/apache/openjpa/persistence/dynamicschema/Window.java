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
package org.apache.openjpa.persistence.dynamicschema;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity whose name and attribute names are words Derby rejects as
 * unquoted identifiers (OPENJPA-3075). Its default table name is also a word
 * that MySQL 8.0 and later reserve (OPENJPA-3074). The table and the
 * collection table are named by the JPA mapping defaults.
 */
@Entity
public class Window {

    @Id
    private int id;

    private String cursors;

    private String window;

    private String definer;

    private String invoker;

    private String xmlquery;

    private boolean deterministic;

    private String label;

    @ElementCollection
    private List<String> panes = new ArrayList<>();

    public Window() {
    }

    public Window(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCursors() {
        return cursors;
    }

    public void setCursors(String cursors) {
        this.cursors = cursors;
    }

    public String getWindow() {
        return window;
    }

    public void setWindow(String window) {
        this.window = window;
    }

    public String getDefiner() {
        return definer;
    }

    public void setDefiner(String definer) {
        this.definer = definer;
    }

    public String getInvoker() {
        return invoker;
    }

    public void setInvoker(String invoker) {
        this.invoker = invoker;
    }

    public String getXmlquery() {
        return xmlquery;
    }

    public void setXmlquery(String xmlquery) {
        this.xmlquery = xmlquery;
    }

    public boolean isDeterministic() {
        return deterministic;
    }

    public void setDeterministic(boolean deterministic) {
        this.deterministic = deterministic;
    }

    public List<String> getPanes() {
        return panes;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
