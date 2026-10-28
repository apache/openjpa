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
package org.apache.openjpa.persistence.lifecycle.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner of relations mapped with orphan-removal and a cascaded remove in
 * orphan-orm.xml.
 */
public class OrphanXmlParent {

    private int id;

    private List<OrphanXmlChild> children = new ArrayList<>();

    private List<OrphanXmlItem> items = new ArrayList<>();

    private OrphanXmlDetail detail;

    private OrphanXmlDetail extra;

    public OrphanXmlParent() {
    }

    public OrphanXmlParent(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public List<OrphanXmlChild> getChildren() {
        return children;
    }

    public List<OrphanXmlItem> getItems() {
        return items;
    }

    public OrphanXmlDetail getDetail() {
        return detail;
    }

    public void setDetail(OrphanXmlDetail detail) {
        this.detail = detail;
    }

    public OrphanXmlDetail getExtra() {
        return extra;
    }

    public void setExtra(OrphanXmlDetail extra) {
        this.extra = extra;
    }
}
